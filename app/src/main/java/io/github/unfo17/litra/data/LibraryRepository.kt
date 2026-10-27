package io.github.unfo17.litra.data

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.util.Size
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.readium.r2.shared.publication.services.coverFitting
import java.io.File
import java.io.IOException
import java.security.MessageDigest
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LibraryRepository @Inject constructor(@ApplicationContext private val context: Context, val db: LitraDatabase, val readium: ReadiumService) {
    val dao = db.library()
    private val importMutex = Mutex()
    val books = dao.books()
    val categories = dao.categories()
    val memberships = dao.memberships()
    fun file(name: String) = File(context.filesDir, name)

    suspend fun import(uri: Uri): Book = importMutex.withLock {
        withContext(Dispatchers.IO) {
            val id = UUID.randomUUID().toString()
            val bookFile = file("books/$id.epub").also { it.parentFile?.mkdirs() }
            val coverFile = file("covers/$id.jpg").also { it.parentFile?.mkdirs() }
            var saved = false
            try {
                val digest = MessageDigest.getInstance("SHA-256")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    bookFile.outputStream().use { output ->
                        val buffer = ByteArray(64 * 1024)
                        var total = 0L
                        while (true) {
                            ensureActive()
                            val n = input.read(buffer)
                            if (n < 0) break
                            total += n
                            if (total > 512L * 1024 * 1024) throw IOException("文件超过 512 MB 导入上限")
                            digest.update(buffer, 0, n); output.write(buffer, 0, n)
                        }
                    }
                } ?: throw IOException("无法读取所选文件")
                val hash = digest.digest().joinToString("") { "%02x".format(it) }
                dao.byHash(hash)?.let { return@withContext it }
                val pub = readium.open(bookFile)
                try {
                    require(pub.readingOrder.isNotEmpty()) { "EPUB 中没有可阅读的章节" }
                    val bitmap = pub.coverFitting(Size(600, 900))
                    bitmap?.let { image -> coverFile.outputStream().use { image.compress(Bitmap.CompressFormat.JPEG, 85, it) }; image.recycle() }
                    val meta = pub.metadata
                    val book = Book(id = id, title = meta.title?.takeIf { it.isNotBlank() } ?: "未命名图书", authors = meta.authors.joinToString("、") { it.name }, fileName = "books/$id.epub", coverName = if (coverFile.length() > 0) "covers/$id.jpg" else null, hash = hash, description = meta.description.orEmpty(), language = meta.languages.joinToString(", "), publisher = meta.publishers.joinToString("、") { it.name }, size = bookFile.length())
                    // Finish the short commit even if the picker/activity is cancelled.
                    // Otherwise a committed row could be left pointing at a deleted EPUB.
                    withContext(NonCancellable) { dao.insert(book); saved = true }
                    book
                } finally { pub.close() }
            } finally {
                if (!saved) { bookFile.delete(); coverFile.delete() }
            }
        }
    }

    suspend fun delete(book: Book) = withContext(Dispatchers.IO) {
        dao.delete(book)
        file(book.fileName).delete()
        book.coverName?.let { file(it).delete() }
    }
    suspend fun progress(id: String, json: String, progression: Double) = dao.saveProgress(id, json, BookRules.progress(progression), System.currentTimeMillis())
    suspend fun finish(id: String) = dao.finish(id, System.currentTimeMillis())
    suspend fun category(name: String) {
        val value = name.trim().take(60)
        require(value.isNotEmpty()) { "请输入分类名称" }
        require(value !in listOf("图书", "欲读清单", "已读完", "PDF", "有声书")) { "请使用不同于默认藏书的分类名" }
        dao.category(Category(name = value))
    }
}
