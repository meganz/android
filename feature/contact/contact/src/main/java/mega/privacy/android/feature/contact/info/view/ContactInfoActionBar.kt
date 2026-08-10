package mega.privacy.android.feature.contact.info.view

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.SecondaryLargeIconButton
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Row with the three chat actions of the contact info screen: send message, start an audio call
 * and start a video call.
 *
 * @param callButtonsEnabled False disables the audio/video call buttons (e.g. while a call is
 * already in progress); the send message button is always enabled.
 * @param onSendMessageClick
 * @param onStartAudioCallClick
 * @param onStartVideoCallClick
 * @param modifier
 */
@Composable
internal fun ContactInfoActionBar(
    callButtonsEnabled: Boolean,
    onSendMessageClick: () -> Unit,
    onStartAudioCallClick: () -> Unit,
    onStartVideoCallClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp)
            .testTag(CONTACT_INFO_ACTION_BAR_TAG),
    ) {
        ActionBarItem(
            modifier = Modifier
                .weight(1f)
                .testTag(CONTACT_INFO_ACTION_MESSAGE_TAG),
            icon = IconPack.Medium.Thin.Outline.MessageChatCircle,
            label = stringResource(sharedR.string.contacts_action_send_message),
            enabled = true,
            onClick = onSendMessageClick,
        )
        ActionBarItem(
            modifier = Modifier
                .weight(1f)
                .testTag(CONTACT_INFO_ACTION_AUDIO_CALL_TAG),
            icon = IconPack.Medium.Thin.Outline.Phone01,
            label = stringResource(sharedR.string.contacts_action_audio_call),
            enabled = callButtonsEnabled,
            onClick = onStartAudioCallClick,
        )
        ActionBarItem(
            modifier = Modifier
                .weight(1f)
                .testTag(CONTACT_INFO_ACTION_VIDEO_CALL_TAG),
            icon = IconPack.Medium.Thin.Outline.Video,
            label = stringResource(sharedR.string.contacts_action_video_call),
            enabled = callButtonsEnabled,
            onClick = onStartVideoCallClick,
        )
    }
}

@Composable
private fun ActionBarItem(
    icon: ImageVector,
    label: String,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        SecondaryLargeIconButton(
            icon = rememberVectorPainter(icon),
            onClick = onClick,
            enabled = enabled,
            contentDescription = label,
        )
        MegaText(
            text = label,
            textColor = if (enabled) TextColor.Primary else TextColor.Disabled,
            style = AppTheme.typography.bodySmall,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

internal const val CONTACT_INFO_ACTION_BAR_TAG = "contact_info_action_bar"
internal const val CONTACT_INFO_ACTION_MESSAGE_TAG = "contact_info_action_bar:button_message"
internal const val CONTACT_INFO_ACTION_AUDIO_CALL_TAG = "contact_info_action_bar:button_audio_call"
internal const val CONTACT_INFO_ACTION_VIDEO_CALL_TAG = "contact_info_action_bar:button_video_call"

@CombinedThemePreviews
@Composable
private fun ContactInfoActionBarPreview() {
    AndroidThemeForPreviews {
        ContactInfoActionBar(
            callButtonsEnabled = true,
            onSendMessageClick = {},
            onStartAudioCallClick = {},
            onStartVideoCallClick = {},
        )
    }
}

@CombinedThemePreviews
@Composable
private fun ContactInfoActionBarCallsDisabledPreview() {
    AndroidThemeForPreviews {
        ContactInfoActionBar(
            callButtonsEnabled = false,
            onSendMessageClick = {},
            onStartAudioCallClick = {},
            onStartVideoCallClick = {},
        )
    }
}
