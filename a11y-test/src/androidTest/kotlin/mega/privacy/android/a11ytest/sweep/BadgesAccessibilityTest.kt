package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.R as CoreUiR
import mega.android.core.ui.components.badge.Badge
import mega.android.core.ui.components.badge.BadgeType
import mega.android.core.ui.components.badge.NotificationBadge
import mega.privacy.android.a11ytest.AccessibilityCheckReporter
import mega.privacy.android.a11ytest.megaAccessibilityValidator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility baseline sweep of the design-system badge family, covering every status badge
 * type and the notification badge variants.
 */
@RunWith(AndroidJUnit4::class)
class BadgesAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporter = AccessibilityCheckReporter(reportName = "BadgesAccessibilityTest")

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(megaAccessibilityValidator(reporter))
    }

    @Test
    fun test_that_badges_of_every_type_pass_accessibility_checks() {
        composeTestRule.setSweepContent {
            BadgeType.entries.forEach { type ->
                Badge(
                    badgeType = type,
                    text = type.name,
                    icon = painterResource(CoreUiR.drawable.ic_close_medium_thin_outline),
                )
            }
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_notification_badges_pass_accessibility_checks() {
        composeTestRule.setSweepContent {
            NotificationBadge(
                imageVector = ImageVector.vectorResource(
                    CoreUiR.drawable.ic_close_medium_thin_outline
                ),
            )
            NotificationBadge(
                imageVector = ImageVector.vectorResource(
                    CoreUiR.drawable.ic_close_medium_thin_outline
                ),
                small = true,
            )
            NotificationBadge(count = 5)
            NotificationBadge(count = 120)
            NotificationBadge(text = "new")
            NotificationBadge()
        }

        composeTestRule.onRoot().tryPerformAccessibilityChecks()
    }
}
