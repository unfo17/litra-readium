package io.github.unfo17.litra.data

import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Explicit JSON codec: stable names on disk, independent of Room and R8 reflection. */
data class BackupFile(val name: String, val sha256: String, val size: Long)
data class BackupManifest(
    val version: Int = 1,
    val createdAt: Long = System.currentTimeMillis(),
    val books: List<Book> = emptyList(),
    val categories: List<Category> = emptyList(),
    val memberships: List<BookCategory> = emptyList(),
    val bookmarks: List<Bookmark> = emptyList(),
    val sessions: List<ReadingSession> = emptyList(),
    val goals: List<ReadingGoal> = emptyList(),
    val checkIns: List<GoalCheckIn> = emptyList(),
    val history: List<SearchEntry> = emptyList(),
    val files: List<BackupFile> = emptyList(),
) {
    fun encode(): String = json("format" to "LitraBackup", "version" to version, "createdAt" to createdAt,
        "books" to JSONArray(books.map { it.toJson() }),
        "categories" to JSONArray(categories.map { it.toJson() }),
        "memberships" to JSONArray(memberships.map { it.toJson() }),
        "bookmarks" to JSONArray(bookmarks.map { it.toJson() }),
        "sessions" to JSONArray(sessions.map { it.toJson() }),
        "goals" to JSONArray(goals.map { it.toJson() }),
        "checkIns" to JSONArray(checkIns.map { it.toJson() }),
        "history" to JSONArray(history.map { it.toJson() }),
        "files" to JSONArray(files.map { it.toJson() })
    ).toString()

    fun validate() {
        require(version == 1) { "不支持此备份版本" }
        fun <T> unique(items: List<T>) = require(items.distinct().size == items.size) { "备份中存在重复标识" }
        unique(books.map { it.id }); unique(books.map { it.hash }); unique(categories.map { it.id }); unique(categories.map { it.name })
        unique(bookmarks.map { it.id }); unique(sessions.map { it.id }); unique(goals.map { it.id })
        unique(checkIns.map { it.period to it.start }); unique(history.map { it.query }); unique(files.map { it.name })
        val ids = books.map { it.id }.toSet()
        val cats = categories.map { it.id }.toSet()
        val media = files.associateBy { it.name }
        require(files.all { BackupArchive.mediaName.matches(it.name) && it.sha256.matches(Regex("[a-f0-9]{64}")) && it.size in 1..BackupArchive.entryLimit(it.name) }) { "无效的备份文件清单" }
        books.forEach { b ->
            require(b.hash.matches(Regex("[a-f0-9]{64}")) && b.fileName == "media/${b.hash}.epub" && media[b.fileName]?.sha256 == b.hash) { "无效的 EPUB 校验信息" }
            require(b.coverName == null || (b.coverName == "media/${b.hash}.jpg" && b.coverName in media)) { "无效的封面路径" }
            require(b.status in listOf("UNREAD", "WANT_TO_READ", "READING", "FINISHED") && b.kind in listOf("图书", "漫画") && b.format == "EPUB") { "无效的书籍状态" }
            require(b.progress.isFinite() && b.progress in 0.0..0.99 && b.createdAt >= 0 && (b.lastReadAt ?: 0) >= 0 && (b.finishedAt ?: 0) >= 0) { "无效的阅读进度" }
            b.locator?.let { require(JSONObject(it).has("href")) { "无效的阅读位置" } }
        }
        require(media.keys == books.flatMap { listOfNotNull(it.fileName, it.coverName) }.toSet()) { "文件清单与书目不一致" }
        require(categories.all { it.name.isNotBlank() && it.name.length <= 60 && it.name !in listOf("图书", "欲读清单", "已读完", "PDF", "有声书") }) { "无效的分类名称" }
        require(memberships.all { it.bookId in ids && it.categoryId in cats }) { "无效的分类关系" }
        require(bookmarks.all { it.bookId in ids && JSONObject(it.locator).has("href") }) { "无效的书签" }
        require(sessions.all { it.bookId in ids && it.startedAt >= 0 && it.seconds in 0..86400 && runCatching { java.time.LocalDate.parse(it.localDay) }.isSuccess }) { "无效的阅读会话" }
        require(goals.all { it.period in periods && it.minutes in 1..1440 && it.effectiveAt >= 0 }) { "无效的阅读目标" }
        require(checkIns.all { it.period in periods && it.target in 1..1440 && it.start >= 0 && it.end > it.start && it.seconds >= 0 }) { "无效的打卡记录" }
        require(history.all { it.query.isNotBlank() && it.query.length <= 120 && it.usedAt >= 0 }) { "无效的搜索记录" }
    }
    companion object {
        private val periods = listOf("DAILY", "WEEKLY", "MONTHLY")
        fun decode(text: String): BackupManifest {
            val j = JSONObject(text)
            require(j.getString("format") == "LitraBackup" && j.getInt("version") == 1) { "无效或不支持的备份版本" }
            return BackupManifest(version = j.getInt("version"), createdAt = j.getLong("createdAt"),
                books = j.rows("books") { Book(id = it.getString("id"), title = it.getString("title"), authors = it.getString("authors"), fileName = it.getString("fileName"), coverName = if (it.isNull("coverName")) null else it.getString("coverName"), hash = it.getString("hash"), format = it.getString("format"), kind = it.getString("kind"), status = it.getString("status"), progress = it.getDouble("progress"), locator = if (it.isNull("locator")) null else it.getString("locator"), createdAt = it.getLong("createdAt"), lastReadAt = if (it.isNull("lastReadAt")) null else it.getLong("lastReadAt"), finishedAt = if (it.isNull("finishedAt")) null else it.getLong("finishedAt"), description = it.getString("description"), language = it.getString("language"), publisher = it.getString("publisher"), size = it.getLong("size")) },
                categories = j.rows("categories") { Category(id = it.getString("id"), name = it.getString("name")) },
                memberships = j.rows("memberships") { BookCategory(bookId = it.getString("bookId"), categoryId = it.getString("categoryId")) },
                bookmarks = j.rows("bookmarks") { Bookmark(id = it.getString("id"), bookId = it.getString("bookId"), locator = it.getString("locator"), label = it.getString("label"), createdAt = it.getLong("createdAt")) },
                sessions = j.rows("sessions") { ReadingSession(id = it.getString("id"), bookId = it.getString("bookId"), startedAt = it.getLong("startedAt"), seconds = it.getLong("seconds"), localDay = it.getString("localDay")) },
                goals = j.rows("goals") { ReadingGoal(id = it.getLong("id"), period = it.getString("period"), minutes = it.getInt("minutes"), effectiveAt = it.getLong("effectiveAt")) },
                checkIns = j.rows("checkIns") { GoalCheckIn(period = it.getString("period"), start = it.getLong("start"), end = it.getLong("end"), target = it.getInt("target"), seconds = it.getLong("seconds"), completedAt = if (it.isNull("completedAt")) null else it.getLong("completedAt")) },
                history = j.rows("history") { SearchEntry(query = it.getString("query"), usedAt = it.getLong("usedAt")) },
                files = j.rows("files") { BackupFile(name = it.getString("name"), sha256 = it.getString("sha256"), size = it.getLong("size")) }
            ).also { it.validate() }
        }
    }
}

private fun json(vararg fields: Pair<String, Any?>) = JSONObject().apply { fields.forEach { (key, value) -> put(key, value ?: JSONObject.NULL) } }
private fun <T> JSONObject.rows(key: String, read: (JSONObject) -> T): List<T> {
    val array = getJSONArray(key)
    require(array.length() <= 100_000) { "备份记录过多" }
    return (0 until array.length()).map { read(array.getJSONObject(it)) }
}
private fun Book.toJson() = json("id" to id, "title" to title, "authors" to authors, "fileName" to fileName, "coverName" to coverName, "hash" to hash, "format" to format, "kind" to kind, "status" to status, "progress" to progress, "locator" to locator, "createdAt" to createdAt, "lastReadAt" to lastReadAt, "finishedAt" to finishedAt, "description" to description, "language" to language, "publisher" to publisher, "size" to size)
private fun Category.toJson() = json("id" to id, "name" to name)
private fun BookCategory.toJson() = json("bookId" to bookId, "categoryId" to categoryId)
private fun Bookmark.toJson() = json("id" to id, "bookId" to bookId, "locator" to locator, "label" to label, "createdAt" to createdAt)
private fun ReadingSession.toJson() = json("id" to id, "bookId" to bookId, "startedAt" to startedAt, "seconds" to seconds, "localDay" to localDay)
private fun ReadingGoal.toJson() = json("id" to id, "period" to period, "minutes" to minutes, "effectiveAt" to effectiveAt)
private fun GoalCheckIn.toJson() = json("period" to period, "start" to start, "end" to end, "target" to target, "seconds" to seconds, "completedAt" to completedAt)
private fun SearchEntry.toJson() = json("query" to query, "usedAt" to usedAt)
private fun BackupFile.toJson() = json("name" to name, "sha256" to sha256, "size" to size)

object BackupMerge {
    /** Local wins ties. Completion is monotonic; the latest reading position wins. */
    fun book(local: Book, incoming: Book): Book {
        val newer = (incoming.lastReadAt ?: 0) > (local.lastReadAt ?: 0)
        val finished = local.status == "FINISHED" || incoming.status == "FINISHED"
        return local.copy(
            status = if (finished) "FINISHED" else if (newer) incoming.status else local.status,
            progress = BookRules.progress(if (newer) incoming.progress else local.progress),
            locator = if (newer) incoming.locator else local.locator,
            lastReadAt = listOfNotNull(local.lastReadAt, incoming.lastReadAt).maxOrNull(),
            finishedAt = listOfNotNull(local.finishedAt, incoming.finishedAt).maxOrNull(),
        )
    }
    fun checkIn(local: GoalCheckIn?, incoming: GoalCheckIn): GoalCheckIn = if (local == null) incoming else local.copy(
        seconds = maxOf(local.seconds, incoming.seconds),
        completedAt = listOfNotNull(local.completedAt, incoming.completedAt).maxOrNull(),
    )
}

object BackupArchive {
    const val MAX_TOTAL = 2L * 1024 * 1024 * 1024
    const val MAX_ENTRIES = 20_001
    val mediaName = Regex("media/[a-f0-9]{64}\\.(epub|jpg)")
    fun entryLimit(name: String): Long = when {
        name == "manifest.json" -> 8L * 1024 * 1024
        name.endsWith(".jpg") -> 16L * 1024 * 1024
        else -> 512L * 1024 * 1024
    }
    fun checkedEntry(root: File, name: String): File {
        require(name == "manifest.json" || mediaName.matches(name)) { "不安全的备份路径：$name" }
        val file = File(root, name).canonicalFile
        require(file.path.startsWith(root.canonicalPath + File.separator)) { "备份路径越界" }
        return file
    }
}
