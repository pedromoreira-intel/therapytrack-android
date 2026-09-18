package com.therapytrack.android.core

/**
 * Whether the outcome of a token refresh may be applied.
 *
 * A refresh is a network round trip, and the signed-in account can change while
 * it is in flight: sign out, sign in as someone else. The response belongs to the
 * session that *sent* the refresh token, so it may be stored — or, if rejected,
 * may end the session — only if that token is still the one on the device.
 * Otherwise the refresh is about a session that no longer exists here.
 *
 * The same rule as the iOS app's SessionRefreshGuard, kept as a pure function
 * so it is tested on the JVM without the client around it.
 */
object SessionRefreshGuard {
    fun shouldApply(refreshTokenSent: String, refreshTokenNow: String?): Boolean =
        refreshTokenNow == refreshTokenSent
}
