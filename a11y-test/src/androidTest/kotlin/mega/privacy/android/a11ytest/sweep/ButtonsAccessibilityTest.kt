package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.common.truth.Truth.assertThat
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
import mega.privacy.android.a11ytest.AccessibilityCheckReporter
import mega.privacy.android.a11ytest.megaAccessibilityValidator
import org.junit.Before
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

    private val reporter = AccessibilityCheckReporter(reportName = "ButtonsAccessibilityTest")

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(megaAccessibilityValidator(reporter))
    }

    @Test
    fun test_that_filled_buttons_pass_accessibility_checks() {
        composeTestRule.setSweepContent {
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

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_outlined_and_text_only_buttons_pass_accessibility_checks() {
        composeTestRule.setSweepContent {
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

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_icon_buttons_pass_accessibility_checks() {
        composeTestRule.setSweepContent {
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

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_radio_buttons_pass_accessibility_checks() {
        composeTestRule.setSweepContent {
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

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_checkboxes_pass_accessibility_checks_when_the_missing_label_baseline_is_suppressed() {
        // The core-ui Checkbox ships without a speakable label and cannot be fixed in this
        // repository, so SpeakableTextPresentCheck is baselined for this test only.
        composeTestRule.enableAccessibilityChecks(
            megaAccessibilityValidator(
                reporter = reporter,
                baselineSuppressions = matchesCheck(SpeakableTextPresentCheck::class.java),
            )
        )
        composeTestRule.setSweepContent {
            Checkbox(
                checked = true,
                onCheckStateChanged = {},
            )
            Checkbox(
                checked = false,
                onCheckStateChanged = {},
            )
            Checkbox(
                checked = true,
                onCheckStateChanged = {},
                enabled = false,
            )
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()

        val suppressedBaseline = reporter.entries.filter {
            it.checkName == "SpeakableTextPresentCheck" && it.resultType == "SUPPRESSED"
        }
        assertThat(suppressedBaseline).isNotEmpty()
    }

    @Test
    fun test_that_toggles_pass_accessibility_checks() {
        composeTestRule.setSweepContent {
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

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
