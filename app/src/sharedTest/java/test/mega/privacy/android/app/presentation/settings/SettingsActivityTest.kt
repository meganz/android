package test.mega.privacy.android.app.presentation.settings

import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.SettingsActivity
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.RecyclerViewAssertions
import test.mega.privacy.android.app.RecyclerViewAssertions.Companion.onViewHolder
import test.mega.privacy.android.app.di.TestSettingsAdvancedUseCases

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsActivityTest{

    @get:Rule(order = 0)
    var hiltRule = HiltAndroidRule(this)

    @get:Rule(order = 1)
    var activityRule = ActivityScenarioRule(SettingsActivity::class.java)

    @Before
    fun setUp() {
        runBlocking { whenever(TestSettingsAdvancedUseCases.isUseHttpsEnabled()).thenReturn(true) }
        hiltRule.inject()
    }

    @Test
    @SdkSuppress(minSdkVersion = 30)
    fun test_that_settings_advanced_fragment_is_loaded_post_30() {
        testNavigateToAdvancedSettings()
    }

    @Test
    @SdkSuppress(maxSdkVersion = 29)
    fun test_that_settings_advanced_fragment_is_loaded_pre_30() {
        testNavigateToAdvancedSettings()
    }

    private fun testNavigateToAdvancedSettings() {
        onPreferences()
            .perform(
                RecyclerViewActions.scrollToHolder(
                    hasDescendant(withText(R.string.settings_advanced_features)).onViewHolder()
                )
            )

        onPreferences()
            .perform(
                RecyclerViewActions.actionOnHolderItem(
                    hasDescendant(withText(R.string.settings_advanced_features)).onViewHolder(),
                    click()
                )
            )


        onPreferences()
            .check(
                RecyclerViewAssertions.withRowContaining(
                    hasDescendant(withText(R.string.setting_subtitle_use_https_only))
                )
            )
    }

}