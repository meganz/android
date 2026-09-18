package mega.privacy.android.app.presentation.meeting.chat.view.message.management

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.RetentionTimeUpdatedMessageView
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.SECONDS_IN_DAY
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.SECONDS_IN_HOUR
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.SECONDS_IN_MONTH_30
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.SECONDS_IN_WEEK
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.SECONDS_IN_YEAR
import mega.privacy.android.app.presentation.meeting.chat.view.message.management.getRetentionTimeString
import mega.privacy.android.app.utils.TextUtil
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class RetentionTimeUpdatedMessageViewTest {

    @get:Rule
    var composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val ownerActionFullName = "Owner"

    @Test
    fun `test that retention time updated messages shows correctly when it is disabled`() {
        initComposeRuleContent(retentionTime = 0L)
        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(
                    R.string.retention_history_disabled,
                    ownerActionFullName,
                )
            )
        ).assertExists()
    }

    @Test
    fun `test that retention time updated messages shows correctly when it is enabled or updated for 1 year`() {
        val retentionTime = SECONDS_IN_YEAR

        initComposeRuleContent(retentionTime = retentionTime)

        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(
                    R.string.retention_history_changed_by,
                    ownerActionFullName,
                    getRetentionTimeString(composeTestRule.activity, retentionTime),
                )
            )
        ).assertExists()
    }

    @Test
    fun `test that retention time updated messages shows correctly when it is enabled or updated for several years`() {
        val retentionTime = 2 * SECONDS_IN_YEAR

        initComposeRuleContent(retentionTime = retentionTime)

        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(
                    R.string.retention_history_changed_by,
                    ownerActionFullName,
                    getRetentionTimeString(composeTestRule.activity, retentionTime),
                )
            )
        ).assertExists()
    }

    @Test
    fun `test that getRetentionTimeString returns a pluralised year count when the time is a multiple of a year`() {
        assertThat(getRetentionTimeString(composeTestRule.activity, SECONDS_IN_YEAR))
            .isEqualTo("1 year")
        assertThat(getRetentionTimeString(composeTestRule.activity, 2 * SECONDS_IN_YEAR))
            .isEqualTo("2 years")
        assertThat(getRetentionTimeString(composeTestRule.activity, 3 * SECONDS_IN_YEAR))
            .isEqualTo("3 years")
    }

    @Test
    fun `test that retention time updated messages shows correctly when it is enabled or updated for 1 month`() {
        val retentionTime = SECONDS_IN_MONTH_30

        initComposeRuleContent(retentionTime = retentionTime)

        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(
                    R.string.retention_history_changed_by,
                    ownerActionFullName,
                    getRetentionTimeString(composeTestRule.activity, retentionTime),
                )
            )
        ).assertExists()
    }

    @Test
    fun `test that retention time updated messages shows correctly when it is enabled or updated for 1 week`() {
        val retentionTime = SECONDS_IN_WEEK

        initComposeRuleContent(retentionTime = retentionTime)

        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(
                    R.string.retention_history_changed_by,
                    ownerActionFullName,
                    getRetentionTimeString(composeTestRule.activity, retentionTime),
                )
            )
        ).assertExists()
    }

    @Test
    fun `test that retention time updated messages shows correctly when it is enabled or updated for 1 day`() {
        val retentionTime = SECONDS_IN_DAY

        initComposeRuleContent(retentionTime = retentionTime)

        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(
                    R.string.retention_history_changed_by,
                    ownerActionFullName,
                    getRetentionTimeString(composeTestRule.activity, retentionTime),
                )
            )
        ).assertExists()
    }

    @Test
    fun `test that retention time updated messages shows correctly when it is enabled or updated for 1 hour`() {
        val retentionTime = SECONDS_IN_HOUR

        initComposeRuleContent(retentionTime = retentionTime)

        composeTestRule.onNodeWithText(
            TextUtil.removeFormatPlaceholder(
                composeTestRule.activity.getString(
                    R.string.retention_history_changed_by,
                    ownerActionFullName,
                    getRetentionTimeString(composeTestRule.activity, retentionTime),
                )
            )
        ).assertExists()
    }

    private fun initComposeRuleContent(retentionTime: Long) {
        composeTestRule.setContent {
            RetentionTimeUpdatedMessageView(
                ownerActionFullName = ownerActionFullName,
                newRetentionTime = retentionTime,
            )
        }
    }
}