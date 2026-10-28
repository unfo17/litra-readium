package io.github.unfo17.litra.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface LibraryDao {
    @Update suspend fun update(book: Book)
    @Query("SELECT * FROM bookmarks") suspend fun allBookmarks(): List<Bookmark>
    @Query("SELECT * FROM goal_check_ins") suspend fun allCheckIns(): List<GoalCheckIn>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun restoreBookmark(value: Bookmark)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun restoreSession(value: ReadingSession)
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun restoreGoal(value: ReadingGoal)
    @Query("SELECT * FROM books ORDER BY COALESCE(lastReadAt, createdAt) DESC") fun books(): Flow<List<Book>>
    @Query("SELECT * FROM books WHERE id = :id") suspend fun book(id: String): Book?
    @Query("SELECT * FROM books WHERE hash = :hash") suspend fun byHash(hash: String): Book?
    @Insert suspend fun insert(book: Book)
    @Query("UPDATE books SET locator = :locator, progress = :progress, lastReadAt = :now, status = CASE WHEN status = 'FINISHED' THEN status ELSE 'READING' END WHERE id = :id")
    suspend fun saveProgress(id: String, locator: String, progress: Double, now: Long)
    @Query("UPDATE books SET status = 'FINISHED', finishedAt = COALESCE(finishedAt, :now) WHERE id = :id") suspend fun finish(id: String, now: Long)
    @Query("UPDATE books SET status = 'WANT_TO_READ' WHERE id = :id AND status != 'FINISHED'") suspend fun want(id: String)
    @Query("UPDATE books SET title = :title, authors = :authors, kind = :kind WHERE id = :id") suspend fun updateMetadata(id: String, title: String, authors: String, kind: String)
    @Delete suspend fun delete(book: Book)
    @Query("SELECT * FROM categories ORDER BY name") fun categories(): Flow<List<Category>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun category(category: Category)
    @Query("DELETE FROM categories WHERE id IN (:ids)") suspend fun deleteCategories(ids: List<String>)
    @Query("SELECT * FROM book_categories") fun memberships(): Flow<List<BookCategory>>
    @Insert(onConflict = OnConflictStrategy.IGNORE) suspend fun addToCategory(value: BookCategory)
    @Query("DELETE FROM book_categories WHERE bookId = :bookId AND categoryId = :categoryId") suspend fun removeFromCategory(bookId: String, categoryId: String)
    @Query("SELECT * FROM search_history ORDER BY usedAt DESC LIMIT 10") fun history(): Flow<List<SearchEntry>>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun addSearch(entry: SearchEntry)
    @Query("DELETE FROM search_history WHERE query NOT IN (SELECT query FROM search_history ORDER BY usedAt DESC LIMIT 10)") suspend fun trimHistory()
    @Transaction suspend fun rememberSearch(query: String) { addSearch(SearchEntry(query)); trimHistory() }
    @Query("DELETE FROM search_history") suspend fun clearHistory()
    @Query("SELECT * FROM bookmarks WHERE bookId = :bookId ORDER BY createdAt DESC") fun bookmarks(bookId: String): Flow<List<Bookmark>>
    @Insert suspend fun addBookmark(bookmark: Bookmark)
    @Delete suspend fun deleteBookmark(bookmark: Bookmark)
    @Insert suspend fun session(session: ReadingSession)
    @Query("SELECT * FROM reading_sessions ORDER BY startedAt DESC") fun sessions(): Flow<List<ReadingSession>>
    @Query("SELECT * FROM reading_goals ORDER BY effectiveAt DESC") fun goals(): Flow<List<ReadingGoal>>
    @Query("SELECT * FROM reading_goals WHERE effectiveAt <= :now ORDER BY effectiveAt DESC LIMIT 1") suspend fun activeGoal(now: Long): ReadingGoal?
    @Query("SELECT * FROM reading_goals WHERE effectiveAt > :now ORDER BY effectiveAt LIMIT 1") suspend fun pendingGoal(now: Long): ReadingGoal?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun goal(goal: ReadingGoal)
    @Query("SELECT * FROM goal_check_ins ORDER BY start DESC LIMIT 100") fun checkIns(): Flow<List<GoalCheckIn>>
    @Query("SELECT * FROM goal_check_ins WHERE period = :period AND start = :start") suspend fun checkIn(period: String, start: Long): GoalCheckIn?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun checkIn(checkIn: GoalCheckIn)
}

@Database(entities = [Book::class, Category::class, BookCategory::class, SearchEntry::class, Bookmark::class, ReadingSession::class, ReadingGoal::class, GoalCheckIn::class], version = 1, exportSchema = true)
abstract class LitraDatabase : RoomDatabase() { abstract fun library(): LibraryDao }
