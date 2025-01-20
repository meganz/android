package mega.privacy.android.app.presentation.documentscanner

import android.net.Uri
import androidx.core.net.toUri
import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.consumed
import de.palm.composestateevents.triggered
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.AnalyticsTestExtension
import mega.privacy.android.app.presentation.documentscanner.model.ScanDestination
import mega.privacy.android.app.presentation.documentscanner.model.ScanFileType
import mega.privacy.android.core.test.extension.CoroutineMainDispatcherExtension
import mega.privacy.android.domain.entity.documentscanner.ScanFilenameValidationStatus
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.documentscanner.ValidateScanFilenameUseCase
import mega.privacy.android.domain.usecase.file.RenameFileAndDeleteOriginalUseCase
import mega.privacy.mobile.analytics.event.DocumentScannerSaveImageToChatEvent
import mega.privacy.mobile.analytics.event.DocumentScannerSaveImageToCloudDriveEvent
import mega.privacy.mobile.analytics.event.DocumentScannerSavePDFToChatEvent
import mega.privacy.mobile.analytics.event.DocumentScannerSavePDFToCloudDriveEvent
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.api.extension.RegisterExtension
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.Mockito
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.reset
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
import java.io.File
import java.util.Calendar
import java.util.Locale

/**
 * Test class for [SaveScannedDocumentsViewModel]
 */
@ExtendWith(CoroutineMainDispatcherExtension::class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
internal class SaveScannedDocumentsViewModelTest {

    private lateinit var underTest: SaveScannedDocumentsViewModel

    private val validateScanFilenameUseCase = spy<ValidateScanFilenameUseCase>()
    private val renameFileAndDeleteOriginalUseCase = mock<RenameFileAndDeleteOriginalUseCase>()
    private var savedStateHandle = SavedStateHandle(mapOf())

    private val cloudDriveParentHandle = 123456L
    private val pdfUriPath = "/data/user/0/app_location/cache/test_scan.pdf"
    private val pdfUri = mock<Uri> {
        on { toString() } doReturn "/data/user/0/app_location/cache/test_scan.pdf"
        on { path } doReturn pdfUriPath
    }
    private val soloImageUri = mock<Uri> {
        on { toString() } doReturn "/data/user/0/app_location/cache/test_solo_scan.jpg"
        on { path } doReturn "/data/user/0/app_location/cache/test_solo_scan.jpg"
    }

    private fun initViewModel() {
        underTest = SaveScannedDocumentsViewModel(
            validateScanFilenameUseCase = validateScanFilenameUseCase,
            renameFileAndDeleteOriginalUseCase = renameFileAndDeleteOriginalUseCase,
            savedStateHandle = savedStateHandle,
        )
    }

    @BeforeEach
    fun reset() {
        savedStateHandle = SavedStateHandle(mapOf())
        reset(renameFileAndDeleteOriginalUseCase)
    }

    @Test
    fun `test that state parameters without any logic checking are immediately set upon initialization`() =
        runTest {
            val originatedFromChat = false
            savedStateHandle[SaveScannedDocumentsActivity.EXTRA_ORIGINATED_FROM_CHAT] =
                originatedFromChat
            savedStateHandle[SaveScannedDocumentsActivity.EXTRA_CLOUD_DRIVE_PARENT_HANDLE] =
                cloudDriveParentHandle
            savedStateHandle[SaveScannedDocumentsActivity.EXTRA_SCAN_PDF_URI] = pdfUri
            savedStateHandle[SaveScannedDocumentsActivity.EXTRA_SCAN_SOLO_IMAGE_URI] = soloImageUri

            initViewModel()

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.originatedFromChat).isEqualTo(originatedFromChat)
                assertThat(state.cloudDriveParentHandle).isEqualTo(cloudDriveParentHandle)
                assertThat(state.pdfUri).isEqualTo(pdfUri)
                assertThat(state.soloImageUri).isEqualTo(soloImageUri)
            }
        }

    @Test
    fun `test that the default scan destination is set to cloud drive upon initialization if document scanning is accessed anywhere other than chat`() =
        runTest {
            savedStateHandle[SaveScannedDocumentsActivity.EXTRA_ORIGINATED_FROM_CHAT] = false

            initViewModel()

            underTest.uiState.test {
                assertThat(awaitItem().scanDestination).isEqualTo(ScanDestination.CloudDrive)
            }
        }

    @Test
    fun `test that the default scan destination is set to chat upon initialization if document scanning is accessed from chat`() =
        runTest {
            savedStateHandle[SaveScannedDocumentsActivity.EXTRA_ORIGINATED_FROM_CHAT] = true

            initViewModel()

            underTest.uiState.test {
                assertThat(awaitItem().scanDestination).isEqualTo(ScanDestination.Chat)
            }
        }

    @Test
    fun `test that the default scan file type is set to PDF upon initialization`() =
        runTest {
            initViewModel()

            underTest.uiState.test {
                assertThat(awaitItem().scanFileType).isEqualTo(ScanFileType.Pdf)
            }
        }

    @Test
    fun `test that the default actual filename ends with PDF upon initialization`() = runTest {
        val expectedFilename = String.format(
            Locale.getDefault(),
            "Scanned_%1tY%<tm%<td%<tH%<tM%<tS",
            Calendar.getInstance(),
        )
        val actualFilename = "$expectedFilename.pdf"

        initViewModel()

        underTest.uiState.test {
            assertThat(awaitItem().actualFilename).isEqualTo(actualFilename)
        }
    }

    @Test
    fun `test that both filename and filename validation status are updated when the filename changes`() =
        runTest {
            val filename = "New Scan"

            initViewModel()
            // Just to ensure that the selected scan file type is a PDF
            underTest.onScanFileTypeSelected(ScanFileType.Pdf)
            underTest.onFilenameChanged(filename)

            underTest.uiState.test {
                val state = awaitItem()
                assertThat(state.filename).isEqualTo(filename)
                assertThat(state.filenameValidationStatus).isEqualTo(
                    ScanFilenameValidationStatus.ValidFilename
                )
            }
        }

    @Test
    fun `test that the snackbar is shown when confirming an empty filename`() = runTest {
        initViewModel()

        underTest.onFilenameConfirmed("")

        underTest.uiState.test {
            assertThat(awaitItem().snackbarMessage).isEqualTo(triggered(ScanFilenameValidationStatus.EmptyFilename))
        }
    }

    @Test
    fun `test that the snackbar is not shown when confirming a filename with invalid characters`() =
        runTest {
            initViewModel()

            underTest.onFilenameConfirmed("New Filename?")

            underTest.uiState.test {
                assertThat(awaitItem().snackbarMessage).isEqualTo(consumed())
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
                    triggered(ScanFilenameValidationStatus.EmptyFilename)
                )
            }
        }

    @Test
    fun `test that the snackbar is not shown when proceeding to upload the scanned documents using a filename with invalid characters`() =
        runTest {
            initViewModel()
            // First, set a filename with invalid characters to the UI State
            underTest.onFilenameChanged("New Filename?")

            underTest.onSaveButtonClicked()

            underTest.uiState.test {
                assertThat(awaitItem().snackbarMessage).isEqualTo(consumed())
            }
        }

    @Test
    fun `test that the snackbar is not shown when proceeding to upload the scanned documents using a valid filename`() =
        runTest {
            initViewModel()
            // First, set a valid filename to the UI State
            underTest.onFilenameChanged("New Filename")

            underTest.onSaveButtonClicked()

            underTest.uiState.test {
                assertThat(awaitItem().snackbarMessage).isEqualTo(consumed())
            }
        }

    @Test
    fun `test that the scans are not uploaded when the filename is valid but the Uri to be uploaded is missing`() =
        runTest {
            // This assumes that no URIs for both PDF and Solo Image were not initialized
            initViewModel()

            underTest.onFilenameChanged("new_filename")
            underTest.onScanFileTypeSelected(ScanFileType.Pdf)
            underTest.onScanDestinationSelected(ScanDestination.CloudDrive)
            // Trigger the scan uploading process
            underTest.onSaveButtonClicked()

            underTest.uiState.test {
                assertThat(awaitItem().uploadScansEvent).isEqualTo(consumed())
            }
        }

    @Test
    fun `test that the scans are not uploaded when the filename and Uri to be uploaded are valid but the Uri path is missing`() =
        runTest {
            // Initialize the PDF and Solo Image URIs with null Uri paths
            val modifiedPdfUri = mock<Uri> {
                on { toString() } doReturn "/data/user/0/app_location/cache/test_scan.pdf"
                on { path } doReturn null
            }
            val modifiedSoloImageUri = mock<Uri> {
                on { toString() } doReturn "/data/user/0/app_location/cache/test_solo_scan.jpg"
                on { path } doReturn null
            }
            savedStateHandle[SaveScannedDocumentsActivity.EXTRA_ORIGINATED_FROM_CHAT] =
                false
            savedStateHandle[SaveScannedDocumentsActivity.EXTRA_CLOUD_DRIVE_PARENT_HANDLE] =
                cloudDriveParentHandle
            savedStateHandle[SaveScannedDocumentsActivity.EXTRA_SCAN_PDF_URI] = modifiedPdfUri
            savedStateHandle[SaveScannedDocumentsActivity.EXTRA_SCAN_SOLO_IMAGE_URI] =
                modifiedSoloImageUri

            initViewModel()

            underTest.onFilenameChanged("new_filename")
            underTest.onScanFileTypeSelected(ScanFileType.Pdf)
            underTest.onScanDestinationSelected(ScanDestination.CloudDrive)
            // Trigger the scan uploading process
            underTest.onSaveButtonClicked()

            underTest.uiState.test {
                assertThat(awaitItem().uploadScansEvent).isEqualTo(consumed())
            }
        }

    @Test
    fun `test that the scans are not uploaded when the filename and Uri and its path are valid but an error occurred in the renaming process`() =
        runTest {
            val newFilename = "new_filename"
            // Initialize the PDF and Solo Image URIs
            savedStateHandle[SaveScannedDocumentsActivity.EXTRA_ORIGINATED_FROM_CHAT] = false
            savedStateHandle[SaveScannedDocumentsActivity.EXTRA_CLOUD_DRIVE_PARENT_HANDLE] =
                cloudDriveParentHandle
            savedStateHandle[SaveScannedDocumentsActivity.EXTRA_SCAN_PDF_URI] = pdfUri
            savedStateHandle[SaveScannedDocumentsActivity.EXTRA_SCAN_SOLO_IMAGE_URI] = soloImageUri
            whenever(
                renameFileAndDeleteOriginalUseCase(
                    originalUriPath = UriPath(pdfUriPath),
                    newFilename = "${newFilename}.pdf",
                )
            ).thenThrow(RuntimeException())

            initViewModel()

            underTest.onFilenameChanged(newFilename)
            underTest.onScanFileTypeSelected(ScanFileType.Pdf)
            underTest.onScanDestinationSelected(ScanDestination.CloudDrive)
            // Trigger the scan uploading process
            assertDoesNotThrow { underTest.onSaveButtonClicked() }

            underTest.uiState.test {
                assertThat(awaitItem().uploadScansEvent).isEqualTo(consumed())
            }
        }

    @Test
    fun `test that the scans with the correct filename are uploaded`() = runTest {
        val uriMock = Mockito.mockStatic(Uri::class.java)
        val newFilename = "new_filename"
        val fileToUploadUri = mock<Uri> {
            on { toString() } doReturn "/data/user/0/app_location/cache/renamed_test_scan.pdf"
        }
        val fileToUpload = mock<File> {
            on { toUri() }.thenReturn(fileToUploadUri)
        }
        // Initialize the PDF and Solo Image URIs
        savedStateHandle[SaveScannedDocumentsActivity.EXTRA_ORIGINATED_FROM_CHAT] = false
        savedStateHandle[SaveScannedDocumentsActivity.EXTRA_CLOUD_DRIVE_PARENT_HANDLE] =
            cloudDriveParentHandle
        savedStateHandle[SaveScannedDocumentsActivity.EXTRA_SCAN_PDF_URI] = pdfUri
        savedStateHandle[SaveScannedDocumentsActivity.EXTRA_SCAN_SOLO_IMAGE_URI] = soloImageUri
        whenever(
            renameFileAndDeleteOriginalUseCase(
                originalUriPath = UriPath(pdfUriPath),
                newFilename = "${newFilename}.pdf",
            )
        ).thenReturn(fileToUpload)

        initViewModel()

        underTest.onFilenameChanged(newFilename)
        underTest.onScanFileTypeSelected(ScanFileType.Pdf)
        underTest.onScanDestinationSelected(ScanDestination.CloudDrive)
        // Trigger the scan uploading process
        underTest.onSaveButtonClicked()

        underTest.uiState.test {
            assertThat(awaitItem().uploadScansEvent).isEqualTo(triggered(fileToUploadUri))
        }

        uriMock.close()
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
            assertThat(awaitItem().uploadScansEvent).isEqualTo(consumed())
        }
    }

    @ParameterizedTest(name = "when scan file type is {0} and the destination is {1}")
    @MethodSource("provideScanFileTypeAndDestination")
    fun `test that correct analytics event is tracked`(
        scanFileType: ScanFileType,
        scanDestination: ScanDestination,
    ) = runTest {
        initViewModel()
        underTest.logDocumentScanEvent(scanFileType, scanDestination)
        when {
            scanFileType == ScanFileType.Pdf && scanDestination == ScanDestination.CloudDrive ->
                assertThat(analyticsExtension.events.first()).isInstanceOf(
                    DocumentScannerSavePDFToCloudDriveEvent::class.java
                )

            scanFileType == ScanFileType.Pdf && scanDestination == ScanDestination.Chat ->
                assertThat(analyticsExtension.events.first()).isInstanceOf(
                    DocumentScannerSavePDFToChatEvent::class.java
                )

            scanFileType == ScanFileType.Jpg && scanDestination == ScanDestination.CloudDrive ->
                assertThat(analyticsExtension.events.first()).isInstanceOf(
                    DocumentScannerSaveImageToCloudDriveEvent::class.java
                )

            scanFileType == ScanFileType.Jpg && scanDestination == ScanDestination.Chat ->
                assertThat(analyticsExtension.events.first()).isInstanceOf(
                    DocumentScannerSaveImageToChatEvent::class.java
                )
        }
    }

    private fun provideScanFileTypeAndDestination() = listOf(
        Arguments.of(ScanFileType.Pdf, ScanDestination.CloudDrive),
        Arguments.of(ScanFileType.Pdf, ScanDestination.Chat),
        Arguments.of(ScanFileType.Jpg, ScanDestination.CloudDrive),
        Arguments.of(ScanFileType.Jpg, ScanDestination.Chat),
    )

    companion object {
        @JvmField
        @RegisterExtension
        val analyticsExtension = AnalyticsTestExtension()
    }
}