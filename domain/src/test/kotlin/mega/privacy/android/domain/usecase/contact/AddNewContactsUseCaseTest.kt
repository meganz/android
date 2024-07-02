package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactData
import mega.privacy.android.domain.entity.contacts.ContactItem
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestStatus
import mega.privacy.android.domain.entity.contacts.UserChatStatus
import mega.privacy.android.domain.entity.user.UserVisibility
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class AddNewContactsUseCaseTest {

    private val contactsRepository: ContactsRepository = mock()

    private lateinit var underTest: AddNewContactsUseCase

    @BeforeEach
    fun setUp() {
        underTest = AddNewContactsUseCase(
            contactsRepository = contactsRepository
        )
    }

    @AfterEach
    fun tearDown() {
        reset(contactsRepository)
    }

    @Test
    fun `test that the correct list of contact items is returned`() = runTest {
        val outdatedContactList = listOf(
            ContactItem(
                handle = 1,
                email = "qwerty@uiop.com",
                contactData = ContactData(
                    "Full Name",
                    "alias",
                    "https://cdn.pixabay.com/photo/2023/05/05/11/07/sweet-7972193_1280.jpg"
                ),
                defaultAvatarColor = "#fff",
                visibility = UserVisibility.Visible,
                timestamp = 1231231,
                areCredentialsVerified = true,
                status = UserChatStatus.Online,
                lastSeen = null,
                chatroomId = null,
            )
        )
        val newContacts = listOf(
            ContactRequest(
                handle = 2L,
                sourceEmail = "test@test.com",
                sourceMessage = null,
                targetEmail = "",
                creationTime = 1L,
                modificationTime = 1L,
                status = ContactRequestStatus.Unresolved,
                isOutgoing = false,
                isAutoAccepted = false,
            )
        )
        val contactItems = listOf(
            ContactItem(
                handle = 1,
                email = "qwerty@uiop.com",
                contactData = ContactData(
                    "Full Name",
                    "alias",
                    "https://cdn.pixabay.com/photo/2023/05/05/11/07/sweet-7972193_1280.jpg"
                ),
                defaultAvatarColor = "#fff",
                visibility = UserVisibility.Visible,
                timestamp = 1231231,
                areCredentialsVerified = true,
                status = UserChatStatus.Online,
                lastSeen = null,
                chatroomId = null,
            ),
            ContactItem(
                handle = 2,
                email = "test@test.com",
                contactData = ContactData(
                    "Full Name",
                    "alias",
                    "https://cdn.pixabay.com/photo/2023/05/05/11/07/sweet-7972193_1280.jpg"
                ),
                defaultAvatarColor = "#fff",
                visibility = UserVisibility.Visible,
                timestamp = 1231231,
                areCredentialsVerified = true,
                status = UserChatStatus.Online,
                lastSeen = null,
                chatroomId = null,
            )
        )
        whenever(
            contactsRepository.addNewContacts(
                outdatedContactList = outdatedContactList,
                newContacts = newContacts
            )
        ) doReturn contactItems

        val actual = underTest(
            outdatedContactList = outdatedContactList,
            newContacts = newContacts
        )

        verify(contactsRepository).addNewContacts(
            outdatedContactList = outdatedContactList,
            newContacts = newContacts
        )
        assertThat(actual).isEqualTo(contactItems)
    }
}
