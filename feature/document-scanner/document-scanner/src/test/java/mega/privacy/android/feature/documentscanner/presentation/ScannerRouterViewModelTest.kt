package mega.privacy.android.feature.documentscanner.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.documentscanner.domain.launchmode.CellularConsentRequiredException
import mega.privacy.android.feature.documentscanner.domain.launchmode.LegacyReason
import mega.privacy.android.feature.documentscanner.domain.launchmode.ScannerLaunchMode
import mega.privacy.android.feature.documentscanner.domain.usecase.GetScannerLaunchModeUseCase
import mega.privacy.android.feature.documentscanner.presentation.model.ScannerRoute
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ExtendWith(CoroutineMainDispatcherExtension::class)
class ScannerRouterViewModelTest {

    private lateinit var underTest: ScannerRouterViewModel

    private val getScannerLaunchMode = mock<GetScannerLaunchModeUseCase>()

    @BeforeEach
    fun resetMocks() {
        reset(getScannerLaunchMode)
    }

    private fun initTestSubject() {
        underTest = ScannerRouterViewModel(getScannerLaunchMode)
    }

    @Test
    fun `test that route is LaunchCamera when launch mode is New`() = runTest {
        whenever(getScannerLaunchMode()).thenReturn(ScannerLaunchMode.New)

        initTestSubject()

        underTest.route.test {
            assertThat(awaitItem()).isEqualTo(ScannerRoute.LaunchCamera)
        }
    }

    @Test
    fun `test that route is NeedsDownload when launch mode is NeedsDownload`() = runTest {
        whenever(getScannerLaunchMode()).thenReturn(ScannerLaunchMode.NeedsDownload)

        initTestSubject()

        underTest.route.test {
            assertThat(awaitItem()).isEqualTo(ScannerRoute.NeedsDownload)
        }
    }

    @Test
    fun `test that route is UseLegacy with the same reason when launch mode is Legacy`() = runTest {
        whenever(getScannerLaunchMode())
            .thenReturn(ScannerLaunchMode.Legacy(LegacyReason.FlagOff))

        initTestSubject()

        underTest.route.test {
            assertThat(awaitItem())
                .isEqualTo(ScannerRoute.UseLegacy(LegacyReason.FlagOff))
        }
    }

    @Test
    fun `test that route is NeedsCellularConsent when the use case requires cellular consent`() =
        runTest {
            whenever(getScannerLaunchMode()).thenAnswer { throw CellularConsentRequiredException() }

            initTestSubject()

            underTest.route.test {
                assertThat(awaitItem()).isEqualTo(ScannerRoute.NeedsCellularConsent)
            }
        }

    @Test
    fun `test that route falls back to legacy when the use case fails unexpectedly`() = runTest {
        whenever(getScannerLaunchMode()).thenAnswer { throw IllegalStateException("boom") }

        initTestSubject()

        underTest.route.test {
            assertThat(awaitItem())
                .isEqualTo(ScannerRoute.UseLegacy(LegacyReason.Unknown))
        }
    }
}
