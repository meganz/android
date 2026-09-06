package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.components.settings.SettingsActionItem
import mega.android.core.ui.components.settings.SettingsNavigationItem
import mega.android.core.ui.components.settings.SettingsOptionsItem
import mega.android.core.ui.components.settings.SettingsToggleItem
import mega.privacy.android.a11ytest.ThemeReporters
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility baseline sweep of the design-system settings item family, covering toggle,
 * navigation, options and action items.
 */
@RunWith(AndroidJUnit4::class)
class SettingsItemsAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporters = ThemeReporters("SettingsItemsAccessibilityTest")

    @Test
    fun test_that_settings_toggle_items_pass_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            SettingsToggleItem(
                key = "cameraUploads",
                title = "Camera uploads",
                subtitle = "Upload photos automatically",
                checked = true,
                onSettingsChanged = { _, _ -> },
            )
            SettingsToggleItem(
                key = "mobileData",
                title = "Use mobile data",
                subtitle = null,
                checked = false,
                onSettingsChanged = { _, _ -> },
            )
            SettingsToggleItem(
                key = "disabled",
                title = "Include videos",
                subtitle = "Requires camera uploads",
                checked = false,
                enabled = false,
                onSettingsChanged = { _, _ -> },
            )
        }

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_settings_navigation_and_options_items_pass_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            SettingsNavigationItem(
                key = "security",
                title = "Security",
                subtitle = "Passcode and two-factor authentication",
                onClicked = {},
            )
            SettingsOptionsItem(
                key = "viewMode",
                title = "View mode",
                values = listOf("List", "Grid"),
                selectedValue = "List",
                footerText = "Applies to all folders.",
                onValueSelected = { _, _ -> },
            )
        }

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_settings_action_items_pass_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            SettingsActionItem(
                key = "clearCache",
                title = "Clear cache",
                subtitle = "1.2 GB",
                onClicked = {},
            )
            SettingsActionItem(
                key = "logout",
                title = "Log out",
                destructive = true,
                onClicked = {},
            )
        }

        sweep.checkInEachTheme()
    }
}
