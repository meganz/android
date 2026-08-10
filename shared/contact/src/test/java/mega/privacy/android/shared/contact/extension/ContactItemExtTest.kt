package mega.privacy.android.shared.contact.extension

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import org.junit.jupiter.api.Test

class ContactItemExtTest {

    @Test
    fun `test that displayName returns alias when alias is non-blank`() {
        val contactItem = createContactItem(alias = "MyAlias", fullName = "Full Name")

        assertThat(contactItem.displayName()).isEqualTo("MyAlias")
    }

    @Test
    fun `test that displayName returns full name when alias is null`() {
        val contactItem = createContactItem(alias = null, fullName = "Full Name")

        assertThat(contactItem.displayName()).isEqualTo("Full Name")
    }

    @Test
    fun `test that displayName returns full name when alias is blank`() {
        val contactItem = createContactItem(alias = "  ", fullName = "Full Name")

        assertThat(contactItem.displayName()).isEqualTo("Full Name")
    }

    @Test
    fun `test that displayName returns email when alias and full name are null`() {
        val contactItem = createContactItem(alias = null, fullName = null)

        assertThat(contactItem.displayName()).isEqualTo("test@example.com")
    }

    @Test
    fun `test that displayName returns email when alias and full name are blank`() {
        val contactItem = createContactItem(alias = "  ", fullName = "  ")

        assertThat(contactItem.displayName()).isEqualTo("test@example.com")
    }

    private fun createContactItem(
        alias: String?,
        fullName: String?,
        email: String = "test@example.com",
    ) = ContactItem(
        handle = 1L,
        email = email,
        contactData = ContactData(
            fullName = fullName,
            alias = alias,
            avatarUri = null,
            userVisibility = UserVisibility.Visible,
        ),
        defaultAvatarColor = null,
        visibility = UserVisibility.Visible,
        timestamp = 0L,
        areCredentialsVerified = false,
        status = UserChatStatus.Offline,
        lastSeen = null,
        chatroomId = null,
    )
}
