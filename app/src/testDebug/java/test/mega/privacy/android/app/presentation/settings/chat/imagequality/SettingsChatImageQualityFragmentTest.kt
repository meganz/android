package test.mega.privacy.android.app.presentation.settings.chat.imagequality

import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.HiltTestActivity
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.chat.imagequality.SettingsChatImageQualityFragment
import mega.privacy.android.domain.entity.ChatImageQuality
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.di.TestSettingsModule
import test.mega.privacy.android.app.fromId
import test.mega.privacy.android.app.launchFragmentInHiltContainer

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Ignore("Ignore the unstable test. Will add the tests back once stability issue is resolved.")
class SettingsChatImageQualityFragmentTest {

    private val hiltRule = HiltAndroidRule(this)

    private val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun test_that_automatic_option_is_checked_when_no_quality_selected() {
        runBlocking {
            whenever(TestSettingsModule.getChatImageQuality())
                .thenReturn(flowOf(ChatImageQuality.Automatic))
        }

        launchFragmentInHiltContainer<SettingsChatImageQualityFragment>()

        composeRule.onNodeWithText(fromId(R.string.automatic_image_quality)).assertIsOn()
        composeRule.onNodeWithText(fromId(R.string.high_image_quality)).assertIsOff()
        composeRule.onNodeWithText(fromId(R.string.optimised_image_quality)).assertIsOff()
    }

    @Test
    fun test_that_original_option_is_checked_when_clicked() {
        runBlocking {
            whenever(TestSettingsModule.setChatImageQuality(ChatImageQuality.Original))
                .thenReturn(Unit)
            whenever(TestSettingsModule.getChatImageQuality())
                .thenReturn(flowOf(ChatImageQuality.Original))
        }

        launchFragmentInHiltContainer<SettingsChatImageQualityFragment>()

        composeRule.onNodeWithText(fromId(R.string.high_image_quality)).performClick()
        composeRule.onNodeWithText(fromId(R.string.automatic_image_quality)).assertIsOff()
        composeRule.onNodeWithText(fromId(R.string.high_image_quality)).assertIsOn()
        composeRule.onNodeWithText(fromId(R.string.optimised_image_quality)).assertIsOff()
    }

    @Test
    fun test_that_optimised_option_is_checked_when_clicked() {
        runBlocking {
            whenever(TestSettingsModule.setChatImageQuality(ChatImageQuality.Optimised))
                .thenReturn(Unit)
            whenever(TestSettingsModule.getChatImageQuality())
                .thenReturn(flowOf(ChatImageQuality.Optimised))
        }

        launchFragmentInHiltContainer<SettingsChatImageQualityFragment>()

        composeRule.onNodeWithText(fromId(R.string.optimised_image_quality)).performClick()
        composeRule.onNodeWithText(fromId(R.string.automatic_image_quality)).assertIsOff()
        composeRule.onNodeWithText(fromId(R.string.high_image_quality)).assertIsOff()
        composeRule.onNodeWithText(fromId(R.string.optimised_image_quality)).assertIsOn()
    }
}