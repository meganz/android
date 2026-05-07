package mega.privacy.android.shared.contact.mapper


import androidx.compose.ui.graphics.Color
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.shared.contact.model.AvatarData
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ContactItemUiStateMapperTest {

    private lateinit var underTest: ContactItemUiStateMapper

    @BeforeEach
    fun setUp() {
        underTest = ContactItemUiStateMapper()
    }

    @Test
    fun `test that display name uses alias when alias is non-blank`() {
        val contactItem = createContactItem(alias = "MyAlias", fullName = "Full Name")

        val result = underTest(contactItem)

        assertThat(result.displayName).isEqualTo("MyAlias")
    }

    @Test
    fun `test that display name uses full name when alias is blank`() {
        val contactItem = createContactItem(alias = "  ", fullName = "Full Name")

        val result = underTest(contactItem)

        assertThat(result.displayName).isEqualTo("Full Name")
    }

    @Test
    fun `test that display name uses email when alias and full name are both null`() {
        val contactItem = createContactItem(
            alias = null,
            fullName = null,
            email = "user@example.com",
        )

        val result = underTest(contactItem)

        assertThat(result.displayName).isEqualTo("user@example.com")
    }

    @Test
    fun `test that last seen is set correctly`() {
        val lastSeen = 123
        val contactItem = createContactItem(lastSeen = lastSeen)

        val result = underTest(contactItem)

        assertThat(result.lastSeen).isEqualTo(lastSeen)
    }

    @Test
    fun `test that avatar is Image when avatarUri is non-null`() {
        val contactItem = createContactItem(avatarUri = "/data/user/0/avatar.jpg")

        val result = underTest(contactItem)

        assertThat(result.avatar).isInstanceOf(AvatarData.Image::class.java)
        assertThat((result.avatar as AvatarData.Image).file.path)
            .isEqualTo("/data/user/0/avatar.jpg")
    }

    @Test
    fun `test that avatar is Initials when avatarUri is null`() {
        val contactItem = createContactItem(
            avatarUri = null,
            fullName = "A",
            defaultAvatarColor = "#2E7D32",
        )

        val result = underTest(contactItem)

        assertThat(result.avatar).isInstanceOf(AvatarData.Initials::class.java)
    }

    @Test
    fun `test that initials avatar parses six-char hex avatar color`() {
        val contactItem = createContactItem(
            avatarUri = null,
            fullName = "A",
            defaultAvatarColor = "#2E7D32",
        )

        val result = underTest(contactItem)

        val initials = result.avatar as AvatarData.Initials
        assertThat(initials.avatarColor).isEqualTo(Color(0xFF2E7D32.toInt()))
    }

    @Test
    fun `test that initials avatar falls back to black when defaultAvatarColor is null`() {
        val contactItem = createContactItem(
            avatarUri = null,
            fullName = "A",
            defaultAvatarColor = null,
        )

        val result = underTest(contactItem)

        val initials = result.avatar as AvatarData.Initials
        assertThat(initials.avatarColor).isEqualTo(Color.Black)
    }

    @Test
    fun `test that initials avatar falls back to black when defaultAvatarColor is malformed`() {
        val contactItem = createContactItem(
            avatarUri = null,
            fullName = "A",
            defaultAvatarColor = "not-a-color",
        )

        val result = underTest(contactItem)

        val initials = result.avatar as AvatarData.Initials
        assertThat(initials.avatarColor).isEqualTo(Color.Black)
    }

    @Test
    fun `test that isVerified mirrors areCredentialsVerified true`() {
        val contactItem = createContactItem(areCredentialsVerified = true)

        val result = underTest(contactItem)

        assertThat(result.isVerified).isTrue()
    }

    @Test
    fun `test that isVerified mirrors areCredentialsVerified false`() {
        val contactItem = createContactItem(areCredentialsVerified = false)

        val result = underTest(contactItem)

        assertThat(result.isVerified).isFalse()
    }

    private fun createContactItem(
        handle: Long = 1L,
        email: String = "test@example.com",
        alias: String? = null,
        fullName: String? = "A",
        avatarUri: String? = null,
        defaultAvatarColor: String? = "#FF0000",
        status: UserChatStatus = UserChatStatus.Offline,
        lastSeen: Int? = null,
        timestamp: Long = 0L,
        chatroomId: Long? = null,
        areCredentialsVerified: Boolean = false,
    ) = ContactItem(
        handle = handle,
        email = email,
        contactData = ContactData(
            fullName = fullName,
            alias = alias,
            avatarUri = avatarUri,
            userVisibility = UserVisibility.Visible,
        ),
        defaultAvatarColor = defaultAvatarColor,
        visibility = UserVisibility.Visible,
        timestamp = timestamp,
        areCredentialsVerified = areCredentialsVerified,
        status = status,
        lastSeen = lastSeen,
        chatroomId = chatroomId,
    )
}
