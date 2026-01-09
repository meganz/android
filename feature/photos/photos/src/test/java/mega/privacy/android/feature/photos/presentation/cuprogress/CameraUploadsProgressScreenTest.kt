package mega.privacy.android.feature.photos.presentation.cuprogress

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import mega.privacy.android.domain.entity.photos.CameraUploadsTransferType
import mega.privacy.android.domain.entity.transfer.InProgressTransfer
import mega.privacy.android.domain.entity.transfer.TransferState
import mega.privacy.android.feature.transfers.components.TEST_TAG_CAMERA_UPLOADS_ACTIVE_TRANSFER_ITEM
import mega.privacy.android.feature.transfers.components.TEST_TAG_CAMERA_UPLOADS_IN_QUEUE_TRANSFER_ITEM
import mega.privacy.android.shared.resources.R as SharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(sdk = [34])
class CameraUploadsProgressScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun `test that the top bar is displayed`() {
        composeRuleScope {
            setScreen()

            onNodeWithTag(CAMERA_UPLOADS_PROGRESS_SCREEN_TOP_BAR_TAG).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the top bar subtitle is displayed when the pending count is greater than 0`() {
        val pendingCount = 10
        composeRuleScope {
            setScreen(
                uiState = CameraUploadsProgressUiState(pendingCount = pendingCount)
            )

            val subtitle = context.resources.getQuantityString(
                SharedR.plurals.camera_uploads_tranfer_top_bar_subtitle,
                pendingCount,
                pendingCount,
            )
            onNodeWithText(subtitle).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the top bar subtitle is not displayed when the pending count is 0`() {
        val pendingCount = 0
        composeRuleScope {
            setScreen(
                uiState = CameraUploadsProgressUiState(pendingCount = pendingCount)
            )

            val subtitle = context.resources.getQuantityString(
                SharedR.plurals.camera_uploads_tranfer_top_bar_subtitle,
                pendingCount,
                pendingCount,
            )
            onNodeWithText(subtitle).assertDoesNotExist()
        }
    }

    @Test
    fun `test that the loading skeleton is displayed when the content is loading`() {
        composeRuleScope {
            setScreen(
                uiState = CameraUploadsProgressUiState(isLoading = true)
            )

            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_SKELETON_LOADING_VIEW).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the empty view is displayed when the no transfer available`() {
        composeRuleScope {
            setScreen(
                uiState = CameraUploadsProgressUiState(
                    isLoading = false,
                    transfers = listOf()
                )
            )

            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_EMPTY_VIEW).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the in progress transfer header is displayed when the transfer type is in-progress`() {
        val inProgressTransfer = mock<InProgressTransfer.Upload> {
            on { tag } doReturn 1
            on { fileName } doReturn "Video name.mp4"
            on { state } doReturn TransferState.STATE_ACTIVE
        }
        composeRuleScope {
            setScreen(
                uiState = CameraUploadsProgressUiState(
                    isLoading = false,
                    transfers = listOf(
                        CameraUploadsTransferType.InProgress(
                            items = listOf(inProgressTransfer)
                        )
                    )
                )
            )

            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_IN_PROGRESS_HEADER).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the in progress transfer item is displayed when the transfer type is in-progress`() {
        val tagValue = 1
        val inProgressTransfer = mock<InProgressTransfer.Upload> {
            on { tag } doReturn tagValue
            on { fileName } doReturn "Video name.mp4"
            on { state } doReturn TransferState.STATE_ACTIVE
        }
        composeRuleScope {
            setScreen(
                uiState = CameraUploadsProgressUiState(
                    isLoading = false,
                    transfers = listOf(
                        CameraUploadsTransferType.InProgress(
                            items = listOf(inProgressTransfer)
                        )
                    )
                )
            )

            onNodeWithTag("${TEST_TAG_CAMERA_UPLOADS_ACTIVE_TRANSFER_ITEM}_$tagValue")
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that the in queue transfer header is displayed when the transfer type is in-queue`() {
        val inProgressTransfer = mock<InProgressTransfer.Upload> {
            on { tag } doReturn 1
            on { fileName } doReturn "Video name.mp4"
            on { state } doReturn TransferState.STATE_ACTIVE
        }
        composeRuleScope {
            setScreen(
                uiState = CameraUploadsProgressUiState(
                    isLoading = false,
                    transfers = listOf(
                        CameraUploadsTransferType.InQueue(
                            items = listOf(inProgressTransfer)
                        )
                    )
                )
            )

            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_TRANSFER_IN_QUEUE_HEADER).assertIsDisplayed()
        }
    }

    @Test
    fun `test that the in queue transfer item is displayed when the transfer type is in-queue`() {
        val tagValue = 1
        val inProgressTransfer = mock<InProgressTransfer.Upload> {
            on { tag } doReturn tagValue
            on { fileName } doReturn "Video name.mp4"
            on { state } doReturn TransferState.STATE_ACTIVE
        }
        composeRuleScope {
            setScreen(
                uiState = CameraUploadsProgressUiState(
                    isLoading = false,
                    transfers = listOf(
                        CameraUploadsTransferType.InQueue(
                            items = listOf(inProgressTransfer)
                        )
                    )
                )
            )

            onNodeWithTag("${TEST_TAG_CAMERA_UPLOADS_IN_QUEUE_TRANSFER_ITEM}_$tagValue")
                .assertIsDisplayed()
        }
    }

    @Test
    fun `test that the add transfer callback is called when the transfer list is not empty`() {
        val inProgressTransfer = mock<InProgressTransfer.Upload> {
            on { tag } doReturn 1
            on { fileName } doReturn "Video name.mp4"
            on { state } doReturn TransferState.STATE_ACTIVE
        }
        composeRuleScope {
            val addTransfer = mock<(transfer: InProgressTransfer) -> Unit>()
            setScreen(
                uiState = CameraUploadsProgressUiState(
                    isLoading = false,
                    transfers = listOf(
                        CameraUploadsTransferType.InProgress(
                            items = listOf(inProgressTransfer)
                        )
                    )
                ),
                addTransfer = addTransfer
            )

            verify(addTransfer).invoke(inProgressTransfer)
        }
    }

    private fun composeRuleScope(block: ComposeContentTestRule.() -> Unit) {
        with(composeRule) {
            block()
        }
    }

    private fun ComposeContentTestRule.setScreen(
        uiState: CameraUploadsProgressUiState = CameraUploadsProgressUiState(),
        cameraUploadsTransferItemUiState: (id: Int) -> StateFlow<CameraUploadsTransferItemUiState> = {
            MutableStateFlow(
                CameraUploadsTransferItemUiState()
            )
        },
        onNavigateUp: () -> Unit = {},
        addTransfer: (transfer: InProgressTransfer) -> Unit = {},
    ) {
        setContent {
            CameraUploadsProgressScreen(
                uiState = uiState,
                cameraUploadsTransferItemUiState = cameraUploadsTransferItemUiState,
                onNavigateUp = onNavigateUp,
                addTransfer = addTransfer
            )
        }
    }
}
