package mega.privacy.android.app.smsVerification

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.verification.SMSVerificationTextViewModel
import mega.privacy.android.app.presentation.verification.model.SmsVerificationTextState
import mega.privacy.android.app.presentation.verification.model.mapper.SmsVerificationTextErrorMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.verification.VerifyPhoneNumber
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub

internal class SMSVerificationTextViewModelTest {
    private lateinit var underTest: SMSVerificationTextViewModel

    private val verifyPhoneNumber = mock<VerifyPhoneNumber>()
    private val verificationTextErrorMapper = mock<SmsVerificationTextErrorMapper>()

    @BeforeEach
    fun setUp() {
        underTest = SMSVerificationTextViewModel(
            verifyPhoneNumber = verifyPhoneNumber,
            verificationTextErrorMapper = verificationTextErrorMapper,
        )
    }

    @Test
    fun `test that initial state is Empty`() = runTest {
        underTest.state.test {
            assertThat(awaitItem()).isEqualTo(SmsVerificationTextState.Empty)
        }
    }

    @Test
    fun `test that loading state is returned while awaiting response`() = runTest {
        underTest.state
            .filterNot { it is SmsVerificationTextState.Empty }
            .test {
                underTest.submitPin("234")
                assertThat(awaitItem()).isEqualTo(SmsVerificationTextState.Loading)
                cancelAndIgnoreRemainingEvents()
            }
    }

    @Test
    fun `test that successfully verifying sets the state to VerifiedSuccessfully`() = runTest {
        underTest.state
            .filterNot { it is SmsVerificationTextState.Empty }
            .filterNot { it is SmsVerificationTextState.Loading }
            .test {
                underTest.submitPin("234")
                assertThat(awaitItem()).isEqualTo(SmsVerificationTextState.VerifiedSuccessfully)
            }
    }

    @Test
    fun `test that failure during verification returns error state`() = runTest {
        val error = "Expected error string"
        verificationTextErrorMapper.stub {
            on { invoke(any()) }.thenReturn(error)
        }

        verifyPhoneNumber.stub {
            onBlocking { invoke(any()) }.thenAnswer { throw Throwable() }
        }

        underTest.state
            .filterNot { it is SmsVerificationTextState.Empty }
            .filterNot { it is SmsVerificationTextState.Loading }
            .test {
                underTest.submitPin("234")
                assertThat(awaitItem()).isEqualTo(SmsVerificationTextState.Failed(error))
            }
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension(StandardTestDispatcher())
    }
}