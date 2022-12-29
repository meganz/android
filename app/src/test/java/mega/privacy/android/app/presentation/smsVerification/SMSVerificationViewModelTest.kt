package mega.privacy.android.app.presentation.smsVerification

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.presentation.favourites.facade.StringUtilWrapper
import mega.privacy.android.app.smsVerification.SMSVerificationViewModel
import mega.privacy.android.app.smsVerification.model.SMSVerificationUIState
import mega.privacy.android.domain.entity.achievement.MegaAchievement
import mega.privacy.android.domain.usecase.AreAccountAchievementsEnabled
import mega.privacy.android.domain.usecase.GetAccountAchievements
import mega.privacy.android.domain.usecase.GetCountryCallingCodes
import mega.privacy.android.domain.usecase.Logout
import mega.privacy.android.domain.usecase.SendSMSVerificationCode
import mega.privacy.android.domain.usecase.SetSMSVerificationShown
import org.junit.Before
import org.junit.Rule
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
    private val logout: Logout = mock()
    private val sendSMSVerificationCode: SendSMSVerificationCode = mock()
    private val areAccountAchievementsEnabled: AreAccountAchievementsEnabled = mock()
    private val getAccountAchievements: GetAccountAchievements = mock()
    private val stringUtilWrapper: StringUtilWrapper = mock()
    private val savedState: SavedStateHandle = mock()

    private val countryCallingCodes = listOf("A,B,C,D")
    private val countryCode = "A"
    private val countryName = "B"
    private val dialCode = "+1"

    @get:Rule
    var instantExecutorRule = InstantTaskExecutorRule()

    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        initViewModel()
    }

    private fun initViewModel() {
        runBlocking {
            whenever(getCountryCallingCodes()).thenReturn(countryCallingCodes)
            whenever(savedState.get<String>(COUNTRY_CODE)).thenReturn(countryCode)
            whenever(savedState.get<String>(COUNTRY_NAME)).thenReturn(countryName)
            whenever(savedState.get<String>(DIAL_CODE)).thenReturn(dialCode)
        }
        underTest = SMSVerificationViewModel(
            setSMSVerificationShown = setSMSVerificationShown,
            getCountryCallingCodes = getCountryCallingCodes,
            logout = logout,
            sendSMSVerificationCode = sendSMSVerificationCode,
            areAccountAchievementsEnabled = areAccountAchievementsEnabled,
            getAccountAchievements = getAccountAchievements,
            stringUtilWrapper = stringUtilWrapper,
            savedState = savedState,
        )
    }

    private fun getInitialState() = SMSVerificationUIState(
        countryCallingCodes = countryCallingCodes,
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
            assertThat(actual.isSelectedCountryCodeValid).isEqualTo(expected.isSelectedCountryCodeValid)
            assertThat(actual.selectedCountryName).isEqualTo(expected.selectedCountryName)
            assertThat(actual.selectedDialCode).isEqualTo(expected.selectedDialCode)
            assertThat(actual.isUserLocked).isEqualTo(expected.isUserLocked)
            assertThat(actual.countryCallingCodes).isEqualTo(expected.countryCallingCodes)
            assertThat(actual.isAchievementsEnabled).isEqualTo(expected.isAchievementsEnabled)
            assertThat(actual.bonusStorageSMS).isEqualTo(expected.bonusStorageSMS)
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
        val bonusStorage = "10"
        whenever(areAccountAchievementsEnabled()).thenReturn(true)
        whenever(getAccountAchievements.invoke(any(), any())).thenReturn(mock<MegaAchievement>())
        whenever(stringUtilWrapper.getSizeString(any())).thenReturn(bonusStorage)
        underTest.uiState.test {
            underTest.setIsUserLocked(false)
            val expected =
                getInitialState().copy(isUserLocked = false, bonusStorageSMS = bonusStorage)
            awaitItem()
            val actual = awaitItem()
            assertThat(actual.isUserLocked).isEqualTo(expected.isUserLocked)
            assertThat(actual.bonusStorageSMS).isEqualTo(expected.bonusStorageSMS)
        }
    }
}
