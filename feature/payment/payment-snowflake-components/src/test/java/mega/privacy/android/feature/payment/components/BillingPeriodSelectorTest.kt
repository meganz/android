package mega.privacy.android.feature.payment.components

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BillingPeriodSelectorTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun `test that selecting monthly invokes callback with true`() {
        var selectedMonthly: Boolean? = null
        composeRule.setContent {
            BillingPeriodSelector(
                modifier = Modifier.fillMaxWidth(),
                isMonthly = false,
                onPeriodSelected = { selectedMonthly = it },
                monthlyLabel = "Monthly",
                yearlyLabel = "Yearly",
                saveLabel = "Save up to 16%",
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_BILLING_PERIOD_MONTHLY).performClick()
        assertThat(selectedMonthly).isTrue()
    }

    @Test
    fun `test that selecting yearly invokes callback with false`() {
        var selectedMonthly: Boolean? = null
        composeRule.setContent {
            BillingPeriodSelector(
                modifier = Modifier.fillMaxWidth(),
                isMonthly = true,
                onPeriodSelected = { selectedMonthly = it },
                monthlyLabel = "Monthly",
                yearlyLabel = "Yearly",
                saveLabel = "Save up to 16%",
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_BILLING_PERIOD_YEARLY).performClick()
        assertThat(selectedMonthly).isFalse()
    }

    @Test
    fun `test that save label is shown`() {
        composeRule.setContent {
            BillingPeriodSelector(
                modifier = Modifier.fillMaxWidth(),
                isMonthly = true,
                onPeriodSelected = {},
                monthlyLabel = "Monthly",
                yearlyLabel = "Yearly",
                saveLabel = "Save up to 16%",
            )
        }
        composeRule.onNodeWithTag(TEST_TAG_BILLING_PERIOD_SAVE_LABEL, useUnmergedTree = true)
            .assertExists()
    }
}
