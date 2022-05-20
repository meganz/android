package test.mega.privacy.android.app.presentation.settings.chat.imagequality

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.app.R
import mega.privacy.android.app.domain.entity.ChatImageQuality
import mega.privacy.android.app.presentation.settings.chat.imagequality.ChatImageQualityView
import mega.privacy.android.app.presentation.settings.chat.imagequality.model.SettingsChatImageQualityState
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import test.mega.privacy.android.app.onNodeWithText

@RunWith(AndroidJUnit4::class)
class ChatImageQualityViewTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun test_that_automatic_text_is_shown_and_has_the_correct_description() {
        initComposeRuleContent()
        composeRule.onNodeWithText(R.string.automatic_image_quality).assertExists()
        composeRule.onNodeWithText(R.string.automatic_image_quality_text).assertExists()
    }

    @Test
    fun test_that_original_text_is_shown_and_has_the_correct_description() {
        initComposeRuleContent()
        composeRule.onNodeWithText(R.string.high_image_quality).assertExists()
        composeRule.onNodeWithText(R.string.high_image_quality_text).assertExists()
    }

    @Test
    fun test_that_optimised_text_is_shown_and_has_the_correct_description() {
        initComposeRuleContent()
        composeRule.onNodeWithText(R.string.optimised_image_quality).assertExists()
        composeRule.onNodeWithText(R.string.optimised_image_quality_text).assertExists()
    }

    @Test
    fun test_that_automatic_is_checked_if_selected_image_quality_is_automatic() {
        initComposeRuleContent(
            SettingsChatImageQualityState(selectedQuality = ChatImageQuality.Automatic)
        )
        composeRule.onNodeWithText(R.string.automatic_image_quality).assertIsOn()
        composeRule.onNodeWithText(R.string.high_image_quality).assertIsOff()
        composeRule.onNodeWithText(R.string.optimised_image_quality).assertIsOff()
    }

    @Test
    fun test_that_original_is_checked_if_selected_image_quality_is_original() {
        initComposeRuleContent(
            SettingsChatImageQualityState(selectedQuality = ChatImageQuality.Original)
        )
        composeRule.onNodeWithText(R.string.automatic_image_quality).assertIsOff()
        composeRule.onNodeWithText(R.string.high_image_quality).assertIsOn()
        composeRule.onNodeWithText(R.string.optimised_image_quality).assertIsOff()
    }

    @Test
    fun test_that_optimised_is_checked_if_selected_image_quality_is_optimised() {
        initComposeRuleContent(
            SettingsChatImageQualityState(selectedQuality = ChatImageQuality.Optimised)
        )
        composeRule.onNodeWithText(R.string.automatic_image_quality).assertIsOff()
        composeRule.onNodeWithText(R.string.high_image_quality).assertIsOff()
        composeRule.onNodeWithText(R.string.optimised_image_quality).assertIsOn()
    }

    private fun initComposeRuleContent(
        state: SettingsChatImageQualityState = SettingsChatImageQualityState()
    ) {
        composeRule.setContent {
            ChatImageQualityView(
                settingsChatImageQualityState = state,
                onOptionChanged = mock()
            )
        }
    }
}