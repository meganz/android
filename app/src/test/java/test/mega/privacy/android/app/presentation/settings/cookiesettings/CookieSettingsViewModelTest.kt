package test.mega.privacy.android.app.presentation.settings.cookiesettings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.fragments.settingsFragments.cookie.CookieSettingsViewModel
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.login.GetSessionTransferURLUseCase
import mega.privacy.android.domain.usecase.setting.BroadcastCookieSettingsSavedUseCase
import mega.privacy.android.domain.usecase.setting.GetCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCrashAndPerformanceReportersUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.InstantExecutorExtension
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(value = [InstantExecutorExtension::class])
class CookieSettingsViewModelTest {

    private lateinit var underTest: CookieSettingsViewModel
    private val getCookieSettingsUseCase = mock<GetCookieSettingsUseCase>()
    private val updateCookieSettingsUseCase = mock<UpdateCookieSettingsUseCase>()
    private val broadcastCookieSettingsSavedUseCase = mock<BroadcastCookieSettingsSavedUseCase>()
    private val updateCrashAndPerformanceReportersUseCase =
        mock<UpdateCrashAndPerformanceReportersUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val getSessionTransferURLUseCase = mock<GetSessionTransferURLUseCase>()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }

    fun initTestClass() {
        underTest = CookieSettingsViewModel(
            getCookieSettingsUseCase = getCookieSettingsUseCase,
            updateCookieSettingsUseCase = updateCookieSettingsUseCase,
            broadcastCookieSettingsSavedUseCase = broadcastCookieSettingsSavedUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            updateCrashAndPerformanceReportersUseCase = updateCrashAndPerformanceReportersUseCase,
            getSessionTransferURLUseCase = getSessionTransferURLUseCase,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getCookieSettingsUseCase,
            updateCookieSettingsUseCase,
            broadcastCookieSettingsSavedUseCase,
            getFeatureFlagValueUseCase,
            updateCrashAndPerformanceReportersUseCase,
            getSessionTransferURLUseCase,
        )
    }

    private fun provideShowAdsCookiePreferenceParameters(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(true, true),
            Arguments.of(false, false)
        )
    }

    private fun provideCookiePolicyWithAdsLinkParameters(): Stream<Arguments> {
        return Stream.of(
            Arguments.of("https://mega.nz/testcookie", "https://mega.nz/testcookie"),
            Arguments.of(null, null)
        )
    }

    @ParameterizedTest(name = "when all Feature flags are: {0}, then showAdsCookiePreference is: {1}")
    @MethodSource("provideShowAdsCookiePreferenceParameters")
    fun `test that showAdsCookiePreference is updated when checkForInAppAdvertisementUseCase is successful`(
        input: Boolean,
        expected: Boolean,
    ) = runTest {
        whenever(getFeatureFlagValueUseCase(any())).thenReturn(input)
        initTestClass()
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.showAdsCookiePreference).isEqualTo(expected)
        }
    }

    @ParameterizedTest(name = "when sessionTransferURL is {0} then cookiePolicyWithAdsLink is: {1}")
    @MethodSource("provideCookiePolicyWithAdsLinkParameters")
    fun `test that cookiePolicyWithAdsLink is updated when getSessionTransferURLUseCase is successful`(
        link: String?,
        expected: String?,
    ) = runTest {
        whenever(getSessionTransferURLUseCase(any())).thenReturn(link)
        initTestClass()
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.cookiePolicyWithAdsLink).isEqualTo(expected)
        }
    }

    @Test
    fun `test that showAdsCookiePreference is false when checkForInAppAdvertisementUseCase is not successful`() =
        runTest {
            whenever(getFeatureFlagValueUseCase(any())).thenAnswer {
                throw Exception()
            }
            initTestClass()
            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.showAdsCookiePreference).isEqualTo(false)
            }
        }

    @Test
    fun `test that updateCrashAndPerformanceReportersUseCase is invoked when updateCookieSettingsUseCase is successful`() =
        runTest {
            val enabledCookies = setOf(CookieType.ESSENTIAL)
            whenever(updateCookieSettingsUseCase(enabledCookies)).thenReturn(Unit)
            whenever(broadcastCookieSettingsSavedUseCase(enabledCookies)).thenReturn(Unit)
            initTestClass()
            underTest.saveCookieSettings()
            verify(updateCrashAndPerformanceReportersUseCase).invoke()
        }
}
