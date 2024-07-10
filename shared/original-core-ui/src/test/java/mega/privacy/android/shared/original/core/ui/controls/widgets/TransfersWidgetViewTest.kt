package mega.privacy.android.shared.original.core.ui.controls.widgets

import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.shared.original.core.ui.model.TransfersInfo
import mega.privacy.android.shared.original.core.ui.model.TransfersStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class TransfersWidgetViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that when transfersInfo status is paused the paused icon is shown`() {
        testStatusIcon(TransfersStatus.Paused)
    }

    @Test
    fun `test that when transfersInfo status is over quota the over quota icon is shown`() {
        testStatusIcon(TransfersStatus.OverQuota)
    }

    @Test
    fun `test that when transfersInfo status is transfer error the transfer error icon is shown`() {
        testStatusIcon(TransfersStatus.TransferError)
    }

    @Test
    fun `test that when transfersInfo status is Transferring the transfer error icon is not shown`() {
        composeTestRule.setContent {
            TransfersWidgetView(TransfersInfo(TransfersStatus.Transferring, 10, 5, true), {})
        }
        composeTestRule.onNodeWithTag(TAG_STATUS_ICON).assertDoesNotExist()
    }

    @Test
    fun `test that when transfersInfo is uploading then the uploading icon is shown`() {
        composeTestRule.setContent {
            TransfersWidgetView(TransfersInfo(TransfersStatus.Transferring, 10, 5, true), {})
        }
        composeTestRule.onNodeWithTag(TAG_UPLOADING_DOWNLOADING_ICON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that when transfersInfo is downloading then the downloading icon is shown`() {
        composeTestRule.setContent {
            TransfersWidgetView(TransfersInfo(TransfersStatus.Transferring, 10, 5, false), {})
        }
        composeTestRule.onNodeWithTag(TAG_UPLOADING_DOWNLOADING_ICON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that transfer widget animated is shown when visibility is set to true`() {
        composeTestRule.setContent {
            TransfersWidgetViewAnimated(
                TransfersInfo(TransfersStatus.Transferring, 10, 5, false),
                visible = true,
                onClick = {},
            )
        }
        composeTestRule.onNodeWithTag(TAG_TRANSFERS_WIDGET, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that transfer widget animated is not shown when visibility is set to false`() {
        composeTestRule.setContent {
            TransfersWidgetViewAnimated(
                TransfersInfo(TransfersStatus.Transferring, 10, 5, false),
                visible = false,
                onClick = {},
            )
        }
        composeTestRule.onNodeWithTag(TAG_TRANSFERS_WIDGET, useUnmergedTree = true)
            .assertIsNotDisplayed()
    }

    private fun testStatusIcon(status: TransfersStatus) {
        composeTestRule.setContent {
            TransfersWidgetView(TransfersInfo(status, 10, 5, true), {})
        }
        composeTestRule.onNodeWithTag(TAG_STATUS_ICON, useUnmergedTree = true)
            .assertContentDescriptionEquals(status.name)
    }
}