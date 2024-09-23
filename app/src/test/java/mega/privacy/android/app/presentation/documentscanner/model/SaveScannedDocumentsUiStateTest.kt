package mega.privacy.android.app.presentation.documentscanner.model

import android.net.Uri
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

/**
 * Test class for [SaveScannedDocumentsUiState]
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaveScannedDocumentsUiStateTest {

    private lateinit var underTest: SaveScannedDocumentsUiState

    @Test
    fun `test that the user can select the scan file type to upload the scanned document`() =
        runTest {
            val soloImageUri = mock<Uri> {
                on { toString() } doReturn "/data/user/0/app_location/cache/test_image.jpg"
            }

            underTest = SaveScannedDocumentsUiState(soloImageUri = soloImageUri)

            assertThat(underTest.canSelectScanFileType).isTrue()
        }

    @Test
    fun `test that the user cannot select the scan file type to upload the scanned documents`() =
        runTest {
            underTest = SaveScannedDocumentsUiState(soloImageUri = null)

            assertThat(underTest.canSelectScanFileType).isFalse()
        }

    @Test
    fun `test that the actual filename of the pdf to be uploaded has the correct suffix`() =
        runTest {
            val filename = "test_filename"

            underTest = SaveScannedDocumentsUiState(
                filename = filename,
                scanFileType = ScanFileType.Pdf,
            )

            assertThat(underTest.actualFilename).isEqualTo("$filename.pdf")
        }

    @Test
    fun `test that the actual filename of the image to be uploaded has the correct suffix`() =
        runTest {
            val filename = "test_filename"

            underTest = SaveScannedDocumentsUiState(
                filename = filename,
                scanFileType = ScanFileType.Jpg,
            )

            assertThat(underTest.actualFilename).isEqualTo("$filename.jpg")
        }
}