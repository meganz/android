package test.mega.privacy.android.app.presentation.settings.calls

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
import mega.privacy.android.app.presentation.settings.calls.SettingsCallsFragment
import mega.privacy.android.domain.entity.CallsSoundNotifications
import org.junit.Before
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
class SettingsCallsFragmentTest {

    private val hiltRule = HiltAndroidRule(this)

    private val composeRule = createAndroidComposeRule<HiltTestActivity>()

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(hiltRule).around(composeRule)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun test_that_switch_is_on_when_no_sounds_notification_status_is_selected() {
        runBlocking {
            whenever(TestSettingsModule.getCallsSoundNotifications())
                .thenReturn(flowOf(CallsSoundNotifications.Enabled))
        }

        launchFragmentInHiltContainer<SettingsCallsFragment>()

        composeRule
            .onNodeWithText(fromId(R.string.settings_calls_preferences_sound_notifications))
            .assertIsOn()
    }

    @Test
    fun test_that_switch_is_on_when_clicked() {
        runBlocking {
            whenever(TestSettingsModule.setCallsSoundNotifications(CallsSoundNotifications.Enabled))
                .thenReturn(Unit)
            whenever(TestSettingsModule.getCallsSoundNotifications())
                .thenReturn(flowOf(CallsSoundNotifications.Enabled))
        }

        launchFragmentInHiltContainer<SettingsCallsFragment>()

        composeRule
            .onNodeWithText(fromId(R.string.settings_calls_preferences_sound_notifications))
            .performClick()

        composeRule
            .onNodeWithText(fromId(R.string.settings_calls_preferences_sound_notifications))
            .assertIsOn()
    }

    @Test
    fun test_that_switch_is_off_when_clicked() {
        runBlocking {
            whenever(TestSettingsModule.setCallsSoundNotifications(CallsSoundNotifications.Disabled))
                .thenReturn(Unit)
            whenever(TestSettingsModule.getCallsSoundNotifications())
                .thenReturn(flowOf(CallsSoundNotifications.Disabled))
        }

        launchFragmentInHiltContainer<SettingsCallsFragment>()

        composeRule
            .onNodeWithText(fromId(R.string.settings_calls_preferences_sound_notifications))
            .performClick()

        composeRule
            .onNodeWithText(fromId(R.string.settings_calls_preferences_sound_notifications))
            .assertIsOff()
    }
}