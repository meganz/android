package test.mega.privacy.android.app.upgradeAccount.components


import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.upgradeAccount.view.components.MONTHLY_CHECK_ICON_TAG
import mega.privacy.android.app.upgradeAccount.view.components.MonthlyYearlyTabs
import mega.privacy.android.app.upgradeAccount.view.components.YEARLY_CHECK_ICON_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MonthlyYearlyTabsTest {
    @get:Rule
    var composeRule = createComposeRule()

    @Test
    fun `test that if isMonthly is true the monthly button displays correctly`() {
        composeRule.setContent {
            MonthlyYearlyTabs(
                isMonthly = true,
                onTabClicked = {},
                testTag = ""
            )
        }
        composeRule.onNodeWithTag(MONTHLY_CHECK_ICON_TAG, useUnmergedTree = true)
            .assertExists()
        composeRule.onNodeWithTag(YEARLY_CHECK_ICON_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
    }

    @Test
    fun `test that if isMonthly is false the yearly button displays correctly`() {
        composeRule.setContent {
            MonthlyYearlyTabs(
                isMonthly = false,
                onTabClicked = {},
                testTag = ""
            )
        }
        composeRule.onNodeWithTag(MONTHLY_CHECK_ICON_TAG, useUnmergedTree = true)
            .assertDoesNotExist()
        composeRule.onNodeWithTag(YEARLY_CHECK_ICON_TAG, useUnmergedTree = true).assertExists()
    }
}