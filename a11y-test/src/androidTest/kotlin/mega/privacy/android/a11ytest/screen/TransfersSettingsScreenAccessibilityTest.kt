package mega.privacy.android.a11ytest.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.a11ytest.ThemeReporters
import mega.privacy.android.analytics.test.AnalyticsTestRule
import mega.privacy.android.feature.transfers.presentation.settings.model.TransfersSettingsUiState
import mega.privacy.android.feature.transfers.presentation.settings.view.TransfersSettingsView
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility sweep of the Transfers settings screen from `:feature:transfers:transfers`.
 *
 * The screen tracks a screen-view analytics event on composition, so [AnalyticsTestRule] installs
 * a recording tracker into the `Analytics` singleton. That rule is the only collaborator the
 * stateless mount needs — the UI states themselves are hand-built.
 */
@RunWith(AndroidJUnit4::class)
class TransfersSettingsScreenAccessibilityTest {

    @get:Rule
    val analyticsRule = AnalyticsTestRule()

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporters = ThemeReporters("TransfersSettingsScreenAccessibilityTest")

    @Test
    fun test_that_the_populated_transfers_settings_screen_passes_accessibility_checks() {
        val sweep = composeTestRule.setScreenContent(reporters) {
            TransfersSettingsView(
                uiState = TransfersSettingsUiState.Data(
                    maxDownloadConnections = 4,
                    maxUploadConnections = 2,
                    maxTransferConnectionsRange = 1..6,
                ),
                onSetMaxDownloadConnections = {},
                onSetMaxUploadConnections = {},
                onNavigateBack = {},
            )
        }

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_the_connections_modal_passes_accessibility_checks() {
        val sweep = composeTestRule.setScreenContent(reporters) {
            TransfersSettingsView(
                uiState = TransfersSettingsUiState.Data(
                    maxDownloadConnections = 4,
                    maxUploadConnections = 3,
                    maxTransferConnectionsRange = 1..6,
                ),
                onSetMaxDownloadConnections = {},
                onSetMaxUploadConnections = {},
                onNavigateBack = {},
            )
        }

        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(
                    sharedR.string.settings_transfer_download_connections
                )
            )
            .performClick()

        // The options modal renders in its own window, so the checks have to start from a node
        // inside it rather than from the screen root.
        sweep.checkInEachTheme {
            onNodeWithText(UNLABELLED_CONNECTIONS_OPTION).tryPerformAccessibilityChecks()
        }
    }

    @Test
    fun test_that_the_loading_transfers_settings_screen_passes_accessibility_checks() {
        val sweep = composeTestRule.setScreenContent(reporters) {
            TransfersSettingsView(
                uiState = TransfersSettingsUiState.Loading,
                onSetMaxDownloadConnections = {},
                onSetMaxUploadConnections = {},
                onNavigateBack = {},
            )
        }

        sweep.checkInEachTheme()
    }
}

/**
 * A connection count the screen renders verbatim, unlike the values that carry a "default" or
 * "best for slow networks" caption. It only appears inside the options modal, so matching it
 * proves the checks run against the modal window.
 */
private const val UNLABELLED_CONNECTIONS_OPTION = "2"
