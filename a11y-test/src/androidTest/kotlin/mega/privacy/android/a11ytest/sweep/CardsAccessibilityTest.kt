package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.apps.common.testing.accessibility.framework.AccessibilityCheckResultUtils.matchesCheck
import com.google.android.apps.common.testing.accessibility.framework.checks.TouchTargetSizeCheck
import com.google.common.truth.Truth.assertThat
import mega.android.core.ui.R as CoreUiR
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.card.PlanFeature
import mega.android.core.ui.components.card.PlansCard
import mega.android.core.ui.components.card.RoundCard
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.a11ytest.ThemeReporters
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility baseline sweep of the design-system card family, covering the plain round card
 * container and the fully populated plans card.
 */
@RunWith(AndroidJUnit4::class)
class CardsAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporters = ThemeReporters("CardsAccessibilityTest")

    @Test
    fun test_that_round_card_passes_accessibility_checks() {
        val sweep = composeTestRule.setSweepContent(reporters) {
            RoundCard(modifier = Modifier.fillMaxWidth()) {
                MegaText(
                    modifier = Modifier.padding(16.dp),
                    text = "Card content",
                    textColor = TextColor.Primary,
                )
            }
        }

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_plans_card_passes_accessibility_checks_when_the_link_touch_target_baseline_is_suppressed() {
        // The plans card renders its link as a plain clickable text only 40dp tall, below the
        // 48dp touch target minimum. This cannot be fixed in this repository, so
        // TouchTargetSizeCheck is baselined for this test only.
        val sweep = composeTestRule.setSweepContent(
            reporters = reporters,
            baselineSuppressions = matchesCheck(TouchTargetSizeCheck::class.java),
        ) {
            PlansCard(
                modifier = Modifier.fillMaxWidth(),
                title = "Pro I",
                subtitle = "Renews 01/01/2027",
                price = "9.99 EUR / month",
                featuresSectionTitle = "Your plan includes",
                features = listOf(
                    PlanFeature(
                        icon = CoreUiR.drawable.ic_close_medium_thin_outline,
                        title = "2 TB storage",
                    ),
                    PlanFeature(
                        icon = CoreUiR.drawable.ic_close_medium_thin_outline,
                        title = "2 TB transfer",
                    ),
                ),
                linkText = "See all features",
                onLinkClickListener = {},
                primaryButtonText = "Upgrade",
                onPrimaryButtonClick = {},
                secondaryButtonText = "Cancel plan",
                onSecondaryButtonClick = {},
                footerText = "Prices include VAT",
            )
        }

        sweep.checkInEachTheme()

        val suppressedBaseline = reporters.entries.filter {
            it.checkName == "TouchTargetSizeCheck" &&
                it.resultType == "SUPPRESSED" &&
                it.metadata.containsKey(TouchTargetSizeCheck.KEY_HEIGHT)
        }
        assertThat(suppressedBaseline).isNotEmpty()
    }
}
