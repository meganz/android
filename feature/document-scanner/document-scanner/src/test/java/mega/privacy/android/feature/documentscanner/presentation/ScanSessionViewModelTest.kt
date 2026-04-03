package mega.privacy.android.feature.documentscanner.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScanSessionViewModelTest {

    private lateinit var underTest: ScanSessionViewModel

    @BeforeEach
    fun setUp() {
        underTest = ScanSessionViewModel()
    }

    @Test
    fun `test that initial state has camera permission not granted`() = runTest {
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isCameraPermissionGranted).isFalse()
        }
    }

    @Test
    fun `test that onCameraPermissionGranted updates state`() = runTest {
        underTest.onCameraPermissionGranted()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isCameraPermissionGranted).isTrue()
        }
    }

    @Test
    fun `test that onCameraPermissionDenied updates state`() = runTest {
        underTest.onCameraPermissionGranted()
        underTest.onCameraPermissionDenied()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.isCameraPermissionGranted).isFalse()
        }
    }
}
