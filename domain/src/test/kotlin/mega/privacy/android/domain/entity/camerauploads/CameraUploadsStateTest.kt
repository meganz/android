package mega.privacy.android.domain.entity.camerauploads

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Hashtable
import kotlin.math.roundToInt

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class CameraUploadsStateTest {

    lateinit var underTest: CameraUploadsState

    @Test
    fun `test that totalToUpload returns the sum of primaryToUpload and secondaryToUpload`() {
        val primaryToUpload = 30
        val secondaryToUpload = 10
        val expected = (primaryToUpload + secondaryToUpload)


        underTest = CameraUploadsState(
            primaryCameraUploadsState = CameraUploadsFolderState(
                toUploadCount = primaryToUpload,
            ),
            secondaryCameraUploadsState = CameraUploadsFolderState(
                toUploadCount = secondaryToUpload,
            ),
        )

        assertThat(underTest.totalPendingCount).isEqualTo(expected)
    }

    @Test
    fun `test that totalUploaded returns the sum of primaryUploaded and secondaryUploaded`() {
        val primaryUploaded = 30
        val secondaryUploaded = 10
        val expected = (primaryUploaded + secondaryUploaded)

        underTest = CameraUploadsState(
            primaryCameraUploadsState = CameraUploadsFolderState(
                uploadedCount = primaryUploaded,
            ),
            secondaryCameraUploadsState = CameraUploadsFolderState(
                uploadedCount = secondaryUploaded,
            ),
        )

        assertThat(underTest.totalUploadedCount).isEqualTo(expected)
    }

    @Test
    fun `test that totalPending returns the difference between totalToUpload and totalUploaded`() {
        val primaryToUpload = 30
        val secondaryToUpload = 10
        val primaryUploaded = 20
        val secondaryUploaded = 5
        val expected = (primaryToUpload + secondaryToUpload) - (primaryUploaded + secondaryUploaded)

        underTest = CameraUploadsState(
            primaryCameraUploadsState = CameraUploadsFolderState(
                toUploadCount = primaryToUpload,
                uploadedCount = primaryUploaded,
            ),
            secondaryCameraUploadsState = CameraUploadsFolderState(
                toUploadCount = secondaryToUpload,
                uploadedCount = secondaryUploaded,
            ),
        )

        assertThat(underTest.totalPendingCount).isEqualTo(expected)
    }

    @Test
    fun `test that totalBytesToUpload returns the sum of primaryBytesToUpload and secondaryBytesToUpload`() {
        val primaryBytesToUpload = 10000000L
        val secondaryBytesToUpload = 2000000L
        val expected = primaryBytesToUpload + secondaryBytesToUpload

        underTest = CameraUploadsState(
            primaryCameraUploadsState = CameraUploadsFolderState(
                bytesToUploadCount = primaryBytesToUpload,
            ),
            secondaryCameraUploadsState = CameraUploadsFolderState(
                bytesToUploadCount = secondaryBytesToUpload,
            ),
        )

        assertThat(underTest.totalBytesToUploadCount).isEqualTo(expected)
    }

    @Test
    fun `test that totalBytesUploaded returns the sum of primaryBytesUploaded and secondaryBytesUploaded`() {
        val fileA = 1L to 10000000L / 2
        val fileB = 2L to 10000000L / 2
        val primaryBytesFinishedUploadedCount = 1000L
        val primaryBytesUploaded =
            primaryBytesFinishedUploadedCount + fileA.second + fileB.second
        val fileC = 3L to 2000000L / 2
        val fileD = 4L to 2000000L / 2
        val secondaryBytesFinishedUploadedCount = 2000L
        val secondaryBytesUploaded =
            secondaryBytesFinishedUploadedCount + fileC.second + fileD.second
        val expected = primaryBytesUploaded + secondaryBytesUploaded

        val primaryBytesInProgressUploadedTable = Hashtable<Long, Long>().apply {
            this[fileA.first] = fileA.second
            this[fileB.first] = fileB.second
        }
        val secondaryBytesInProgressUploadedTable = Hashtable<Long, Long>().apply {
            this[fileC.first] = fileC.second
            this[fileD.first] = fileD.second
        }

        underTest = CameraUploadsState(
            primaryCameraUploadsState = CameraUploadsFolderState(
                bytesInProgressUploadedTable = primaryBytesInProgressUploadedTable,
                bytesFinishedUploadedCount = primaryBytesFinishedUploadedCount,
            ),
            secondaryCameraUploadsState = CameraUploadsFolderState(
                bytesInProgressUploadedTable = secondaryBytesInProgressUploadedTable,
                bytesFinishedUploadedCount = secondaryBytesFinishedUploadedCount,
            ),
        )

        assertThat(underTest.totalBytesUploadedCount).isEqualTo(expected)
    }

    @Test
    fun `test that when totalBytesToUpload is equal to 0 then totalProgress returns 0`() {
        val primaryBytesToUpload = 0L
        val secondaryBytesToUpload = 0L
        val expected = 0

        underTest = CameraUploadsState(
            primaryCameraUploadsState = CameraUploadsFolderState(
                bytesToUploadCount = primaryBytesToUpload,
            ),
            secondaryCameraUploadsState = CameraUploadsFolderState(
                bytesToUploadCount = secondaryBytesToUpload,
            ),
        )

        assertThat(underTest.totalProgress).isEqualTo(expected)
    }

    @Test
    fun `test that progress returns the correct progress if totalUploadBytes is not 0L`() {
        val primaryBytesToUpload = 2000L
        val secondaryBytesToUpload = 2000L
        val fileA = 1L to 500L / 2
        val fileB = 2L to 500L / 2
        val primaryBytesFinishedUploadedCount = 1000L
        val primaryBytesUploaded =
            primaryBytesFinishedUploadedCount + fileA.second + fileB.second
        val fileC = 3L to 500L / 2
        val fileD = 4L to 500L / 2
        val secondaryBytesFinishedUploadedCount = 2000L
        val secondaryBytesUploaded =
            secondaryBytesFinishedUploadedCount + fileC.second + fileD.second
        val expected =
            (((primaryBytesUploaded + secondaryBytesUploaded).toDouble() / (primaryBytesToUpload + secondaryBytesToUpload)) * 100)
                .roundToInt()

        val primaryBytesInProgressUploadedTable = Hashtable<Long, Long>().apply {
            this[fileA.first] = fileA.second
            this[fileB.first] = fileB.second
        }

        val secondaryBytesInProgressUploadedTable = Hashtable<Long, Long>().apply {
            this[fileC.first] = fileC.second
            this[fileD.first] = fileD.second
        }

        underTest = CameraUploadsState(
            primaryCameraUploadsState = CameraUploadsFolderState(
                bytesToUploadCount = primaryBytesToUpload,
                bytesInProgressUploadedTable = primaryBytesInProgressUploadedTable,
                bytesFinishedUploadedCount = primaryBytesFinishedUploadedCount,
            ),
            secondaryCameraUploadsState = CameraUploadsFolderState(
                bytesToUploadCount = secondaryBytesToUpload,
                bytesInProgressUploadedTable = secondaryBytesInProgressUploadedTable,
                bytesFinishedUploadedCount = secondaryBytesFinishedUploadedCount,
            ),
        )

        assertThat(underTest.totalProgress).isEqualTo(expected)
    }
}
