package `in`.sms.tringapps.core.designsystem.format

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class DurationTest {

    @Test
    fun `zero or negative duration has no label`() {
        assertNull(durationLabelOf(0))
        assertNull(durationLabelOf(-5))
    }

    @Test
    fun `under a minute shows seconds`() {
        assertEquals(DurationLabel.Seconds(45), durationLabelOf(45))
    }

    @Test
    fun `under an hour shows whole minutes`() {
        assertEquals(DurationLabel.Minutes(15), durationLabelOf(959))
        assertEquals(DurationLabel.Minutes(1), durationLabelOf(60))
    }

    @Test
    fun `an hour or more shows hours and minutes`() {
        assertEquals(DurationLabel.HoursMinutes(1, 32), durationLabelOf(5566))
        assertEquals(DurationLabel.HoursMinutes(1, 0), durationLabelOf(3600))
    }
}
