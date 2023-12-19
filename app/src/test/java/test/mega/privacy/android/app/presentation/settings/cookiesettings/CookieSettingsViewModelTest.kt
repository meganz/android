package test.mega.privacy.android.app.presentation.settings.cookiesettings

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.app.fragments.settingsFragments.cookie.CookieSettingsViewModel
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.usecase.setting.GetCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCookieSettingsUseCase
import mega.privacy.android.domain.usecase.setting.UpdateCrashAndPerformanceReportersUseCase
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CookieSettingsViewModelTest {

    private lateinit var underTest: CookieSettingsViewModel
    private val getCookieSettingsUseCase = mock<GetCookieSettingsUseCase>()
    private val updateCookieSettingsUseCase = mock<UpdateCookieSettingsUseCase>()
    private val updateCrashAndPerformanceReportersUseCase =
        mock<UpdateCrashAndPerformanceReportersUseCase>()

    @BeforeAll
    fun setup() {
        Dispatchers.setMain(StandardTestDispatcher())
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getCookieSettingsUseCase,
            updateCookieSettingsUseCase,
            updateCrashAndPerformanceReportersUseCase
        )
    }

    private fun initTestClass() {
        underTest = CookieSettingsViewModel(
            getCookieSettingsUseCase = getCookieSettingsUseCase,
            updateCookieSettingsUseCase = updateCookieSettingsUseCase,
            updateCrashAndPerformanceReportersUseCase = updateCrashAndPerformanceReportersUseCase
        )
    }

    @Test
    fun `test that updateCrashAndPerformanceReportersUseCase is invoked when updateCookieSettingsUseCase is successful`() =
        runTest {
            whenever(updateCookieSettingsUseCase(setOf(CookieType.ESSENTIAL))).thenReturn(Unit)
            initTestClass()
            underTest.saveCookieSettings()
            advanceUntilIdle()
            verify(updateCrashAndPerformanceReportersUseCase).invoke()
        }

    @AfterAll
    fun tearDown() {
        Dispatchers.resetMain()
    }
}