package mega.privacy.android.app.presentation.meeting.mapper

import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.components.contact.state.ContactItemStatus
import mega.privacy.android.domain.entity.ChatRoomPermission
import mega.privacy.android.domain.entity.chat.ChatParticipant
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.shared.contact.mapper.ContactItemStatusMapper
import mega.privacy.android.shared.contact.model.AvatarData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ChatParticipantUiStateMapperTest {

    private lateinit var underTest: ChatParticipantUiStateMapper

    @BeforeEach
    fun setUp() {
        underTest = ChatParticipantUiStateMapper(
            contactItemStatusMapper = ContactItemStatusMapper(),
        )
    }

    @Test
    fun `test that display name uses alias when alias is non-null`() {
        val result = underTest(
            createParticipant(alias = "MyAlias", fullName = "Full Name", email = "u@e.com")
        )

        assertThat(result.contactItem.displayName).isEqualTo("MyAlias")
    }

    @Test
    fun `test that display name uses full name when alias is null`() {
        val result = underTest(
            createParticipant(alias = null, fullName = "Full Name", email = "u@e.com")
        )

        assertThat(result.contactItem.displayName).isEqualTo("Full Name")
    }

    @Test
    fun `test that display name uses email when alias and full name are null`() {
        val result = underTest(
            createParticipant(alias = null, fullName = null, email = "u@e.com")
        )

        assertThat(result.contactItem.displayName).isEqualTo("u@e.com")
    }

    @Test
    fun `test that display name falls back to empty when all sources are null`() {
        val result = underTest(
            createParticipant(alias = null, fullName = null, email = null)
        )

        assertThat(result.contactItem.displayName).isEqualTo("")
    }

    @Test
    fun `test that avatar is Image when avatarUri is non-null`() {
        val result = underTest(
            createParticipant(avatarUri = "/data/user/0/avatar.jpg")
        )

        assertThat(result.contactItem.avatar).isInstanceOf(AvatarData.Image::class.java)
        assertThat((result.contactItem.avatar as AvatarData.Image).file.path)
            .isEqualTo("/data/user/0/avatar.jpg")
    }

    @Test
    fun `test that avatar is Initials when avatarUri is null`() {
        val result = underTest(
            createParticipant(avatarUri = null, defaultAvatarColor = 0xFF2E7D32.toInt())
        )

        val avatar = result.contactItem.avatar
        assertThat(avatar).isInstanceOf(AvatarData.Initials::class.java)
        assertThat((avatar as AvatarData.Initials).avatarColor)
            .isEqualTo(Color(0xFF2E7D32.toInt()))
    }

    @Test
    fun `test that status maps Online correctly`() {
        val result = underTest(createParticipant(status = UserChatStatus.Online))

        assertThat(result.contactItem.status).isEqualTo(ContactItemStatus.Online)
    }

    @Test
    fun `test that status maps Invalid to Unknown`() {
        val result = underTest(createParticipant(status = UserChatStatus.Invalid))

        assertThat(result.contactItem.status).isEqualTo(ContactItemStatus.Unknown)
    }

    @Test
    fun `test that wrapper fields mirror the source participant`() {
        val result = underTest(
            createParticipant(
                handle = 42L,
                email = "x@example.com",
                isMe = true,
                privilege = ChatRoomPermission.Moderator,
                avatarUpdateTimestamp = 1700000000L,
            )
        )

        assertThat(result.handle).isEqualTo(42L)
        assertThat(result.email).isEqualTo("x@example.com")
        assertThat(result.isMe).isTrue()
        assertThat(result.privilege).isEqualTo(ChatRoomPermission.Moderator)
        assertThat(result.avatarUpdateTimestamp).isEqualTo(1700000000L)
    }

    @Test
    fun `test that isVerified mirrors areCredentialsVerified`() {
        val result = underTest(createParticipant(areCredentialsVerified = true))

        assertThat(result.contactItem.isVerified).isTrue()
    }

    @Test
    fun `test that lastSeen mirrors source`() {
        val result = underTest(createParticipant(lastSeen = 42))

        assertThat(result.contactItem.lastSeen).isEqualTo(42)
    }

    private fun createParticipant(
        handle: Long = 1L,
        alias: String? = null,
        fullName: String? = "Alice",
        email: String? = "alice@example.com",
        avatarUri: String? = null,
        defaultAvatarColor: Int = 0xFF2E7D32.toInt(),
        status: UserChatStatus = UserChatStatus.Offline,
        lastSeen: Int? = null,
        avatarUpdateTimestamp: Long? = null,
        privilege: ChatRoomPermission = ChatRoomPermission.Standard,
        isMe: Boolean = false,
        areCredentialsVerified: Boolean = false,
    ) = ChatParticipant(
        handle = handle,
        data = ContactData(
            fullName = fullName,
            alias = alias,
            avatarUri = avatarUri,
            userVisibility = UserVisibility.Visible,
        ),
        email = email,
        isMe = isMe,
        privilege = privilege,
        defaultAvatarColor = defaultAvatarColor,
        areCredentialsVerified = areCredentialsVerified,
        status = status,
        lastSeen = lastSeen,
        avatarUpdateTimestamp = avatarUpdateTimestamp,
    )
}
