package mega.privacy.android.domain.entity.camerauploads

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.math.roundToInt

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CameraUploadsStateTest {

    lateinit var underTest: CameraUploadsState

    @BeforeAll
    fun setUp() {
        underTest = CameraUploadsState(
            primaryCameraUploadsState = CameraUploadsFolderState(),
            secondaryCameraUploadsState = CameraUploadsFolderState(),
        )
    }

    @Test
    fun `test that totalToUpload returns the sum of primaryToUpload and secondaryToUpload`() {
        val primaryToUpload = 30
        val secondaryToUpload = 10
        val expected = (primaryToUpload + secondaryToUpload)

        with(underTest) {
            primaryCameraUploadsState.toUploadCount = primaryToUpload
            secondaryCameraUploadsState.toUploadCount = secondaryToUpload
        }

        assertThat(underTest.totalPendingCount).isEqualTo(expected)
    }

    @Test
    fun `test that totalUploaded returns the sum of primaryUploaded and secondaryUploaded`() {
        val primaryUploaded = 30
        val secondaryUploaded = 10
        val expected = (primaryUploaded + secondaryUploaded)

        with(underTest) {
            primaryCameraUploadsState.uploadedCount = primaryUploaded
            secondaryCameraUploadsState.uploadedCount = secondaryUploaded
        }

        assertThat(underTest.totalUploadedCount).isEqualTo(expected)
    }

    @Test
    fun `test that totalPending returns the difference between totalToUpload and totalUploaded`() {
        val primaryToUpload = 30
        val secondaryToUpload = 10
        val primaryUploaded = 20
        val secondaryUploaded = 5
        val expected = (primaryToUpload + secondaryToUpload) - (primaryUploaded + secondaryUploaded)

        with(underTest) {
            primaryCameraUploadsState.toUploadCount = primaryToUpload
            secondaryCameraUploadsState.toUploadCount = secondaryToUpload
            primaryCameraUploadsState.uploadedCount = primaryUploaded
            secondaryCameraUploadsState.uploadedCount = secondaryUploaded
        }

        assertThat(underTest.totalPendingCount).isEqualTo(expected)
    }

    @Test
    fun `test that totalBytesToUpload returns the sum of primaryBytesToUpload and secondaryBytesToUpload`() {
        val primaryBytesToUpload = 10000000L
        val secondaryBytesToUpload = 2000000L
        val expected = primaryBytesToUpload + secondaryBytesToUpload

        with(underTest) {
            primaryCameraUploadsState.bytesToUploadCount = primaryBytesToUpload
            secondaryCameraUploadsState.bytesToUploadCount = secondaryBytesToUpload
        }

        assertThat(underTest.totalBytesToUploadCount).isEqualTo(expected)
    }

    @Test
    fun `test that totalBytesUploaded returns the sum of primaryBytesUploaded and secondaryBytesUploaded`() {
        val fileA = 1 to 10000000L / 2
        val fileB = 2 to 10000000L / 2
        val primaryBytesFinishedUploadedCount = 1000L
        val primaryBytesUploaded =
            primaryBytesFinishedUploadedCount + fileA.second + fileB.second
        val fileC = 3 to 2000000L / 2
        val fileD = 4 to 2000000L / 2
        val secondaryBytesFinishedUploadedCount = 2000L
        val secondaryBytesUploaded =
            secondaryBytesFinishedUploadedCount + fileC.second + fileD.second
        val expected = primaryBytesUploaded + secondaryBytesUploaded

        with(underTest) {
            primaryCameraUploadsState.bytesInProgressUploadedTable[fileA.first] = fileA.second
            primaryCameraUploadsState.bytesInProgressUploadedTable[fileB.first] = fileB.second
            primaryCameraUploadsState.bytesFinishedUploadedCount = primaryBytesFinishedUploadedCount
            secondaryCameraUploadsState.bytesInProgressUploadedTable[fileC.first] = fileC.second
            secondaryCameraUploadsState.bytesInProgressUploadedTable[fileD.first] = fileD.second
            secondaryCameraUploadsState.bytesFinishedUploadedCount =
                secondaryBytesFinishedUploadedCount
        }

        assertThat(underTest.totalBytesUploadedCount).isEqualTo(expected)
    }

    @Test
    fun `test that when totalBytesToUpload is equal to 0 then totalProgress returns 0`() {
        val primaryBytesToUpload = 0L
        val secondaryBytesToUpload = 0L
        val expected = 0

        with(underTest) {
            primaryCameraUploadsState.bytesToUploadCount = primaryBytesToUpload
            secondaryCameraUploadsState.bytesToUploadCount = secondaryBytesToUpload
        }

        assertThat(underTest.totalProgress).isEqualTo(expected)
    }

    @Test
    fun `test that progress returns the correct progress if totalUploadBytes is not 0L`() {
        val primaryBytesToUpload = 2000L
        val secondaryBytesToUpload = 2000L
        val fileA = 1 to 500L / 2
        val fileB = 2 to 500L / 2
        val primaryBytesFinishedUploadedCount = 1000L
        val primaryBytesUploaded =
            primaryBytesFinishedUploadedCount + fileA.second + fileB.second
        val fileC = 3 to 500L / 2
        val fileD = 4 to 500L / 2
        val secondaryBytesFinishedUploadedCount = 2000L
        val secondaryBytesUploaded =
            secondaryBytesFinishedUploadedCount + fileC.second + fileD.second
        val expected =
            (((primaryBytesUploaded + secondaryBytesUploaded).toDouble() / (primaryBytesToUpload + secondaryBytesToUpload)) * 100)
                .roundToInt()

        with(underTest) {
            primaryCameraUploadsState.bytesToUploadCount = primaryBytesToUpload
            secondaryCameraUploadsState.bytesToUploadCount = secondaryBytesToUpload
            primaryCameraUploadsState.bytesInProgressUploadedTable[fileA.first] = fileA.second
            primaryCameraUploadsState.bytesInProgressUploadedTable[fileB.first] = fileB.second
            primaryCameraUploadsState.bytesFinishedUploadedCount = primaryBytesFinishedUploadedCount
            secondaryCameraUploadsState.bytesInProgressUploadedTable[fileC.first] = fileC.second
            secondaryCameraUploadsState.bytesInProgressUploadedTable[fileD.first] = fileD.second
            secondaryCameraUploadsState.bytesFinishedUploadedCount =
                secondaryBytesFinishedUploadedCount
        }

        assertThat(underTest.totalProgress).isEqualTo(expected)
    }

    @Test
    fun `test that when resetUploadCount reset values to 0`() {
        val primaryToUpload = 20
        val secondaryToUpload = 10
        val primaryUploaded = 20
        val secondaryUploaded = 10
        val primaryBytesToUpload = 2000L
        val secondaryBytesToUpload = 1000L
        val fileA = 1 to 500L / 2
        val fileB = 2 to 500L / 2
        val fileC = 3 to 500L / 2
        val fileD = 4 to 500L / 2
        val primaryBytesFinishedUploadedCount = 1000L
        val secondaryBytesFinishedUploadedCount = 2000L

        with(underTest) {
            primaryCameraUploadsState.toUploadCount = primaryToUpload
            secondaryCameraUploadsState.toUploadCount = secondaryToUpload
            primaryCameraUploadsState.uploadedCount = primaryUploaded
            secondaryCameraUploadsState.uploadedCount = secondaryUploaded
            primaryCameraUploadsState.bytesToUploadCount = primaryBytesToUpload
            secondaryCameraUploadsState.bytesToUploadCount = secondaryBytesToUpload
            primaryCameraUploadsState.bytesInProgressUploadedTable[fileA.first] = fileA.second
            primaryCameraUploadsState.bytesInProgressUploadedTable[fileB.first] = fileB.second
            secondaryCameraUploadsState.bytesInProgressUploadedTable[fileC.first] = fileC.second
            secondaryCameraUploadsState.bytesInProgressUploadedTable[fileD.first] = fileD.second
            primaryCameraUploadsState.bytesFinishedUploadedCount = primaryBytesFinishedUploadedCount
            secondaryCameraUploadsState.bytesFinishedUploadedCount =
                secondaryBytesFinishedUploadedCount
        }

        underTest.resetUploadsCounts()

        with(underTest) {
            assertThat(primaryCameraUploadsState.toUploadCount).isEqualTo(0)
            assertThat(secondaryCameraUploadsState.toUploadCount).isEqualTo(0)
            assertThat(primaryCameraUploadsState.uploadedCount).isEqualTo(0)
            assertThat(secondaryCameraUploadsState.uploadedCount).isEqualTo(0)
            assertThat(primaryCameraUploadsState.bytesToUploadCount).isEqualTo(0)
            assertThat(secondaryCameraUploadsState.bytesToUploadCount).isEqualTo(0)
            assertThat(primaryCameraUploadsState.bytesUploadedCount).isEqualTo(0)
            assertThat(secondaryCameraUploadsState.bytesUploadedCount).isEqualTo(0)
            assertThat(primaryCameraUploadsState.bytesFinishedUploadedCount).isEqualTo(0)
            assertThat(secondaryCameraUploadsState.bytesFinishedUploadedCount).isEqualTo(0)
        }
    }

}
