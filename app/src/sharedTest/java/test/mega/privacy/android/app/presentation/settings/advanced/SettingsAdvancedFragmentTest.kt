package test.mega.privacy.android.app.presentation.settings.advanced

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions.click
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.isEnabled
import androidx.test.espresso.matcher.ViewMatchers.isNotChecked
import androidx.test.espresso.matcher.ViewMatchers.isNotEnabled
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.advanced.SettingsAdvancedFragment
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.RecyclerViewAssertions.Companion.withRowContaining
import test.mega.privacy.android.app.di.TestInitialiseUseCases
import test.mega.privacy.android.app.di.TestSettingsAdvancedUseCases
import test.mega.privacy.android.app.launchFragmentInHiltContainer
import test.mega.privacy.android.app.presentation.settings.onPreferences

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
class SettingsAdvancedFragmentTest {

    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun test_that_checkbox_is_checked_if_preference_is_set_to_true() {
        runBlocking { whenever(TestSettingsAdvancedUseCases.isUseHttpsEnabled()).thenReturn(true) }

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        verifyPreference(isChecked())
    }

    @Test
    fun test_that_checkbox_is_not_checked_if_preference_is_false() {
        runBlocking { whenever(TestSettingsAdvancedUseCases.isUseHttpsEnabled()).thenReturn(false) }

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        verifyPreference(isNotChecked())
    }

    @Test
    fun test_that_checkbox_is_enabled_if_online_and_root_node_exists() {
        setInitialState(isOnline = flowOf(true), rootNodeExists = true)

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        verifyPreference(isEnabled())
    }

    @Test
    fun test_that_checkbox_is_not_enabled_if_offline() {
        setInitialState(flowOf(value = false), true)

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        verifyPreference(isNotEnabled())
    }

    @Test
    fun test_that_checkbox_is_not_enabled_if_root_node_is_null() {
        setInitialState(rootNodeExists = false)

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        verifyPreference(isNotEnabled())
    }

    @Test
    fun test_that_checkbox_becomes_not_enabled_if_connection_is_lost() {
        val isOnline = MutableStateFlow(true)
        setInitialState(isOnline)

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        verifyPreference(isEnabled())

        isOnline.tryEmit(false)

        Thread.sleep(200) //TODO: Find an alternative. This is a hack to get around Robolectric not respecting idling resources.

        verifyPreference(isNotEnabled())
    }

    @Test
    fun test_that_checkbox_becomes_enabled_if_connection_established() {
        val isOnline = MutableStateFlow(false)
        setInitialState(isOnline = isOnline)

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        verifyPreference(isNotEnabled())

        isOnline.tryEmit(true)

        Thread.sleep(200) //TODO: Find an alternative. This is a hack to get around Robolectric not respecting idling resources.

        verifyPreference(isEnabled())
    }

    @Test
    fun test_that_the_set_use_case_is_called_with_true_when_checkbox_is_checked() {
        setInitialState()

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        onPreferences().perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click())
        )

        verifyPreference(isChecked())

        runBlocking { verify(TestSettingsAdvancedUseCases.setUseHttps).invoke(true) }
    }

    @Test
    fun test_that_the_set_use_case_is_called_with_false_when_checkbox_is_unchecked() {
        runBlocking { whenever(TestSettingsAdvancedUseCases.isUseHttpsEnabled()).thenReturn(true) }
        setInitialState()

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        onPreferences().perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(0, click())
        )

        verifyPreference(isNotChecked())

        runBlocking { verify(TestSettingsAdvancedUseCases.setUseHttps).invoke(false) }
    }

    private fun setInitialState(
        isOnline: Flow<Boolean> = flowOf(true),
        rootNodeExists: Boolean = true
    ) {
        whenever(TestInitialiseUseCases.monitorConnectivity()).thenReturn(isOnline)
        runBlocking { whenever(TestInitialiseUseCases.rootNodeExists()).thenReturn(rootNodeExists) }
    }

    private fun verifyPreference(enabled: Matcher<View>?): ViewInteraction? {
        return onPreferences().check(
            withRowContaining(
                allOf(
                    hasDescendant(withText(R.string.setting_subtitle_use_https_only)),
                    hasSibling(hasDescendant(enabled))
                )
            )
        )
    }
}