package mega.privacy.android.app.presentation.settings.cookiesettings

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.InstantExecutorExtension
import mega.privacy.android.app.fragments.settingsFragments.cookie.CookieSettingsViewModel
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.setting.GetCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCrashAndPerformanceReportersUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(value = [InstantExecutorExtension::class])
class CookieSettingsViewModelTest {

    private lateinit var underTest: CookieSettingsViewModel
    private val getCookieSettingsUseCase = mock<GetCookieSettingsUseCase>()
    private val updateCookieSettingsUseCase = mock<UpdateCookieSettingsUseCase>()
    private val updateCrashAndPerformanceReportersUseCase =
        mock<UpdateCrashAndPerformanceReportersUseCase>()
    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val coroutineScope = CoroutineScope(extension.testDispatcher)

    fun initTestClass() {
        underTest = CookieSettingsViewModel(
            getCookieSettingsUseCase = getCookieSettingsUseCase,
            updateCookieSettingsUseCase = updateCookieSettingsUseCase,
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            updateCrashAndPerformanceReportersUseCase = updateCrashAndPerformanceReportersUseCase,
            applicationScope = coroutineScope,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getCookieSettingsUseCase,
            updateCookieSettingsUseCase,
            getFeatureFlagValueUseCase,
            updateCrashAndPerformanceReportersUseCase,
        )
    }

    private fun provideShowAdsCookiePreferenceParameters(): Stream<Arguments> {
        return Stream.of(
            Arguments.of(true, true),
            Arguments.of(false, false)
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
            initTestClass()
            underTest.saveCookieSettings()
            verify(updateCrashAndPerformanceReportersUseCase).invoke(enabledCookies)
        }

    @Test
    fun `test that update cookie is invoked when changeCookie is called`() = runTest {
        val cookie = CookieType.ESSENTIAL
        val enable = true
        initTestClass()
        underTest.changeCookie(cookie, enable)
        verify(updateCookieSettingsUseCase).invoke(any())
    }

    @Test
    fun `test that toggleCookies is invoked when toggleCookies is called`() = runTest {
        val enable = true
        initTestClass()
        underTest.toggleCookies(enable)
        verify(updateCookieSettingsUseCase).invoke(any())
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension()
    }
}
