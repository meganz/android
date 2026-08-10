package mega.privacy.android.feature.documentscanner.domain.usecase

import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.entity.ConnectivityState
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.network.GetCurrentConnectivityStateUseCase
import mega.privacy.android.feature.documentscanner.domain.launchmode.CellularConsentRequiredException
import mega.privacy.android.feature.documentscanner.domain.launchmode.LegacyReason
import mega.privacy.android.feature.documentscanner.domain.launchmode.ScannerLaunchMode
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelProvider
import mega.privacy.android.feature.documentscanner.domain.repository.ScannerPreferencesRepository
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class GetScannerLaunchModeUseCaseTest {

    private lateinit var underTest: GetScannerLaunchModeUseCase

    private val getFeatureFlagValue = mock<GetFeatureFlagValueUseCase>()
    private val getCurrentConnectivityState = mock<GetCurrentConnectivityStateUseCase>()
    private val scannerPreferences = mock<ScannerPreferencesRepository>()
    private val scannerModelProvider = mock<ScannerModelProvider>()

    @BeforeAll
    fun setUp() {
        underTest = GetScannerLaunchModeUseCase(
            getFeatureFlagValue = getFeatureFlagValue,
            getCurrentConnectivityState = getCurrentConnectivityState,
            scannerPreferences = scannerPreferences,
            scannerModelProvider = scannerModelProvider,
        )
    }

    @BeforeEach
    fun resetMocks() {
        reset(
            getFeatureFlagValue,
            getCurrentConnectivityState,
            scannerPreferences,
            scannerModelProvider,
        )
    }

    @Test
    fun `test that invoke returns Legacy FlagOff when the feature flag is disabled`() = runTest {
        whenever(getFeatureFlagValue(ApiFeatures.ContinuousDocumentScanner)).thenReturn(false)

        val actual = underTest()

        assertThat(actual).isEqualTo(ScannerLaunchMode.Legacy(LegacyReason.FlagOff))
        // Short-circuits before consulting the cache, network, or consent.
        verifyNoInteractions(
            scannerModelProvider,
            getCurrentConnectivityState,
            scannerPreferences,
        )
    }

    @Test
    fun `test that invoke returns New when the model is already cached`() = runTest {
        whenever(getFeatureFlagValue(ApiFeatures.ContinuousDocumentScanner)).thenReturn(true)
        whenever(scannerModelProvider.cachedModelFile()).thenReturn(File("midv500_unet.tflite"))

        val actual = underTest()

        assertThat(actual).isEqualTo(ScannerLaunchMode.New)
        // A cached model runs offline — network and consent are never consulted.
        verifyNoInteractions(getCurrentConnectivityState, scannerPreferences)
    }

    @Test
    fun `test that invoke returns Legacy NoNetwork when not cached and the device is disconnected`() =
        runTest {
            whenever(getFeatureFlagValue(ApiFeatures.ContinuousDocumentScanner)).thenReturn(true)
            whenever(scannerModelProvider.cachedModelFile()).thenReturn(null)
            whenever(getCurrentConnectivityState()).thenReturn(ConnectivityState.Disconnected)

            val actual = underTest()

            assertThat(actual).isEqualTo(ScannerLaunchMode.Legacy(LegacyReason.NoNetwork))
            verifyNoInteractions(scannerPreferences)
        }

    @Test
    fun `test that invoke returns NeedsDownload when not cached and on wifi regardless of cellular consent`() =
        runTest {
            whenever(getFeatureFlagValue(ApiFeatures.ContinuousDocumentScanner)).thenReturn(true)
            whenever(scannerModelProvider.cachedModelFile()).thenReturn(null)
            whenever(getCurrentConnectivityState())
                .thenReturn(ConnectivityState.Connected(isOnWifi = true))

            val actual = underTest()

            assertThat(actual).isEqualTo(ScannerLaunchMode.NeedsDownload)
            // Wi-Fi path never has to read consent.
            verifyNoInteractions(scannerPreferences)
        }

    @Test
    fun `test that invoke returns NeedsDownload when not cached and on cellular with prior consent`() =
        runTest {
            whenever(getFeatureFlagValue(ApiFeatures.ContinuousDocumentScanner)).thenReturn(true)
            whenever(scannerModelProvider.cachedModelFile()).thenReturn(null)
            whenever(getCurrentConnectivityState())
                .thenReturn(ConnectivityState.Connected(isOnWifi = false))
            whenever(scannerPreferences.hasGrantedCellularConsent()).thenReturn(true)

            val actual = underTest()

            assertThat(actual).isEqualTo(ScannerLaunchMode.NeedsDownload)
        }

    @Test
    fun `test that invoke throws CellularConsentRequiredException when not cached and on cellular without consent`() =
        runTest {
            whenever(getFeatureFlagValue(ApiFeatures.ContinuousDocumentScanner)).thenReturn(true)
            whenever(scannerModelProvider.cachedModelFile()).thenReturn(null)
            whenever(getCurrentConnectivityState())
                .thenReturn(ConnectivityState.Connected(isOnWifi = false))
            whenever(scannerPreferences.hasGrantedCellularConsent()).thenReturn(false)

            assertThrows<CellularConsentRequiredException> { underTest() }
        }
}
