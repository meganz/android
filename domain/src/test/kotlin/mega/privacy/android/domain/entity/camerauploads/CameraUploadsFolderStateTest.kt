package mega.privacy.android.domain.entity.camerauploads

import com.google.common.truth.Truth.assertThat
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import kotlin.math.roundToInt

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class CameraUploadsFolderStateTest {

    lateinit var underTest: CameraUploadsFolderState

    @BeforeAll
    fun setUp() {
        underTest = CameraUploadsFolderState()
    }

    @Test
    fun `test that pendingCount returns the difference between toUploadCount and uploadedCount`() {
        val toUploadCount = 30
        val uploadedCount = 10
        val expected = (toUploadCount - uploadedCount)

        with(underTest) {
            this.toUploadCount = toUploadCount
            this.uploadedCount = uploadedCount
        }

        assertThat(underTest.pendingCount).isEqualTo(expected)
    }

    @Test
    fun `test that when bytesToUploadCount is equal to 0 then progress returns 0`() {
        val bytesToUploadCount = 0L
        val expected = 0

        with(underTest) {
            this.bytesToUploadCount = bytesToUploadCount
        }

        assertThat(underTest.progress).isEqualTo(expected)
    }

    @Test
    fun `test that totalUploadedCount is equal to the sum of bytes transferred`() {
        val fileA: Pair<String, Long> = Pair("pathA", 1000)
        val fileB: Pair<String, Long> = Pair("pathB", 2000)
        val expected = fileA.second + fileB.second

        with(underTest) {
            this.bytesUploadedTable[fileA.first] = fileA.second
            this.bytesUploadedTable[fileB.first] = fileB.second
        }

        assertThat(underTest.bytesUploadedCount).isEqualTo(expected)
    }

    @Test
    fun `test that when bytesToUploadCount is not 0 then progress returns the current progress`() {
        val bytesToUploadCount = 1000L
        val fileA: Pair<String, Long> = Pair("pathA", 250)
        val fileB: Pair<String, Long> = Pair("pathB", 250)

        val expected =
            ((bytesToUploadCount) / (fileA.second + fileB.second)) / 100
                .toDouble()
                .roundToInt()

        with(underTest) {
            this.bytesToUploadCount = bytesToUploadCount
            this.bytesUploadedTable[fileA.first] = fileA.second
            this.bytesUploadedTable[fileB.first] = fileB.second
        }

        assertThat(underTest.progress).isEqualTo(expected)
    }


}
