package test.mega.privacy.android.app.presentation.cancelaccountplan.view

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.presentation.cancelaccountplan.model.UIAccountDetails
import mega.privacy.android.app.presentation.cancelaccountplan.view.CANCEL_ACCOUNT_PLAN_FEATURE_TABLE_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.CANCEL_ACCOUNT_PLAN_STORAGE_HINT_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.CANCEL_ACCOUNT_PLAN_SUBTITLE_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.CANCEL_ACCOUNT_PLAN_TITLE_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.CONTINUE_CANCELLATION_BUTTON_TEST_TAG
import mega.privacy.android.app.presentation.cancelaccountplan.view.CancelAccountPlanView
import mega.privacy.android.app.presentation.cancelaccountplan.view.KEEP_PRO_PLAN_BUTTON_TEST_TAG
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import test.mega.privacy.android.app.fromId

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class CancelAccountPlanViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that all screen components are displayed correctly`() {

        val proPlanDetails = UIAccountDetails(
            accountType = "PRO I",
            storageQuotaSize = "200 GB",
            usedStorageSize = "100 GB",
            transferQuotaSize = "1 TB",
            freeStorageQuota = "1 GB",
            rewindDaysQuota = "90",
        )
        composeTestRule.setContent {
            CancelAccountPlanView(
                accountDetailsUI = proPlanDetails,
                onKeepPlanButtonClicked = { }) {
            }
        }

        composeTestRule.onNodeWithTag(CANCEL_ACCOUNT_PLAN_TITLE_TEST_TAG)
            .assertIsDisplayed().assert(
                hasText(
                    fromId(
                        R.string.account_cancel_account_screen_plan_miss_out_on_features,
                    )
                )
            )
        composeTestRule.onNodeWithTag(CANCEL_ACCOUNT_PLAN_SUBTITLE_TEST_TAG).assertIsDisplayed()
            .assert(
                hasText(
                    fromId(
                        R.string.account_cancel_account_screen_plan_access_until_expiration,
                    )
                )
            )
        composeTestRule.onNodeWithTag(CANCEL_ACCOUNT_PLAN_STORAGE_HINT_TEST_TAG).assertIsDisplayed()
            .assert(
                hasText(
                    fromId(
                        R.string.account_cancel_account_screen_plan_current_storage_warning,
                        proPlanDetails.usedStorageSize
                    )
                )
            )
        composeTestRule.onNodeWithTag(CANCEL_ACCOUNT_PLAN_FEATURE_TABLE_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(KEEP_PRO_PLAN_BUTTON_TEST_TAG).assertIsDisplayed().assert(
            hasText(
                fromId(
                    R.string.account_cancel_account_plan_keep_pro_plan,
                    proPlanDetails.accountType
                )
            )
        ).assertHasClickAction()
        composeTestRule.onNodeWithTag(CONTINUE_CANCELLATION_BUTTON_TEST_TAG).assertIsDisplayed()
            .assert(
                hasText(fromId(R.string.account_cancel_account_plan_continue_cancellation))
            ).assertHasClickAction()

    }
}