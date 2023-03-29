package test.mega.privacy.android.app.presentation.fingerprintauth

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.fingerprintauth.SecurityUpgradeDialogView
import mega.privacy.android.app.presentation.shares.outgoing.model.OutgoingSharesState
import mega.privacy.android.domain.entity.ShareData
import nz.mega.sdk.MegaNode
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import test.mega.privacy.android.app.onNodeWithText

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SecurityUpgradeDialogTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val node1 = mock<MegaNode>()
    private val node2 = mock<MegaNode>()

    private val shareData1 = mock<ShareData>()
    private val shareData2 = mock<ShareData>()

    private val pair1: Pair<MegaNode, ShareData> = Pair(node1, shareData1)
    private val pair2: Pair<MegaNode, ShareData> = Pair(node2, shareData2)

    private val nodes = listOf(pair1, pair2)

    private fun initComposeRule() {
        composeTestRule.setContent {
            SecurityUpgradeDialogView(state = OutgoingSharesState(nodes = nodes),
                onOkClick = { },
                onCloseClick = { })
        }
    }

    @Test
    fun test_that_imageview_resource_is_as_expected() {
        initComposeRule()
        composeTestRule.run {
            onNodeWithTag("HeaderImage").assertIsDisplayed()
            onNodeWithText(R.string.shared_items_security_upgrade_dialog_title).assertIsDisplayed()
            onNodeWithText(R.string.shared_items_security_upgrade_dialog_content).assertIsDisplayed()
            onNodeWithTag("SharedNodeInfo").assertIsDisplayed()
            onNodeWithText(R.string.shared_items_security_upgrade_dialog_button_ok_got_it).assertIsDisplayed()
        }
    }
}