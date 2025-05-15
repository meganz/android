package mega.privacy.android.feature.transfers.components

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
    private val progressSizeString = "6MB of 10MB"
    private val progressPercentString = "60%"
    private val progress = 0.6f
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
                progressSizeString = progressSizeString,
                progressPercentageString = progressPercentString,
                progress = progress,
                speed = speed,
                isPaused = false,
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
            onNodeWithText(progressSizeString, substring = true).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_SUBTITLE).assertIsDisplayed()
            onNodeWithText(speed, substring = true).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_PAUSE_ICON).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_TYPE_ICON).assertIsDisplayed()
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
                progressSizeString = progressSizeString,
                progressPercentageString = progressPercentString,
                progress = progress,
                speed = speed,
                isPaused = true,
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
            onNodeWithText(progressSizeString, substring = true).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_SUBTITLE).assertIsDisplayed()
            onNodeWithText(speed, substring = true).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_PLAY_ICON).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_IN_PROGRESS_TRANSFER_TYPE_ICON).assertIsDisplayed()
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
                    this@InProgressTransferItemTest.progressSizeString,
                    this@InProgressTransferItemTest.progressSizeString,
                    progress,
                    speed,
                    isPaused,
                    isOverQuota,
                    areTransfersPaused,
                    onPlayPauseClicked,
                )
            }
        }
}