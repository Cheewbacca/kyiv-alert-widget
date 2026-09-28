package ua.kyiv.alertwidget

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class WidgetStatePolicyTest {
    private val now = TimeUnit.HOURS.toMillis(10)

    @Test fun refreshKeepsTheLastVerifiedAlarm() {
        val state = WidgetStatePolicy.evaluate(AlertStatus.RED, now - 60_000, now, now)
        assertEquals(WidgetLevel.RED, state.level)
        assertTrue(state.refreshing)
        assertFalse(state.stale)
    }

    @Test fun firstCheckHasNoImpliedAllClear() {
        val state = WidgetStatePolicy.evaluate(null, 0, now, now)
        assertEquals(WidgetLevel.UNKNOWN, state.level)
        assertTrue(state.refreshing)
    }

    @Test fun expiredClearCannotStayGreenWhileRefreshing() {
        val state = WidgetStatePolicy.evaluate(
            AlertStatus.CLEAR, now - TimeUnit.MINUTES.toMillis(45), now, now
        )
        assertEquals(WidgetLevel.UNKNOWN, state.level)
        assertTrue(state.refreshing)
        assertTrue(state.stale)
    }

    @Test fun unknownActiveCauseIsTreatedAsAlarm() {
        val state = WidgetStatePolicy.evaluate(AlertStatus.ACTIVE_UNKNOWN, now - 60_000, 0, now)
        assertEquals(WidgetLevel.RED, state.level)
    }

    @Test fun completedRefreshDoesNotLeaveLoadingIndicator() {
        val state = WidgetStatePolicy.evaluate(AlertStatus.YELLOW, now - 60_000, now - 15_000, now)
        assertEquals(WidgetLevel.YELLOW, state.level)
        assertFalse(state.refreshing)
    }
}
