package com.therapytrack.android

import com.therapytrack.android.core.SessionRefreshGuard
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SessionRefreshGuardTest {
    @Test fun `a refresh applies only to the session that started it`() {
        assertTrue(SessionRefreshGuard.shouldApply("r1", "r1"))
        assertFalse("another account signed in meanwhile", SessionRefreshGuard.shouldApply("r1", "r2"))
        assertFalse("signed out meanwhile", SessionRefreshGuard.shouldApply("r1", null))
    }
}
