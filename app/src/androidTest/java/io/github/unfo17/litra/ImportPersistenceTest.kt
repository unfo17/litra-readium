package io.github.unfo17.litra

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.unfo17.litra.data.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.readium.r2.shared.publication.services.coverFitting
import java.io.*
import java.util.zip.*

/** Real EPUB files exercise SAF's repository boundary, Readium and persistent Room storage. */
@RunWith(AndroidJUnit4::class)
class ImportPersistenceTest {
    @Test fun importTenBooksDeduplicateAndPersistCompletion() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "import-test-${System.nanoTime()}.db"
        var db = Room.databaseBuilder(context, LitraDatabase::class.java, name).build()
        val repository = LibraryRepository(context, db, ReadiumService(context))
        val books = mutableListOf<Book>()
        val fixtures = mutableListOf<File>()
        try {
            repeat(10) { n ->
                val file = fixture(context, n).also(fixtures::add)
                val book = repository.import(Uri.fromFile(file))
                books += book
                assertEquals("Litra EPUB $n", book.title)
                assertEquals("Litra Test", book.authors)
                assertTrue(repository.file(book.coverName!!).length() > 0)
                assertEquals(book.id, repository.import(Uri.fromFile(file)).id)
            }
            assertEquals(10, db.library().books().first().size)
            val broken = File(context.cacheDir, "broken.epub").also { it.writeText("PK truncated EPUB"); fixtures += it }
            val existingFiles = repository.file("books").list()?.toSet()
            try { repository.import(Uri.fromFile(broken)); fail("Corrupt EPUB must be rejected") }
            catch (e: IOException) { assertTrue(e.message.orEmpty().contains("EPUB")) }
            assertEquals(existingFiles, repository.file("books").list()?.toSet())
            assertEquals(10, db.library().books().first().size)
            val first = books.first()
            repository.progress(first.id, "{\"href\":\"chapter.xhtml\",\"type\":\"application/xhtml+xml\",\"locations\":{\"totalProgression\":1.0}}", 1.0)
            assertEquals("READING", db.library().book(first.id)!!.status)
            assertEquals(0.99, db.library().book(first.id)!!.progress, 0.0)
            repository.finish(first.id)
            val finishedAt = db.library().book(first.id)!!.finishedAt
            repository.progress(first.id, "{\"href\":\"chapter.xhtml\"}", 0.4)
            repository.finish(first.id)
            assertEquals("FINISHED", db.library().book(first.id)!!.status)
            assertEquals(finishedAt, db.library().book(first.id)!!.finishedAt)
            val category = Category(name = "Test collection")
            db.library().category(category)
            db.library().addToCategory(BookCategory(first.id, category.id))
            db.library().deleteCategories(listOf(category.id))
            assertTrue(db.library().memberships().first().isEmpty())
            assertNotNull(db.library().book(first.id))
            repeat(12) { db.library().rememberSearch("Query $it") }
            assertEquals(10, db.library().history().first().size)
            db.close()
            db = Room.databaseBuilder(context, LitraDatabase::class.java, name).build()
            assertEquals(10, db.library().books().first().size)
            assertEquals("FINISHED", db.library().book(first.id)!!.status)
            assertEquals(0.4, db.library().book(first.id)!!.progress, 0.0)
        } finally {
            db.close(); context.deleteDatabase(name)
            books.forEach { repository.file(it.fileName).delete(); it.coverName?.let { cover -> repository.file(cover).delete() } }
            fixtures.forEach { it.delete() }
        }
    }
    companion object {
        fun fixture(context: Context, n: Int, fixed: Boolean = false): File {
            val epub2 = n % 2 == 0
            val file = File(context.cacheDir, "fixture-$n.epub")
            val bitmap = Bitmap.createBitmap(100, 150, Bitmap.Config.ARGB_8888).apply { eraseColor(Color.rgb(80 + n * 10, 90, 110)) }
            val cover = ByteArrayOutputStream().also { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }.toByteArray(); bitmap.recycle()
            ZipOutputStream(file.outputStream()).use { zip ->
                fun entry(name: String, bytes: ByteArray, stored: Boolean = false) {
                    val e = ZipEntry(name)
                    if (stored) { e.method = ZipEntry.STORED; e.size = bytes.size.toLong(); e.crc = CRC32().also { it.update(bytes) }.value }
                    zip.putNextEntry(e); zip.write(bytes); zip.closeEntry()
                }
                fun xml(name: String, text: String) = entry(name, text.toByteArray())
                entry("mimetype", "application/epub+zip".toByteArray(), true)
                xml("META-INF/container.xml", """<?xml version="1.0"?><container version="1.0" xmlns="urn:oasis:names:tc:opendocument:xmlns:container"><rootfiles><rootfile full-path="EPUB/package.opf" media-type="application/oebps-package+xml"/></rootfiles></container>""")
                xml("EPUB/package.opf", """<?xml version="1.0"?><package xmlns="http://www.idpf.org/2007/opf" version="${if (epub2) "2.0" else "3.0"}" unique-identifier="id"><metadata xmlns:dc="http://purl.org/dc/elements/1.1/"><dc:identifier id="id">urn:litra:test:$n</dc:identifier><dc:title>Litra EPUB $n</dc:title><dc:creator>Litra Test</dc:creator><dc:language>en</dc:language><meta name="cover" content="cover"/>${if (epub2) "" else "<meta property=\"dcterms:modified\">2026-01-01T00:00:00Z</meta>"}${if (fixed) "<meta property=\"rendition:layout\">pre-paginated</meta>" else ""}</metadata><manifest><item id="nav" href="nav.xhtml" media-type="application/xhtml+xml" ${if (epub2) "" else "properties=\"nav\""}/><item id="ncx" href="toc.ncx" media-type="application/x-dtbncx+xml"/><item id="chapter" href="chapter.xhtml" media-type="application/xhtml+xml"/><item id="cover" href="cover.png" media-type="image/png" ${if (epub2) "" else "properties=\"cover-image\""}/></manifest><spine toc="ncx"><itemref idref="chapter"/></spine></package>""")
                xml("EPUB/toc.ncx", """<ncx xmlns="http://www.daisy.org/z3986/2005/ncx/" version="2005-1"><head><meta name="dtb:uid" content="urn:litra:test:$n"/></head><docTitle><text>Contents</text></docTitle><navMap><navPoint id="one" playOrder="1"><navLabel><text>Chapter One</text></navLabel><content src="chapter.xhtml"/></navPoint></navMap></ncx>""")
                xml("EPUB/nav.xhtml", """<html xmlns="http://www.w3.org/1999/xhtml" xmlns:epub="http://www.idpf.org/2007/ops"><head><title>Contents</title></head><body><nav epub:type="toc"><ol><li><a href="chapter.xhtml">Chapter One</a></li></ol></nav></body></html>""")
                xml("EPUB/chapter.xhtml", """<html xmlns="http://www.w3.org/1999/xhtml"><head><title>Chapter One</title><meta name="viewport" content="width=600,height=900"/></head><body><h1>Litra EPUB $n</h1>${(1..80).joinToString("") { "<p>Paragraph $it. A quiet morning invites a new reading adventure. This is a real EPUB integration test.</p>" }}</body></html>""")
                entry("EPUB/cover.png", cover)
            }
            return file
        }
    }
}
