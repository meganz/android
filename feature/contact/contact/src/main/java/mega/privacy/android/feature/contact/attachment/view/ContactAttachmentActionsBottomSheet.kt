package mega.privacy.android.feature.contact.attachment.view

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import kotlinx.coroutines.launch
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.feature.contact.attachment.model.ContactAttachmentItem
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Per-row bottom sheet offering the actions available for a single attached contact: view contact
 * info and start a conversation for a visible contact, or send a contact invitation for a
 * non-contact.
 *
 * @param contact
 * @param onDismiss
 * @param onViewContactInfo
 * @param onStartConversation
 * @param onInviteContact
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ContactAttachmentActionsBottomSheet(
    contact: ContactAttachmentItem,
    onDismiss: () -> Unit,
    onViewContactInfo: () -> Unit,
    onStartConversation: () -> Unit,
    onInviteContact: () -> Unit,
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
        modifier = Modifier.testTag(CONTACT_ATTACHMENT_SHEET_TAG),
        bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
        sheetState = sheetState,
        onDismissRequest = onDismiss,
    ) {
        Column {
            if (contact.isContact) {
                NodeActionListTile(
                    text = stringResource(sharedR.string.contacts_action_contact_info),
                    icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Info),
                    onActionClicked = { close(onViewContactInfo) },
                    modifier = Modifier.testTag(CONTACT_ATTACHMENT_SHEET_INFO_TAG),
                )
                NodeActionListTile(
                    text = stringResource(sharedR.string.group_chat_start_conversation_label),
                    icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.MessageChatCircle),
                    onActionClicked = { close(onStartConversation) },
                    modifier = Modifier.testTag(CONTACT_ATTACHMENT_SHEET_START_CONVERSATION_TAG),
                )
            } else if (!contact.isMe) {
                NodeActionListTile(
                    text = stringResource(sharedR.string.menu_add_contact),
                    icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.UserPlus),
                    onActionClicked = { close(onInviteContact) },
                    modifier = Modifier.testTag(CONTACT_ATTACHMENT_SHEET_INVITE_TAG),
                )
            }
        }
    }
}

internal const val CONTACT_ATTACHMENT_SHEET_TAG = "contact_attachment_sheet"
internal const val CONTACT_ATTACHMENT_SHEET_INFO_TAG = "contact_attachment_sheet:contact_info"
internal const val CONTACT_ATTACHMENT_SHEET_START_CONVERSATION_TAG =
    "contact_attachment_sheet:start_conversation"
internal const val CONTACT_ATTACHMENT_SHEET_INVITE_TAG = "contact_attachment_sheet:invite"
