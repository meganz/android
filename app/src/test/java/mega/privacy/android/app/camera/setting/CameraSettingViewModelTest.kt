package mega.privacy.android.app.camera.setting

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.setting.EnableGeoTaggingUseCase
import mega.privacy.android.domain.usecase.setting.MonitorGeoTaggingStatusUseCase
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CameraSettingViewModelTest {

    private lateinit var underTest: CameraSettingViewModel

    private val enableGeoTaggingUseCase = mock<EnableGeoTaggingUseCase>()
    private val monitorGeoTaggingStatusUseCase = mock<MonitorGeoTaggingStatusUseCase>() {
        onBlocking { invoke() }.thenReturn(flowOf(true))
    }

    @BeforeAll
    fun setUp() {
        whenever(monitorGeoTaggingStatusUseCase()).thenReturn(flowOf(true))
        underTest = CameraSettingViewModel(enableGeoTaggingUseCase, monitorGeoTaggingStatusUseCase)
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test monitorGeoTaggingStatusUseCase`(isEnable: Boolean) = runTest {
        whenever(monitorGeoTaggingStatusUseCase()).thenReturn(flowOf(isEnable))
        underTest = CameraSettingViewModel(enableGeoTaggingUseCase, monitorGeoTaggingStatusUseCase)
        underTest.uiState.test {
            assertThat(awaitItem().isGeoTaggingEnabled).isEqualTo(isEnable)
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = [true, false])
    fun `test setSaveLocationToMedia invokes use case`(isEnable: Boolean) = runTest {
        underTest.setSaveLocationToMedia(isEnable)
        verify(enableGeoTaggingUseCase).invoke(isEnable)
    }

    companion object {
        @JvmField
        @RegisterExtension
        val extension = CoroutineMainDispatcherExtension()
    }
}