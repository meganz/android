package mega.privacy.android.app.main.share

import android.os.Build
import android.view.View
import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.presentation.manager.model.SharesTab
import mega.privacy.android.app.presentation.shares.incoming.model.IncomingSharesState
import mega.privacy.android.app.presentation.shares.links.model.LinksUiState
import mega.privacy.android.app.presentation.shares.outgoing.model.OutgoingSharesState
import mega.privacy.android.shared.original.core.ui.controls.appbar.TEST_TAG_APP_BAR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(sdk = [Build.VERSION_CODES.Q])
@RunWith(AndroidJUnit4::class)
class SharesScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that app bar shows correctly`() {
        val uiState = SharesUiState(currentTab = SharesTab.INCOMING_TAB)

        initCompose(uiState)

        composeTestRule
            .onNodeWithTag(TEST_TAG_APP_BAR)
            .assertIsDisplayed()
    }

    @Test
    fun `test that tab row shows correctly`() {
        val uiState = SharesUiState(currentTab = SharesTab.INCOMING_TAB)

        initCompose(uiState = uiState)

        composeTestRule
            .onNodeWithTag(TAB_ROW_TEST_TAG)
            .assertIsDisplayed()
    }

    @Test
    fun `test that search menu item is shown`() {
        val uiState = SharesUiState(currentTab = SharesTab.INCOMING_TAB)

        initCompose(uiState = uiState)

        composeTestRule
            .onNodeWithTag("shares_view:action_search")
            .assertIsDisplayed()

        composeTestRule
            .onNodeWithTag("shares_view:action_more")
            .assertDoesNotExist()
    }

    @Test
    fun `test that more menu item is shown`() {
        val uiState = SharesUiState(currentTab = SharesTab.INCOMING_TAB)
        val incomingUiState = IncomingSharesState(
            currentHandle = 1L,
        )

        initCompose(uiState = uiState, incomingUiState = incomingUiState)

        composeTestRule
            .onNodeWithTag("shares_view:action_more")
            .assertIsDisplayed()
    }

    private fun initCompose(
        uiState: SharesUiState,
        incomingUiState: IncomingSharesState = IncomingSharesState(),
        outgoingUiState: OutgoingSharesState = OutgoingSharesState(),
        linksUiState: LinksUiState = LinksUiState(),
    ) {
        val customView = object : View(composeTestRule.activity) {
            override fun isInEditMode(): Boolean {
                return true
            }
        }

        composeTestRule.setContent {
            CompositionLocalProvider(LocalView provides customView) {
                SharesScreen(
                    uiState = uiState,
                    incomingUiState = incomingUiState,
                    outgoingUiState = outgoingUiState,
                    linksUiState = linksUiState,
                    onSearchClick = {},
                    onMoreClick = {},
                    onPageSelected = {}
                )
            }
        }
    }
}