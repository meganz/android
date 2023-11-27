package mega.privacy.android.app.presentation.meeting.chat.view.dialog

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import mega.privacy.android.app.R
import mega.privacy.android.core.ui.controls.dialogs.ConfirmationDialogWithRadioButtons
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.domain.entity.chat.ChatPushNotificationMuteOption
import timber.log.Timber

/**
 * The dialog to choose different options of mute push notification.
 */
@Composable
fun MutePushNotificationDialog(
    isMeeting: Boolean = false,
    options: List<ChatPushNotificationMuteOption> = emptyList(),
    onCancel: () -> Unit = {},
    onConfirm: (ChatPushNotificationMuteOption) -> Unit = {},
) =
    ConfirmationDialogWithRadioButtons(
        titleText = getTitle(isMeeting),
        subTitleText = "",
        confirmButtonText = stringResource(id = R.string.general_ok),
        cancelButtonText = stringResource(id = R.string.general_cancel),
        radioOptions = options,
        initialSelectedOption = null,
        onDismissRequest = onCancel,
        onConfirmRequest = onConfirm,
        onOptionSelected = {},
        optionDescriptionMapper = { muteOptionTextMapper(it) }
    )


private fun removeFormatPlaceholder(text: String): String {
    runCatching {
        return text.replace("[A]", "")
            .replace("[/A]", "")
            .replace("[B]", "")
            .replace("[/B]", "")
            .replace("[C]", "")
            .replace("[/C]", "")
    }.onFailure {
        Timber.w(it, "Error replacing text. ")
    }
    return text
}

@Composable
fun muteOptionTextMapper(option: ChatPushNotificationMuteOption): String = when (option) {
    ChatPushNotificationMuteOption.Mute30Minutes -> removeFormatPlaceholder(
        pluralStringResource(
            id = R.plurals.plural_call_ended_messages_minutes,
            count = 30,
            30
        )
    )

    ChatPushNotificationMuteOption.Mute1Hour -> removeFormatPlaceholder(
        pluralStringResource(
            id = R.plurals.plural_call_ended_messages_hours,
            count = 1,
            1
        )
    )

    ChatPushNotificationMuteOption.Mute6Hours -> removeFormatPlaceholder(
        pluralStringResource(
            id = R.plurals.plural_call_ended_messages_hours,
            count = 6,
            6
        )
    )

    ChatPushNotificationMuteOption.Mute24Hours -> removeFormatPlaceholder(
        pluralStringResource(
            id = R.plurals.plural_call_ended_messages_hours,
            count = 24,
            24
        )
    )

    ChatPushNotificationMuteOption.MuteUntilThisMorning ->
        stringResource(id = R.string.mute_chatroom_notification_option_until_this_morning)

    ChatPushNotificationMuteOption.MuteUntilTomorrowMorning ->
        stringResource(id = R.string.mute_chatroom_notification_option_until_tomorrow_morning)

    ChatPushNotificationMuteOption.MuteUntilTurnBackOn ->
        stringResource(id = R.string.mute_chatroom_notification_option_forever)

    else -> ""
}

@Composable
private fun getTitle(isMeeting: Boolean) =
    if (isMeeting)
        stringResource(id = R.string.meetings_mute_notifications_dialog_title)
    else
        stringResource(
            id = R.string.title_dialog_mute_chatroom_notifications
        )

@CombinedThemePreviews
@Composable
private fun MutePushNotificationDialogPreview() {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        MutePushNotificationDialog()
    }
}
