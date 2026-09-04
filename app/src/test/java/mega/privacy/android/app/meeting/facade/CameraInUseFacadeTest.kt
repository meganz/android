package mega.privacy.android.app.meeting.facade

import com.google.common.truth.Truth.assertThat
import mega.privacy.android.app.utils.VideoCaptureUtils
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.mockito.Mockito

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CameraInUseFacadeTest {

    private lateinit var underTest: CameraInUseFacade

    @BeforeEach
    fun setUp() {
        underTest = CameraInUseFacade()
    }

    @ParameterizedTest(name = "when VideoCaptureUtils returns {0}")
    @ValueSource(booleans = [true, false])
    fun `test that isFrontCameraInUse returns the value from VideoCaptureUtils`(
        isFrontCameraInUse: Boolean,
    ) {
        Mockito.mockStatic(VideoCaptureUtils::class.java).use { mocked ->
            mocked.`when`<Boolean> { VideoCaptureUtils.isFrontCameraInUse() }
                .thenReturn(isFrontCameraInUse)

            assertThat(underTest.isFrontCameraInUse()).isEqualTo(isFrontCameraInUse)
        }
    }
}
