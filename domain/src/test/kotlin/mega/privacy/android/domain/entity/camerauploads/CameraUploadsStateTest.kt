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
        underTest = CameraUploadsState()
    }

    @Test
    fun `test that pendingToUpload returns the difference between totalToUpload and totalUploaded`() {
        val totalToUpload = 1000
        val totalUploaded = 500
        val expected = totalToUpload - totalUploaded

        with(underTest) {
            this.totalToUpload = totalToUpload
            this.totalUploaded = totalUploaded
        }

        assertThat(underTest.pendingToUpload).isEqualTo(expected)
    }

    @Test
    fun `test that totalUploadBytes returns the sum of primaryTotalUploadBytes and secondaryTotalUploadBytes`() {
        val primaryTotalUploadBytes = 100000L
        val secondaryTotalUploadBytes = 500000L
        val expected = primaryTotalUploadBytes + secondaryTotalUploadBytes

        with(underTest) {
            this.primaryTotalUploadBytes = primaryTotalUploadBytes
            this.secondaryTotalUploadBytes = secondaryTotalUploadBytes
        }

        assertThat(underTest.totalUploadBytes).isEqualTo(expected)
    }

    @Test
    fun `test that totalUploadedBytes returns the sum of primaryTotalUploadedBytes and secondaryTotalUploadedBytes`() {
        val primaryTotalUploadedBytes = 100000L
        val secondaryTotalUploadedBytes = 500000L
        val expected = primaryTotalUploadedBytes + secondaryTotalUploadedBytes

        with(underTest) {
            this.primaryTotalUploadedBytes = primaryTotalUploadedBytes
            this.secondaryTotalUploadedBytes = secondaryTotalUploadedBytes
        }

        assertThat(underTest.totalUploadedBytes).isEqualTo(expected)
    }

    @Test
    fun `test that progress returns the 0 if totalUploadBytes is 0L`() {
        val primaryTotalUploadBytes = 0L
        val secondaryTotalUploadBytes = 0L
        val expected = 0

        with(underTest) {
            this.primaryTotalUploadBytes = primaryTotalUploadBytes
            this.secondaryTotalUploadBytes = secondaryTotalUploadBytes
        }

        assertThat(underTest.progress).isEqualTo(expected)
    }

    @Test
    fun `test that progress returns the correct progress if totalUploadBytes is not 0L`() {
        val primaryTotalUploadBytes = 1000L
        val secondaryTotalUploadBytes = 1000L
        val primaryTotalUploadedBytes = 500L
        val secondaryTotalUploadedBytes = 500L
        val expected =
            (((primaryTotalUploadedBytes + secondaryTotalUploadedBytes).toDouble() / (primaryTotalUploadBytes + secondaryTotalUploadBytes)) * 100)
                .roundToInt()

        with(underTest) {
            this.primaryTotalUploadBytes = primaryTotalUploadBytes
            this.secondaryTotalUploadBytes = secondaryTotalUploadBytes
            this.primaryTotalUploadedBytes = primaryTotalUploadedBytes
            this.secondaryTotalUploadedBytes = secondaryTotalUploadedBytes
        }

        assertThat(underTest.progress).isEqualTo(expected)
    }


}
