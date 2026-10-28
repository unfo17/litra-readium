package io.github.unfo17.litra

import io.github.unfo17.litra.data.UserPreferences
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTapPagingTest {
    @Test fun edgeTapPagingIsEnabledByDefaultAndCanBeDisabled() {
        assertTrue(UserPreferences().tapPaging)
        assertFalse(UserPreferences(tapPaging = false).tapPaging)
    }
}
