package mega.privacy.android.app.presentation.smsVerification

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.presentation.verification.SMSVerificationViewModel
import mega.privacy.android.app.presentation.verification.model.SMSVerificationUIState
import mega.privacy.android.app.presentation.verification.model.mapper.SMSVerificationTextMapper
import mega.privacy.android.app.presentation.verification.model.mapper.SmsVerificationTextErrorMapper
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.GetCurrentCountryCodeUseCase
import mega.privacy.android.domain.usecase.login.LogoutUseCase
import mega.privacy.android.domain.usecase.verification.GetCountryCallingCodesUseCase
import mega.privacy.android.domain.usecase.verification.GetFormattedPhoneNumberUseCase
import mega.privacy.android.domain.usecase.verification.SendSMSVerificationCodeUseCase
import mega.privacy.android.domain.usecase.verification.SetSMSVerificationShownUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
class SMSVerificationViewModelTest {

    private companion object {
        const val COUNTRY_NAME = "name"
        const val DIAL_CODE = "dial_code"
        const val COUNTRY_CODE = "code"
    }

    private lateinit var underTest: SMSVerificationViewModel

    private val setSMSVerificationShownUseCase: SetSMSVerificationShownUseCase = mock()
    private val getCountryCallingCodesUseCase: GetCountryCallingCodesUseCase = mock()
    private val sendSMSVerificationCodeUseCase: SendSMSVerificationCodeUseCase = mock()
    private val smsVerificationTextMapper: SMSVerificationTextMapper = mock()
    private val smsVerificationTextErrorMapper: SmsVerificationTextErrorMapper = mock()
    private val getCurrentCountryCodeUseCase: GetCurrentCountryCodeUseCase = mock()
    private val savedState: SavedStateHandle = mock()
    private val getFormattedPhoneNumberUseCase: GetFormattedPhoneNumberUseCase = mock()
    private val logoutUseCase: LogoutUseCase = mock()

    private val countryCallingCodes = listOf("BD:880,", "AU:61,", "NZ:64,", "IN:91,")
    private val countryCode = "NZ"
    private val countryName = "New Zealand"
    private val dialCode = "+64"

    @BeforeEach
    fun setUp() {
        initViewModel()
    }

    private fun initViewModel() {
        runBlocking {
            whenever(getCurrentCountryCodeUseCase()).thenReturn(countryCode)
            whenever(smsVerificationTextMapper(any())).thenReturn(getInitialState())
            whenever(savedState.get<String>(COUNTRY_CODE)).thenReturn(countryCode)
            whenever(savedState.get<String>(COUNTRY_NAME)).thenReturn(countryName)
            whenever(savedState.get<String>(DIAL_CODE)).thenReturn(dialCode)
            whenever(getCountryCallingCodesUseCase()).thenReturn(countryCallingCodes)
        }
        underTest = SMSVerificationViewModel(
            setSMSVerificationShownUseCase = setSMSVerificationShownUseCase,
            getCountryCallingCodesUseCase = getCountryCallingCodesUseCase,
            sendSMSVerificationCodeUseCase = sendSMSVerificationCodeUseCase,
            getCurrentCountryCodeUseCase = getCurrentCountryCodeUseCase,
            getFormattedPhoneNumberUseCase = getFormattedPhoneNumberUseCase,
            savedState = savedState,
            smsVerificationTextMapper = smsVerificationTextMapper,
            smsVerificationTextErrorMapper = smsVerificationTextErrorMapper,
            logoutUseCase = logoutUseCase,
        )
    }

    private fun getInitialState() = SMSVerificationUIState(
        countryCallingCodes = countryCallingCodes,
        inferredCountryCode = countryCode,
        selectedCountryCode = countryCode,
        selectedCountryName = countryName,
        selectedDialCode = dialCode,
    )

    @Test
    fun `test that initial state is returned`() = runTest {
        underTest.uiState.test {
            val expected = getInitialState()
            val actual = awaitItem()
            assertThat(actual.phoneNumber).isEqualTo(expected.phoneNumber)
            assertThat(actual.isPhoneNumberValid).isEqualTo(expected.isPhoneNumberValid)
            assertThat(actual.isPhoneNumberValid).isEqualTo(expected.isPhoneNumberValid)
            assertThat(actual.inferredCountryCode).isEqualTo(expected.inferredCountryCode)
            assertThat(actual.selectedCountryCode).isEqualTo(expected.selectedCountryCode)
            assertThat(actual.selectedCountryName).isEqualTo(expected.selectedCountryName)
            assertThat(actual.selectedDialCode).isEqualTo(expected.selectedDialCode)
            assertThat(actual.isUserLocked).isEqualTo(expected.isUserLocked)
            assertThat(actual.countryCallingCodes).isEqualTo(expected.countryCallingCodes)
        }
    }

    @Test
    fun `test that state is updated if set is user locked is called with true`() = runTest {
        underTest.uiState.test {
            underTest.setIsUserLocked(true)
            val expected = getInitialState().copy(isUserLocked = true)
            awaitItem()
            val actual = awaitItem()
            assertThat(actual.isUserLocked).isEqualTo(expected.isUserLocked)
        }
    }

    @Test
    fun `test that state is updated if set is user locked is called with false`() = runTest {
        val expected =
            getInitialState().copy(isUserLocked = false)
        whenever(smsVerificationTextMapper(any())).thenReturn(expected)
        underTest.setIsUserLocked(false)
        advanceUntilIdle()
        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.isUserLocked).isEqualTo(expected.isUserLocked)
        }
    }

    @Test
    fun `test that sms send code states are updated when verification code is sent`() = runTest {
        val phoneNumber = "+012324567"
        val expected =
            getInitialState().copy(
                isVerificationCodeSent = true,
                isNextEnabled = true,
                phoneNumber = phoneNumber
            )
        whenever(getFormattedPhoneNumberUseCase(any(), any())).thenReturn(phoneNumber)
        whenever(sendSMSVerificationCodeUseCase(phoneNumber)).thenReturn(Unit)
        underTest.setPhoneNumber(phoneNumber)
        underTest.validatePhoneNumber()
        advanceUntilIdle()
        underTest.uiState.test {
            val actual = awaitItem()
            assertThat(actual.isVerificationCodeSent).isEqualTo(expected.isVerificationCodeSent)
            assertThat(actual.isNextEnabled).isEqualTo(expected.isNextEnabled)
        }
    }
}
