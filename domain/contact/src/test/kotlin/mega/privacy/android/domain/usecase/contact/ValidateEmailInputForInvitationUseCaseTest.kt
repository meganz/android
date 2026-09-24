package mega.privacy.android.domain.usecase.contact

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity.AlreadyInContacts
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity.MyOwnEmail
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity.Pending
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity.Valid
import mega.privacy.android.domain.usecase.account.IsTheEmailMineUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ValidateEmailInputForInvitationUseCaseTest {

    private lateinit var underTest: ValidateEmailInputForInvitationUseCase

    private val isTheEmailMineUseCase: IsTheEmailMineUseCase = mock()
    private val isEmailInContactsUseCase: IsEmailInContactsUseCase = mock()
    private val isEmailInPendingStateUseCase: IsEmailInPendingStateUseCase = mock()

    private val email = "email@email.com"

    @BeforeEach
    fun setUp() {
        underTest = ValidateEmailInputForInvitationUseCase(
            isTheEmailMineUseCase = isTheEmailMineUseCase,
            isEmailInContactsUseCase = isEmailInContactsUseCase,
            isEmailInPendingStateUseCase = isEmailInPendingStateUseCase
        )
    }

    @AfterEach
    fun tearDown() {
        reset(
            isTheEmailMineUseCase,
            isEmailInContactsUseCase,
            isEmailInPendingStateUseCase
        )
    }

    @Test
    fun `test that the 'personal email' status is returned when the inputted email is a personal email`() =
        runTest {
            whenever(isTheEmailMineUseCase(email)) doReturn true

            val actual = underTest(email)

            assertThat(actual).isEqualTo(MyOwnEmail)
        }

    @Test
    fun `test that the 'already in contact' status is returned when the inputted email is already a contact`() =
        runTest {
            whenever(isTheEmailMineUseCase(email)) doReturn false
            whenever(isEmailInContactsUseCase(email)) doReturn true

            val actual = underTest(email)

            assertThat(actual).isEqualTo(AlreadyInContacts)
        }

    @Test
    fun `test that the 'pending' status is returned when the inputted email is already invited but still pending`() =
        runTest {
            whenever(isTheEmailMineUseCase(email)) doReturn false
            whenever(isEmailInContactsUseCase(email)) doReturn false
            whenever(isEmailInPendingStateUseCase(email)) doReturn true

            val actual = underTest(email)

            assertThat(actual).isEqualTo(Pending)
        }

    @Test
    fun `test that the 'valid' status is returned when the inputted email is valid`() =
        runTest {
            whenever(isTheEmailMineUseCase(email)) doReturn false
            whenever(isEmailInContactsUseCase(email)) doReturn false
            whenever(isEmailInPendingStateUseCase(email)) doReturn false

            val actual = underTest(email)

            assertThat(actual).isEqualTo(Valid)
        }
}
