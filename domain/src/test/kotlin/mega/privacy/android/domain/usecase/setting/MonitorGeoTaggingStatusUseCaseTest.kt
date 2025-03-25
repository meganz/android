package mega.privacy.android.domain.usecase.setting

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.domain.repository.PermissionRepository
import mega.privacy.android.domain.repository.SettingsRepository
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.util.stream.Stream

@ExperimentalCoroutinesApi
class MonitorGeoTaggingStatusUseCaseTest {

    private lateinit var underTest: MonitorGeoTaggingStatusUseCase
    private val settingsRepository: SettingsRepository = mock()
    private val permissionRepository: PermissionRepository = mock()

    @BeforeEach
    fun setUp() {
        underTest = MonitorGeoTaggingStatusUseCase(settingsRepository, permissionRepository)
    }

    @ParameterizedTest
    @MethodSource("provideGeoTaggingStatus")
    fun `test monitorGeoTaggingStatus emits correctly`(
        geoTaggingEnabled: Boolean,
        locationPermissionGranted: Boolean,
        expected: Boolean,
    ) = runTest {
        whenever(settingsRepository.monitorGeoTaggingStatus()).thenReturn(flowOf(geoTaggingEnabled))
        whenever(permissionRepository.isLocationPermissionGranted()).thenReturn(
            locationPermissionGranted
        )

        underTest().test {
            assertThat(awaitItem()).isEqualTo(expected)
            awaitComplete()
        }
    }

    companion object {
        @JvmStatic
        fun provideGeoTaggingStatus(): Stream<Arguments> {
            return Stream.of(
                Arguments.of(true, true, true),
                Arguments.of(false, true, false),
                Arguments.of(true, false, false),
                Arguments.of(false, false, false)
            )
        }
    }
}