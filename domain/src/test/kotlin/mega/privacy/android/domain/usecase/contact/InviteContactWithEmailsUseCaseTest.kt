package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.InviteContactRequest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class InviteContactWithEmailsUseCaseTest {

    private lateinit var underTest: InviteContactWithEmailsUseCase

    private val inviteContactWithEmailUseCase: InviteContactWithEmailUseCase = mock()

    private val defaultDispatcher = UnconfinedTestDispatcher()

    @BeforeEach
    fun setUp() {
        underTest = InviteContactWithEmailsUseCase(
            inviteContactWithEmailUseCase = inviteContactWithEmailUseCase,
            defaultDispatcher = defaultDispatcher
        )
    }

    @AfterEach
    fun tearDown() {
        reset(inviteContactWithEmailUseCase)
    }

    @Test
    fun `test that the correct list of contact request results is returned after successfully inviting contacts by email`() =
        runTest {
            val emails = listOf(
                "email1@email.com",
                "email2@email.com",
                "email3@email.com",
                "email4@email.com",
            )
            whenever(inviteContactWithEmailUseCase(emails[0])) doReturn InviteContactRequest.Sent
            whenever(inviteContactWithEmailUseCase(emails[1])) doThrow RuntimeException()
            whenever(inviteContactWithEmailUseCase(emails[2])) doReturn InviteContactRequest.Sent
            whenever(inviteContactWithEmailUseCase(emails[3])) doReturn InviteContactRequest.Resent

            val actual = underTest(emails)

            val expected = listOf(
                InviteContactRequest.Sent,
                InviteContactRequest.InvalidStatus,
                InviteContactRequest.Sent,
                InviteContactRequest.Resent
            )
            assertThat(actual).isEqualTo(expected)
        }
}
