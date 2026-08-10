package mega.privacy.android.app.presentation.meeting.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.app.presentation.meeting.model.ChatParticipantUiState
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.shared.contact.model.AvatarData
import mega.privacy.android.shared.contact.model.ContactItemUiState

/**
 * Screen-level baselines for the participant row rendered by
 * [ParticipantItemView] in [ChatInfoView]. Covers every [ContactItemStatus]
 * variant so the migration to the core-ui `ContactStatusDot` can be
 * validated against a stable reference set.
 */
class ChatInfoParticipantItemScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ParticipantItemViewOnline() {
        AndroidThemeForPreviews {
            ParticipantItemView(
                participant = sampleParticipant(ContactItemStatus.Online, "A"),
                showDivider = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ParticipantItemViewAway() {
        AndroidThemeForPreviews {
            ParticipantItemView(
                participant = sampleParticipant(ContactItemStatus.Away, "B"),
                showDivider = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ParticipantItemViewBusy() {
        AndroidThemeForPreviews {
            ParticipantItemView(
                participant = sampleParticipant(ContactItemStatus.Busy, "C"),
                showDivider = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ParticipantItemViewOffline() {
        AndroidThemeForPreviews {
            ParticipantItemView(
                participant = sampleParticipant(ContactItemStatus.Offline, "D"),
                showDivider = true,
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ParticipantItemViewUnknown() {
        AndroidThemeForPreviews {
            ParticipantItemView(
                participant = sampleParticipant(ContactItemStatus.Unknown, "E"),
                showDivider = false,
            )
        }
    }

    private fun sampleParticipant(
        status: ContactItemStatus,
        displayName: String,
    ): ChatParticipantUiState = ChatParticipantUiState(
        contactItem = ContactItemUiState(
            handle = 1L,
            displayName = displayName,
            status = status,
            lastSeen = null,
            avatar = AvatarData.Initials(
                initials = displayName,
                avatarColor = Color(0xFF2E7D32),
            ),
            isVerified = false,
        ),
        isMe = false,
        privilege = ChatRoomPermission.Standard,
        email = "user@example.com",
        avatarUpdateTimestamp = null,
    )
}
