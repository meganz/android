package mega.privacy.android.app.presentation.documentscanner

import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.documentscanner.model.SaveScannedDocumentsSnackbarMessageUiItem
import mega.privacy.android.app.presentation.documentscanner.model.ScanDestination
import mega.privacy.android.app.presentation.documentscanner.model.ScanFileType
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.usecase.documentscanner.IsScanFilenameValidUseCase
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.mockito.Mockito.spy
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import java.util.Calendar
import java.util.Locale

/**
 * Test class for [SaveScannedDocumentsViewModel]
 */
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaveScannedDocumentsViewModelTest {

    private lateinit var underTest: SaveScannedDocumentsViewModel

    private val isScanFilenameValidUseCase = spy<IsScanFilenameValidUseCase>()
    private var savedStateHandle = SavedStateHandle(mapOf())

    private val cloudDriveParentHandle = 123456L
    private val pdfUri = mock<Uri> {
        on { toString() } doReturn "/data/user/0/app_location/cache/test_scan.pdf"
    }
    private val soloImageUri = mock<Uri> {
        on { toString() } doReturn "/data/user/0/app_location/cache/test_solo_scan.jpg"
    }

    private fun initViewModel() {
        underTest = SaveScannedDocumentsViewModel(
            isScanFilenameValidUseCase = isScanFilenameValidUseCase,
            savedStateHandle = savedStateHandle,
        )
    }

    @BeforeEach
    fun reset() {
        savedStateHandle = SavedStateHandle(mapOf())
    }

    @Test
    fun `test that the saved state handle values are set upon initialization`() =
        runTest {
            savedStateHandle[SaveScannedDocumentsActivity.EXTRA_CLOUD_DRIVE_PARENT_HANDLE] =
                cloudDriveParentHandle
            savedStateHandle[SaveScannedDocumentsActivity.EXTRA_SCAN_PDF_URI] = pdfUri
            savedStateHandle[SaveScannedDocumentsActivity.EXTRA_SCAN_SOLO_IMAGE_URI] = soloImageUri

            initViewModel()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.cloudDriveParentHandle).isEqualTo(cloudDriveParentHandle)
                assertThat(state.pdfUri).isEqualTo(pdfUri)
                assertThat(state.soloImageUri).isEqualTo(soloImageUri)
            }
        }

    @Test
    fun `test that the correct default filename is set upon initialization`() = runTest {
        val expectedFilename = String.format(
            Locale.getDefault(),
            "Scanned_%1tY%<tm%<td%<tH%<tM%<tS",
            Calendar.getInstance(),
        )
        savedStateHandle[SaveScannedDocumentsActivity.EXTRA_CLOUD_DRIVE_PARENT_HANDLE] = 123456L
        savedStateHandle[SaveScannedDocumentsActivity.EXTRA_SCAN_PDF_URI] = pdfUri
        savedStateHandle[SaveScannedDocumentsActivity.EXTRA_SCAN_SOLO_IMAGE_URI] = soloImageUri

        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().filename).isEqualTo(expectedFilename)
        }
    }

    @Test
    fun `test that the filename is updated when changed`() = runTest {
        val filename = "New Filename"

        initViewModel()
        underTest.onFilenameChanged(filename)

        underTest.uiState.test {
            assertThat(awaitItem().filename).isEqualTo(filename)
        }
    }

    @Test
    fun `test that the specific filename input error message is shown when the new filename is empty`() =
        runTest {
            initViewModel()
            underTest.onFilenameChanged("")

            underTest.uiState.test {
                assertThat(awaitItem().filenameErrorMessage).isEqualTo(R.string.scan_incorrect_name)
            }
        }

    @Test
    fun `test that the specific filename input error message is shown when the new filename only contains whitespaces`() =
        runTest {
            initViewModel()
            underTest.onFilenameChanged("   ")

            underTest.uiState.test {
                assertThat(awaitItem().filenameErrorMessage).isEqualTo(R.string.scan_incorrect_name)
            }
        }

    @Test
    fun `test that the specific filename input error message is shown when the new filename contains invalid characters`() =
        runTest {
            initViewModel()
            underTest.onFilenameChanged("New Filename?")

            underTest.uiState.test {
                assertThat(awaitItem().filenameErrorMessage).isEqualTo(R.string.scan_invalid_characters)
            }
        }

    @Test
    fun `test that no filename input error message is shown when the new filename is valid`() =
        runTest {
            initViewModel()
            underTest.onFilenameChanged("New Filename")

            underTest.uiState.test {
                assertThat(awaitItem().filenameErrorMessage).isNull()
            }
        }

    @Test
    fun `test that the snackbar is shown when confirming a blank filename`() = runTest {
        initViewModel()
        underTest.onFilenameConfirmed("")

        underTest.uiState.test {
            assertThat(awaitItem().snackbarMessage).isEqualTo(
                triggered(SaveScannedDocumentsSnackbarMessageUiItem.BlankFilename)
            )
        }
    }

    @Test
    fun `test that the snackbar is shown when confirming a filename with invalid characters`() =
        runTest {
            initViewModel()
            underTest.onFilenameConfirmed("New Filename?")

            underTest.uiState.test {
                assertThat(awaitItem().snackbarMessage).isEqualTo(
                    triggered(SaveScannedDocumentsSnackbarMessageUiItem.FilenameWithInvalidCharacters)
                )
            }
        }

    @Test
    fun `test that the snackbar is not shown when confirming a valid filename`() = runTest {
        initViewModel()
        underTest.onFilenameConfirmed("New Filename")

        underTest.uiState.test {
            assertThat(awaitItem().snackbarMessage).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that the snackbar is shown when proceeding to upload the scanned documents using a blank filename`() =
        runTest {
            initViewModel()
            // First, set the blank filename to the UI State
            underTest.onFilenameChanged("")

            underTest.onSaveButtonClicked()

            underTest.uiState.test {
                assertThat(awaitItem().snackbarMessage).isEqualTo(
                    triggered(
                        SaveScannedDocumentsSnackbarMessageUiItem.BlankFilename
                    )
                )
            }
        }

    @Test
    fun `test that the snackbar is shown when proceeding to upload the scanned documents using a filename with invalid characters`() =
        runTest {
            initViewModel()
            // First, set a filename with invalid characters to the UI State
            underTest.onFilenameChanged("new*filename")

            underTest.onSaveButtonClicked()

            underTest.uiState.test {
                assertThat(awaitItem().snackbarMessage).isEqualTo(
                    triggered(
                        SaveScannedDocumentsSnackbarMessageUiItem.FilenameWithInvalidCharacters
                    )
                )
            }
        }

    @Test
    fun `test that the snackbar is not shown when proceeding to upload the scanned documents using a valid filename`() =
        runTest {
            initViewModel()
            // First, set a valid filename to the UI State
            underTest.onFilenameChanged("new_filename")

            underTest.onSaveButtonClicked()

            underTest.uiState.test {
                assertThat(awaitItem().snackbarMessage).isEqualTo(consumed())
            }
        }

    @Test
    fun `test that a state event to upload the scanned documents is triggered when the filename is valid`() =
        runTest {
            initViewModel()
            // First, set a valid filename to the UI State
            underTest.onFilenameChanged("new_filename")

            underTest.onSaveButtonClicked()

            underTest.uiState.test {
                assertThat(awaitItem().uploadScansEvent).isEqualTo(triggered)
            }
        }

    @Test
    fun `test that the resulting scan file type is changed to PDF format`() =
        testScanFileType(ScanFileType.Pdf)

    @Test
    fun `test that the resulting scan file type is changed to JPG format`() =
        testScanFileType(ScanFileType.Jpg)

    private fun testScanFileType(scanFileType: ScanFileType) = runTest {
        initViewModel()
        underTest.onScanFileTypeSelected(scanFileType)

        underTest.uiState.test {
            assertThat(awaitItem().scanFileType).isEqualTo(scanFileType)
        }
    }

    @Test
    fun `test that the scan destination is changed to cloud drive`() =
        testScanDestination(ScanDestination.CloudDrive)

    @Test
    fun `test that the scan destination is changed to chat`() =
        testScanDestination(ScanDestination.Chat)

    private fun testScanDestination(scanDestination: ScanDestination) = runTest {
        initViewModel()
        underTest.onScanDestinationSelected(scanDestination)

        underTest.uiState.test {
            assertThat(awaitItem().scanDestination).isEqualTo(scanDestination)
        }
    }

    @Test
    fun `test that the snackbar message is consumed`() = runTest {
        initViewModel()
        underTest.onSnackbarMessageConsumed()

        underTest.uiState.test {
            assertThat(awaitItem().snackbarMessage).isEqualTo(consumed())
        }
    }

    @Test
    fun `test that the state event to upload the scanned documents is consumed`() = runTest {
        initViewModel()
        underTest.onUploadScansEventConsumed()

        underTest.uiState.test {
            assertThat(awaitItem().uploadScansEvent).isEqualTo(consumed)
        }
    }
}