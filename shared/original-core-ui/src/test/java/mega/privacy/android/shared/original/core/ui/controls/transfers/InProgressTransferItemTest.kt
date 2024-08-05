package mega.privacy.android.shared.original.core.ui.controls.transfers

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.icon.pack.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

@RunWith(AndroidJUnit4::class)
class InProgressTransferItemTest {

    @get:Rule
    var composeRule = createComposeRule()

    private val onPlayPauseClicked = mock<() -> Unit>()
    private val name = "File name.pdf"
    private val progress = "63% of 1MB"
    private val speed = "4.2MB/s"
    private val tag = 1

    @Test
    fun `test that non paused, in progress transfer shows correctly`() {
        initComposeRuleContent(
            InProgressTransferUI(
                isDownload = true,
                fileTypeResId = R.drawable.ic_pdf_medium_solid,
                previewUri = null,
                fileName = name,
                progress = progress,
                speed = speed,
                isPaused = false,
                isQueued = false,
                isOverQuota = false,
                areTransfersPaused = false,
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_ITEM + "_$tag").assertIsDisplayed()
            onNodeWithTag(TEST_TAG_QUEUE_ICON).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithText(name).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(progress).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_PROGRESS).assertIsDisplayed()
            onNodeWithText(speed).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_SPEED).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_PAUSE_ICON).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_QUEUED_ICON).assertDoesNotExist()
            onNodeWithTag(TEST_TAG_PLAY_ICON).assertDoesNotExist()
        }
    }

    @Test
    fun `test that paused, in progress transfer shows correctly`() {
        initComposeRuleContent(
            InProgressTransferUI(
                isDownload = true,
                fileTypeResId = R.drawable.ic_pdf_medium_solid,
                previewUri = null,
                fileName = name,
                progress = progress,
                speed = speed,
                isPaused = true,
                isQueued = false,
                isOverQuota = false,
                areTransfersPaused = false,
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_ITEM + "_$tag").assertIsDisplayed()
            onNodeWithTag(TEST_TAG_QUEUE_ICON).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithText(name).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(progress).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_PROGRESS).assertIsDisplayed()
            onNodeWithText(speed).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_SPEED).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_PLAY_ICON).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_QUEUED_ICON).assertDoesNotExist()
            onNodeWithTag(TEST_TAG_PAUSE_ICON).assertDoesNotExist()
        }
    }

    @Test
    fun `test that queued transfer shows correctly`() {
        val queued = "Queued"
        initComposeRuleContent(
            InProgressTransferUI(
                isDownload = true,
                fileTypeResId = R.drawable.ic_pdf_medium_solid,
                previewUri = null,
                fileName = name,
                progress = progress,
                speed = queued,
                isPaused = true,
                isQueued = true,
                isOverQuota = false,
                areTransfersPaused = false,
            )
        )
        with(composeRule) {
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_ITEM + "_$tag").assertIsDisplayed()
            onNodeWithTag(TEST_TAG_QUEUE_ICON).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithText(name).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(progress).assertDoesNotExist()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_PROGRESS).assertDoesNotExist()
            onNodeWithText(queued).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_SPEED).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_PLAY_ICON).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_QUEUED_ICON).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_PAUSE_ICON).assertDoesNotExist()
        }
    }

    private fun initComposeRuleContent(inProgressTransferUI: InProgressTransferUI) =
        with(inProgressTransferUI) {
            composeRule.setContent {
                InProgressTransferItem(
                    tag = tag,
                    isDownload,
                    fileTypeResId,
                    previewUri,
                    fileName,
                    progress,
                    speed,
                    isPaused,
                    isQueued,
                    isOverQuota,
                    areTransfersPaused,
                    onPlayPauseClicked,
                )
            }
        }
}