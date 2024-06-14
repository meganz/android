package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestStatus
import mega.privacy.android.domain.entity.contacts.LocalContact
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.repository.ContactsRepository
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class FilterPendingOrAcceptedLocalContactsByEmailUseCaseTest {

    private lateinit var underTest: FilterPendingOrAcceptedLocalContactsByEmailUseCase

    private val contactsRepository: ContactsRepository = mock()
    private val isContactRequestByEmailInPendingOrAcceptedStateUseCase: IsContactRequestByEmailInPendingOrAcceptedStateUseCase =
        mock()

    private val photoUri = UriPath("photoUri")

    @BeforeEach
    fun setup() {
        underTest = FilterPendingOrAcceptedLocalContactsByEmailUseCase(
            contactsRepository = contactsRepository,
            isContactRequestByEmailInPendingOrAcceptedStateUseCase = isContactRequestByEmailInPendingOrAcceptedStateUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            contactsRepository,
            isContactRequestByEmailInPendingOrAcceptedStateUseCase
        )
    }

    @Test
    fun `test that a successfully filtered list of local contacts is returned`() = runTest {
        val notPendingNorAcceptedEmail = "test@test.com"
        val pendingEmail = "test2@test.com"
        val defaultEmail = "default@email.com"
        val firstUserID = 1L
        val secondUserID = 2L
        val users = listOf(
            ContactRequest(
                handle = 1L,
                sourceEmail = defaultEmail,
                sourceMessage = "",
                targetEmail = defaultEmail,
                creationTime = System.currentTimeMillis(),
                modificationTime = System.currentTimeMillis(),
                status = ContactRequestStatus.Denied,
                isOutgoing = true,
                isAutoAccepted = true
            )
        )
        whenever(contactsRepository.getOutgoingContactRequests()).thenReturn(users)
        whenever(
            isContactRequestByEmailInPendingOrAcceptedStateUseCase(
                any(),
                eq(notPendingNorAcceptedEmail)
            )
        ).thenReturn(false)
        whenever(
            isContactRequestByEmailInPendingOrAcceptedStateUseCase(
                any(),
                eq(pendingEmail)
            )
        ).thenReturn(true)

        val firstUserName = "name1"
        val secondUserName = "name2"
        val localContacts = listOf(
            LocalContact(
                id = firstUserID,
                name = firstUserName,
                emails = listOf(notPendingNorAcceptedEmail, pendingEmail),
                photoUri = photoUri
            ),
            LocalContact(
                id = secondUserID,
                name = secondUserName,
                emails = listOf(pendingEmail, pendingEmail)
            )
        )
        val actual = underTest(localContacts)

        val expected = listOf(
            LocalContact(
                id = firstUserID,
                name = firstUserName,
                emails = listOf(notPendingNorAcceptedEmail),
                photoUri = photoUri
            ),
            LocalContact(
                id = secondUserID,
                name = secondUserName,
                emails = listOf()
            )
        )
        assertThat(actual).isEqualTo(expected)
    }

    @Test
    fun `test that the initial list of emails is returned when no outgoing contact requests are available`() =
        runTest {
            whenever(contactsRepository.getOutgoingContactRequests()).thenReturn(listOf())

            val email = "test@test.com"
            val contactID = 1L
            val userName = "name1"
            val localContacts = listOf(
                LocalContact(
                    id = contactID,
                    name = userName,
                    emails = listOf(email),
                    photoUri = photoUri
                )
            )
            val actual = underTest(localContacts)

            val expected = listOf(
                LocalContact(
                    id = contactID,
                    name = userName,
                    emails = listOf(email),
                    photoUri = photoUri
                )
            )
            assertThat(actual).isEqualTo(expected)
        }
}
