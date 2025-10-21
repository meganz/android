package mega.privacy.android.app.usecase.orientation

import android.os.Build
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.features.OrientationMigrationFeature
import mega.privacy.android.domain.repository.EnvironmentRepository
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.whenever
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension

@ExtendWith(CoroutineMainDispatcherExtension::class)
@ExperimentalCoroutinesApi
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ShouldShowAdaptiveLayoutUseCaseTest {

    private lateinit var underTest: ShouldShowAdaptiveLayoutUseCase

    private val getFeatureFlagValueUseCase = mock<GetFeatureFlagValueUseCase>()
    private val environmentRepository = mock<EnvironmentRepository>()

    @BeforeEach
    fun setup() {
        underTest = ShouldShowAdaptiveLayoutUseCase(
            getFeatureFlagValueUseCase = getFeatureFlagValueUseCase,
            environmentRepository = environmentRepository
        )
    }

    @AfterEach
    fun resetMocks() {
        reset(getFeatureFlagValueUseCase, environmentRepository)
    }

    @Test
    fun `test that adaptive layout is shown when Android 16+ and feature flag is enabled`() =
        runTest {
            // Given
            val android16SdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM
            whenever(environmentRepository.getDeviceSdkVersionInt()).thenReturn(android16SdkVersion)
            whenever(getFeatureFlagValueUseCase(OrientationMigrationFeature.Android16OrientationMigrationEnabled))
                .thenReturn(true)

            // When
            val result = underTest()

            // Then
            assertThat(result).isTrue()
        }

    @Test
    fun `test that adaptive layout is not shown when Android 16+ but feature flag is disabled`() =
        runTest {
            // Given
            val android16SdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM
            whenever(environmentRepository.getDeviceSdkVersionInt()).thenReturn(android16SdkVersion)
            whenever(getFeatureFlagValueUseCase(OrientationMigrationFeature.Android16OrientationMigrationEnabled))
                .thenReturn(false)

            // When
            val result = underTest()

            // Then
            assertThat(result).isFalse()
        }

    @Test
    fun `test that adaptive layout is not shown when Android 15 and feature flag is enabled`() =
        runTest {
            // Given
            val android15SdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM - 1
            whenever(environmentRepository.getDeviceSdkVersionInt()).thenReturn(android15SdkVersion)
            whenever(getFeatureFlagValueUseCase(OrientationMigrationFeature.Android16OrientationMigrationEnabled))
                .thenReturn(true)

            // When
            val result = underTest()

            // Then
            assertThat(result).isFalse()
        }

    @Test
    fun `test that adaptive layout is not shown when Android 15 and feature flag is disabled`() =
        runTest {
            // Given
            val android15SdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM - 1
            whenever(environmentRepository.getDeviceSdkVersionInt()).thenReturn(android15SdkVersion)
            whenever(getFeatureFlagValueUseCase(OrientationMigrationFeature.Android16OrientationMigrationEnabled))
                .thenReturn(false)

            // When
            val result = underTest()

            // Then
            assertThat(result).isFalse()
        }

    @Test
    fun `test that adaptive layout is shown when Android 17 and feature flag is enabled`() =
        runTest {
            // Given
            val android17SdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM + 1
            whenever(environmentRepository.getDeviceSdkVersionInt()).thenReturn(android17SdkVersion)
            whenever(getFeatureFlagValueUseCase(OrientationMigrationFeature.Android16OrientationMigrationEnabled))
                .thenReturn(true)

            // When
            val result = underTest()

            // Then
            assertThat(result).isTrue()
        }

    @Test
    fun `test that adaptive layout is not shown when Android 17 but feature flag is disabled`() =
        runTest {
            // Given
            val android17SdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM + 1
            whenever(environmentRepository.getDeviceSdkVersionInt()).thenReturn(android17SdkVersion)
            whenever(getFeatureFlagValueUseCase(OrientationMigrationFeature.Android16OrientationMigrationEnabled))
                .thenReturn(false)

            // When
            val result = underTest()

            // Then
            assertThat(result).isFalse()
        }

    @Test
    fun `test that adaptive layout is not shown when Android 14 and feature flag is enabled`() =
        runTest {
            // Given
            val android14SdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM - 2
            whenever(environmentRepository.getDeviceSdkVersionInt()).thenReturn(android14SdkVersion)
            whenever(getFeatureFlagValueUseCase(OrientationMigrationFeature.Android16OrientationMigrationEnabled))
                .thenReturn(true)

            // When
            val result = underTest()

            // Then
            assertThat(result).isFalse()
        }

    @Test
    fun `test that adaptive layout is not shown when Android 13 and feature flag is enabled`() =
        runTest {
            // Given
            val android13SdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM - 3
            whenever(environmentRepository.getDeviceSdkVersionInt()).thenReturn(android13SdkVersion)
            whenever(getFeatureFlagValueUseCase(OrientationMigrationFeature.Android16OrientationMigrationEnabled))
                .thenReturn(true)

            // When
            val result = underTest()

            // Then
            assertThat(result).isFalse()
        }

    @Test
    fun `test that adaptive layout is shown when exactly Android 16 and feature flag is enabled`() =
        runTest {
            // Given
            val exactAndroid16SdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM
            whenever(environmentRepository.getDeviceSdkVersionInt()).thenReturn(
                exactAndroid16SdkVersion
            )
            whenever(getFeatureFlagValueUseCase(OrientationMigrationFeature.Android16OrientationMigrationEnabled))
                .thenReturn(true)

            // When
            val result = underTest()

            // Then
            assertThat(result).isTrue()
        }

    @Test
    fun `test that adaptive layout is not shown when exactly Android 16 but feature flag is disabled`() =
        runTest {
            // Given
            val exactAndroid16SdkVersion = Build.VERSION_CODES.VANILLA_ICE_CREAM
            whenever(environmentRepository.getDeviceSdkVersionInt()).thenReturn(
                exactAndroid16SdkVersion
            )
            whenever(getFeatureFlagValueUseCase(OrientationMigrationFeature.Android16OrientationMigrationEnabled))
                .thenReturn(false)

            // When
            val result = underTest()

            // Then
            assertThat(result).isFalse()
        }
}
