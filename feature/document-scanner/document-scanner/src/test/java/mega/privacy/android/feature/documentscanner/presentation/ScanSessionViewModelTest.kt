package mega.privacy.android.feature.documentscanner.presentation

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.feature.documentscanner.domain.boundary.DocumentBoundaryDetector
import mega.privacy.android.feature.documentscanner.domain.boundary.StabilityTracker
import mega.privacy.android.feature.documentscanner.domain.capture.DocumentPageCapturer
import mega.privacy.android.feature.documentscanner.domain.entity.CaptureMode
import mega.privacy.android.feature.documentscanner.domain.entity.DetectionResult
import mega.privacy.android.feature.documentscanner.domain.entity.DocumentBoundary
import mega.privacy.android.feature.documentscanner.domain.entity.PageQuality
import mega.privacy.android.feature.documentscanner.domain.entity.Point
import mega.privacy.android.feature.documentscanner.domain.entity.ScanSession
import mega.privacy.android.feature.documentscanner.domain.entity.ScannedPage
import mega.privacy.android.feature.documentscanner.domain.entity.StabilityState
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelProvider
import mega.privacy.android.feature.documentscanner.domain.smoother.BoundarySmoother
import mega.privacy.android.feature.documentscanner.domain.usecase.AddScannedPageUseCase
import mega.privacy.android.feature.documentscanner.domain.usecase.MonitorScanSessionUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.reset
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever
import java.io.File

@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ScanSessionViewModelTest {

    private lateinit var underTest: ScanSessionViewModel

    private val boundaryDetector = mock<DocumentBoundaryDetector>()
    private val boundarySmoother = mock<BoundarySmoother>()
    private val stabilityTracker = mock<StabilityTracker>()
    private val scannerModelProvider = mock<ScannerModelProvider>()
    private val documentPageCapturer = mock<DocumentPageCapturer>()
    private val addScannedPageUseCase = mock<AddScannedPageUseCase>()
    private val monitorScanSessionUseCase = mock<MonitorScanSessionUseCase>()

    private val sessionFlow = MutableStateFlow(emptySession())

    private val rawBoundary = boundaryAt(0.1f)
    private val smoothedBoundary = boundaryAt(0.2f)

    @BeforeEach
    fun setUp() {
        reset(
            boundaryDetector,
            boundarySmoother,
            stabilityTracker,
            scannerModelProvider,
            documentPageCapturer,
            addScannedPageUseCase,
            monitorScanSessionUseCase,
        )
        sessionFlow.value = emptySession()
        whenever(monitorScanSessionUseCase()).thenReturn(sessionFlow)
        underTest = ScanSessionViewModel(
            boundaryDetector = boundaryDetector,
            boundarySmoother = boundarySmoother,
            stabilityTracker = stabilityTracker,
            scannerModelProvider = scannerModelProvider,
            documentPageCapturer = documentPageCapturer,
            addScannedPageUseCase = addScannedPageUseCase,
            monitorScanSessionUseCase = monitorScanSessionUseCase,
        )
    }

    private fun modelPresent() {
        whenever(scannerModelProvider.cachedModelFile()).thenReturn(mock<File>())
    }

    private fun frame() = underTest.onAnalysisFrame(
        grayBytes = ByteArray(4), width = 2, height = 2, rotationDegrees = 90, timestamp = 1L,
    )

    private fun stableFrameAt(timestamp: Long) {
        whenever(boundaryDetector.detect(any(), any(), any(), any(), any()))
            .thenReturn(DetectionResult(rawBoundary, frameTimestamp = timestamp, frameWidth = 100, frameHeight = 200))
        whenever(boundarySmoother.smooth(rawBoundary)).thenReturn(smoothedBoundary)
        whenever(stabilityTracker.onDetectionResult(any())).thenReturn(StabilityState.STABLE)
        underTest.onAnalysisFrame(ByteArray(4), 2, 2, 90, timestamp)
    }

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
    fun `test that the initial capture mode is AUTO`() = runTest {
        underTest.uiState.test {
            assertThat(awaitItem().captureMode).isEqualTo(CaptureMode.AUTO)
        }
    }

    @Test
    fun `test that onToggleAutoCapture switches AUTO to MANUAL and back`() = runTest {
        underTest.onToggleAutoCapture()
        underTest.uiState.test {
            assertThat(awaitItem().captureMode).isEqualTo(CaptureMode.MANUAL)
        }

        underTest.onToggleAutoCapture()
        underTest.uiState.test {
            assertThat(awaitItem().captureMode).isEqualTo(CaptureMode.AUTO)
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
    fun `test that a stable document auto-captures in AUTO mode`() = runTest {
        modelPresent()
        stableFrameAt(1_000)
        underTest.uiState.test {
            assertThat(awaitItem().captureEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that MANUAL mode does not auto-capture on a stable document`() = runTest {
        modelPresent()
        underTest.onToggleAutoCapture() // AUTO -> MANUAL
        stableFrameAt(1_000)
        underTest.uiState.test {
            assertThat(awaitItem().captureEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that a non-stable frame does not auto-capture`() = runTest {
        modelPresent()
        whenever(boundaryDetector.detect(any(), any(), any(), any(), any()))
            .thenReturn(DetectionResult(rawBoundary, frameTimestamp = 1_000, frameWidth = 100, frameHeight = 200))
        whenever(boundarySmoother.smooth(rawBoundary)).thenReturn(smoothedBoundary)
        whenever(stabilityTracker.onDetectionResult(any())).thenReturn(StabilityState.STABILIZING)

        underTest.onAnalysisFrame(ByteArray(4), 2, 2, 90, 1_000)

        underTest.uiState.test {
            assertThat(awaitItem().captureEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that auto-capture respects the cooldown`() = runTest {
        modelPresent()
        stableFrameAt(1_000)                       // fires
        underTest.onCaptureHandled()               // consume
        stableFrameAt(2_000)                       // within 3s cooldown -> no fire
        underTest.uiState.test {
            assertThat(awaitItem().captureEvent).isEqualTo(consumed)
        }
        stableFrameAt(4_001)                       // past cooldown -> fires again
        underTest.uiState.test {
            assertThat(awaitItem().captureEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that onManualCapture triggers a capture regardless of mode`() = runTest {
        underTest.onToggleAutoCapture() // MANUAL
        underTest.onManualCapture()
        underTest.uiState.test {
            assertThat(awaitItem().captureEvent).isEqualTo(triggered)
        }
    }

    @Test
    fun `test that onCaptureHandled consumes the capture event`() = runTest {
        underTest.onManualCapture()
        underTest.onCaptureHandled()
        underTest.uiState.test {
            assertThat(awaitItem().captureEvent).isEqualTo(consumed)
        }
    }

    @Test
    fun `test that onFrameCaptured captures the frame and adds the page to the session`() = runTest {
        val bytes = byteArrayOf(1, 2, 3)
        val page = scannedPage("p1")
        whenever(documentPageCapturer.capture(any(), any(), anyOrNull())).thenReturn(page)

        underTest.onFrameCaptured(bytes, rotationDegrees = 90)

        verify(documentPageCapturer).capture(bytes, 90, null)
        verify(addScannedPageUseCase).invoke(page)
    }

    @Test
    fun `test that the session pages populate the deck thumbnails and count`() = runTest {
        sessionFlow.value = ScanSession(
            id = "session",
            pages = listOf(scannedPage("a"), scannedPage("b")),
            captureMode = CaptureMode.AUTO,
            createdAt = 0L,
        )

        underTest.uiState.test {
            val state = awaitItem()
            assertThat(state.capturedPageCount).isEqualTo(2)
            assertThat(state.capturedPageThumbnails).containsExactly("a_thumb", "b_thumb").inOrder()
        }
    }

    @Test
    fun `test that a failed capture does not add a page`() = runTest {
        whenever(documentPageCapturer.capture(any(), any(), anyOrNull()))
            .thenThrow(IllegalArgumentException("decode failed"))

        underTest.onFrameCaptured(byteArrayOf(1), rotationDegrees = 0)

        verify(addScannedPageUseCase, never()).invoke(any())
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
        fun emptySession() = ScanSession(
            id = "empty",
            pages = emptyList(),
            captureMode = CaptureMode.AUTO,
            createdAt = 0L,
        )

        fun scannedPage(id: String) = ScannedPage(
            id = id,
            imageUri = "$id.jpg",
            thumbnailUri = "${id}_thumb",
            order = 0,
            capturedAt = 0L,
            quality = PageQuality.GOOD,
            boundary = null,
        )

        fun boundaryAt(offset: Float) = DocumentBoundary(
            topLeft = Point(offset, offset),
            topRight = Point(1f - offset, offset),
            bottomRight = Point(1f - offset, 1f - offset),
            bottomLeft = Point(offset, 1f - offset),
            confidence = 1f,
        )
    }
}
