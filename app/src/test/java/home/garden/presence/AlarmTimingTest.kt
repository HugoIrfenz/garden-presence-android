package home.garden.presence

import org.junit.Assert.*
import org.junit.Test
import java.time.ZonedDateTime

class AlarmTimingTest {
    @Test fun snoozeIsFiveMinutesFromThePressNotOriginalAlarm(){
        val press=1791819000123L
        assertEquals(press+300_000,AlarmTiming.snoozeAt(press))
    }
    @Test fun passedTimeMovesToTomorrow(){
        val now=ZonedDateTime.parse("2026-10-12T08:10:32+07:00[Asia/Jakarta]")
        assertEquals(ZonedDateTime.parse("2026-10-13T07:00:00+07:00[Asia/Jakarta]").toInstant().toEpochMilli(),AlarmTiming.nextTime(now,7,0))
        assertEquals(ZonedDateTime.parse("2026-10-12T09:00:00+07:00[Asia/Jakarta]").toInstant().toEpochMilli(),AlarmTiming.nextTime(now,9,0))
    }
    @Test fun nextDayUsesCalendarAcrossDaylightSavings(){
        val now=ZonedDateTime.parse("2026-03-07T08:00:00-05:00[America/New_York]")
        assertEquals(ZonedDateTime.parse("2026-03-08T07:00:00-04:00[America/New_York]").toInstant().toEpochMilli(),AlarmTiming.nextTime(now,7,0))
    }
    @Test fun staleOrEarlyBroadcastCannotRing(){
        assertFalse(AlarmTiming.deliverable(10000,0))
        assertFalse(AlarmTiming.deliverable(10000,20000))
        assertFalse(AlarmTiming.deliverable(200000,10000))
        assertTrue(AlarmTiming.deliverable(10000,10000))
        assertTrue(AlarmTiming.deliverable(129999,10000))
    }
}
