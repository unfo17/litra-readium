@file:OptIn(org.readium.r2.shared.ExperimentalReadiumApi::class)
package io.github.unfo17.litra

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.compose.runtime.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.unfo17.litra.data.*
import io.github.unfo17.litra.ui.LitraTheme
import kotlinx.coroutines.*
import org.json.JSONObject
import org.junit.*
import org.junit.Assert.*
import org.readium.navigator.web.reflowable.*
import org.readium.r2.shared.publication.Locator
import org.readium.r2.shared.publication.services.positions
import org.readium.r2.shared.publication.services.search.search
import org.readium.r2.shared.util.getOrElse

class ReadiumNavigatorTest {
    @get:Rule val compose = createComposeRule()
    @Test fun realNavigatorReachesEndRestoresLocatorAndSearches() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val app = context.applicationContext as Application
        val db = Room.inMemoryDatabaseBuilder(context, LitraDatabase::class.java).build()
        val repo = LibraryRepository(context, db, ReadiumService(context))
        val fixture = ImportPersistenceTest.fixture(context, 25)
        val book = runBlocking { repo.import(Uri.fromFile(fixture)) }
        val pub = runBlocking { repo.readium.open(repo.file(book.fileName)) }
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
        var active by mutableStateOf<ReflowableWebRenditionState?>(null)
        try {
            val factory = ReflowableWebRenditionFactory(app, pub)!!
            active = runBlocking { factory.createRenditionState().getOrElse { error(it.message) } }
            compose.setContent { LitraTheme { active?.let { state -> key(state) { ReflowableWebRendition(state) } } } }
            compose.waitUntil(20000) { active?.controller != null }
            val positions = runBlocking { pub.positions() }
            var navigation: Job? = null
            compose.runOnIdle { navigation = scope.launch { active!!.controller!!.goTo(ReflowableWebGoLocation(positions.last())); repeat(25) { delay(150); if (active!!.controller!!.canMoveForward) active!!.controller!!.moveForward() } } }
            compose.waitUntil(20000) { navigation?.isCompleted == true }
            assertFalse(active!!.controller!!.canMoveForward)
            val locator = active!!.controller!!.location.toLocator()
            runBlocking { repo.progress(book.id, locator.toJSON().toString(), 1.0) }
            val saved = runBlocking { db.library().book(book.id)!! }
            assertEquals("READING", saved.status)
            assertEquals(0.99, saved.progress, 0.0)
            val restored = Locator.fromJSON(JSONObject(saved.locator!!))!!
            val nextState = runBlocking { factory.createRenditionState(initialLocation = ReflowableWebGoLocation(restored)).getOrElse { error(it.message) } }
            compose.runOnIdle { active = nextState }
            compose.waitUntil(20000) { nextState.controller != null }
            assertEquals(restored.href, nextState.controller!!.location.toLocator().href)
            assertEquals(restored.locations.position, nextState.controller!!.location.toLocator().locations.position)
            runBlocking {
                val search = pub.search("quiet")!!
                try { assertTrue(search.next().getOrElse { error(it.message) }!!.locators.isNotEmpty()) } finally { search.close() }
            }
        } finally {
            compose.runOnIdle { active = null }
            scope.cancel(); pub.close(); db.close(); fixture.delete()
            repo.file(book.fileName).delete(); book.coverName?.let { repo.file(it).delete() }
        }
    }
}
