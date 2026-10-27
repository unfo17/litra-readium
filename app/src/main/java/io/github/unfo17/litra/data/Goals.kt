package io.github.unfo17.litra.data

import androidx.room.withTransaction
import java.time.*
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject
import javax.inject.Singleton

object GoalPeriods {
    data class Range(val start: Long, val end: Long)
    fun range(period: String, now: Long, zone: ZoneId): Range {
        val day = Instant.ofEpochMilli(now).atZone(zone).toLocalDate()
        val start = when (period) {
            "WEEKLY" -> day.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
            "MONTHLY" -> day.withDayOfMonth(1)
            else -> day
        }
        val end = when (period) { "WEEKLY" -> start.plusWeeks(1); "MONTHLY" -> start.plusMonths(1); else -> start.plusDays(1) }
        return Range(start.atStartOfDay(zone).toInstant().toEpochMilli(), end.atStartOfDay(zone).toInstant().toEpochMilli())
    }
    fun label(period: String): String = when (period) { "WEEKLY" -> "每周"; "MONTHLY" -> "每月"; else -> "每天" }
    fun activeRange(goal: ReadingGoal, now: Long, zone: ZoneId): Range {
        val range = range(goal.period, now, zone)
        return range.copy(start = maxOf(range.start, goal.effectiveAt))
    }
}

@Singleton
class GoalsRepository @Inject constructor(private val db: LitraDatabase) {
    private val dao = db.library()
    val goals = dao.goals()
    val checkIns = dao.checkIns()
    val sessions = dao.sessions()
    suspend fun initialize() { db.withTransaction { if (dao.activeGoal(System.currentTimeMillis()) == null) dao.goal(ReadingGoal(id = 0, effectiveAt = 0)) } }
    suspend fun configure(period: String, minutes: Int, now: Long = System.currentTimeMillis(), zone: ZoneId = ZoneId.systemDefault()) {
        require(period in listOf("DAILY", "WEEKLY", "MONTHLY")); require(minutes in 1..1440)
        initialize()
        db.withTransaction {
            val current = dao.activeGoal(now)!!
            val effective = GoalPeriods.activeRange(current, now, zone).end
            dao.goal(ReadingGoal(id = effective, period = period, minutes = minutes, effectiveAt = effective))
        }
    }
    suspend fun record(bookId: String, startedAt: Long, seconds: Long, zone: ZoneId = ZoneId.systemDefault()) {
        if (seconds <= 0) return
        initialize()
        db.withTransaction {
            if (dao.book(bookId) == null) return@withTransaction
            var cursor = startedAt
            val finish = startedAt + seconds * 1000
            var accountedSeconds = 0L
            while (cursor < finish) {
                val goal = dao.activeGoal(cursor) ?: ReadingGoal(id = 0)
                val range = GoalPeriods.activeRange(goal, cursor, zone)
                val dayRange = GoalPeriods.range("DAILY", cursor, zone)
                val pending = dao.pendingGoal(cursor)?.effectiveAt ?: Long.MAX_VALUE
                val end = minOf(finish, range.end, dayRange.end, pending)
                val cumulativeSeconds = (end - startedAt) / 1000
                val duration = cumulativeSeconds - accountedSeconds
                accountedSeconds = cumulativeSeconds
                if (duration > 0) {
                    dao.session(ReadingSession(bookId = bookId, startedAt = cursor, seconds = duration, localDay = Instant.ofEpochMilli(cursor).atZone(zone).toLocalDate().toString()))
                    val old = dao.checkIn(goal.period, range.start) ?: GoalCheckIn(goal.period, range.start, range.end, goal.minutes)
                    val total = old.seconds + duration
                    dao.checkIn(old.copy(seconds = total, completedAt = old.completedAt ?: if (total >= old.target * 60L) end else null))
                }
                cursor = end
            }
        }
    }
}
