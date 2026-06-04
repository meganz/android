package mega.privacy.android.shared.contact.mapper


import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class ContactItemUiStateMapperTest {

    private lateinit var underTest: ContactItemUiStateMapper

    @BeforeEach
    fun setUp() {
        underTest = ContactItemUiStateMapper(
            contactItemStatusMapper = ContactItemStatusMapper(),
            contactItemAvatarMapper = ContactItemAvatarMapper(),
        )
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
