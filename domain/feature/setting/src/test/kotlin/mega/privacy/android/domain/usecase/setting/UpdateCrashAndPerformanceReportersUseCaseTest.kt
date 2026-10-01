package mega.privacy.android.domain.usecase.setting

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import mega.privacy.android.domain.entity.settings.cookie.CookieType
import mega.privacy.android.domain.monitoring.CrashReporter
import mega.privacy.android.domain.usecase.monitoring.EnablePerformanceReporterUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class UpdateCrashAndPerformanceReportersUseCaseTest {

    private lateinit var underTest: UpdateCrashAndPerformanceReportersUseCase
    private val enablePerformanceReporterUseCase = mock<EnablePerformanceReporterUseCase>()
    private val crashReporter = mock<CrashReporter>()

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun resetAndTearDown() {
        Dispatchers.resetMain()
        reset(
            enablePerformanceReporterUseCase,
            crashReporter
        )
    }

    private fun initTestClass() {
        underTest = UpdateCrashAndPerformanceReportersUseCase(
            enablePerformanceReporterUseCase,
            crashReporter
        )
    }

    private fun provideTestCases() = listOf(
        Arguments.of(true, true),
        Arguments.of(false, false)
    )

    @ParameterizedTest
    @MethodSource("provideTestCases")
    fun `test that reporters are enabled or disabled based on the received cookie settings`(
        isEnabled: Boolean,
        containsAnalytics: Boolean,
    ) = runTest {

        initTestClass()
        underTest.invoke(if (containsAnalytics) setOf(CookieType.ANALYTICS) else emptySet())

        verify(crashReporter).setEnabled(isEnabled)
        verify(enablePerformanceReporterUseCase).invoke(isEnabled)
    }
}