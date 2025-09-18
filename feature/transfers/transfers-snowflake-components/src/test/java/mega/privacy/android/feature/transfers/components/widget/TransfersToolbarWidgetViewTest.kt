package mega.privacy.android.feature.transfers.components.widget

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
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class TransfersToolbarWidgetViewTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that when transfersInfo status is paused the paused icon is shown`() {
        testStatusIcon(TransfersToolbarWidgetStatus.Paused)
    }

    @Test
    fun `test that when transfersInfo status is over quota the over quota icon is shown`() {
        testStatusIcon(TransfersToolbarWidgetStatus.OverQuota)
    }

    @Test
    fun `test that when transfersInfo status is transfer error the transfer error icon is shown`() {
        testStatusIcon(TransfersToolbarWidgetStatus.Error)
    }

    @Test
    fun `test that when transfersInfo status is completed the transfer completed icon is shown`() {
        testStatusIcon(TransfersToolbarWidgetStatus.Completed)
    }

    @Test
    fun `test that when transfersInfo status is Transferring the transfer error icon is not shown`() {
        composeTestRule.setContent {
            TransfersToolbarWidgetView(TransfersToolbarWidgetStatus.Transferring, 10, 5) {}
        }
        composeTestRule.onNodeWithTag(TAG_STATUS_ICON).assertDoesNotExist()
    }


    @Test
    fun `test that transfer widget animated is shown when status is transferring`() {
        composeTestRule.setContent {
            TransfersToolbarWidgetViewAnimated(
                TransfersToolbarWidgetStatus.Transferring, 10, 5,
            ) {}
        }
        composeTestRule.onNodeWithTag(TAG_TRANSFERS_WIDGET, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that transfer widget animated is not shown when visibility is set to false`() {
        composeTestRule.setContent {
            TransfersToolbarWidgetViewAnimated(
                TransfersToolbarWidgetStatus.Completed, 10, 5,
                onClick = {},
            )
        }
        composeTestRule.onNodeWithTag(TAG_TRANSFERS_WIDGET, useUnmergedTree = true)
            .assertIsNotDisplayed()
    }

    @Test
    fun `test that completed is shown for a while and then hide when status changes to Completed from Transferring`() =
        runTest {
            var transfersInfo by mutableStateOf(TransfersToolbarWidgetStatus.Transferring)

            composeTestRule.setContent {
                TransfersToolbarWidgetViewAnimated(
                    transfersInfo, 1L,1L
                )
            }

            composeTestRule.onNodeWithTag(TAG_TRANSFERS_WIDGET, useUnmergedTree = true)
                .assertIsDisplayed()

            transfersInfo = TransfersToolbarWidgetStatus.Completed
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

    @Test
    fun `test that completed is not shown when status changes to Cancelled`() =
        runTest {
            var transfersInfo by mutableStateOf(TransfersToolbarWidgetStatus.Transferring)

            composeTestRule.setContent {
                TransfersToolbarWidgetViewAnimated(
                    transfersInfo, 1L,1L
                )
            }

            composeTestRule.onNodeWithTag(TAG_TRANSFERS_WIDGET, useUnmergedTree = true)
                .assertIsDisplayed()

            transfersInfo = TransfersToolbarWidgetStatus.Idle
            composeTestRule.waitForIdle()

            //completed is not visible
            composeTestRule.onNodeWithTag(TAG_STATUS_ICON, useUnmergedTree = true)
                .assertDoesNotExist()
        }

    @Test
    fun `test that completed is not shown when status changes to Completed from Cancelled`() =
        runTest {
            var transfersInfo by mutableStateOf(TransfersToolbarWidgetStatus.Transferring)

            composeTestRule.setContent {
                TransfersToolbarWidgetViewAnimated(
                    transfersInfo, 1L,1L
                )
            }

            composeTestRule.onNodeWithTag(TAG_TRANSFERS_WIDGET, useUnmergedTree = true)
                .assertIsDisplayed()

            transfersInfo = TransfersToolbarWidgetStatus.Idle
            composeTestRule.waitForIdle()
            transfersInfo = TransfersToolbarWidgetStatus.Completed
            composeTestRule.waitForIdle()

            //completed is not visible
            composeTestRule.onNodeWithTag(TAG_STATUS_ICON, useUnmergedTree = true)
                .assertDoesNotExist()
        }

    private fun testStatusIcon(status: TransfersToolbarWidgetStatus) {
        composeTestRule.setContent {
            TransfersToolbarWidgetView(status, 10, 5) {}
        }
        composeTestRule.onNodeWithTag(TAG_STATUS_ICON, useUnmergedTree = true)
            .assertContentDescriptionEquals(status.name)
    }
}