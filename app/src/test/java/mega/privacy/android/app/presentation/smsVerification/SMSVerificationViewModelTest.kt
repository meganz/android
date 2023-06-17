package mega.privacy.android.app.presentation.smsVerification

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.verification.SMSVerificationViewModel
import mega.privacy.android.app.presentation.verification.model.SMSVerificationUIState
import mega.privacy.android.app.presentation.verification.model.mapper.SMSVerificationTextMapper
import mega.privacy.android.app.presentation.verification.model.mapper.SmsVerificationTextErrorMapper
import mega.privacy.android.domain.usecase.GetCountryCallingCodes
import mega.privacy.android.domain.usecase.GetCurrentCountryCode
import mega.privacy.android.domain.usecase.SetSMSVerificationShown
import mega.privacy.android.domain.usecase.verification.FormatPhoneNumber
import mega.privacy.android.domain.usecase.verification.SendSMSVerificationCode
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class SMSVerificationViewModelTest {

    private companion object {
        const val COUNTRY_NAME = "name"
        const val DIAL_CODE = "dial_code"
        const val COUNTRY_CODE = "code"
    }

    private lateinit var underTest: SMSVerificationViewModel

    private val setSMSVerificationShown: SetSMSVerificationShown = mock()
    private val getCountryCallingCodes: GetCountryCallingCodes = mock()
    private val sendSMSVerificationCode: SendSMSVerificationCode = mock()
    private val smsVerificationTextMapper: SMSVerificationTextMapper = mock()
    private val smsVerificationTextErrorMapper: SmsVerificationTextErrorMapper = mock()
    private val getCurrentCountryCode: GetCurrentCountryCode = mock()
    private val savedState: SavedStateHandle = mock()
    private val formatPhoneNumber: FormatPhoneNumber = mock()

    private val countryCallingCodes = listOf("BD:880,", "AU:61,", "NZ:64,", "IN:91,")
    private val countryCode = "NZ"
    private val countryName = "New Zealand"
    private val dialCode = "+64"

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initViewModel()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun initViewModel() {
        runBlocking {
            whenever(getCurrentCountryCode()).thenReturn(countryCode)
            whenever(smsVerificationTextMapper(any())).thenReturn(getInitialState())
            whenever(savedState.get<String>(COUNTRY_CODE)).thenReturn(countryCode)
            whenever(savedState.get<String>(COUNTRY_NAME)).thenReturn(countryName)
            whenever(savedState.get<String>(DIAL_CODE)).thenReturn(dialCode)
            whenever(getCountryCallingCodes()).thenReturn(countryCallingCodes)
        }
        underTest = SMSVerificationViewModel(
            setSMSVerificationShown = setSMSVerificationShown,
            getCountryCallingCodes = getCountryCallingCodes,
            sendSMSVerificationCode = sendSMSVerificationCode,
            getCurrentCountryCode = getCurrentCountryCode,
            formatPhoneNumber = formatPhoneNumber,
            savedState = savedState,
            smsVerificationTextMapper = smsVerificationTextMapper,
            smsVerificationTextErrorMapper = smsVerificationTextErrorMapper,
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
        whenever(formatPhoneNumber(any(), any())).thenReturn(phoneNumber)
        whenever(sendSMSVerificationCode(phoneNumber)).thenReturn(Unit)
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
