package mega.privacy.android.app.presentation.photos.compose.camerauploads

import android.content.Context
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import mega.privacy.android.app.presentation.photos.model.CameraUploadsTransferType
import mega.privacy.android.app.presentation.transfers.model.image.ActiveTransferImageViewModel
import mega.privacy.android.app.presentation.transfers.model.image.TransferImageUiState
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.transfers.components.TEST_TAG_CAMERA_UPLOADS_ACTIVE_TRANSFER_ITEM
import mega.privacy.android.feature.transfers.components.TEST_TAG_CAMERA_UPLOADS_IN_QUEUE_TRANSFER_ITEM
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class CameraUploadsTransferScreenTest {
    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private val context = ApplicationProvider.getApplicationContext<Context>()

    private val tag1 = 1
    private val tag2 = 2

    private val state =
        TransferImageUiState(fileTypeResId = iconPackR.drawable.ic_text_medium_solid)

    private val viewModel = mock<ActiveTransferImageViewModel> {
        on { getUiStateFlow(tag1) } doReturn MutableStateFlow(state)
        on { getUiStateFlow(tag2) } doReturn MutableStateFlow(state)
    }
    private val viewModelStore = mock<ViewModelStore> {
        on { get(argThat<String> { contains(ActiveTransferImageViewModel::class.java.canonicalName.orEmpty()) }) } doReturn viewModel
    }
    private val viewModelStoreOwner = mock<ViewModelStoreOwner> {
        on { viewModelStore } doReturn viewModelStore
    }

    @Test
    fun `test that CameraUploadsTransferEmptyView shows correctly`() {
        composeRule.setContent {
            CameraUploadsTransferEmptyView()
        }

        with(composeRule) {
            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW_ICON).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW_TITLE).assertIsDisplayed()
            onNodeWithText(
                context.getString(sharedR.string.camera_uploads_tranfer_empty_view_title)
            ).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW_DESCRIPTION).assertIsDisplayed()
            onNodeWithText(
                context.getString(sharedR.string.camera_uploads_tranfer_empty_view_description)
            ).assertIsDisplayed()
        }
    }

    @Test
    fun `test that CameraUploadsTransferItem shows correctly when isInProgress is true`() {
        val mockTag = 1
        val mockInProgressTranfer = mock<InProgressTransfer.Upload> {
            on { tag }.thenReturn(mockTag)
            on { fileName }.thenReturn("Video name.mp4")
            on { state }.then { TransferState.STATE_ACTIVE }
        }
        composeRule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                CameraUploadsTransferItem(mockInProgressTranfer, true)
            }
        }

        with(composeRule) {
            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_ACTIVE_TRANSFER_ITEM + "_$mockTag").assertIsDisplayed()
        }
    }

    @Test
    fun `test that CameraUploadsTransferItem shows correctly when isInProgress is false`() {
        val mockTag = 1
        val mockInProgressTranfer = mock<InProgressTransfer.Upload> {
            on { tag }.thenReturn(mockTag)
            on { fileName }.thenReturn("Video name.mp4")
            on { state }.then { TransferState.STATE_QUEUED }
        }
        composeRule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                CameraUploadsTransferItem(mockInProgressTranfer, false)
            }
        }

        with(composeRule) {
            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_IN_QUEUE_TRANSFER_ITEM + "_$mockTag").assertIsDisplayed()
        }
    }

    @Test
    fun `test that CameraUploadsTransferEmptyView is shown when there is no transfer`() {
        composeRule.setContent {
            CameraUploadsTranferView(emptyList())
        }

        with(composeRule) {
            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW).assertIsDisplayed()
        }
    }

    @Test
    fun `test that in progress UIs are shown correctly`() {
        val testTag = 1
        val mockInProgressTransfer = mock<InProgressTransfer.Upload> {
            on { tag }.thenReturn(testTag)
            on { fileName }.thenReturn("Video name.mp4")
            on { state }.then { TransferState.STATE_ACTIVE }
        }
        val mockCameraUploadsTransferType =
            listOf(CameraUploadsTransferType.InProgress(listOf(mockInProgressTransfer)))
        composeRule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                CameraUploadsTranferView(mockCameraUploadsTransferType)
            }
        }

        with(composeRule) {
            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_IN_PROGRESS_HEADER).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_ACTIVE_TRANSFER_ITEM + "_$testTag").assertIsDisplayed()
            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_IN_QUEUE_HEADER).assertIsNotDisplayed()
        }
    }

    @Test
    fun `test that in queue UIs are shown correctly`() {
        val testTag = 1
        val mockInQueueTransfer = mock<InProgressTransfer.Upload> {
            on { tag }.thenReturn(testTag)
            on { fileName }.thenReturn("Video name.mp4")
            on { state }.then { TransferState.STATE_QUEUED }
        }
        val mockCameraUploadsTransferType =
            listOf(CameraUploadsTransferType.InQueue(listOf(mockInQueueTransfer)))
        composeRule.setContent {
            CompositionLocalProvider(LocalViewModelStoreOwner provides viewModelStoreOwner) {
                CameraUploadsTranferView(mockCameraUploadsTransferType)
            }
        }

        with(composeRule) {
            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_IN_QUEUE_HEADER).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_IN_QUEUE_TRANSFER_ITEM + "_$testTag").assertIsDisplayed()
            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_IN_PROGRESS_HEADER).assertIsNotDisplayed()
        }
    }
}