package mega.privacy.android.feature.contact.info.view

import androidx.activity.ComponentActivity
import androidx.annotation.PluralsRes
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.shared.resources.R as sharedR

@RunWith(AndroidJUnit4::class)
class ContactInfoRetentionTimeTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `test that getRetentionTimeString returns null when history clearing is disabled`() {
        assertThat(retentionTimeStrings(0L)).containsExactly(null)
    }

    @Test
    fun `test that getRetentionTimeString returns null when the time is not a whole unit`() {
        assertThat(retentionTimeStrings(SECONDS_IN_HOUR + 1, 1L)).containsExactly(null, null)
    }

    @Test
    fun `test that getRetentionTimeString returns hours when the time is a number of hours`() {
        assertThat(retentionTimeStrings(SECONDS_IN_HOUR, 5 * SECONDS_IN_HOUR))
            .containsExactly(
                quantityString(sharedR.plurals.contact_info_retention_time_hours, 1),
                quantityString(sharedR.plurals.contact_info_retention_time_hours, 5),
            )
            .inOrder()
    }

    @Test
    fun `test that getRetentionTimeString returns days when the time is a number of days`() {
        assertThat(retentionTimeStrings(SECONDS_IN_DAY, 3 * SECONDS_IN_DAY))
            .containsExactly(
                quantityString(sharedR.plurals.contact_info_retention_time_days, 1),
                quantityString(sharedR.plurals.contact_info_retention_time_days, 3),
            )
            .inOrder()
    }

    @Test
    fun `test that getRetentionTimeString returns weeks when the time is a number of weeks`() {
        assertThat(retentionTimeStrings(SECONDS_IN_WEEK, 2 * SECONDS_IN_WEEK))
            .containsExactly(
                quantityString(sharedR.plurals.contact_info_retention_time_weeks, 1),
                quantityString(sharedR.plurals.contact_info_retention_time_weeks, 2),
            )
            .inOrder()
    }

    @Test
    fun `test that getRetentionTimeString returns months when the time is a multiple of 30 days`() {
        assertThat(retentionTimeStrings(SECONDS_IN_MONTH_30, 2 * SECONDS_IN_MONTH_30))
            .containsExactly(
                quantityString(sharedR.plurals.contact_info_retention_time_months, 1),
                quantityString(sharedR.plurals.contact_info_retention_time_months, 2),
            )
            .inOrder()
    }

    @Test
    fun `test that getRetentionTimeString returns years when the time is a number of years`() {
        assertThat(retentionTimeStrings(SECONDS_IN_YEAR, 2 * SECONDS_IN_YEAR, 3 * SECONDS_IN_YEAR))
            .containsExactly(
                quantityString(sharedR.plurals.contact_info_retention_time_years, 1),
                quantityString(sharedR.plurals.contact_info_retention_time_years, 2),
                quantityString(sharedR.plurals.contact_info_retention_time_years, 3),
            )
            .inOrder()
    }

    @Test
    fun `test that getRetentionTimeString returns a pluralised year count when the time is a multiple of a year`() {
        assertThat(retentionTimeStrings(SECONDS_IN_YEAR, 2 * SECONDS_IN_YEAR))
            .containsExactly("1 year", "2 years")
            .inOrder()
    }

    private fun retentionTimeStrings(vararg timesInSeconds: Long): List<String?> {
        val results = MutableList<String?>(timesInSeconds.size) { null }
        composeTestRule.setContent {
            timesInSeconds.forEachIndexed { index, timeInSeconds ->
                results[index] = getRetentionTimeString(timeInSeconds)
            }
        }
        composeTestRule.waitForIdle()
        return results
    }

    private fun quantityString(@PluralsRes id: Int, quantity: Int) =
        context.resources.getQuantityString(id, quantity, quantity)
}
