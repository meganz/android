package test.mega.privacy.android.app.presentation.settings.cookiesettings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.fragments.settingsFragments.cookie.CookieSettingsViewModel
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.usecase.setting.BroadcastCookieSettingsSavedUseCase
import mega.privacy.android.domain.usecase.setting.GetCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCrashAndPerformanceReportersUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.InstantExecutorExtension

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

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @BeforeEach
    fun initialise() {
        underTest = CookieSettingsViewModel(
            getCookieSettingsUseCase = getCookieSettingsUseCase,
            updateCookieSettingsUseCase = updateCookieSettingsUseCase,
            broadcastCookieSettingsSavedUseCase = broadcastCookieSettingsSavedUseCase,
            updateCrashAndPerformanceReportersUseCase = updateCrashAndPerformanceReportersUseCase
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(
            getCookieSettingsUseCase,
            updateCookieSettingsUseCase,
            updateCrashAndPerformanceReportersUseCase
        )
    }

    @Test
    fun `test that updateCrashAndPerformanceReportersUseCase is invoked when updateCookieSettingsUseCase is successful`() =
        runTest {
            val enabledCookies = setOf(CookieType.ESSENTIAL)
            whenever(updateCookieSettingsUseCase(enabledCookies)).thenReturn(Unit)
            whenever(broadcastCookieSettingsSavedUseCase(enabledCookies)).thenReturn(Unit)
            underTest.saveCookieSettings()
            verify(updateCrashAndPerformanceReportersUseCase).invoke()
        }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }
}
