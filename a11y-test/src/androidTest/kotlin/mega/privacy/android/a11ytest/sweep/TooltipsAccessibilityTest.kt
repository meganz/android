package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.SpeakableTextPresentCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.components.tooltip.popup.interactive.InteractiveRightDirectionTooltipPopup
import mega.android.core.ui.components.tooltip.popup.interactive.InteractiveTooltipButtonProperties
import mega.android.core.ui.components.tooltip.popup.interactive.InteractiveTooltipStepCounterProperties
import mega.android.core.ui.components.tooltip.popup.simple.SimpleLeftDirectionTooltipPopup
import mega.android.core.ui.components.tooltip.popup.simple.SimpleTooltipContentTitleAndBody
import mega.privacy.android.a11ytest.AccessibilityCheckReporter
import mega.privacy.android.a11ytest.megaAccessibilityValidator
import org.hamcrest.CoreMatchers.anyOf
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility baseline sweep of the design-system tooltip family, covering the simple and
 * interactive tooltip popups anchored to a plain box. Tooltips render in their own popup
 * window, so checks are triggered from a node inside the tooltip rather than the host activity
 * root.
 */
@RunWith(AndroidJUnit4::class)
class TooltipsAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporter = AccessibilityCheckReporter(reportName = "TooltipsAccessibilityTest")

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(megaAccessibilityValidator(reporter))
    }

    @Test
    fun test_that_simple_tooltip_passes_accessibility_checks() {
        composeTestRule.setSweepContent {
            var anchorCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .onGloballyPositioned { anchorCoordinates = it },
            )
            anchorCoordinates?.let { coordinates ->
                SimpleLeftDirectionTooltipPopup(
                    anchorViewCoordinates = coordinates,
                    content = SimpleTooltipContentTitleAndBody(
                        title = "New feature",
                        body = "Tap here to manage your files offline.",
                    ),
                )
            }
        }

        composeTestRule.onNodeWithText("New feature").tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_interactive_tooltip_passes_accessibility_checks_when_the_baseline_is_suppressed() {
        // Two interactive tooltip findings cannot be fixed in this repository and are baselined
        // for this test only: the close icon is a clickable without speakable text, and the
        // inline text buttons are clickables only 28dp tall, below the 48dp touch target
        // minimum.
        composeTestRule.enableAccessibilityChecks(
            megaAccessibilityValidator(
                reporter = reporter,
                baselineSuppressions = anyOf(
                    matchesCheck(SpeakableTextPresentCheck::class.java),
                    matchesCheck(TouchTargetSizeCheck::class.java),
                ),
            )
        )
        composeTestRule.setSweepContent {
            var anchorCoordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .onGloballyPositioned { anchorCoordinates = it },
            )
            anchorCoordinates?.let { coordinates ->
                InteractiveRightDirectionTooltipPopup(
                    title = "Sync your folders",
                    anchorViewCoordinates = coordinates,
                    body = "Keep local and cloud folders in sync automatically.",
                    primaryButton = InteractiveTooltipButtonProperties(
                        text = "Next",
                        onClick = {},
                    ),
                    secondaryButton = InteractiveTooltipButtonProperties(
                        text = "Skip",
                        onClick = {},
                    ),
                    stepCounter = InteractiveTooltipStepCounterProperties(
                        currentPage = 1,
                        totalPage = 3,
                    ),
                    needCloseIcon = true,
                    needDivider = true,
                )
            }
        }

        composeTestRule.onNodeWithText("Sync your folders").tryPerformAccessibilityChecks()

        val suppressedMissingLabel = reporter.entries.filter {
            it.checkName == "SpeakableTextPresentCheck" && it.resultType == "SUPPRESSED"
        }
        assertThat(suppressedMissingLabel).isNotEmpty()
        val suppressedTouchTarget = reporter.entries.filter {
            it.checkName == "TouchTargetSizeCheck" &&
                it.resultType == "SUPPRESSED" &&
                it.metadata.containsKey(TouchTargetSizeCheck.KEY_HEIGHT)
        }
        assertThat(suppressedTouchTarget).isNotEmpty()
    }
}
