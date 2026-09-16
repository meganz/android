package mega.privacy.android.feature.chat.list.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.toImmutableList
import mega.android.core.ui.components.dialogs.BasicDialogButton
import mega.android.core.ui.components.dialogs.BasicDialogRadioOption
import mega.android.core.ui.components.dialogs.BasicRadioDialog
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Picker for how long every chat should stay silenced, offered by the do-not-disturb action of the
 * chat list top app bar.
 *
 * The period is applied as soon as it is picked, so the only button is the one that backs out
 * without silencing anything.
 *
 * @param options Mute periods to offer, in the order they should be shown.
 * @param onOptionSelected Called with the period the user picked.
 * @param onDismiss Called when the dialog is dismissed.
 */
@Composable
internal fun ChatDoNotDisturbDialog(
    options: List<ChatPushNotificationMuteOption>,
    onOptionSelected: (ChatPushNotificationMuteOption) -> Unit,
    onDismiss: () -> Unit,
) {
    val radioOptions = options.mapIndexed { index, option ->
        BasicDialogRadioOption(ordinal = index, text = option.label())
    }.toImmutableList()

    BasicRadioDialog(
        modifier = Modifier.testTag(CHAT_DO_NOT_DISTURB_DIALOG_TAG),
        title = SpannableText(
            stringResource(sharedR.string.contact_info_mute_notifications_dialog_title)
        ),
        options = radioOptions,
        buttons = persistentListOf(
            BasicDialogButton(
                text = stringResource(sharedR.string.general_dialog_cancel_button),
                onClick = onDismiss,
            ),
        ),
        onOptionSelected = { option ->
            onOptionSelected(options[option.ordinal])
            onDismiss()
        },
        onDismissRequest = onDismiss,
    )
}

@Composable
private fun ChatPushNotificationMuteOption.label(): String = when (this) {
    ChatPushNotificationMuteOption.Mute30Minutes ->
        pluralStringResource(sharedR.plurals.contact_info_mute_option_minutes, 30, 30)

    ChatPushNotificationMuteOption.Mute1Hour ->
        pluralStringResource(sharedR.plurals.contact_info_mute_option_hours, 1, 1)

    ChatPushNotificationMuteOption.Mute6Hours ->
        pluralStringResource(sharedR.plurals.contact_info_mute_option_hours, 6, 6)

    ChatPushNotificationMuteOption.Mute24Hours ->
        pluralStringResource(sharedR.plurals.contact_info_mute_option_hours, 24, 24)

    ChatPushNotificationMuteOption.MuteUntilThisMorning ->
        stringResource(sharedR.string.contact_info_mute_option_until_this_morning)

    ChatPushNotificationMuteOption.MuteUntilTomorrowMorning ->
        stringResource(sharedR.string.contact_info_mute_option_until_tomorrow_morning)

    ChatPushNotificationMuteOption.MuteUntilTurnBackOn ->
        stringResource(sharedR.string.contact_info_mute_option_until_turned_back_on)

    ChatPushNotificationMuteOption.Mute -> stringResource(sharedR.string.general_mute)
    ChatPushNotificationMuteOption.Unmute -> stringResource(sharedR.string.general_unmute)
}

internal const val CHAT_DO_NOT_DISTURB_DIALOG_TAG = "chat_do_not_disturb_dialog:dialog"

@CombinedThemePreviews
@Composable
private fun ChatDoNotDisturbDialogPreview() {
    AndroidThemeForPreviews {
        ChatDoNotDisturbDialog(
            options = listOf(
                ChatPushNotificationMuteOption.Mute30Minutes,
                ChatPushNotificationMuteOption.Mute1Hour,
                ChatPushNotificationMuteOption.Mute6Hours,
                ChatPushNotificationMuteOption.Mute24Hours,
                ChatPushNotificationMuteOption.MuteUntilTomorrowMorning,
            ),
            onOptionSelected = {},
            onDismiss = {},
        )
    }
}
