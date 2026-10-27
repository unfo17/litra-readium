package io.github.unfo17.litra

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import io.github.unfo17.litra.data.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import java.time.*

class GoalPersistenceTest {
    @Test fun goalEditsApplyNextPeriodAndMidnightSplitsSessions() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, LitraDatabase::class.java).build()
        try {
            val dao = db.library()
            val repo = GoalsRepository(db)
            val zone = ZoneId.of("Asia/Shanghai")
            val noon = LocalDate.of(2026, 10, 27).atTime(12, 0).atZone(zone).toInstant().toEpochMilli()
            val midnight = LocalDate.of(2026, 10, 28).atStartOfDay(zone).toInstant().toEpochMilli()
            val book = Book(title = "Goal test", authors = "", fileName = "goal.epub", hash = "goal-test")
            dao.insert(book)
            repo.configure("DAILY", 1, noon, zone)
            assertEquals(30, dao.activeGoal(noon)!!.minutes)
            assertEquals(1, dao.activeGoal(midnight)!!.minutes)
            repo.record(book.id, midnight - 60000, 120, zone)
            val checkIns = dao.checkIns().first()
            assertEquals(2, checkIns.size)
            val previous = checkIns.first { it.start < midnight }
            val next = checkIns.first { it.start == midnight }
            assertEquals(30, previous.target)
            assertEquals(60L, previous.seconds)
            assertNull(previous.completedAt)
            assertEquals(1, next.target)
            assertNotNull(next.completedAt)
            repo.configure("WEEKLY", 90, midnight + 120000, zone)
            repo.record(book.id, midnight + 120000, 60, zone)
            assertEquals(1, dao.checkIn("DAILY", midnight)!!.target)
            assertEquals(next.completedAt, dao.checkIn("DAILY", midnight)!!.completedAt)
            assertEquals(3, dao.sessions().first().size)
            val before = dao.sessions().first().sumOf { it.seconds }
            repo.record(book.id, midnight - 1501, 3, zone)
            assertEquals(3, dao.sessions().first().sumOf { it.seconds } - before)
        } finally { db.close() }
    }
}
