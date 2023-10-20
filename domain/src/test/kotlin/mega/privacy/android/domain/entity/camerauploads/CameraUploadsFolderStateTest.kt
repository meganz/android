package mega.privacy.android.domain.entity.camerauploads

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import java.util.Hashtable
import kotlin.math.roundToInt

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CameraUploadsFolderStateTest {

    lateinit var underTest: CameraUploadsFolderState

    @Test
    fun `test that pendingCount returns the difference between toUploadCount and uploadedCount`() {
        val toUploadCount = 30
        val uploadedCount = 10
        val expected = (toUploadCount - uploadedCount)

        underTest = CameraUploadsFolderState(
            toUploadCount = toUploadCount,
            uploadedCount = uploadedCount,
        )

        assertThat(underTest.pendingCount).isEqualTo(expected)
    }

    @Test
    fun `test that when bytesToUploadCount is equal to 0 then progress returns 0`() {
        val bytesToUploadCount = 0L
        val expected = 0

        underTest = CameraUploadsFolderState(
            bytesToUploadCount = bytesToUploadCount,
        )

        assertThat(underTest.progress).isEqualTo(expected)
    }

    @Test
    fun `test that totalUploadedCount is equal to the sum of bytes transferred`() {
        val fileA = 1L to 1000L
        val fileB = 2L to 2000L
        val bytesFinishedUploadedCount = 100L
        val expected = bytesFinishedUploadedCount + fileA.second + fileB.second
        val bytesInProgressUploadedTable = Hashtable<Long, Long>().apply {
            this[fileA.first] = fileA.second
            this[fileB.first] = fileB.second
        }

        underTest = CameraUploadsFolderState(
            bytesFinishedUploadedCount = bytesFinishedUploadedCount,
            bytesInProgressUploadedTable = bytesInProgressUploadedTable
        )

        assertThat(underTest.bytesUploadedCount).isEqualTo(expected)
    }

    @Test
    fun `test that when bytesToUploadCount is not 0 then progress returns the current progress`() {
        val bytesToUploadCount = 1000L
        val fileA = 1L to 250L
        val fileB = 2L to 250L
        val bytesFinishedUploadedCount = 100L

        val expected =
            (((bytesFinishedUploadedCount + fileA.second + fileB.second).toDouble() / (bytesToUploadCount)) * 100).roundToInt()

        val bytesInProgressUploadedTable = Hashtable<Long, Long>().apply {
            this[fileA.first] = fileA.second
            this[fileB.first] = fileB.second
        }

        underTest = CameraUploadsFolderState(
            bytesToUploadCount = bytesToUploadCount,
            bytesFinishedUploadedCount = bytesFinishedUploadedCount,
            bytesInProgressUploadedTable = bytesInProgressUploadedTable
        )

        assertThat(underTest.progress).isEqualTo(expected)
    }
}
