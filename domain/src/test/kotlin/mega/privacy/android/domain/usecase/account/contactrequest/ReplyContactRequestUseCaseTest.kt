package mega.privacy.android.domain.usecase.account.contactrequest

import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactRequestAction
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoMoreInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ReplyContactRequestUseCaseTest {

    private lateinit var underTest: ReplyContactRequestUseCase

    private val contactsRepository = mock<ContactsRepository>()

    @BeforeEach
    fun setUp() {
        underTest = ReplyContactRequestUseCase(
            contactsRepository = contactsRepository,
        )
    }

    @Test
    fun `test that invoke manages the sent contact request when the request is outgoing`() =
        runTest {
            underTest(handle = 1L, action = ContactRequestAction.Delete, isOutgoing = true)

            verify(contactsRepository).manageSentContactRequest(1L, ContactRequestAction.Delete)
            verifyNoMoreInteractions(contactsRepository)
        }

    @Test
    fun `test that invoke manages the received contact request when the request is incoming`() =
        runTest {
            underTest(handle = 2L, action = ContactRequestAction.Accept, isOutgoing = false)

            verify(contactsRepository).manageReceivedContactRequest(2L, ContactRequestAction.Accept)
            verifyNoMoreInteractions(contactsRepository)
        }
}
