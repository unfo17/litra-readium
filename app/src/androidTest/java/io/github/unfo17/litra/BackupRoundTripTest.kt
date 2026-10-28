package io.github.unfo17.litra

import android.content.Context
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.unfo17.litra.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import java.io.File
import java.util.zip.*

class BackupRoundTripTest {
    @Test fun twoBooksAllRecordsIdempotentMergeAndAtomicFailure() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LitraDatabase::class.java).build()
        val repo = LibraryRepository(context, db, ReadiumService(context))
        val backup = BackupRepository(context, repo)
        val archive = File(context.cacheDir, "test.litrabackup")
        val bad = File(context.cacheDir, "bad.litrabackup")
        val fixtures = listOf(ImportPersistenceTest.fixture(context, 61), ImportPersistenceTest.fixture(context, 62))
        val created = mutableSetOf<String>()
        try {
            val books = fixtures.map { repo.import(Uri.fromFile(it)) }
            books.forEach { created += it.fileName; it.coverName?.let(created::add) }
            val first = books.first()
            repo.progress(first.id, "{\"href\":\"EPUB/chapter.xhtml\"}", .99); repo.finish(first.id)
            val saved = db.library().book(first.id)!!
            db.library().category(Category("category", "Test collection"))
            db.library().addToCategory(BookCategory(first.id, "category"))
            db.library().addBookmark(Bookmark("bookmark", first.id, saved.locator!!, "Chapter"))
            db.library().session(ReadingSession("session", first.id, 10, 60, "2026-10-28"))
            db.library().goal(ReadingGoal(0, "WEEKLY", 60))
            db.library().checkIn(GoalCheckIn("WEEKLY", 10, 20, 60, 3600, 20))
            db.library().rememberSearch("a book")
            assertEquals(2, backup.export(Uri.fromFile(archive)) {})
            // Empty database, original EPUBs removed: the archive alone must restore readable books.
            db.clearAllTables(); created.forEach { repo.file(it).delete() }
            assertEquals(2, backup.restore(Uri.fromFile(archive)) {})
            val restored = db.library().books().first()
            assertEquals(2, restored.size)
            val done = restored.first { it.hash == first.hash }
            assertEquals("FINISHED", done.status); assertEquals(saved.locator, done.locator); assertEquals(saved.finishedAt, done.finishedAt)
            assertEquals(1, db.library().memberships().first().size); assertEquals(1, db.library().allBookmarks().size)
            assertEquals(1, db.library().sessions().first().size); assertEquals(60, db.library().goals().first().single().minutes)
            assertEquals(1, db.library().allCheckIns().size); assertEquals(1, db.library().history().first().size)
            restored.forEach { b ->
                assertTrue(repo.file(b.coverName!!).length() > 0)
                val pub = repo.readium.open(repo.file(b.fileName)); try { assertTrue(pub.readingOrder.isNotEmpty()) } finally { pub.close() }
            }
            repo.progress(done.id, "{\"href\":\"EPUB/chapter.xhtml\",\"locations\":{\"totalProgression\":0.4}}", .4)
            val newer = db.library().book(done.id)!!
            backup.restore(Uri.fromFile(archive)) {}
            assertEquals(newer, db.library().book(done.id))
            assertEquals(2, db.library().books().first().size); assertEquals(1, db.library().sessions().first().size)
            assertEquals(1, db.library().allBookmarks().size)
            val beforeFiles = repo.file("restores").list()?.toSet()
            ZipOutputStream(bad.outputStream()).use { it.putNextEntry(ZipEntry("../escape.epub")); it.write(byteArrayOf(1)); it.closeEntry() }
            assertTrue(runCatching { backup.restore(Uri.fromFile(bad)) {} }.isFailure)
            assertEquals(beforeFiles, repo.file("restores").list()?.toSet()); assertEquals(2, db.library().books().first().size)
            ZipOutputStream(bad.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("media/${"a".repeat(64)}.jpg"))
                repeat(257) { zip.write(ByteArray(64 * 1024)) }; zip.closeEntry()
            }
            assertTrue(runCatching { backup.restore(Uri.fromFile(bad)) {} }.isFailure)
            assertEquals(beforeFiles, repo.file("restores").list()?.toSet())
            // Fail after installing files and inserting the book rows: transaction and files roll back.
            restored.forEach { b -> created += b.fileName; b.coverName?.let(created::add) }
            db.clearAllTables()
            db.openHelper.writableDatabase.execSQL("CREATE TRIGGER reject_category BEFORE INSERT ON categories BEGIN SELECT RAISE(ABORT, 'test rollback'); END")
            assertTrue(runCatching { backup.restore(Uri.fromFile(archive)) {} }.isFailure)
            assertTrue(db.library().books().first().isEmpty())
            assertTrue(repo.file("restores").list().isNullOrEmpty())
        } finally {
            db.library().books().first().forEach { repo.file(it.fileName).delete(); it.coverName?.let { name -> repo.file(name).delete() } }
            created.forEach { repo.file(it).delete() }; db.close(); fixtures.forEach { it.delete() }; archive.delete(); bad.delete()
        }
    }
}
