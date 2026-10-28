@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)
package io.github.unfo17.litra

import android.app.Application
import android.content.Context
import android.net.Uri
import android.graphics.Bitmap
import android.os.ParcelFileDescriptor
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import io.github.unfo17.litra.ui.ReaderTapZones
import io.github.unfo17.litra.ui.ReaderBrightnessHud
import io.github.unfo17.litra.ui.LitraTheme
import io.github.unfo17.litra.data.PreferencesRepository
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import org.readium.navigator.web.reflowable.*
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.util.getOrElse
import java.io.File

class ReaderTapZonesTest {
    @get:Rule val compose = createComposeRule()

    @Test fun realEpubEdgeZonesTurnPagesRespectBoundariesAndKeepCenterTap() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val settings = PreferencesRepository(context)
        val originalBrightness = runBlocking { settings.preferences.first().brightness }
        val app = context.applicationContext as Application
        val db = androidx.room.Room.inMemoryDatabaseBuilder(context, io.github.unfo17.litra.data.LitraDatabase::class.java).build()
        val repository = io.github.unfo17.litra.data.LibraryRepository(context, db, io.github.unfo17.litra.data.ReadiumService(context))
        val fixture = ImportPersistenceTest.fixture(context, 75)
        val book = runBlocking { repository.import(Uri.fromFile(fixture)) }
        val publication = runBlocking { repository.readium.open(repository.file(book.fileName)) }
        val factory = ReflowableWebRenditionFactory(app, publication)!!
        val rendition = runBlocking { factory.createRenditionState().getOrElse { error(it.message) } }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        var paging by mutableStateOf(true)
        var brightnessGesture by mutableStateOf(true)
        var brightness by mutableFloatStateOf(.5f)
        var brightnessHud by mutableStateOf<Float?>(null)
        var brightnessEndCount by mutableIntStateOf(0)
        var previousTapCount by mutableIntStateOf(0)
        var chromeVisible by mutableStateOf(true)
        try {
            compose.setContent {
                LitraTheme {
                    Box(Modifier.fillMaxSize()) {
                        ReflowableWebRendition(rendition, Modifier.fillMaxSize())
                        ReaderTapZones(
                            tapPagingEnabled = paging,
                            brightnessGestureEnabled = brightnessGesture,
                            brightness = brightness,
                            onBrightnessGestureStart = { brightnessHud = brightness },
                            onBrightnessChange = { brightness = it; brightnessHud = it },
                            onBrightnessGestureEnd = { brightness = it; brightnessHud = it; brightnessEndCount++; scope.launch { settings.number("brightness", it.toDouble()) } },
                            onPreviousPage = { previousTapCount++; scope.launch { rendition.controller?.moveBackward() } },
                            onNextPage = { scope.launch { rendition.controller?.moveForward() } },
                            onToggleChrome = { chromeVisible = !chromeVisible },
                            chromeVisible = chromeVisible,
                        )
                        ReaderBrightnessHud(brightnessHud, onDismiss = { brightnessHud = null }, modifier = Modifier.align(androidx.compose.ui.Alignment.CenterStart))
                    }
                }
            }
            compose.waitUntil(20_000) { rendition.controller != null && compose.onAllNodesWithTag("reader-right-tap-zone").fetchSemanticsNodes().isNotEmpty() }
            val controller = rendition.controller!!
            fun position() = controller.location.toLocator().toJSON().toString()
            val left = compose.onNodeWithTag("reader-left-tap-zone").fetchSemanticsNode().boundsInRoot
            val center = compose.onNodeWithTag("reader-center-tap-zone").fetchSemanticsNode().boundsInRoot
            val right = compose.onNodeWithTag("reader-right-tap-zone").fetchSemanticsNode().boundsInRoot
            val width = compose.onRoot().fetchSemanticsNode().boundsInRoot.width
            assertEquals(width * .318f, left.width, 2f)
            assertEquals(width * .318f, right.width, 2f)
            assert(left.right <= center.left) { "Left tap zone overlaps center zone: $left / $center" }
            assert(center.right <= right.left) { "Center tap zone overlaps right tap zone: $center / $right" }

            val first = position()
            compose.onNodeWithTag("reader-left-tap-zone").performClick()
            compose.waitUntil(1_000) { position() == first }
            assertEquals("Previous-page tap at the book start must be a safe no-op", first, position())
            compose.onNodeWithTag("reader-right-tap-zone").performClick()
            compose.waitUntil(5_000) { position() != first }
            val next = position()
            assertNotEquals(first, next)
            compose.onNodeWithTag("reader-left-tap-zone").performClick()
            compose.waitUntil(5_000) { position() == first }

            val leftBounds = compose.onNodeWithTag("reader-left-tap-zone").fetchSemanticsNode().boundsInRoot
            val start = Offset(leftBounds.left + 24f, leftBounds.center.y)
            compose.onNodeWithTag("reader-left-tap-zone").performTouchInput {
                down(start)
                moveTo(start + Offset(0f, -140f), 300)
                up()
            }
            compose.waitUntil(2_000) { brightnessEndCount == 1 }
            compose.waitUntil(5_000) { runBlocking { settings.preferences.first().brightness } == brightness.toDouble() }
            assertEquals("A brightness drag must not also turn the page", first, position())
            assert(brightness > .5f) { "Upward drag should increase brightness, got $brightness" }
            compose.onNodeWithText("${(brightness * 100).toInt()}%").assertExists()
            context.getExternalFilesDir(null)?.let { directory ->
                val screenshot = compose.onRoot().captureToImage().asAndroidBitmap()
                val screenshotFile = File(directory, "reader-brightness-gesture.png")
                screenshotFile.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
                screenshot.recycle()
                val result = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cp ${screenshotFile.absolutePath} /sdcard/Download/reader-brightness-gesture.png")
                ParcelFileDescriptor.AutoCloseInputStream(result).use { it.readBytes() }
            }
            compose.waitUntil(2_000) { compose.onAllNodesWithText("${(brightness * 100).toInt()}%").fetchSemanticsNodes().isEmpty() }

            compose.onNodeWithTag("reader-right-tap-zone").performClick()
            compose.waitUntil(5_000) { position() != first }
            val middle = position()
            compose.runOnIdle { brightnessGesture = false }
            val disabledStart = Offset(leftBounds.left + 24f, leftBounds.center.y)
            compose.onNodeWithTag("reader-left-tap-zone").performTouchInput {
                down(disabledStart)
                moveTo(disabledStart + Offset(0f, -100f), 300)
                up()
            }
            assertEquals("A drag must not turn a page when brightness gestures are disabled", middle, position())
            val previousCountBeforeTap = previousTapCount
            compose.onNodeWithTag("reader-left-tap-zone").assertHasClickAction().performTouchInput { click(Offset(24f, leftBounds.height / 2f)) }
            compose.waitUntil(2_000) { previousTapCount > previousCountBeforeTap }
            compose.waitUntil(5_000) { position() == first }
            compose.runOnIdle { brightnessGesture = true }

            val lastPosition = runBlocking { publication.positions().last() }
            var finishJob: Job? = null
            compose.runOnIdle {
                finishJob = scope.launch {
                    controller.goTo(ReflowableWebGoLocation(lastPosition))
                    repeat(25) { delay(150); if (controller.canMoveForward) controller.moveForward() }
                }
            }
            compose.waitUntil(15_000) { finishJob?.isCompleted == true && !controller.canMoveForward }
            val end = position()
            compose.onNodeWithTag("reader-right-tap-zone").performClick()
            compose.waitUntil(1_000) { position() == end }
            assertEquals("Next-page tap at the book end must be a safe no-op", end, position())

            compose.onNodeWithTag("reader-center-tap-zone").performClick()
            compose.waitUntil(2_000) { !chromeVisible }
            compose.onNodeWithTag("reader-center-tap-zone").performClick()
            compose.waitUntil(2_000) { chromeVisible }
            context.getExternalFilesDir(null)?.let { directory ->
                val screenshot = compose.onRoot().captureToImage().asAndroidBitmap()
                val screenshotFile = File(directory, "reader-tap-zones.png")
                screenshotFile.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
                screenshot.recycle()
                val result = InstrumentationRegistry.getInstrumentation().uiAutomation.executeShellCommand("cp ${screenshotFile.absolutePath} /sdcard/Download/reader-tap-zones.png")
                ParcelFileDescriptor.AutoCloseInputStream(result).use { it.readBytes() }
            }
            compose.runOnIdle { paging = false }
            compose.waitUntil(2_000) { compose.onAllNodesWithTag("reader-right-tap-zone").fetchSemanticsNodes().isEmpty() }
            compose.onNodeWithTag("reader-left-tap-zone").assertExists()
            compose.onNodeWithTag("reader-left-brightness-zone").assertExists()
            compose.onNodeWithTag("reader-center-tap-zone").assertExists()
        } finally {
            compose.runOnIdle { paging = false }
            scope.cancel()
            runBlocking { settings.number("brightness", originalBrightness) }
            publication.close()
            db.close()
            fixture.delete()
            repository.file(book.fileName).delete()
            book.coverName?.let { repository.file(it).delete() }
        }
    }
}
