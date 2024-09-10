package mega.privacy.android.app.presentation.cancelaccountplan.view

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.fromId
import mega.privacy.android.app.presentation.cancelaccountplan.model.UICancellationSurveyAnswer
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class CancelSubscriptionSurveyViewTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private val possibleCancellationReasons = listOf(
        UICancellationSurveyAnswer.Answer1,
        UICancellationSurveyAnswer.Answer2,
        UICancellationSurveyAnswer.Answer3,
        UICancellationSurveyAnswer.Answer4,
        UICancellationSurveyAnswer.Answer5,
        UICancellationSurveyAnswer.Answer6,
        UICancellationSurveyAnswer.Answer7,
        UICancellationSurveyAnswer.Answer9,
        UICancellationSurveyAnswer.Answer10,
    ).shuffled() + UICancellationSurveyAnswer.Answer8

    @Test
    fun `test that all screen components are displayed correctly`() {

        composeTestRule.setContent {
            CancelSubscriptionSurveyView(
                possibleCancellationReasons = possibleCancellationReasons,
                onCancelSubscriptionButtonClicked = { _, _ -> },
                onDoNotCancelButtonClicked = { }
            )
        }

        composeTestRule.onNodeWithTag(TITLE_TEST_TAG)
            .assertIsDisplayed()
            .assert(
                hasText(
                    fromId(
                        R.string.account_cancel_subscription_survey_title,
                    )
                )
            )
        composeTestRule.onNodeWithTag(SUBTITLE_TEST_TAG)
            .assertIsDisplayed()
            .assert(
                hasText(
                    fromId(
                        R.string.account_cancel_subscription_survey_cancellation_message,
                    )
                )
            )
        composeTestRule.onNodeWithTag(SURVEY_OPTIONS_GROUP_TEST_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(CANCEL_SUBSCRIPTION_SURVEY_ALLOW_CONTACT_CHECKBOX_TEST_TAG)
            .assertIsDisplayed()
            .assertHasClickAction()
        composeTestRule.onNodeWithTag(CANCEL_SUBSCRIPTION_SURVEY_ALLOW_CONTACT_TEXT_TEST_TAG)
            .assertIsDisplayed()
            .assert(
                hasText(
                    fromId(
                        R.string.account_cancel_subscription_survey_allow_contact,
                    )
                )
            )
        composeTestRule.onNodeWithTag(CANCEL_SUBSCRIPTION_BUTTON_TEST_TAG)
            .assertIsDisplayed()
            .assertHasClickAction()
            .assert(
                hasText(
                    fromId(
                        R.string.account_cancel_subscription_survey_cancel_button,
                    )
                )
            )


    }

    @Test
    fun `test that survey options components are displayed correctly`() {
        composeTestRule.setContent {
            SurveyOptions(
                possibleCancellationReasons = possibleCancellationReasons,
                onItemClicked = { },
                selectedOptionId = -1,
            )
        }

        composeTestRule.onAllNodesWithTag(SURVEY_OPTIONS_ROW_TEST_TAG)
            .assertCountEquals(possibleCancellationReasons.size)
        composeTestRule.onAllNodesWithTag(SURVEY_OPTIONS_OPTION_RADIO_TEST_TAG)
            .assertCountEquals(possibleCancellationReasons.size)
        composeTestRule.onAllNodesWithTag(
            SURVEY_OPTIONS_OPTION_TEXT_TEST_TAG,
            useUnmergedTree = true
        )
            .assertCountEquals(possibleCancellationReasons.size)

    }
}
