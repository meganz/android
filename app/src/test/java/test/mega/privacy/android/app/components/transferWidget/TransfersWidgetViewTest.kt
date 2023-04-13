package test.mega.privacy.android.app.components.transferWidget

import androidx.compose.ui.res.stringResource
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.components.transferWidget.TAG_STATUS_ICON
import mega.privacy.android.app.components.transferWidget.TAG_UPLOADING_DOWNLOADING_ICON
import mega.privacy.android.app.components.transferWidget.TransfersWidgetView
import mega.privacy.android.domain.entity.TransfersInfo
import mega.privacy.android.domain.entity.TransfersStatus
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
        var description = ""
        composeTestRule.setContent {
            description = stringResource(id = R.string.context_upload)
            TransfersWidgetView(TransfersInfo(TransfersStatus.Transferring, 10, 5, true), {})
        }
        composeTestRule.onNodeWithTag(TAG_UPLOADING_DOWNLOADING_ICON, useUnmergedTree = true)
            .assertContentDescriptionEquals(description)
    }

    @Test
    fun `test that when transfersInfo is downloading then the downloading icon is shown`() {
        var description = ""
        composeTestRule.setContent {
            description = stringResource(id = R.string.context_download)
            TransfersWidgetView(TransfersInfo(TransfersStatus.Transferring, 10, 5, false), {})
        }
        composeTestRule.onNodeWithTag(TAG_UPLOADING_DOWNLOADING_ICON, useUnmergedTree = true)
            .assertContentDescriptionEquals(description)
    }

    private fun testStatusIcon(status: TransfersStatus) {
        composeTestRule.setContent {
            TransfersWidgetView(TransfersInfo(status, 10, 5, true), {})
        }
        composeTestRule.onNodeWithTag(TAG_STATUS_ICON, useUnmergedTree = true)
            .assertContentDescriptionEquals(status.name)
    }
}