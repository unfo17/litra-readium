package io.github.unfo17.litra

import io.github.unfo17.litra.data.*
import org.junit.Assert.*
import org.junit.Test
import java.io.File

class BackupTest {
    private val hash = "a".repeat(64)
    private fun book() = Book(id = "book", title = "A book", authors = "An author", hash = hash, fileName = "media/$hash.epub", coverName = "media/$hash.jpg", status = "FINISHED", progress = .99, locator = "{\"href\":\"chapter.xhtml\"}", lastReadAt = 200, finishedAt = 100)
    private fun manifest() = BackupManifest(createdAt = 300, books = listOf(book()), categories = listOf(Category("cat", "Literature")), memberships = listOf(BookCategory("book", "cat")), bookmarks = listOf(Bookmark("mark", "book", "{\"href\":\"chapter.xhtml\"}", "Chapter", 10)), sessions = listOf(ReadingSession("session", "book", 10, 60, "2026-10-28")), goals = listOf(ReadingGoal(0)), checkIns = listOf(GoalCheckIn("DAILY", 10, 20, 30, 1800, 20)), history = listOf(SearchEntry("book", 30)), files = listOf(BackupFile("media/$hash.epub", hash, 123), BackupFile("media/$hash.jpg", "b".repeat(64), 20)))
    @Test fun manifestRoundTripsEveryEntity() { val value = manifest(); assertEquals(value, BackupManifest.decode(value.encode())) }
    @Test fun rejectsUnsupportedOrIncompleteManifest() {
        listOf(manifest().encode().replace("\"version\":1", "\"version\":2"), manifest().encode().replace("media/$hash.epub", "../outside.epub"), "{}").forEach { assertTrue(runCatching { BackupManifest.decode(it) }.isFailure) }
    }
    @Test fun rejectsZipSlipAndUnexpectedPaths() {
        val root = File("/tmp/litra-backup-test")
        listOf("../manifest.json", "/manifest.json", "media/../../outside.epub", "media\\evil.epub", "media/unknown.epub", "manifest.json/extra").forEach { assertTrue(it, runCatching { BackupArchive.checkedEntry(root, it) }.isFailure) }
        assertEquals(File(root, "media/$hash.epub").canonicalFile, BackupArchive.checkedEntry(root, "media/$hash.epub"))
        assertEquals(16L * 1024 * 1024, BackupArchive.entryLimit("media/$hash.jpg"))
    }
    @Test fun newerPositionWinsWithoutLosingCompletion() {
        val local = book().copy(progress = .4, lastReadAt = 300, finishedAt = 250)
        val old = book().copy(progress = .9, lastReadAt = 200, finishedAt = 100)
        assertEquals(local, BackupMerge.book(local, old))
        val incoming = old.copy(progress = .6, locator = "new locator", lastReadAt = 400, finishedAt = 350)
        val merged = BackupMerge.book(local, incoming)
        assertEquals(.6, merged.progress, 0.0); assertEquals("new locator", merged.locator); assertEquals(350L, merged.finishedAt)
        assertEquals("FINISHED", BackupMerge.book(local, incoming.copy(status = "READING", finishedAt = null)).status)
        assertEquals("FINISHED", BackupMerge.book(local.copy(status = "READING", finishedAt = null), old).status)
        assertEquals(local, BackupMerge.book(local, old.copy(lastReadAt = 300)))
    }
    @Test fun checkInsKeepLocalTargetAndMonotonicCompletion() {
        val local = GoalCheckIn("DAILY", 10, 20, 30, 2000, 15)
        assertEquals(local, BackupMerge.checkIn(local, local.copy(target = 10, seconds = 1000, completedAt = 12)))
        assertEquals(3000L, BackupMerge.checkIn(local, local.copy(seconds = 3000, completedAt = 18)).seconds)
    }
}
