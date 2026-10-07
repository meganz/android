package mega.privacy.android.feature.chat.managehistory.view

import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import mega.privacy.android.feature.chat.managehistory.model.ChatHistoryRetention
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews

/**
 * Screenshot tests for [ManageChatHistoryScreen], covering message clearing turned off, turned on
 * with the period on show, and the meeting wording.
 */
class ManageChatHistoryScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ManageChatHistoryScreenClearingOff() {
        AndroidThemeForPreviews {
            ManageChatHistoryScreen(
                isMeeting = false,
                retention = ChatHistoryRetention.Disabled,
                onBackClick = {},
                onSetRetention = {},
                onClearHistory = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ManageChatHistoryScreenClearingOn() {
        AndroidThemeForPreviews {
            ManageChatHistoryScreen(
                isMeeting = false,
                retention = ChatHistoryRetention.OneWeek,
                onBackClick = {},
                onSetRetention = {},
                onClearHistory = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ManageChatHistoryScreenMeeting() {
        AndroidThemeForPreviews {
            ManageChatHistoryScreen(
                isMeeting = true,
                retention = ChatHistoryRetention.OneDay,
                onBackClick = {},
                onSetRetention = {},
                onClearHistory = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatHistoryRetentionDialogOneDaySelected() {
        AndroidThemeForPreviews {
            ChatHistoryRetentionDialog(
                currentRetention = ChatHistoryRetention.OneDay,
                onRetentionConfirmed = {},
                onCustomRequested = {},
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ChatHistoryRetentionDialogCustomSelected() {
        AndroidThemeForPreviews {
            ChatHistoryRetentionDialog(
                currentRetention = ChatHistoryRetention.Custom(3.hours),
                onRetentionConfirmed = {},
                onCustomRequested = {},
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun CustomRetentionTimePickerThreeDays() {
        AndroidThemeForPreviews {
            val state = remember {
                CustomRetentionTimePickerState().apply { initialiseFrom(3.days) }
            }
            CustomRetentionTimePicker(state = state, onConfirm = {})
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun CustomRetentionTimePickerSixMonths() {
        AndroidThemeForPreviews {
            val state = remember {
                CustomRetentionTimePickerState().apply { initialiseFrom(180.days) }
            }
            CustomRetentionTimePicker(state = state, onConfirm = {})
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ClearChatHistoryDialogChat() {
        AndroidThemeForPreviews {
            ClearChatHistoryDialog(
                isMeeting = false,
                onConfirm = {},
                onDismiss = {},
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ClearChatHistoryDialogMeeting() {
        AndroidThemeForPreviews {
            ClearChatHistoryDialog(
                isMeeting = true,
                onConfirm = {},
                onDismiss = {},
            )
        }
    }
}
