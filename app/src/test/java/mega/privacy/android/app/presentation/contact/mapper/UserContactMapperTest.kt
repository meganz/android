package mega.privacy.android.app.presentation.contact.mapper

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.main.megachat.chat.explorer.ContactItemUiState
import mega.privacy.android.domain.entity.Contact
import mega.privacy.android.domain.entity.contacts.User
import mega.privacy.android.domain.entity.contacts.UserContact
import mega.privacy.android.domain.entity.user.UserVisibility
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UserContactMapperTest {

    private lateinit var underTest: UserContactMapper

    @BeforeEach
    fun setUp() {
        underTest = UserContactMapper()
    }

    @Test
    fun `test that the correct mapped contact item ui state is returned`() {
        val email = "test@test.com"
        val contact = Contact(userId = 123L, email = email)
        val user = User(
            handle = 1L,
            email = email,
            visibility = UserVisibility.Visible,
            timestamp = System.currentTimeMillis(),
            userChanges = listOf()
        )
        val userContact = UserContact(contact = contact, user = user)

        val actual = underTest(userContact)

        val expected = ContactItemUiState(contact = contact, user = user)
        assertThat(actual).isEqualTo(expected)
    }
}
