package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.ContactRequest
import mega.privacy.android.domain.entity.contacts.ContactRequestStatus
import mega.privacy.android.domain.usecase.account.contactrequest.GetOutgoingContactRequestsUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class IsEmailInPendingStateUseCaseTest {

    private lateinit var underTest: IsEmailInPendingStateUseCase

    private lateinit var isContactRequestByEmailInPendingOrAcceptedStateUseCase: IsContactRequestByEmailInPendingOrAcceptedStateUseCase
    private val getOutgoingContactRequestsUseCase: GetOutgoingContactRequestsUseCase = mock()

    @BeforeEach
    fun setUp() {
        isContactRequestByEmailInPendingOrAcceptedStateUseCase =
            IsContactRequestByEmailInPendingOrAcceptedStateUseCase()
        underTest = IsEmailInPendingStateUseCase(
            isContactRequestByEmailInPendingOrAcceptedStateUseCase = isContactRequestByEmailInPendingOrAcceptedStateUseCase,
            getOutgoingContactRequestsUseCase = getOutgoingContactRequestsUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(getOutgoingContactRequestsUseCase)
    }

    @Test
    fun `test that True is returned when the email is in a pending state`() = runTest {
        val email = "email@email.com"
        val contactRequest = ContactRequest(
            handle = 2L,
            sourceEmail = "source@email.com",
            sourceMessage = null,
            targetEmail = email,
            creationTime = 1L,
            modificationTime = 1L,
            status = ContactRequestStatus.Unresolved,
            isOutgoing = false,
            isAutoAccepted = false,
        )
        whenever(getOutgoingContactRequestsUseCase()) doReturn listOf(contactRequest)

        val actual = underTest(email)

        assertThat(actual).isTrue()
    }

    @Test
    fun `test that False is returned when the email is not in a pending state`() =
        runTest {
            val email = "email@email.com"
            val contactRequest = ContactRequest(
                handle = 2L,
                sourceEmail = "source@email.com",
                sourceMessage = null,
                targetEmail = "target@email.com",
                creationTime = 1L,
                modificationTime = 1L,
                status = ContactRequestStatus.Unresolved,
                isOutgoing = false,
                isAutoAccepted = false,
            )
            whenever(getOutgoingContactRequestsUseCase()) doReturn listOf(contactRequest)

            val actual = underTest(email)

            assertThat(actual).isFalse()
        }
}
