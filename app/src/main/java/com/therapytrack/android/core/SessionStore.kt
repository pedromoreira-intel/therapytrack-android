package com.therapytrack.android.core

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

/** The signed-in account's credentials and identity. */
data class Session(
    val accessToken: String?,
    val refreshToken: String?,
    val userId: Int?,
    val role: String?
) {
    val isSignedIn: Boolean get() = accessToken != null && userId != null
    companion object { val none = Session(null, null, null, null) }
}

/**
 * Where credentials live: an encrypted preferences file whose key sits in the
 * Android Keystore. Plain SharedPreferences are readable from a backup or a
 * rooted device; the iOS app keeps these in the Keychain for the same reason.
 *
 * Every write goes through this one object, and the API client reads a
 * snapshot at a time — that is what lets the account-binding checks reason
 * about "the session as it was when this request was built".
 */
interface SessionStore {
    fun current(): Session
    fun replace(session: Session)
    fun clear()
}

class EncryptedSessionStore(context: Context) : SessionStore {
    private val prefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    @Synchronized override fun current(): Session = Session(
        accessToken = prefs.getString("access", null),
        refreshToken = prefs.getString("refresh", null),
        userId = if (prefs.contains("userId")) prefs.getInt("userId", 0) else null,
        role = prefs.getString("role", null)
    )

    @Synchronized override fun replace(session: Session) {
        prefs.edit()
            .putString("access", session.accessToken)
            .putString("refresh", session.refreshToken)
            .apply { session.userId?.let { putInt("userId", it) } ?: remove("userId") }
            .putString("role", session.role)
            .commit()
    }

    @Synchronized override fun clear() { prefs.edit().clear().commit() }
}

/** For tests: the same contract, in memory. */
class InMemorySessionStore(private var session: Session = Session.none) : SessionStore {
    @Synchronized override fun current() = session
    @Synchronized override fun replace(session: Session) { this.session = session }
    @Synchronized override fun clear() { session = Session.none }
}
