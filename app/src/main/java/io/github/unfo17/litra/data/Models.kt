package io.github.unfo17.litra.data

import androidx.room.*
import java.util.UUID

object BookRules {
    fun progress(value: Double): Double = if (value.isFinite()) value.coerceIn(0.0, 0.99) else 0.0
    fun percent(value: Double): Int = (progress(value) * 100 + 0.000001).toInt()
    fun readingStatus(current: String): String = if (current == "FINISHED") "FINISHED" else "READING"
}

@Entity(tableName = "books", indices = [Index(value = ["hash"], unique = true)])
data class Book(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val title: String,
    val authors: String,
    val fileName: String,
    val coverName: String? = null,
    val hash: String,
    val format: String = "EPUB",
    val kind: String = "图书",
    val status: String = "UNREAD",
    val progress: Double = 0.0,
    val locator: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val lastReadAt: Long? = null,
    val finishedAt: Long? = null,
    val description: String = "",
    val language: String = "",
    val publisher: String = "",
    val size: Long = 0,
) {
    val progressLabel: String get() = if (status == "FINISHED") "已读完" else "${BookRules.percent(progress)}%"
    val needsFinish: Boolean get() = status != "FINISHED" && BookRules.percent(progress) == 99
}

@Entity(tableName = "categories", indices = [Index(value = ["name"], unique = true)])
data class Category(@PrimaryKey val id: String = UUID.randomUUID().toString(), val name: String)

@Entity(tableName = "book_categories", primaryKeys = ["bookId", "categoryId"],
    foreignKeys = [ForeignKey(entity = Book::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE), ForeignKey(entity = Category::class, parentColumns = ["id"], childColumns = ["categoryId"], onDelete = ForeignKey.CASCADE)],
    indices = [Index("categoryId")])
data class BookCategory(val bookId: String, val categoryId: String)

@Entity(tableName = "search_history")
data class SearchEntry(@PrimaryKey val query: String, val usedAt: Long = System.currentTimeMillis())

@Entity(tableName = "bookmarks", indices = [Index("bookId")], foreignKeys = [ForeignKey(entity = Book::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)])
data class Bookmark(@PrimaryKey val id: String = UUID.randomUUID().toString(), val bookId: String, val locator: String, val label: String, val createdAt: Long = System.currentTimeMillis())

@Entity(tableName = "reading_sessions", indices = [Index("bookId")], foreignKeys = [ForeignKey(entity = Book::class, parentColumns = ["id"], childColumns = ["bookId"], onDelete = ForeignKey.CASCADE)])
data class ReadingSession(@PrimaryKey val id: String = UUID.randomUUID().toString(), val bookId: String, val startedAt: Long, val seconds: Long, val localDay: String)

@Entity(tableName = "reading_goals")
data class ReadingGoal(@PrimaryKey val id: Long = System.currentTimeMillis(), val period: String = "DAILY", val minutes: Int = 30, val effectiveAt: Long = 0)

@Entity(tableName = "goal_check_ins", primaryKeys = ["period", "start"])
data class GoalCheckIn(val period: String, val start: Long, val end: Long, val target: Int, val seconds: Long = 0, val completedAt: Long? = null)
