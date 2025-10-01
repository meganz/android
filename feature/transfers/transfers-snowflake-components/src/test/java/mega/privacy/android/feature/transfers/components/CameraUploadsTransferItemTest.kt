package mega.privacy.android.feature.transfers.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith
import mega.privacy.android.icon.pack.R
import org.junit.Test

@RunWith(AndroidJUnit4::class)
class CameraUploadsTransferItemTest {
    @get:Rule
    var composeRule = createComposeRule()

    private val testName = "File name.pdf"
    private val testProgressSizeString = "6MB of 10MB"
    private val testProgressPercentString = "60%"
    private val testProgress = 0.6f
    private val testSpeed = "4.2MB/s"
    private val testTag = 1

    private fun initCameraUploadsActiveTransferItem() {
        composeRule.setContent {
            CameraUploadsActiveTransferItem(
                tag = testTag,
                fileTypeResId = R.drawable.ic_video_medium_solid,
                previewUri = null,
                fileName = testName,
                progressPercentageString = testProgressSizeString,
                progressSizeString = testProgressPercentString,
                progress = testProgress,
                speed = testSpeed,
            )
        }
    }

    private fun initCameraUploadsInQueueTransferItemItem() {
        composeRule.setContent {
            CameraUploadsInQueueTransferItem(
                tag = testTag,
                fileTypeResId = R.drawable.ic_video_medium_solid,
                previewUri = null,
                fileName = testName,
            )
        }
    }

    @Test
    fun `test that CameraUploadsActiveTransferItem shows correctly`() {
        initCameraUploadsActiveTransferItem()
        with(composeRule) {
            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_ACTIVE_TRANSFER_ITEM + "_$testTag").assertIsDisplayed()
            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithText(testName).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFER_NAME).assertIsDisplayed()
            onNodeWithText(testProgressSizeString, substring = true).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFER_SUBTITLE).assertIsDisplayed()
            onNodeWithText(testSpeed, substring = true).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFER_TYPE_ICON).assertIsDisplayed()
        }
    }

    @Test
    fun `test that initCameraUploadsInQueueTransferItemItem shows correctly`() {
        initCameraUploadsInQueueTransferItemItem()
        with(composeRule) {
            onNodeWithTag(TEST_TAG_CAMERA_UPLOADS_IN_QUEUE_TRANSFER_ITEM + "_$testTag").assertIsDisplayed()
            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFER_IMAGE).assertIsDisplayed()
            onNodeWithText(testName).assertIsDisplayed()
            onNodeWithTag(TEST_TAG_ACTIVE_TRANSFER_NAME).assertIsDisplayed()
        }
    }
}