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
import kotlinx.coroutines.runBlocking
import org.junit.*
import org.junit.Assert.*
import org.readium.navigator.web.fixedlayout.*
import org.readium.navigator.web.fixedlayout.preferences.FixedWebPreferences
import org.readium.r2.shared.publication.Layout
import org.readium.r2.shared.util.getOrElse

class FixedLayoutTest {
    @get:Rule val compose = createComposeRule()
    @Test fun fixedLayoutEpubRenders() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LitraDatabase::class.java).build()
        val repo = LibraryRepository(context, db, ReadiumService(context))
        val fixture = ImportPersistenceTest.fixture(context, 41, fixed = true)
        val book = runBlocking { repo.import(Uri.fromFile(fixture)) }
        val pub = runBlocking { repo.readium.open(repo.file(book.fileName)) }
        var visible by mutableStateOf(true)
        try {
            assertEquals(Layout.FIXED, pub.metadata.layout)
            val factory = FixedWebRenditionFactory(context.applicationContext as Application, pub, FixedWebConfiguration())!!
            val state = runBlocking { factory.createRenditionState(FixedWebPreferences()).getOrElse { error(it.message) } }
            compose.setContent { LitraTheme { if (visible) FixedWebRendition(state) } }
            compose.waitUntil(20000) { state.controller != null }
            assertEquals(pub.readingOrder.first().url(), state.controller!!.location.toLocator().href)
        } finally {
            compose.runOnIdle { visible = false }
            pub.close(); db.close(); fixture.delete()
            repo.file(book.fileName).delete(); book.coverName?.let { repo.file(it).delete() }
        }
    }
}
