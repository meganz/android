package mega.privacy.android.app.presentation.documentscanner

import android.content.Context
import android.net.Uri
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavKey
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.palm.composestateevents.triggered
import java.io.File
import kotlinx.coroutines.test.runTest
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CHAT
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CLOUD_DRIVE
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_HEADER
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_EDIT_FILENAME_IMAGE
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_FILENAME_TEXT_FIELD
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_FILE_TYPE_IMAGE
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_HEADER
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_JPG
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_PDF
import mega.privacy.android.app.presentation.documentscanner.groups.SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_HEADER
import mega.privacy.android.app.presentation.documentscanner.model.SaveScannedDocumentsUiState
import mega.privacy.android.app.presentation.documentscanner.model.ScanDestination
import mega.privacy.android.app.presentation.documentscanner.model.ScanFileType
import mega.privacy.android.app.utils.FileUtil
import mega.privacy.android.data.extensions.toUriPath
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.navigation.destination.UploadScannedDocumentNavKey
import mega.privacy.mobile.analytics.event.DocumentScannerUploadingImageToChatEvent
import mega.privacy.mobile.analytics.event.DocumentScannerUploadingPDFToChatEvent
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

/**
 * Test class for [SaveScannedDocumentsView]
 */
@RunWith(AndroidJUnit4::class)
internal class SaveScannedDocumentsViewTest {

    val composeTestRule = createComposeRule()

    private val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(analyticsRule).around(composeTestRule)

    @Test
    fun `test that all static ui components are displayed`() {
        composeTestRule.setContent {
            SaveScannedDocumentsView(
                uiState = SaveScannedDocumentsUiState(
                    filename = "Scanned_test_document"
                ),
                onFilenameChanged = {},
                onFilenameConfirmed = {},
                onSaveButtonClicked = {},
                onScanFileTypeSelected = {},
                onScanDestinationSelected = {},
                onSnackbarMessageConsumed = {},
                onUploadScansEventConsumed = {},
                onBackToChat = {},
                onNavigate = {}
            )
        }

        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_TOOLBAR).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_HEADER)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_FILE_TYPE_IMAGE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_FILENAME_TEXT_FIELD)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILENAME_GROUP_EDIT_FILENAME_IMAGE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_NAME_DIVIDER).assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_HEADER)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CLOUD_DRIVE)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_GROUP_CHIP_CHAT)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_DESTINATION_DIVIDER)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_SAVE_BUTTON).assertIsDisplayed()
    }

    @Test
    fun `test that the save button is clicked`() = runTest {
        val onSaveDestinationClicked = mock<() -> Unit>()
        composeTestRule.setContent {
            SaveScannedDocumentsView(
                uiState = SaveScannedDocumentsUiState(
                    filename = "Scanned_test_document"
                ),
                onFilenameChanged = {},
                onFilenameConfirmed = {},
                onSaveButtonClicked = onSaveDestinationClicked,
                onScanFileTypeSelected = {},
                onScanDestinationSelected = {},
                onSnackbarMessageConsumed = {},
                onUploadScansEventConsumed = {},
                onBackToChat = {},
                onNavigate = {}
            )
        }

        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_SAVE_BUTTON).performClick()

        verify(onSaveDestinationClicked).invoke()
    }

    @Test
    fun `test that the file type selection is not shown when there is more than one scan`() =
        runTest {
            composeTestRule.setContent {
                SaveScannedDocumentsView(
                    uiState = SaveScannedDocumentsUiState(
                        filename = "Scanned_test_document",
                        soloImageUri = null,
                    ),
                    onFilenameChanged = {},
                    onFilenameConfirmed = {},
                    onSaveButtonClicked = {},
                    onScanFileTypeSelected = {},
                    onScanDestinationSelected = {},
                    onSnackbarMessageConsumed = {},
                    onUploadScansEventConsumed = {},
                    onBackToChat = {},
                    onNavigate = {}
                )
            }

            composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_HEADER)
                .assertDoesNotExist()
            composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_PDF)
                .assertDoesNotExist()
            composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_JPG)
                .assertDoesNotExist()
        }

    @Test
    fun `test that the file type selection is shown when there is only one scan`() = runTest {
        val soloImageUri = mock<Uri> {
            on { toString() } doReturn "/data/user/0/app_location/cache/test_solo_scan.jpg"
        }
        composeTestRule.setContent {
            SaveScannedDocumentsView(
                uiState = SaveScannedDocumentsUiState(
                    filename = "Scanned_test_document",
                    soloImageUri = soloImageUri,
                ),
                onFilenameChanged = {},
                onFilenameConfirmed = {},
                onSaveButtonClicked = {},
                onScanFileTypeSelected = {},
                onScanDestinationSelected = {},
                onSnackbarMessageConsumed = {},
                onUploadScansEventConsumed = {},
                onBackToChat = {},
                onNavigate = {}
            )
        }

        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_HEADER)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_PDF)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(SAVE_SCANNED_DOCUMENTS_FILE_TYPE_GROUP_CHIP_JPG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that onBackToChat is invoked with the upload uri when originated from chat`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "scanned.pdf")
        val expectedUri = runCatching { FileUtil.getUriForFile(context, file) }
            .getOrDefault(file.toUri())
        val onBackToChat = mock<(Uri) -> Unit>()

        composeTestRule.setContent {
            SaveScannedDocumentsView(
                uiState = SaveScannedDocumentsUiState(
                    originatedFromChat = true,
                    scanDestination = ScanDestination.Chat,
                    uploadScansEvent = triggered(file),
                ),
                onFilenameChanged = {},
                onFilenameConfirmed = {},
                onSaveButtonClicked = {},
                onScanFileTypeSelected = {},
                onScanDestinationSelected = {},
                onSnackbarMessageConsumed = {},
                onUploadScansEventConsumed = {},
                onBackToChat = onBackToChat,
                onNavigate = {},
            )
        }

        composeTestRule.waitForIdle()

        verify(onBackToChat).invoke(expectedUri)
    }

    @Test
    fun `test that onNavigate is invoked with UploadScannedDocumentNavKey when cloud explorer is available`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val file = File(context.cacheDir, "scanned.pdf")
        val expectedUri = runCatching { FileUtil.getUriForFile(context, file) }
            .getOrDefault(file.toUri())
        val onNavigate = mock<(List<NavKey>) -> Unit>()

        composeTestRule.setContent {
            SaveScannedDocumentsView(
                uiState = SaveScannedDocumentsUiState(
                    isCloudExplorerAvailable = true,
                    uploadScansEvent = triggered(file),
                ),
                onFilenameChanged = {},
                onFilenameConfirmed = {},
                onSaveButtonClicked = {},
                onScanFileTypeSelected = {},
                onScanDestinationSelected = {},
                onSnackbarMessageConsumed = {},
                onUploadScansEventConsumed = {},
                onBackToChat = {},
                onNavigate = onNavigate,
            )
        }

        composeTestRule.waitForIdle()

        verify(onNavigate).invoke(
            listOf(
                UploadScannedDocumentNavKey(
                    uriPath = expectedUri.toUriPath(),
                    nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                    hasMultipleScans = true,
                )
            )
        )
    }

    @Test
    fun `test that the PDF to chat analytics event is tracked when originated from chat and scan file type is PDF`() {
        val file = File("scanned.pdf")

        composeTestRule.setContent {
            SaveScannedDocumentsView(
                uiState = SaveScannedDocumentsUiState(
                    originatedFromChat = true,
                    scanFileType = ScanFileType.Pdf,
                    scanDestination = ScanDestination.Chat,
                    uploadScansEvent = triggered(file),
                ),
                onFilenameChanged = {},
                onFilenameConfirmed = {},
                onSaveButtonClicked = {},
                onScanFileTypeSelected = {},
                onScanDestinationSelected = {},
                onSnackbarMessageConsumed = {},
                onUploadScansEventConsumed = {},
                onBackToChat = {},
                onNavigate = {}
            )
        }

        composeTestRule.waitForIdle()

        assertThat(analyticsRule.events.first())
            .isInstanceOf(DocumentScannerUploadingPDFToChatEvent::class.java)
    }

    @Test
    fun `test that the image to chat analytics event is tracked when originated from chat and scan file type is JPG`() {
        val file = File("scanned.jpg")
        val soloImageUri = mock<Uri> {
            on { toString() } doReturn "/data/user/0/app_location/cache/test_solo_scan.jpg"
        }

        composeTestRule.setContent {
            SaveScannedDocumentsView(
                uiState = SaveScannedDocumentsUiState(
                    originatedFromChat = true,
                    scanFileType = ScanFileType.Jpg,
                    scanDestination = ScanDestination.Chat,
                    soloImageUri = soloImageUri,
                    uploadScansEvent = triggered(file),
                ),
                onFilenameChanged = {},
                onFilenameConfirmed = {},
                onSaveButtonClicked = {},
                onScanFileTypeSelected = {},
                onScanDestinationSelected = {},
                onSnackbarMessageConsumed = {},
                onUploadScansEventConsumed = {},
                onBackToChat = {},
                onNavigate = {}
            )
        }

        composeTestRule.waitForIdle()

        assertThat(analyticsRule.events.first())
            .isInstanceOf(DocumentScannerUploadingImageToChatEvent::class.java)
    }

    @Test
    fun `test that no analytics event is tracked when not originated from chat`() {
        val file = File("scanned.pdf")

        composeTestRule.setContent {
            SaveScannedDocumentsView(
                uiState = SaveScannedDocumentsUiState(
                    originatedFromChat = false,
                    scanFileType = ScanFileType.Pdf,
                    scanDestination = ScanDestination.CloudDrive,
                    cloudDriveParentHandle = 1L,
                    uploadScansEvent = triggered(file),
                ),
                onFilenameChanged = {},
                onFilenameConfirmed = {},
                onSaveButtonClicked = {},
                onScanFileTypeSelected = {},
                onScanDestinationSelected = {},
                onSnackbarMessageConsumed = {},
                onUploadScansEventConsumed = {},
                onBackToChat = {},
                onNavigate = {}
            )
        }

        composeTestRule.waitForIdle()

        assertThat(analyticsRule.events).isEmpty()
    }
}