package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.R as CoreUiR
import mega.android.core.ui.components.button.MegaOutlinedButton
import mega.android.core.ui.components.button.MegaRadioButton
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.PrimarySmallIconButton
import mega.android.core.ui.components.button.SecondaryFilledButton
import mega.android.core.ui.components.button.SecondaryNavigationIconButton
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.components.checkbox.Checkbox
import mega.android.core.ui.components.toggle.Toggle
import mega.privacy.android.a11ytest.ThemeReporters
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility baseline sweep of the design-system button family, covering filled, outlined,
 * text-only and icon buttons plus the standalone selection controls.
 */
@RunWith(AndroidJUnit4::class)
class ButtonsAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporters = ThemeReporters("ButtonsAccessibilityTest")

    @Test
    fun test_that_filled_buttons_pass_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            PrimaryFilledButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Primary enabled",
                onClick = {},
            )
            PrimaryFilledButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Primary disabled",
                onClick = {},
                enabled = false,
            )
            PrimaryFilledButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Primary with icon",
                onClick = {},
                leadingIcon = painterResource(CoreUiR.drawable.ic_close_medium_thin_outline),
            )
            PrimaryFilledButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Primary loading",
                onClick = {},
                isLoading = true,
            )
            SecondaryFilledButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Secondary enabled",
                onClick = {},
            )
            SecondaryFilledButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Secondary disabled",
                onClick = {},
                enabled = false,
            )
        }

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_outlined_and_text_only_buttons_pass_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            MegaOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Outlined enabled",
                onClick = {},
            )
            MegaOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Outlined disabled",
                onClick = {},
                enabled = false,
            )
            MegaOutlinedButton(
                modifier = Modifier.fillMaxWidth(),
                text = "Outlined with icon",
                onClick = {},
                leadingIcon = painterResource(CoreUiR.drawable.ic_close_medium_thin_outline),
            )
            TextOnlyButton(
                text = "Text only enabled",
                onClick = {},
            )
            TextOnlyButton(
                text = "Text only disabled",
                onClick = {},
                enabled = false,
            )
        }

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_icon_buttons_pass_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            PrimarySmallIconButton(
                icon = painterResource(CoreUiR.drawable.ic_close_medium_thin_outline),
                onClick = {},
                contentDescription = "Dismiss banner",
            )
            PrimarySmallIconButton(
                icon = painterResource(CoreUiR.drawable.ic_close_medium_thin_outline),
                onClick = {},
                enabled = false,
                contentDescription = "Dismiss banner disabled",
            )
            SecondaryNavigationIconButton(
                icon = painterResource(CoreUiR.drawable.ic_close_medium_thin_outline),
                onClick = {},
                contentDescription = "Close screen",
            )
        }

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_radio_buttons_pass_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            MegaRadioButton(
                identifier = "selected",
                onOptionSelected = {},
                selected = true,
            )
            MegaRadioButton(
                identifier = "unselected",
                onOptionSelected = {},
                selected = false,
            )
            MegaRadioButton(
                identifier = "disabled",
                onOptionSelected = {},
                enabled = false,
            )
        }

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_checkboxes_pass_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            Checkbox(
                checked = true,
                onCheckStateChanged = {},
                contentDescription = "Checked option",
            )
            Checkbox(
                checked = false,
                onCheckStateChanged = {},
                contentDescription = "Unchecked option",
            )
            Checkbox(
                checked = true,
                onCheckStateChanged = {},
                enabled = false,
                contentDescription = "Disabled option",
            )
        }

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_toggles_pass_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            Toggle(
                isChecked = true,
                onCheckedChange = {},
            )
            Toggle(
                isChecked = false,
                onCheckedChange = {},
            )
            Toggle(
                isChecked = true,
                onCheckedChange = {},
                isEnabled = false,
            )
        }

        sweep.checkInEachTheme()
    }
}
