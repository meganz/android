package mega.privacy.android.feature.chat.managehistory.view

import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.settings.SettingsActionItem
import mega.android.core.ui.components.settings.SettingsToggleItem
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.chat.managehistory.model.ChatHistoryRetention
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Lets the user decide how long a conversation's messages are kept, and delete them all.
 *
 * @param isMeeting True when the conversation is a meeting, which changes the wording.
 * @param retention How long the chat keeps its messages.
 * @param onBackClick Called when the user navigates back.
 * @param onSetRetention Called with the retention the user settled on.
 * @param onClearHistory Called when the user confirms deleting every message.
 * @param modifier [Modifier]
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ManageChatHistoryScreen(
    isMeeting: Boolean,
    retention: ChatHistoryRetention,
    onBackClick: () -> Unit,
    onSetRetention: (retention: ChatHistoryRetention) -> Unit,
    onClearHistory: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showRetentionDialog by rememberSaveable { mutableStateOf(false) }
    var showClearConfirmation by rememberSaveable { mutableStateOf(false) }
    var showCustomPicker by rememberSaveable { mutableStateOf(false) }
    val pickerState = rememberCustomRetentionTimePickerState()
    val isClearingOn = retention != ChatHistoryRetention.Disabled

    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier
            .fillMaxSize()
            .testTag(MANAGE_CHAT_HISTORY_SCREEN_TAG),
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(MANAGE_CHAT_HISTORY_APP_BAR_TAG),
                title = stringResource(
                    if (isMeeting) {
                        sharedR.string.chat_manage_history_meeting_title
                    } else {
                        sharedR.string.chat_manage_history_title
                    }
                ),
                navigationType = AppBarNavigationType.Back(onBackClick),
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsToggleItem(
                key = HISTORY_CLEARING_KEY,
                title = stringResource(sharedR.string.chat_manage_history_clearing_title),
                subtitle = stringResource(sharedR.string.chat_manage_history_clearing_subtitle),
                checked = isClearingOn,
                onSettingsChanged = { _, enabled ->
                    showCustomPicker = false
                    if (enabled) {
                        showRetentionDialog = true
                    } else {
                        onSetRetention(ChatHistoryRetention.Disabled)
                    }
                },
            )

            retentionTimeLabel(retention.duration)?.let { label ->
                SettingsActionItem(
                    key = RETENTION_PERIOD_KEY,
                    title = stringResource(
                        sharedR.string.chat_manage_history_retention_period_title
                    ),
                    subtitle = label,
                    onClicked = {
                        showCustomPicker = false
                        showRetentionDialog = true
                    },
                )
            }

            if (showCustomPicker) {
                CustomRetentionTimePicker(
                    state = pickerState,
                    onConfirm = { picked ->
                        showCustomPicker = false
                        onSetRetention(ChatHistoryRetention.from(picked))
                    },
                )
            }

            SettingsActionItem(
                key = CLEAR_HISTORY_KEY,
                title = stringResource(
                    if (isMeeting) {
                        sharedR.string.chat_manage_history_clear_meeting_action
                    } else {
                        sharedR.string.chat_manage_history_clear_action
                    }
                ),
                subtitle = stringResource(
                    sharedR.string.chat_manage_history_clear_action_subtitle
                ),
                destructive = true,
                onClicked = { showClearConfirmation = true },
            )
        }
    }

    if (showRetentionDialog) {
        ChatHistoryRetentionDialog(
            currentRetention = retention,
            onRetentionConfirmed = { confirmed ->
                showRetentionDialog = false
                onSetRetention(confirmed)
            },
            onCustomRequested = {
                showRetentionDialog = false
                pickerState.initialiseFrom(retention.duration)
                showCustomPicker = true
            },
            onDismiss = { showRetentionDialog = false },
        )
    }

    if (showClearConfirmation) {
        ClearChatHistoryDialog(
            isMeeting = isMeeting,
            onConfirm = {
                showClearConfirmation = false
                onClearHistory()
            },
            onDismiss = { showClearConfirmation = false },
        )
    }
}

internal const val MANAGE_CHAT_HISTORY_SCREEN_TAG = "manage_chat_history_screen:screen"
internal const val MANAGE_CHAT_HISTORY_APP_BAR_TAG = "manage_chat_history_screen:app_bar"
internal const val HISTORY_CLEARING_KEY = "manage_chat_history_clearing"
internal const val RETENTION_PERIOD_KEY = "manage_chat_history_retention_period"
internal const val CLEAR_HISTORY_KEY = "manage_chat_history_clear"

internal const val HISTORY_CLEARING_TOGGLE_TAG = "settings_$HISTORY_CLEARING_KEY:toggle"
internal const val RETENTION_PERIOD_ROW_TAG = "settings_$RETENTION_PERIOD_KEY:list_item"
internal const val CLEAR_HISTORY_ROW_TAG = "settings_$CLEAR_HISTORY_KEY:list_item"

@CombinedThemePreviews
@Composable
private fun ManageChatHistoryScreenPreview() {
    AndroidThemeForPreviews {
        ManageChatHistoryScreen(
            isMeeting = false,
            retention = ChatHistoryRetention.OneDay,
            onBackClick = {},
            onSetRetention = {},
            onClearHistory = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ManageChatHistoryScreenClearingOffPreview() {
    AndroidThemeForPreviews {
        ManageChatHistoryScreen(
            isMeeting = true,
            retention = ChatHistoryRetention.Disabled,
            onBackClick = {},
            onSetRetention = {},
            onClearHistory = {},
        )
    }
}
