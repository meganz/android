package mega.privacy.android.a11ytest.sweep

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.list.OneLineListItem
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.a11ytest.AccessibilityCheckReporter
import mega.privacy.android.a11ytest.megaAccessibilityValidator
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility baseline sweep of the design-system modal bottom sheet. Sheets render in their
 * own window, so checks are triggered from a node inside the sheet rather than the host
 * activity root.
 */
@OptIn(ExperimentalMaterial3Api::class)
@RunWith(AndroidJUnit4::class)
class BottomSheetsAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporter = AccessibilityCheckReporter(reportName = "BottomSheetsAccessibilityTest")

    @Before
    fun setUp() {
        composeTestRule.enableAccessibilityChecks(megaAccessibilityValidator(reporter))
    }

    @Test
    fun test_that_modal_bottom_sheet_with_list_items_passes_accessibility_checks() {
        composeTestRule.setSweepContent {
            MegaModalBottomSheet(
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                bottomSheetBackground = MegaModalBottomSheetBackground.Surface1,
                onDismissRequest = {},
            ) {
                OneLineListItem(text = "Share link")
                OneLineListItem(text = "Download")
                OneLineListItem(text = "Move to Rubbish bin")
            }
        }

        composeTestRule.onNodeWithText("Share link").tryPerformAccessibilityChecks()
    }

    @Test
    fun test_that_modal_bottom_sheet_with_page_background_passes_accessibility_checks() {
        composeTestRule.setSweepContent {
            MegaModalBottomSheet(
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                bottomSheetBackground = MegaModalBottomSheetBackground.PageBackground,
                onDismissRequest = {},
            ) {
                MegaText(text = "Sheet content", textColor = TextColor.Primary)
            }
        }

        composeTestRule.onNodeWithText("Sheet content").tryPerformAccessibilityChecks()
    }
}
