package mega.privacy.android.feature.contact.mapper.list

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.feature.contact.list.mapper.ContactItemUiModelMapper
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.Instant

class ContactItemUiModelMapperTest {

    private lateinit var underTest: ContactItemUiModelMapper

    @BeforeEach
    fun setUp() {
        underTest = ContactItemUiModelMapper()
    }

    @Test
    fun `test that display name uses alias when available`() {
        val contactItem = createContactItem(alias = "MyAlias", fullName = "Full Name")

        val result = underTest(contactItem)

        assertThat(result.displayName).isEqualTo("MyAlias")
    }

    @Test
    fun `test that display name uses fullName when alias is null`() {
        val contactItem = createContactItem(alias = null, fullName = "Full Name")

        val result = underTest(contactItem)

        assertThat(result.displayName).isEqualTo("Full Name")
    }

    @Test
    fun `test that display name uses email when both are null`() {
        val contactItem = createContactItem(alias = null, fullName = null)

        val result = underTest(contactItem)

        assertThat(result.displayName).isEqualTo("test@example.com")
    }

    @Test
    fun `test that isNew is true when within 3 days and no chatroom`() {
        val recentTimestamp = Instant.now().epochSecond - 60 // 1 minute ago
        val contactItem = createContactItem(
            timestamp = recentTimestamp,
            chatroomId = null,
        )

        val result = underTest(contactItem)

        assertThat(result.isNew).isTrue()
    }

    @Test
    fun `test that isNew is false when older than 3 days`() {
        val oldTimestamp = Instant.now().epochSecond - (4 * 24 * 60 * 60) // 4 days ago
        val contactItem = createContactItem(
            timestamp = oldTimestamp,
            chatroomId = null,
        )

        val result = underTest(contactItem)

        assertThat(result.isNew).isFalse()
    }

    @Test
    fun `test that isNew is false when chatroomId exists`() {
        val recentTimestamp = Instant.now().epochSecond - 60 // 1 minute ago
        val contactItem = createContactItem(
            timestamp = recentTimestamp,
            chatroomId = 123L,
        )

        val result = underTest(contactItem)

        assertThat(result.isNew).isFalse()
    }

    @Test
    fun `test that status maps directly from domain`() {
        val contactItem = createContactItem(status = UserChatStatus.Online)

        val result = underTest(contactItem)

        assertThat(result.status).isEqualTo(UserChatStatus.Online)
    }

    private fun createContactItem(
        handle: Long = 1L,
        email: String = "test@example.com",
        alias: String? = "Alias",
        fullName: String? = "Full Name",
        avatarUri: String? = null,
        defaultAvatarColor: String? = "#FF0000",
        status: UserChatStatus = UserChatStatus.Offline,
        lastSeen: Int? = null,
        timestamp: Long = Instant.now().epochSecond,
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
