package mega.privacy.android.shared.original.core.ui.controls.widgets

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import mega.privacy.android.shared.original.core.ui.model.TransfersInfo
import mega.privacy.android.shared.original.core.ui.model.TransfersStatus
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

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
            TransfersWidgetView(
                TransfersInfo(TransfersStatus.Transferring, 10, 5, true)
            ) {}
        }
        composeTestRule.onNodeWithTag(TAG_STATUS_ICON).assertDoesNotExist()
    }

    @Test
    fun `test that when completed is true the completed icon is shown`() {
        composeTestRule.setContent {
            TransfersWidgetView(
                TransfersInfo(TransfersStatus.NotTransferring, 10, 5, true),
                completed = true
            ) {}
        }
        composeTestRule.onNodeWithTag(TAG_STATUS_ICON, useUnmergedTree = true)
            .assertContentDescriptionEquals("Completed")
    }

    @Test
    fun `test that when transfersInfo is uploading then the uploading icon is shown`() {
        composeTestRule.setContent {
            TransfersWidgetView(
                TransfersInfo(TransfersStatus.Transferring, 10, 5, true)
            ) {}
        }
        composeTestRule.onNodeWithTag(TAG_UPLOADING_DOWNLOADING_ICON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that when transfersInfo is downloading then the downloading icon is shown`() {
        composeTestRule.setContent {
            TransfersWidgetView(
                TransfersInfo(TransfersStatus.Transferring, 10, 5, false)
            ) {}
        }
        composeTestRule.onNodeWithTag(TAG_UPLOADING_DOWNLOADING_ICON, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that transfer widget animated is shown when status is transferring`() {
        composeTestRule.setContent {
            TransfersWidgetViewAnimated(
                TransfersInfo(TransfersStatus.Transferring, 10, 5, false),
            ) {}
        }
        composeTestRule.onNodeWithTag(TAG_TRANSFERS_WIDGET, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that transfer widget animated is not shown when visibility is set to false`() {
        composeTestRule.setContent {
            TransfersWidgetViewAnimated(
                TransfersInfo(TransfersStatus.NotTransferring, 10, 5, false),
                onClick = {},
            )
        }
        composeTestRule.onNodeWithTag(TAG_TRANSFERS_WIDGET, useUnmergedTree = true)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that completed is shown for a while and then hide when status changes to NotTransferring from Transferring`() =
        runTest {
            var transfersInfo by mutableStateOf(TransfersInfo(TransfersStatus.Transferring))

            composeTestRule.setContent {
                TransfersWidgetViewAnimated(
                    transfersInfo = transfersInfo,
                    onClick = {}
                )
            }

            composeTestRule.onNodeWithTag(TAG_TRANSFERS_WIDGET, useUnmergedTree = true)
                .assertIsDisplayed()

            transfersInfo = TransfersInfo(TransfersStatus.NotTransferring)
            composeTestRule.waitForIdle()

            //completed is now visible
            composeTestRule.onNodeWithTag(TAG_STATUS_ICON, useUnmergedTree = true)
                .assertContentDescriptionEquals("Completed")

            //still visible after 3.5 seconds
            composeTestRule.mainClock.advanceTimeBy(3500L)
            composeTestRule.onNodeWithTag(TAG_STATUS_ICON, useUnmergedTree = true)
                .assertContentDescriptionEquals("Completed")

            //not visible after 1 more second (4.5 seconds in total)
            composeTestRule.mainClock.advanceTimeBy(1.seconds.inWholeMilliseconds)
            composeTestRule.onNodeWithTag(TAG_TRANSFERS_WIDGET, useUnmergedTree = true)
                .assertDoesNotExist()
        }

    private fun testStatusIcon(status: TransfersStatus) {
        composeTestRule.setContent {
            TransfersWidgetView(TransfersInfo(status, 10, 5, true)) {}
        }
        composeTestRule.onNodeWithTag(TAG_STATUS_ICON, useUnmergedTree = true)
            .assertContentDescriptionEquals(status.name)
    }
}