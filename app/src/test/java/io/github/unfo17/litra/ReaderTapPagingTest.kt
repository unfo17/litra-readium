package io.github.unfo17.litra

import io.github.unfo17.litra.data.UserPreferences
import io.github.unfo17.litra.ui.adjustReaderBrightness
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReaderTapPagingTest {
    @Test fun edgeTapPagingIsEnabledByDefaultAndCanBeDisabled() {
        assertTrue(UserPreferences().tapPaging)
        assertFalse(UserPreferences(tapPaging = false).tapPaging)
    }

    @Test fun leftBrightnessGestureDefaultsOnAndBrightnessStaysWithinFiveToOneHundredPercent() {
        assertTrue(UserPreferences().leftBrightnessGesture)
        assertFalse(UserPreferences(leftBrightnessGesture = false).leftBrightnessGesture)
        assertEquals(.5475f, adjustReaderBrightness(.5f, -50f, 1000f), .0001f)
        assertEquals(1f, adjustReaderBrightness(.99f, -100f, 1000f), .0001f)
        assertEquals(.05f, adjustReaderBrightness(.06f, 100f, 1000f), .0001f)
        assertEquals(.5f, adjustReaderBrightness(.5f, 100f, 0f), .0001f)
    }
}
