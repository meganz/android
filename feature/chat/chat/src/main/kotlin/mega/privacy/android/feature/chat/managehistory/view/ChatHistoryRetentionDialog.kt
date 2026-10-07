package mega.privacy.android.feature.chat.managehistory.view

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.dialogs.BasicDialogRadioOption
import mega.android.core.ui.components.dialogs.BasicRadioDialog
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.feature.chat.managehistory.model.ChatHistoryRetention
import mega.privacy.android.shared.resources.R as sharedR
import kotlin.time.Duration

/**
 * The presets the dialog offers, in the order they are listed. "Custom" is deliberately absent: it
 * is not a retention but a request to pick one, so it is the last row and handled separately.
 */
private val PRESETS = listOf(
    ChatHistoryRetention.Disabled,
    ChatHistoryRetention.OneDay,
    ChatHistoryRetention.OneWeek,
    ChatHistoryRetention.OneMonth,
)

private const val CUSTOM_INDEX = 4

/**
 * Offers the preset periods after which messages are deleted, plus the option to pick a period of
 * the user's own.
 *
 * @param currentRetention Retention the chat is on, which decides the preselection.
 * @param onRetentionConfirmed Called with the preset the user settled on.
 * @param onCustomRequested Called when the user chose to pick a period of their own.
 * @param onDismiss Called when the dialog is dismissed without a choice.
 * @param modifier [Modifier]
 */
@Composable
internal fun ChatHistoryRetentionDialog(
    currentRetention: ChatHistoryRetention,
    onRetentionConfirmed: (ChatHistoryRetention) -> Unit,
    onCustomRequested: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedIndex by rememberSaveable {
        mutableIntStateOf(PRESETS.indexOf(currentRetention).takeIf { it >= 0 } ?: CUSTOM_INDEX)
    }
    val options = (PRESETS.map { it.label() } + stringResource(
        sharedR.string.chat_manage_history_retention_option_custom
    )).mapIndexed { index, text ->
        BasicDialogRadioOption(ordinal = index, text = text)
    }.toImmutableList()
    val customSelected = selectedIndex == CUSTOM_INDEX
    val confirmEnabled = currentRetention != ChatHistoryRetention.Disabled ||
            selectedIndex != PRESETS.indexOf(ChatHistoryRetention.Disabled)

    BasicRadioDialog(
        modifier = modifier.testTag(CHAT_HISTORY_RETENTION_DIALOG_TAG),
        title = SpannableText(stringResource(sharedR.string.chat_manage_history_clearing_title)),
        description = SpannableText(
            stringResource(sharedR.string.chat_manage_history_retention_period_title)
        ),
        options = options,
        selectedOption = options[selectedIndex],
        onOptionSelected = { selectedIndex = it.ordinal },
        buttons = persistentListOf(
            BasicDialogButton(
                text = stringResource(sharedR.string.general_dialog_cancel_button),
                onClick = onDismiss,
            ),
            BasicDialogButton(
                text = if (customSelected) {
                    stringResource(sharedR.string.chat_manage_history_retention_next_button)
                } else {
                    stringResource(sharedR.string.general_ok_only)
                },
                enabled = confirmEnabled,
                onClick = {
                    if (customSelected) {
                        onCustomRequested()
                    } else {
                        onRetentionConfirmed(PRESETS[selectedIndex])
                    }
                },
            ),
        ),
        onDismissRequest = onDismiss,
    )
}

@Composable
private fun ChatHistoryRetention.label(): String = when (this) {
    ChatHistoryRetention.Disabled ->
        stringResource(sharedR.string.chat_manage_history_retention_option_disabled)

    ChatHistoryRetention.OneDay ->
        stringResource(sharedR.string.chat_manage_history_retention_option_one_day)

    ChatHistoryRetention.OneWeek ->
        stringResource(sharedR.string.chat_manage_history_retention_option_one_week)

    ChatHistoryRetention.OneMonth ->
        stringResource(sharedR.string.chat_manage_history_retention_option_one_month)

    is ChatHistoryRetention.Custom ->
        stringResource(sharedR.string.chat_manage_history_retention_option_custom)
}

internal const val CHAT_HISTORY_RETENTION_DIALOG_TAG =
    "chat_history_retention_dialog:dialog"

@CombinedThemePreviews
@Composable
private fun ChatHistoryRetentionDialogPreview() {
    AndroidThemeForPreviews {
        ChatHistoryRetentionDialog(
            currentRetention = ChatHistoryRetention.OneDay,
            onRetentionConfirmed = {},
            onCustomRequested = {},
            onDismiss = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ChatHistoryRetentionDialogCustomSelectedPreview() {
    AndroidThemeForPreviews {
        ChatHistoryRetentionDialog(
            currentRetention = ChatHistoryRetention.Custom(Duration.ZERO),
            onRetentionConfirmed = {},
            onCustomRequested = {},
            onDismiss = {},
        )
    }
}
