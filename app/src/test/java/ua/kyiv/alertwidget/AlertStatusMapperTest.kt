package ua.kyiv.alertwidget

import org.junit.Assert.assertEquals
import org.junit.Test

class AlertStatusMapperTest {
    @Test fun clearHasNoCauses() {
        assertEquals(AlertStatus.CLEAR, AlertStatusMapper.from(0, emptyList()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun inconsistentClearIsRejected() {
        AlertStatusMapper.from(0, listOf("missile"))
    }

    @Test fun droneOnlyIsYellow() {
        assertEquals(AlertStatus.YELLOW, AlertStatusMapper.from(1, listOf("drone")))
    }

    @Test fun redThreatOverridesDrone() {
        assertEquals(AlertStatus.RED, AlertStatusMapper.from(1, listOf("drone", "ballistic")))
    }

    @Test fun untypedAlertIsUnknown() {
        assertEquals(AlertStatus.ACTIVE_UNKNOWN, AlertStatusMapper.from(1, emptyList()))
    }

    @Test fun unknownThreatDoesNotBecomeYellow() {
        assertEquals(AlertStatus.ACTIVE_UNKNOWN, AlertStatusMapper.from(1, listOf("drone", "mig")))
    }

    @Test(expected = IllegalArgumentException::class)
    fun invalidStateIsRejected() {
        AlertStatusMapper.from(2, emptyList())
    }
}
