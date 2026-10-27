package io.github.unfo17.litra

import io.github.unfo17.litra.data.*
import java.time.*
import org.junit.Assert.*
import org.junit.Test

class GoalPeriodsTest {
    private val zone = ZoneId.of("Asia/Shanghai")
    private fun time(value: String) = LocalDateTime.parse(value).atZone(zone).toInstant().toEpochMilli()
    @Test fun weekStartsMondayAndHandlesYearBoundary() {
        val r = GoalPeriods.range("WEEKLY", time("2027-01-03T23:59:00"), zone)
        assertEquals(time("2026-12-28T00:00:00"), r.start)
        assertEquals(time("2027-01-04T00:00:00"), r.end)
    }
    @Test fun monthHandlesLeapDay() {
        val r = GoalPeriods.range("MONTHLY", time("2028-02-29T22:00:00"), zone)
        assertEquals(time("2028-02-01T00:00:00"), r.start)
        assertEquals(time("2028-03-01T00:00:00"), r.end)
    }
    @Test fun dailyUsesLocalMidnightAcrossDst() {
        val ny = ZoneId.of("America/New_York")
        val now = ZonedDateTime.of(2026, 3, 8, 12, 0, 0, 0, ny).toInstant().toEpochMilli()
        val r = GoalPeriods.range("DAILY", now, ny)
        assertEquals(23L * 3600000, r.end - r.start)
    }
    @Test fun changedPeriodDoesNotIncludeTimeBeforeEffectiveDate() {
        val now = time("2026-10-28T12:00:00")
        val effective = time("2026-10-28T00:00:00")
        val goal = ReadingGoal(period = "WEEKLY", effectiveAt = effective)
        assertEquals(effective, GoalPeriods.activeRange(goal, now, zone).start)
    }
}
