package mega.privacy.android.feature.contact.list.view

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import mega.android.core.ui.components.dialogs.BasicDialog
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.contact.model.ContactItemUiState
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.ContactItemContactInfoMenuItemEvent
import mega.privacy.mobile.analytics.event.ContactItemRemoveContactMenuItemEvent
import mega.privacy.mobile.analytics.event.ContactItemSendMessageMenuItemEvent
import mega.privacy.mobile.analytics.event.ContactItemStartCallMenuItemEvent
import mega.privacy.mobile.analytics.event.ContactItemStartVideoCallMenuItemEvent
import mega.privacy.mobile.analytics.event.RemoveContactConfirmButtonPressedEvent
import mega.privacy.mobile.analytics.event.RemoveContactConfirmationDialogEvent
import mega.privacy.mobile.analytics.event.RemoveContactDismissButtonPressedEvent

/**
 * Contact actions bottom sheet
 *
 * @param contact
 * @param onDismiss
 * @param onSendMessage
 * @param onAudioCall
 * @param onVideoCall
 * @param onContactInfo
 * @param onRemove
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactActionsBottomSheet(
    contact: ContactItemUiState,
    onDismiss: () -> Unit,
    onSendMessage: () -> Unit,
    onAudioCall: () -> Unit,
    onVideoCall: () -> Unit,
    onContactInfo: () -> Unit,
    onRemove: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val close = { action: () -> Unit ->
        coroutineScope
            .launch { sheetState.hide() }
            .invokeOnCompletion {
                onDismiss()
                action()
            }
    }

    MegaModalBottomSheet(
        modifier = Modifier.testTag(CONTACT_ACTIONS_SHEET_TAG),
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        sheetState = sheetState,
        onDismissRequest = onDismiss,
    ) {
        Column {
            NodeActionListTile(
                text = stringResource(sharedR.string.contacts_action_send_message),
                icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.MessageChatCircle),
                onActionClicked = {
                    Analytics.tracker.trackEvent(ContactItemSendMessageMenuItemEvent)
                    close(onSendMessage)
                },
                modifier = Modifier.testTag(CONTACT_ACTION_SEND_MESSAGE_TAG),
            )
            NodeActionListTile(
                text = stringResource(sharedR.string.contacts_action_audio_call),
                icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Phone01),
                onActionClicked = {
                    Analytics.tracker.trackEvent(ContactItemStartCallMenuItemEvent)
                    close(onAudioCall)
                },
                modifier = Modifier.testTag(CONTACT_ACTION_AUDIO_CALL_TAG),
            )
            NodeActionListTile(
                text = stringResource(sharedR.string.contacts_action_video_call),
                icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Video),
                onActionClicked = {
                    Analytics.tracker.trackEvent(ContactItemStartVideoCallMenuItemEvent)
                    close(onVideoCall)
                },
                modifier = Modifier.testTag(CONTACT_ACTION_VIDEO_CALL_TAG),
            )
            if (contact.email.isNotBlank()) {
                NodeActionListTile(
                    text = stringResource(sharedR.string.contacts_action_contact_info),
                    icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Info),
                    onActionClicked = {
                        Analytics.tracker.trackEvent(ContactItemContactInfoMenuItemEvent)
                        close(onContactInfo)
                    },
                    modifier = Modifier.testTag(CONTACT_ACTION_CONTACT_INFO_TAG),
                )
                NodeActionListTile(
                    text = stringResource(sharedR.string.contacts_action_remove_contact),
                    icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.X),
                    isDestructive = true,
                    onActionClicked = {
                        Analytics.tracker.trackEvent(ContactItemRemoveContactMenuItemEvent)
                        close(onRemove)
                    },
                    modifier = Modifier.testTag(CONTACT_ACTION_REMOVE_TAG),
                )
            }
        }
    }
}

@Composable
internal fun RemoveContactDialog(
    displayName: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    LaunchedEffect(Unit) {
        Analytics.tracker.trackEvent(RemoveContactConfirmationDialogEvent)
    }
    BasicDialog(
        modifier = Modifier.testTag(REMOVE_CONTACT_DIALOG_TAG),
        title = stringResource(sharedR.string.contacts_remove_dialog_title, displayName),
        description = stringResource(sharedR.string.contacts_remove_dialog_message),
        positiveButtonText = stringResource(sharedR.string.general_remove),
        negativeButtonText = stringResource(sharedR.string.general_dismiss_dialog),
        onPositiveButtonClicked = {
            Analytics.tracker.trackEvent(RemoveContactConfirmButtonPressedEvent)
            onConfirm()
        },
        onNegativeButtonClicked = {
            Analytics.tracker.trackEvent(RemoveContactDismissButtonPressedEvent)
            onDismiss()
        },
    )
}

internal const val CONTACT_ACTIONS_SHEET_TAG = "contact_actions_sheet"
internal const val CONTACT_ACTION_SEND_MESSAGE_TAG = "contact_actions_sheet:send_message"
internal const val CONTACT_ACTION_AUDIO_CALL_TAG = "contact_actions_sheet:audio_call"
internal const val CONTACT_ACTION_VIDEO_CALL_TAG = "contact_actions_sheet:video_call"
internal const val CONTACT_ACTION_CONTACT_INFO_TAG = "contact_actions_sheet:contact_info"
internal const val CONTACT_ACTION_REMOVE_TAG = "contact_actions_sheet:remove"
internal const val REMOVE_CONTACT_DIALOG_TAG = "contact_actions_sheet:remove_dialog"
