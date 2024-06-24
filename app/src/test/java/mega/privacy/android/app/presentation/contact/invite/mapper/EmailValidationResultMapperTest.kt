package mega.privacy.android.app.presentation.contact.invite.mapper

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.main.model.InviteContactUiState
import mega.privacy.android.app.main.model.InviteContactUiState.MessageTypeUiState.Singular
import mega.privacy.android.app.presentation.contact.invite.model.EmailValidationResult.InvalidResult
import mega.privacy.android.app.presentation.contact.invite.model.EmailValidationResult.ValidResult
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity.AlreadyInContacts
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity.MyOwnEmail
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity.Pending
import mega.privacy.android.domain.entity.contacts.EmailInvitationsInputValidity.Valid
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import java.util.stream.Stream

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class EmailValidationResultMapperTest {

    private lateinit var underTest: EmailValidationResultMapper

    private val email = "email@email.com"

    @BeforeEach
    fun setUp() {
        underTest = EmailValidationResultMapper()
    }

    @Test
    fun `test that the 'Valid' result is returned when the input email is valid`() = runTest {
        val actual = underTest(email = email, validity = Valid)

        assertThat(actual).isEqualTo(ValidResult)
    }

    @ParameterizedTest(name = "{0} message is returned when the input email is invalid with status {1}")
    @MethodSource("provideMessageTypeUiStateAndEmailValidity")
    fun `test that the `(
        message: InviteContactUiState.MessageTypeUiState,
        validity: EmailInvitationsInputValidity,
    ) = runTest {
        val actual = underTest(email = email, validity = validity)

        assertThat(actual).isEqualTo(InvalidResult(message = message))
    }

    private fun provideMessageTypeUiStateAndEmailValidity() = Stream.of(
        Arguments.of(
            Singular(
                R.string.error_own_email_as_contact
            ),
            MyOwnEmail
        ),
        Arguments.of(
            Singular(
                id = R.string.context_contact_already_exists,
                argument = email
            ),
            AlreadyInContacts
        ),
        Arguments.of(
            Singular(
                id = R.string.invite_not_sent_already_sent,
                argument = email
            ),
            Pending
        )
    )
}
