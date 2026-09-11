package mega.privacy.android.feature.contact.info.view

import androidx.activity.ComponentActivity
import androidx.annotation.StringRes
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.text.DateFormat
import java.util.Date
import mega.privacy.android.shared.resources.R as sharedR

@RunWith(AndroidJUnit4::class)
class ContactInfoChatSectionTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    private val invocations = mutableListOf<String>()

    @Test
    fun `test that tapping the notifications row body invokes no callback`() {
        setSection(isNotificationEnabled = true)

        composeTestRule.onNodeWithTag(CONTACT_INFO_NOTIFICATIONS_ROW_TAG)
            .performTouchInput { click(percentOffset(ROW_BODY_HORIZONTAL_PERCENT, 0.5f)) }

        assertThat(invocations).isEmpty()
    }

    @Test
    fun `test that tapping the notifications row body leaves the toggle unchanged`() {
        setSection(isNotificationEnabled = true)

        composeTestRule.onNodeWithTag(CONTACT_INFO_NOTIFICATIONS_ROW_TAG)
            .performTouchInput { click(percentOffset(ROW_BODY_HORIZONTAL_PERCENT, 0.5f)) }

        composeTestRule.onNodeWithTag(CONTACT_INFO_NOTIFICATIONS_TOGGLE_TAG).assertIsOn()
    }

    @Test
    fun `test that tapping the notifications toggle invokes onNotificationToggled`() {
        setSection(isNotificationEnabled = true)

        composeTestRule.onNodeWithTag(CONTACT_INFO_NOTIFICATIONS_TOGGLE_TAG).performClick()

        assertThat(invocations).containsExactly("onNotificationToggled:false")
    }

    @Test
    fun `test that tapping the toggle invokes onNotificationToggled with true when muted`() {
        setSection(isNotificationEnabled = false)

        composeTestRule.onNodeWithTag(CONTACT_INFO_NOTIFICATIONS_TOGGLE_TAG).performClick()

        assertThat(invocations).containsExactly("onNotificationToggled:true")
    }

    @Test
    fun `test that the notifications toggle is on when notifications are unknown`() {
        setSection(isNotificationEnabled = null)

        composeTestRule.onNodeWithTag(CONTACT_INFO_NOTIFICATIONS_TOGGLE_TAG).assertIsOn()
    }

    @Test
    fun `test that the notifications toggle is off when notifications are muted`() {
        setSection(isNotificationEnabled = false)

        composeTestRule.onNodeWithTag(CONTACT_INFO_NOTIFICATIONS_TOGGLE_TAG).assertIsOff()
    }

    @Test
    fun `test that no notifications subtitle is shown when notifications are enabled`() {
        setSection(isNotificationEnabled = true)

        onNodeWithText(sharedR.string.contact_info_notifications_muted).assertDoesNotExist()
    }

    @Test
    fun `test that the muted subtitle is shown when notifications are muted indefinitely`() {
        setSection(isNotificationEnabled = false, notificationsMutedUntilTimestamp = null)

        onNodeWithText(sharedR.string.contact_info_notifications_muted).assertExists()
    }

    @Test
    fun `test that the muted until subtitle is shown when notifications are muted for a period`() {
        setSection(
            isNotificationEnabled = false,
            notificationsMutedUntilTimestamp = MUTED_UNTIL_TIMESTAMP_SECONDS,
        )

        val expectedTime = DateFormat.getTimeInstance(DateFormat.SHORT)
            .format(Date(MUTED_UNTIL_TIMESTAMP_SECONDS * 1000L))
        composeTestRule.onNodeWithText(
            context.getString(
                sharedR.string.contact_info_notifications_muted_until,
                expectedTime,
            )
        ).assertExists()
    }

    @Test
    fun `test that tapping shared files invokes onSharedFilesClick`() {
        setSection(showSharedFiles = true)

        composeTestRule.onNodeWithTag(CONTACT_INFO_SHARED_FILES_ROW_TAG).performClick()

        assertThat(invocations).containsExactly("onSharedFilesClick")
    }

    @Test
    fun `test that the shared files row is not shown when showSharedFiles is false`() {
        setSection(showSharedFiles = false)

        composeTestRule.onNodeWithTag(CONTACT_INFO_SHARED_FILES_ROW_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that tapping manage chat history invokes onManageChatHistoryClick`() {
        setSection(showManageChatHistory = true)

        composeTestRule.onNodeWithTag(CONTACT_INFO_MANAGE_CHAT_HISTORY_ROW_TAG).performClick()

        assertThat(invocations).containsExactly("onManageChatHistoryClick")
    }

    @Test
    fun `test that the manage chat history row is not shown when showManageChatHistory is false`() {
        setSection(showManageChatHistory = false)

        composeTestRule.onNodeWithTag(CONTACT_INFO_MANAGE_CHAT_HISTORY_ROW_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the manage chat history subtitle shows the retention time when it is set`() {
        setSection(showManageChatHistory = true, retentionTimeSeconds = SECONDS_IN_DAY)

        val expectedDuration = context.resources.getQuantityString(
            sharedR.plurals.contact_info_retention_time_days,
            1,
            1,
        )
        val expectedPrefix =
            context.getString(sharedR.string.contact_info_manage_chat_history_subtitle)
        composeTestRule.onNodeWithText("$expectedPrefix $expectedDuration").assertExists()
    }

    @Test
    fun `test that the manage chat history subtitle is hidden when there is no retention time`() {
        setSection(showManageChatHistory = true, retentionTimeSeconds = null)

        onNodeWithText(sharedR.string.contact_info_manage_chat_history_subtitle)
            .assertDoesNotExist()
    }

    @Test
    fun `test that tapping remove contact invokes onRemoveContactClick`() {
        setSection()

        composeTestRule.onNodeWithTag(CONTACT_INFO_REMOVE_CONTACT_ROW_TAG).performClick()

        assertThat(invocations).containsExactly("onRemoveContactClick")
    }

    private fun setSection(
        isNotificationEnabled: Boolean? = true,
        notificationsMutedUntilTimestamp: Long? = null,
        retentionTimeSeconds: Long? = null,
        showSharedFiles: Boolean = true,
        showManageChatHistory: Boolean = true,
    ) {
        composeTestRule.setContent {
            ContactInfoChatSection(
                isNotificationEnabled = isNotificationEnabled,
                notificationsMutedUntilTimestamp = notificationsMutedUntilTimestamp,
                retentionTimeSeconds = retentionTimeSeconds,
                showSharedFiles = showSharedFiles,
                showManageChatHistory = showManageChatHistory,
                onNotificationToggled = { invocations.add("onNotificationToggled:$it") },
                onSharedFilesClick = { invocations.add("onSharedFilesClick") },
                onManageChatHistoryClick = { invocations.add("onManageChatHistoryClick") },
                onRemoveContactClick = { invocations.add("onRemoveContactClick") },
            )
        }
    }

    private fun onNodeWithText(@StringRes id: Int) =
        composeTestRule.onNodeWithText(context.getString(id))

    private companion object {
        const val ROW_BODY_HORIZONTAL_PERCENT = 0.1f
        const val MUTED_UNTIL_TIMESTAMP_SECONDS = 1893456000L
    }
}
