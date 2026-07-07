package mega.privacy.android.feature.documentscanner.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import mega.privacy.android.feature.documentscanner.domain.boundary.DocumentBoundaryDetector
import mega.privacy.android.feature.documentscanner.domain.boundary.StabilityTracker
import mega.privacy.android.feature.documentscanner.domain.entity.DetectionResult
import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary
import mega.privacy.android.feature.documentscanner.domain.entity.Point
import mega.privacy.android.feature.documentscanner.domain.entity.StabilityState
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelProvider
import mega.privacy.android.feature.documentscanner.domain.smoother.BoundarySmoother
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScanSessionViewModelTest {

    private lateinit var underTest: ScanSessionViewModel

    private val boundaryDetector = mock<DocumentBoundaryDetector>()
    private val boundarySmoother = mock<BoundarySmoother>()
    private val stabilityTracker = mock<StabilityTracker>()
    private val scannerModelProvider = mock<ScannerModelProvider>()

    private val rawBoundary = boundaryAt(0.1f)
    private val smoothedBoundary = boundaryAt(0.2f)

    @BeforeEach
    fun setUp() {
        reset(boundaryDetector, boundarySmoother, stabilityTracker, scannerModelProvider)
        underTest = ScanSessionViewModel(
            boundaryDetector = boundaryDetector,
            boundarySmoother = boundarySmoother,
            stabilityTracker = stabilityTracker,
            scannerModelProvider = scannerModelProvider,
        )
    }

    private fun modelPresent() {
        whenever(scannerModelProvider.cachedModelFile()).thenReturn(mock<File>())
    }

    private fun frame() = underTest.onAnalysisFrame(
        grayBytes = ByteArray(4), width = 2, height = 2, rotationDegrees = 90, timestamp = 1L,
    )

    @Test
    fun `test that initial state has camera permission not granted`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem().isCameraPermissionGranted).isFalse()
        }
    }

    @Test
    fun `test that onCameraPermissionGranted updates state`() = runTest {
        underTest.onCameraPermissionGranted()
        underTest.uiState.test {
            assertThat(awaitItem().isCameraPermissionGranted).isTrue()
        }
    }

    @Test
    fun `test that a detected boundary is smoothed and published with its stability state`() = runTest {
        modelPresent()
        whenever(boundaryDetector.detect(any(), any(), any(), any(), any()))
            .thenReturn(DetectionResult(rawBoundary, frameTimestamp = 1L, frameWidth = 100, frameHeight = 200))
        whenever(boundarySmoother.smooth(rawBoundary)).thenReturn(smoothedBoundary)
        whenever(stabilityTracker.onDetectionResult(any())).thenReturn(StabilityState.STABLE)

        frame()

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.boundaryOverlayState.boundary).isEqualTo(smoothedBoundary)
            assertThat(state.boundaryOverlayState.frameWidth).isEqualTo(100)
            assertThat(state.boundaryOverlayState.frameHeight).isEqualTo(200)
            assertThat(state.stabilityState).isEqualTo(StabilityState.STABLE)
        }
    }

    @Test
    fun `test that the smoothed boundary is what feeds the stability tracker`() = runTest {
        modelPresent()
        whenever(boundaryDetector.detect(any(), any(), any(), any(), any()))
            .thenReturn(DetectionResult(rawBoundary, frameTimestamp = 1L, frameWidth = 100, frameHeight = 200))
        whenever(boundarySmoother.smooth(rawBoundary)).thenReturn(smoothedBoundary)
        whenever(stabilityTracker.onDetectionResult(any())).thenReturn(StabilityState.STABILIZING)

        frame()

        verify(stabilityTracker).onDetectionResult(
            DetectionResult(smoothedBoundary, frameTimestamp = 1L, frameWidth = 100, frameHeight = 200),
        )
    }

    @Test
    fun `test that a null detection resets the smoother and clears the overlay`() = runTest {
        modelPresent()
        whenever(boundaryDetector.detect(any(), any(), any(), any(), any())).thenReturn(null)
        whenever(stabilityTracker.onDetectionResult(null)).thenReturn(StabilityState.SEARCHING)

        frame()

        verify(boundarySmoother).reset()
        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.boundaryOverlayState.boundary).isNull()
            assertThat(state.stabilityState).isEqualTo(StabilityState.SEARCHING)
        }
    }

    @Test
    fun `test that a missing model skips detection and raises isModelMissing`() = runTest {
        whenever(scannerModelProvider.cachedModelFile()).thenReturn(null)

        frame()

        verifyNoInteractions(boundaryDetector)
        verify(boundarySmoother, never()).smooth(any())
        underTest.uiState.test {
            assertThat(awaitItem().isModelMissing).isTrue()
        }
    }

    @Test
    fun `test that onCleared releases the detector`() {
        val onCleared = androidx.lifecycle.ViewModel::class.java
            .getDeclaredMethod("onCleared")
            .apply { isAccessible = true }

        onCleared.invoke(underTest)

        verify(boundaryDetector).release()
    }

    private companion object {
        fun boundaryAt(offset: Float) = DocumentBoundary(
            topLeft = Point(offset, offset),
            topRight = Point(1f - offset, offset),
            bottomRight = Point(1f - offset, 1f - offset),
            bottomLeft = Point(offset, 1f - offset),
            confidence = 1f,
        )
    }
}
