package mega.privacy.android.app.usecase

import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.contacts.usecase.ManageContactRequestUseCase
import mega.privacy.android.domain.entity.contacts.ContactRequestAction
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.verifyNoMoreInteractions

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class ManageContactRequestUseCaseTest {
    private lateinit var underTest: ManageContactRequestUseCase

    private val contactsRepository = mock<ContactsRepository>()

    @BeforeEach
    internal fun setUp() {
        underTest = ManageContactRequestUseCase(
            contactsRepository = contactsRepository,
        )
    }

    @ParameterizedTest
    @MethodSource("getReceivedRequestActions")
    fun `test that receive contact requests are managed correctly`(
        requestAction: ContactRequestAction,
    ) = runTest {
        underTest(requestHandle = 1L, contactRequestAction = requestAction)
        verify(contactsRepository).manageReceivedContactRequest(1L, requestAction)
        verifyNoMoreInteractions(contactsRepository)
    }

    private fun getReceivedRequestActions(): List<ContactRequestAction> = listOf(
        ContactRequestAction.Accept, ContactRequestAction.Ignore, ContactRequestAction.Deny,
    )

    @ParameterizedTest
    @MethodSource("getSentRequestActions")
    fun `test that sent contact requests are managed correctly`(
        requestAction: ContactRequestAction,
    ) = runTest {
        underTest(requestHandle = 1L, contactRequestAction = requestAction)
        verify(contactsRepository).manageSentContactRequest(1L, requestAction)
        verifyNoMoreInteractions(contactsRepository)
    }

    private fun getSentRequestActions(): List<ContactRequestAction> = listOf(
        ContactRequestAction.Remind, ContactRequestAction.Delete,
    )

    @Test
    internal fun `test that exception is thrown if invalid reply contact request action`() =
        runTest {
            assertThrows<IllegalArgumentException> {
                underTest(
                    requestHandle = 1L,
                    contactRequestAction = ContactRequestAction.Add
                )
            }
            verifyNoInteractions(contactsRepository)
        }
}
