package mega.privacy.android.feature.contact.attachment.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.android.tools.screenshot.PreviewTest
import de.palm.composestateevents.consumed
import kotlinx.collections.immutable.persistentListOf
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.core.nodecomponents.list.NodeActionListTile
import mega.privacy.android.feature.contact.attachment.model.ContactAttachmentItem
import mega.privacy.android.feature.contact.attachment.model.ContactAttachmentPrimaryAction
import mega.privacy.android.feature.contact.attachment.model.ContactAttachmentUiState
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.shared.resources.R as sharedR

class ContactAttachmentScreenScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactAttachmentScreenList() {
        AndroidThemeForPreviews {
            ContactAttachmentScreenUnderTest(
                state = ContactAttachmentUiState.Data(
                    contacts = persistentListOf(
                        ContactAttachmentItem(
                            handle = 1L,
                            email = "alice@example.com",
                            name = "Alice Anderson",
                            isMe = false,
                            isContact = true,
                            isVerified = true,
                            avatarColor = 0xFF2E7D32.toInt(),
                        ),
                        ContactAttachmentItem(
                            handle = 2L,
                            email = "bob@example.com",
                            name = "Bob Brown",
                            isMe = false,
                            isContact = false,
                            isVerified = false,
                            avatarColor = 0xFF1565C0.toInt(),
                        ),
                    ),
                    primaryAction = ContactAttachmentPrimaryAction.Invite,
                    messageEvent = consumed(),
                    openChatEvent = consumed(),
                    openContactInfoEvent = consumed(),
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactAttachmentScreenEmpty() {
        AndroidThemeForPreviews {
            ContactAttachmentScreenUnderTest(
                state = ContactAttachmentUiState.Data(
                    contacts = persistentListOf(),
                    primaryAction = null,
                    messageEvent = consumed(),
                    openChatEvent = consumed(),
                    openContactInfoEvent = consumed(),
                ),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactAttachmentStartConversationAction() {
        AndroidThemeForPreviews {
            Column {
                NodeActionListTile(
                    text = stringResource(sharedR.string.contacts_action_contact_info),
                    icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.Info),
                    onActionClicked = {},
                )
                NodeActionListTile(
                    text = stringResource(sharedR.string.group_chat_start_conversation_label),
                    icon = rememberVectorPainter(IconPack.Medium.Thin.Outline.MessageChatCircle),
                    onActionClicked = {},
                )
            }
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactAttachmentUserNotContactSnackbar() {
        AndroidThemeForPreviews {
            Snackbar(modifier = Modifier.padding(16.dp)) {
                Text(text = stringResource(sharedR.string.alert_user_is_not_contact))
            }
        }
    }

    @Composable
    private fun ContactAttachmentScreenUnderTest(state: ContactAttachmentUiState) {
        ContactAttachmentScreen(
            state = state,
            onNavigateBack = {},
            onPrimaryActionClick = {},
            onContactClicked = {},
            onViewContactInfo = {},
            onStartConversation = {},
            onInviteContact = {},
            onMessageEventConsumed = {},
        )
    }
}
