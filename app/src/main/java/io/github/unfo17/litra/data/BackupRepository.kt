package io.github.unfo17.litra.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.security.MessageDigest
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BackupRepository @Inject constructor(@ApplicationContext private val context: Context, private val library: LibraryRepository) {
    private val dao get() = library.dao
    private val db get() = library.db

    suspend fun export(uri: Uri, progress: (String) -> Unit): Int = library.storageMutex.withLock { withContext(Dispatchers.IO) {
        val stage = File(context.cacheDir, "backup-${UUID.randomUUID()}").also { it.mkdirs() }
        try {
            progress("正在整理书库…")
            var snapshot = db.withTransaction {
                BackupManifest(books = dao.books().first(), categories = dao.categories().first(), memberships = dao.memberships().first(), bookmarks = dao.allBookmarks(), sessions = dao.sessions().first(), goals = dao.goals().first(), checkIns = dao.allCheckIns(), history = dao.history().first())
            }
            val files = mutableListOf<BackupFile>()
            var total = 0L
            val books = snapshot.books.mapIndexed { index, b ->
                currentCoroutineContext().ensureActive()
                progress("正在打包图书 ${index + 1} / ${snapshot.books.size}")
                fun stageFile(source: String, target: String): String {
                    val from = library.file(source)
                    require(from.length() in 1..BackupArchive.entryLimit(target)) { "图书文件丢失或超过备份上限：${b.title}" }
                    total += from.length()
                    require(total <= BackupArchive.MAX_TOTAL) { "备份超过 2 GB 上限" }
                    val dest = BackupArchive.checkedEntry(stage, target).also { it.parentFile!!.mkdirs() }
                    from.copyTo(dest)
                    files += BackupFile(target, sha256(dest), dest.length())
                    return target
                }
                b.copy(fileName = stageFile(b.fileName, "media/${b.hash}.epub"), coverName = b.coverName?.let { stageFile(it, "media/${b.hash}.jpg") })
            }
            snapshot = snapshot.copy(books = books, files = files).also { it.validate() }
            require(files.size + 1 <= BackupArchive.MAX_ENTRIES) { "备份文件过多" }
            val manifest = snapshot.encode().toByteArray(Charsets.UTF_8)
            require(manifest.size <= BackupArchive.entryLimit("manifest.json")) { "备份记录超过 8 MB 上限" }
            val archive = File(stage, "export.litrabackup")
            ZipOutputStream(archive.outputStream().buffered()).use { zip ->
                zip.putNextEntry(ZipEntry("manifest.json")); zip.write(manifest); zip.closeEntry()
                files.forEach { f ->
                    currentCoroutineContext().ensureActive()
                    zip.putNextEntry(ZipEntry(f.name)); File(stage, f.name).inputStream().use { it.copyTo(zip) }; zip.closeEntry()
                }
            }
            progress("正在写入所选位置…")
            context.contentResolver.openOutputStream(uri, "wt")?.use { out -> archive.inputStream().use { it.copyTo(out) } } ?: error("无法写入备份位置")
            books.size
        } catch (e: Exception) {
            // A failed SAF write must not look like a usable backup.
            runCatching { android.provider.DocumentsContract.deleteDocument(context.contentResolver, uri) }
            throw e
        } finally { stage.deleteRecursively() }
    } }

    suspend fun restore(uri: Uri, progress: (String) -> Unit): Int = library.storageMutex.withLock { withContext(Dispatchers.IO) {
        cleanupAbandonedRestores()
        val stage = File(context.cacheDir, "restore-${UUID.randomUUID()}").also { it.mkdirs() }
        val installed = library.file("restores/${UUID.randomUUID()}")
        var committed = false
        try {
            progress("正在检查备份…")
            val names = mutableSetOf<String>()
            var total = 0L
            context.contentResolver.openInputStream(uri)?.use { input ->
                ZipInputStream(input.buffered()).use { zip ->
                    while (true) {
                        currentCoroutineContext().ensureActive()
                        val entry = zip.nextEntry ?: break
                        val destination = BackupArchive.checkedEntry(stage, entry.name)
                        require(!entry.isDirectory && names.add(entry.name) && names.size <= BackupArchive.MAX_ENTRIES) { "重复或过多的备份文件" }
                        val limit = BackupArchive.entryLimit(entry.name)
                        require(entry.size <= limit) { "备份文件过大" }
                        destination.parentFile!!.mkdirs()
                        destination.outputStream().buffered().use { out ->
                            val buffer = ByteArray(64 * 1024)
                            var bytes = 0L
                            while (true) {
                                currentCoroutineContext().ensureActive()
                                val n = zip.read(buffer)
                                if (n < 0) break
                                bytes += n; total += n
                                require(bytes <= limit && total <= BackupArchive.MAX_TOTAL) { "备份解压超过大小上限" }
                                out.write(buffer, 0, n)
                            }
                        }
                        zip.closeEntry() // checks the CRC as well
                    }
                }
            } ?: error("无法读取备份")
            val manifestFile = File(stage, "manifest.json")
            require(manifestFile.isFile) { "备份缺少 manifest.json" }
            val manifest = BackupManifest.decode(manifestFile.readText(Charsets.UTF_8))
            require(names == manifest.files.map { it.name }.toSet() + "manifest.json") { "备份文件缺失或包含未知文件" }
            manifest.files.forEachIndexed { index, f ->
                currentCoroutineContext().ensureActive()
                progress("正在校验文件 ${index + 1} / ${manifest.files.size}")
                val file = BackupArchive.checkedEntry(stage, f.name)
                require(file.length() == f.size && sha256(file) == f.sha256) { "备份文件校验失败：${f.name}" }
            }
            manifest.books.forEach { b ->
                val file = File(stage, b.fileName)
                requireEpub(file)
                val publication = library.readium.open(file)
                try { require(publication.readingOrder.isNotEmpty()) { "备份包含空 EPUB" } } finally { publication.close() }
            }
            progress("正在合并书库…")
            // Install copies first. Until the transaction commits no existing file/row is changed.
            // A distinct directory makes rollback and recovery after process death unambiguous.
            installed.mkdirs()
            val copies = manifest.books.associate { b ->
                val id = UUID.randomUUID().toString()
                val epub = File(installed, "$id.epub")
                File(stage, b.fileName).copyTo(epub)
                val cover = b.coverName?.let { name -> File(installed, "$id.jpg").also { File(stage, name).copyTo(it) } }
                b.id to b.copy(id = id, fileName = epub.relativeTo(context.filesDir).path, coverName = cover?.relativeTo(context.filesDir)?.path)
            }
            currentCoroutineContext().ensureActive()
            withContext(NonCancellable) {
                db.withTransaction {
                    val bookIds = mutableMapOf<String, String>()
                    manifest.books.forEach { b ->
                        val local = dao.byHash(b.hash)
                        val copy = copies.getValue(b.id)
                        if (local == null) { dao.insert(copy); bookIds[b.id] = copy.id }
                        else {
                            dao.update(BackupMerge.book(local, b))
                            bookIds[b.id] = local.id
                            library.file(copy.fileName).delete(); copy.coverName?.let { library.file(it).delete() }
                        }
                    }
                    val existingCategories = dao.categories().first().associateBy { it.name }.toMutableMap()
                    val catIds = manifest.categories.associate { c ->
                        val existing = existingCategories[c.name]
                        val target = existing ?: Category(name = c.name).also { dao.category(it); existingCategories[c.name] = it }
                        c.id to target.id
                    }
                    manifest.memberships.forEach { dao.addToCategory(BookCategory(bookIds.getValue(it.bookId), catIds.getValue(it.categoryId))) }
                    // Stable IDs make repeated restores idempotent. Remap book IDs after hash merging.
                    manifest.bookmarks.forEach { dao.restoreBookmark(it.copy(bookId = bookIds.getValue(it.bookId))) }
                    manifest.sessions.forEach { dao.restoreSession(it.copy(bookId = bookIds.getValue(it.bookId))) }
                    manifest.goals.forEach { dao.restoreGoal(it) } // same ID: keep the local goal snapshot
                    manifest.checkIns.forEach { dao.checkIn(BackupMerge.checkIn(dao.checkIn(it.period, it.start), it)) }
                    val history = dao.history().first().associateBy { it.query }
                    manifest.history.forEach { if (it.usedAt > (history[it.query]?.usedAt ?: -1)) dao.addSearch(it) }
                    dao.trimHistory()
                }
                committed = true
            }
            if (installed.list().isNullOrEmpty()) installed.delete()
            manifest.books.size
        } finally {
            stage.deleteRecursively()
            if (!committed) installed.deleteRecursively()
        }
    } }

    /** Remove pre-commit files left by a killed restore, without touching committed books. */
    private suspend fun cleanupAbandonedRestores() {
        val referenced = dao.books().first().map { library.file(it.fileName).parentFile!!.canonicalPath }.toSet()
        library.file("restores").listFiles()?.filter { it.isDirectory && it.canonicalPath !in referenced }?.forEach { it.deleteRecursively() }
    }
}

internal fun sha256(file: File): String {
    val digest = MessageDigest.getInstance("SHA-256")
    file.inputStream().buffered().use { input ->
        val buffer = ByteArray(64 * 1024)
        while (true) { val n = input.read(buffer); if (n < 0) break; digest.update(buffer, 0, n) }
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
