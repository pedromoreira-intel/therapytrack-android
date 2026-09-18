package com.therapytrack.android.core

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * The one place the app talks to the server.
 *
 * Hand-rolled on OkHttp rather than Retrofit because the rules that matter are
 * about *when a token is read*, and an interceptor cannot express them:
 *
 *  - Every request is bound to an account — the one the caller named, or the
 *    one signed in when it began — and refused if a different account is signed
 *    in by the time the token is attached. Checked again on the retry after a
 *    refresh, because the refresh is a suspension point.
 *  - A refresh's result is stored only if the refresh token it sent is still the
 *    one on the device (`SessionRefreshGuard`). Otherwise account A's late
 *    refresh would replace account B's login.
 *  - An in-flight refresh is shared only within one session, so B's request
 *    never waits on — or is failed by — A's refresh.
 *
 * The check-then-read pairs sit under one mutex, and every credential write
 * goes through the same mutex, so nothing can change between them.
 */
class ApiClient(
    private val baseUrl: String,
    private val sessions: SessionStore,
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()
) {
    val json = Json { ignoreUnknownKeys = true; explicitNulls = false; encodeDefaults = false }

    /** Serialises credential reads-and-writes that must not interleave. */
    private val sessionLock = Mutex()

    /** The refresh in flight, and the refresh token it was started with. */
    private var refreshInFlight: CompletableDeferred<Boolean>? = null
    private var refreshFor: String? = null

    /** The session as the UI sees it; every credential write publishes here. */
    private val _session = kotlinx.coroutines.flow.MutableStateFlow(sessions.current())
    val session: kotlinx.coroutines.flow.StateFlow<Session> get() = _session

    private fun publish() { _session.value = sessions.current() }

    val currentUserId: Int? get() = sessions.current().userId
    val currentRole: String? get() = sessions.current().role
    val isSignedIn: Boolean get() = sessions.current().isSignedIn

    class Response(val status: Int, val body: String) {
        fun obj(json: Json): JsonObject? = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
    }

    /**
     * @param asUser the account this request belongs to; null binds it to whoever
     *   is signed in when it begins.
     */
    suspend fun request(
        method: String,
        path: String,
        body: JsonObject? = null,
        asUser: Int? = null,
        allowRefresh: Boolean = true
    ): Response = withContext(Dispatchers.IO) {
        val boundTo = asUser ?: sessions.current().userId

        // Bind, then read the token, without letting a sign-in or sign-out
        // between the two.
        val token = sessionLock.withLock {
            val now = sessions.current()
            if (boundTo != null && now.userId != boundTo) throw ApiError.AccountChanged
            now.accessToken
        }

        val builder = Request.Builder().url("$baseUrl$path").header("Content-Type", "application/json")
        token?.let { builder.header("Authorization", "Bearer $it") }
        val payload = body?.let { json.encodeToString(JsonObject.serializer(), it) }
        when (method) {
            "GET" -> builder.get()
            "DELETE" -> builder.delete()
            else -> builder.method(method, (payload ?: "{}").toRequestBody(JSON_TYPE))
        }

        val response = try {
            http.newCall(builder.build()).execute()
        } catch (e: IOException) {
            android.util.Log.w("Api", "$method $path: ${e.javaClass.simpleName}: ${e.message}")
            throw ApiError.Network(e)
        }
        val text = response.body?.string() ?: ""

        if (response.code == 401) {
            if (allowRefresh && sessions.current().refreshToken != null && refreshSession()) {
                // Bound to the same account: if the refresh completed into a
                // different session, this throws rather than sending.
                return@withContext request(method, path, body, asUser = boundTo, allowRefresh = false)
            }
            // A refresh that failed for a session since replaced says nothing
            // about the session that replaced it.
            sessionLock.withLock {
                if (sessions.current().userId == boundTo) { sessions.replace(sessions.current().copy(accessToken = null)); publish() }
            }
            throw ApiError.Unauthorized
        }

        if (response.code == 402) {
            throw ApiError.NotInPlan(errorMessage(text) ?: "Não incluído no seu plano")
        }
        if (response.code >= 400) {
            throw ApiError.Server(response.code, errorMessage(text) ?: when (response.code) {
                404 -> "Essa funcionalidade ainda não está disponível no servidor."
                408, 504 -> "O servidor demorou demasiado a responder. Tente novamente."
                in 500..599 -> "O servidor teve um problema. Tente novamente daqui a pouco."
                else -> "Algo correu mal. Tente novamente."
            })
        }
        Response(response.code, text)
    }

    private fun errorMessage(text: String): String? =
        runCatching { json.parseToJsonElement(text).jsonObject["error"]?.jsonPrimitive?.content }.getOrNull()

    /**
     * Refresh the access token. True if the session now holds a fresh one.
     *
     * Shares an in-flight refresh only with callers on the same session, and
     * applies the result only if that session is still the current one.
     */
    private suspend fun refreshSession(): Boolean {
        val sent = sessions.current().refreshToken ?: return false
        val mine = CompletableDeferred<Boolean>()
        val shared: CompletableDeferred<Boolean> = sessionLock.withLock {
            if (sessions.current().refreshToken != sent) return false   // replaced under us
            val existing = refreshInFlight
            if (existing != null && refreshFor == sent) existing
            else { refreshInFlight = mine; refreshFor = sent; mine }
        }
        if (shared !== mine) return shared.await()

        val outcome = try {
            performRefresh(sent)
        } finally {
            sessionLock.withLock {
                if (refreshFor == sent) { refreshInFlight = null; refreshFor = null }
            }
        }
        mine.complete(outcome)
        return outcome
    }

    private suspend fun performRefresh(sent: String): Boolean = withContext(Dispatchers.IO) {
        val body = """{"refresh_token":${json.encodeToString(kotlinx.serialization.serializer<String>(), sent)}}"""
        val req = Request.Builder().url("$baseUrl/auth/refresh")
            .header("Content-Type", "application/json").post(body.toRequestBody(JSON_TYPE)).build()
        val response = try { http.newCall(req).execute() } catch (e: IOException) {
            return@withContext false   // network, not rejection: keep the refresh token
        }
        val text = response.body?.string() ?: ""

        if (response.code >= 400) {
            // Rejected: that session is over — if it is still the one here.
            sessionLock.withLock {
                if (SessionRefreshGuard.shouldApply(sent, sessions.current().refreshToken)) { sessions.clear(); publish() }
            }
            return@withContext false
        }
        val obj = runCatching { json.parseToJsonElement(text).jsonObject }.getOrNull() ?: return@withContext false
        val refreshed = Session(
            accessToken = obj["token"]?.jsonPrimitive?.content,
            refreshToken = obj["refresh_token"]?.jsonPrimitive?.content,
            userId = obj["user"]?.jsonObject?.get("id")?.jsonPrimitive?.content?.toIntOrNull(),
            role = obj["user"]?.jsonObject?.get("role")?.jsonPrimitive?.content
        )
        sessionLock.withLock {
            // Only if the refresh token we sent is still the one stored.
            if (!SessionRefreshGuard.shouldApply(sent, sessions.current().refreshToken)) return@withLock false
            sessions.replace(refreshed)
            publish()
            true
        }
    }

    /** Called by sign-in flows: replaces the whole session under the lock. */
    suspend fun storeSession(session: Session) = sessionLock.withLock { sessions.replace(session); publish() }

    suspend fun signOut() {
        val refresh = sessions.current().refreshToken
        if (refresh != null) {
            // Tell the server so the refresh token is revoked; a failure here must
            // not block local sign-out.
            runCatching {
                val body = """{"refresh_token":${json.encodeToString(kotlinx.serialization.serializer<String>(), refresh)}}"""
                withContext(Dispatchers.IO) {
                    http.newCall(Request.Builder().url("$baseUrl/auth/logout").post(body.toRequestBody(JSON_TYPE)).build()).execute().close()
                }
            }
        }
        sessionLock.withLock { sessions.clear(); publish() }
    }

    companion object {
        private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
