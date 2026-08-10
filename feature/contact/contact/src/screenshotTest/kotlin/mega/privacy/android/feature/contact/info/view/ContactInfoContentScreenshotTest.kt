package mega.privacy.android.feature.contact.info.view

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.android.tools.screenshot.PreviewTest
import de.palm.composestateevents.consumed
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.feature.contact.info.model.ContactInfoUiState
import mega.privacy.android.shared.contact.model.AvatarData

class ContactInfoContentScreenshotTest {

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactInfoContentVerified() {
        AndroidThemeForPreviews {
            ContactInfoContentUnderTest(state = sampleState(areCredentialsVerified = true))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactInfoContentUnverified() {
        AndroidThemeForPreviews {
            ContactInfoContentUnderTest(state = sampleState(areCredentialsVerified = false))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactInfoContentNotificationsMuted() {
        AndroidThemeForPreviews {
            ContactInfoContentUnderTest(state = sampleState(isNotificationEnabled = false))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactInfoContentWithoutNickname() {
        AndroidThemeForPreviews {
            ContactInfoContentUnderTest(state = sampleState(nickname = null))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactInfoContentZeroSharedFolders() {
        AndroidThemeForPreviews {
            ContactInfoContentUnderTest(state = sampleState(inSharesCount = 0))
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactInfoContentNoChat() {
        AndroidThemeForPreviews {
            ContactInfoContentUnderTest(
                state = sampleState(chatRoomId = null, retentionTimeSeconds = null),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactInfoContentRetentionHours() {
        AndroidThemeForPreviews {
            ContactInfoContentUnderTest(
                state = sampleState(retentionTimeSeconds = 12 * SECONDS_IN_HOUR),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactInfoContentRetentionWeeks() {
        AndroidThemeForPreviews {
            ContactInfoContentUnderTest(
                state = sampleState(retentionTimeSeconds = 3 * SECONDS_IN_WEEK),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactInfoContentRetentionMonths() {
        AndroidThemeForPreviews {
            ContactInfoContentUnderTest(
                state = sampleState(retentionTimeSeconds = 2 * SECONDS_IN_MONTH_30),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactInfoContentRetentionYear() {
        AndroidThemeForPreviews {
            ContactInfoContentUnderTest(
                state = sampleState(retentionTimeSeconds = SECONDS_IN_YEAR),
            )
        }
    }

    @PreviewTest
    @CombinedThemePreviews
    @Composable
    fun ContactInfoContentOffline() {
        AndroidThemeForPreviews {
            ContactInfoContentUnderTest(
                state = sampleState(
                    isOnline = false,
                    enableCallButtons = false,
                    userChatStatus = UserChatStatus.Offline,
                ),
            )
        }
    }

    @Composable
    private fun ContactInfoContentUnderTest(state: ContactInfoUiState.Loaded) {
        ContactInfoContent(
            state = state,
            onSendMessageClick = {},
            onStartAudioCallClick = {},
            onStartVideoCallClick = {},
            onNicknameClick = {},
            onVerifyCredentialsClick = {},
            onShareContactClick = {},
            onSharedFoldersClick = {},
            onNotificationToggled = {},
            onSharedFilesClick = {},
            onManageChatHistoryClick = {},
            onRemoveContactClick = {},
        )
    }

    private fun sampleState(
        nickname: String? = "Ally",
        chatRoomId: Long? = 123L,
        userChatStatus: UserChatStatus = UserChatStatus.Online,
        lastSeenMinutes: Int? = null,
        areCredentialsVerified: Boolean = true,
        isNotificationEnabled: Boolean? = true,
        retentionTimeSeconds: Long? = SECONDS_IN_DAY,
        inSharesCount: Int = 3,
        enableCallButtons: Boolean = true,
        isOnline: Boolean = true,
    ) = ContactInfoUiState.Loaded(
        displayName = "Alice Anderson",
        nickname = nickname,
        email = "alice@example.com",
        userHandle = 1L,
        chatRoomId = chatRoomId,
        isFromContacts = true,
        avatar = AvatarData.Initials(initials = "A", avatarColor = Color(0xFF2E7D32)),
        userChatStatus = userChatStatus,
        lastSeenMinutes = lastSeenMinutes,
        areCredentialsVerified = areCredentialsVerified,
        isNotificationEnabled = isNotificationEnabled,
        retentionTimeSeconds = retentionTimeSeconds,
        inSharesCount = inSharesCount,
        enableCallButtons = enableCallButtons,
        isOnline = isOnline,
        closeEvent = consumed,
    )
}
