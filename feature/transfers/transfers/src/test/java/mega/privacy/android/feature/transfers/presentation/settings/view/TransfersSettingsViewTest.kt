package mega.privacy.android.feature.transfers.presentation.settings.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.transfers.presentation.settings.model.TransfersSettingsUiState
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class TransfersSettingsViewTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private val onSetMaxDownloadConnections = mock<(Int) -> Unit>()
    private val onSetMaxUploadConnections = mock<(Int) -> Unit>()
    private val onNavigateBack = mock<() -> Unit>()

    private fun getString(resId: Int, vararg formatArgs: Any): String =
        composeRule.activity.getString(resId, *formatArgs)

    private fun initComposeRuleContent(
        uiState: TransfersSettingsUiState = TransfersSettingsUiState.Loading,
    ) {
        composeRule.setContent {
            TransfersSettingsView(
                uiState = uiState,
                onSetMaxDownloadConnections = onSetMaxDownloadConnections,
                onSetMaxUploadConnections = onSetMaxUploadConnections,
                onNavigateBack = onNavigateBack,
            )
        }
    }

    @Test
    fun `test that root view is displayed when ui state is Loading`() {
        initComposeRuleContent(uiState = TransfersSettingsUiState.Loading)

        composeRule.onNodeWithTag(TRANSFERS_SETTINGS_VIEW_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that root view is displayed when ui state is Data`() {
        initComposeRuleContent(uiState = DATA_STATE)

        composeRule.onNodeWithTag(TRANSFERS_SETTINGS_VIEW_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that top bar title is displayed`() {
        initComposeRuleContent()

        composeRule
            .onNodeWithText(getString(sharedR.string.general_section_transfers))
            .assertIsDisplayed()
    }

    @Test
    fun `test that onNavigateBack is invoked when navigation icon is clicked`() {
        initComposeRuleContent()

        composeRule.onNodeWithContentDescription(
            label = "Navigation Icon",
            substring = true,
            ignoreCase = true,
            useUnmergedTree = true,
        ).performClick()

        verify(onNavigateBack).invoke()
    }

    @Test
    fun `test that transfer connections title and description are displayed`() {
        initComposeRuleContent(uiState = DATA_STATE)

        composeRule.onNodeWithText(
            getString(sharedR.string.settings_transfer_connections_title)
        ).assertIsDisplayed()
        composeRule.onNodeWithText(
            getString(sharedR.string.settings_transfer_connections_text)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that download connections option is displayed`() {
        initComposeRuleContent(uiState = DATA_STATE)

        composeRule.onNodeWithTag(downloadItemTag).assertIsDisplayed()
        composeRule.onNodeWithText(
            getString(sharedR.string.settings_transfer_download_connections)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that upload connections option is displayed`() {
        initComposeRuleContent(uiState = DATA_STATE)

        composeRule.onNodeWithTag(uploadItemTag).assertIsDisplayed()
        composeRule.onNodeWithText(
            getString(sharedR.string.settings_transfer_upload_connections)
        ).assertIsDisplayed()
    }

    @Test
    fun `test that download connections subtitle shows the selected value when ui state is Data`() {
        initComposeRuleContent(uiState = DATA_STATE)

        composeRule.onNodeWithText(
            getString(
                sharedR.string.settings_transfer_connections_default,
                DEFAULT_DOWNLOAD_CONNECTIONS
            )
        ).assertIsDisplayed()
    }

    @Test
    fun `test that upload connections subtitle shows the selected value when ui state is Data`() {
        initComposeRuleContent(uiState = DATA_STATE)

        composeRule.onNodeWithText(
            getString(
                sharedR.string.settings_transfer_connections_default,
                DEFAULT_UPLOAD_CONNECTIONS
            )
        ).assertIsDisplayed()
    }

    @Test
    fun `test that clicking download connections item does nothing when ui state is Loading`() {
        initComposeRuleContent(uiState = TransfersSettingsUiState.Loading)

        composeRule.onNodeWithTag(downloadItemTag).performClick()
        composeRule.waitForIdle()

        verifyNoInteractions(onSetMaxDownloadConnections)
    }

    @Test
    fun `test that clicking upload connections item does nothing when ui state is Loading`() {
        initComposeRuleContent(uiState = TransfersSettingsUiState.Loading)

        composeRule.onNodeWithTag(uploadItemTag).performClick()
        composeRule.waitForIdle()

        verifyNoInteractions(onSetMaxUploadConnections)
    }

    @Test
    fun `test that clicking download connections item opens the bottom sheet when ui state is Data`() {
        initComposeRuleContent(uiState = DATA_STATE)

        composeRule.onNodeWithTag(downloadItemTag).performClick()
        composeRule.waitForIdle()

        val slowNetworksText = getString(
            sharedR.string.settings_transfer_connections_slow_networs,
            BEST_FOR_SLOW_NETWORKS
        )
        composeRule
            .onNodeWithText(slowNetworksText, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that clicking upload connections item opens the bottom sheet when ui state is Data`() {
        initComposeRuleContent(uiState = DATA_STATE)

        composeRule.onNodeWithTag(uploadItemTag).performClick()
        composeRule.waitForIdle()

        val slowNetworksText = getString(
            sharedR.string.settings_transfer_connections_slow_networs,
            BEST_FOR_SLOW_NETWORKS
        )
        composeRule
            .onNodeWithText(slowNetworksText, useUnmergedTree = true)
            .assertIsDisplayed()
    }

    @Test
    fun `test that onSetMaxDownloadConnections is invoked when a value is selected in the download bottom sheet`() {
        initComposeRuleContent(uiState = DATA_STATE)

        val slowNetworksText = getString(
            sharedR.string.settings_transfer_connections_slow_networs,
            BEST_FOR_SLOW_NETWORKS
        )
        composeRule.onNodeWithTag(downloadItemTag).performClick()
        composeRule.waitForIdle()
        composeRule
            .onNode(hasText(slowNetworksText) and hasClickAction())
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        verify(onSetMaxDownloadConnections).invoke(1)
        verifyNoInteractions(onSetMaxUploadConnections)
    }

    @Test
    fun `test that onSetMaxUploadConnections is invoked when a value is selected in the upload bottom sheet`() {
        initComposeRuleContent(uiState = DATA_STATE)

        val slowNetworksText = getString(
            sharedR.string.settings_transfer_connections_slow_networs,
            BEST_FOR_SLOW_NETWORKS
        )
        composeRule.onNodeWithTag(uploadItemTag).performClick()
        composeRule.waitForIdle()
        composeRule
            .onNode(hasText(slowNetworksText) and hasClickAction())
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        verify(onSetMaxUploadConnections).invoke(1)
        verifyNoInteractions(onSetMaxDownloadConnections)
    }

    @Test
    fun `test that toText returns min label when value is 1`() {
        val resources = composeRule.activity.resources
        assertThat(
            1.transferConnectionsValueToString(
                resources,
                default = DEFAULT_DOWNLOAD_CONNECTIONS
            )
        )
            .isEqualTo(
                getString(
                    sharedR.string.settings_transfer_connections_slow_networs,
                    BEST_FOR_SLOW_NETWORKS
                )
            )
    }

    @Test
    fun `test that toText returns default label when value matches default`() {
        val resources = composeRule.activity.resources
        assertThat(
            DEFAULT_DOWNLOAD_CONNECTIONS.transferConnectionsValueToString(
                resources,
                default = DEFAULT_DOWNLOAD_CONNECTIONS
            )
        ).isEqualTo(
            getString(
                sharedR.string.settings_transfer_connections_default,
                DEFAULT_DOWNLOAD_CONNECTIONS
            )
        )
    }

    @Test
    fun `test that toText returns max label when value is 8`() {
        val resources = composeRule.activity.resources
        assertThat(
            8.transferConnectionsValueToString(
                resources,
                default = DEFAULT_DOWNLOAD_CONNECTIONS
            )
        )
            .isEqualTo(
                getString(
                    sharedR.string.settings_transfer_connections_higher_usage,
                    DEFAULT_UPLOAD_AND_DATA_USAGE
                )
            )
    }

    @Test
    fun `test that toText returns the number as string for other values`() {
        val resources = composeRule.activity.resources
        assertThat(
            5.transferConnectionsValueToString(
                resources,
                default = DEFAULT_DOWNLOAD_CONNECTIONS
            )
        ).isEqualTo("5")
    }

    companion object {
        private val DATA_STATE = TransfersSettingsUiState.Data(
            maxDownloadConnections = DEFAULT_DOWNLOAD_CONNECTIONS,
            maxUploadConnections = DEFAULT_UPLOAD_CONNECTIONS,
            maxTransferConnectionsRange = 1..8,
        )

        private val downloadItemTag = "settings_$DOWNLOAD_CONNECTIONS_VIEW_TAG:list_item"
        private val uploadItemTag = "settings_$UPLOAD_CONNECTIONS_VIEW_TAG:list_item"
    }
}
