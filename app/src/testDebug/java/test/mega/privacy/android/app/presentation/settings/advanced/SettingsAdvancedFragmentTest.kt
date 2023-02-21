package test.mega.privacy.android.app.presentation.settings.advanced

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.ViewInteraction
import androidx.test.espresso.action.ViewActions
import androidx.test.espresso.contrib.RecyclerViewActions
import androidx.test.espresso.matcher.ViewMatchers
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.runBlocking
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.settings.advanced.SettingsAdvancedFragment
import org.hamcrest.CoreMatchers
import org.hamcrest.Matcher
import org.junit.Before
import org.junit.Ignore
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import test.mega.privacy.android.app.RecyclerViewAssertions
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

    @Ignore("These tests are not stable. Refactor SettingsAdvancedFragment to use compose view.")
    @Test
    fun `test that checkbox is checked if preference is set to true`() {
        runBlocking { whenever(TestSettingsAdvancedUseCases.isUseHttpsEnabled()).thenReturn(true) }

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        verifyPreference(ViewMatchers.isChecked())
    }

    @Ignore("These tests are not stable. Refactor SettingsAdvancedFragment to use compose view.")
    @Test
    fun `test that checkbox is not checked if preference is false`() {
        runBlocking { whenever(TestSettingsAdvancedUseCases.isUseHttpsEnabled()).thenReturn(false) }

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        verifyPreference(ViewMatchers.isNotChecked())
    }

    @Ignore("These tests are not stable. Refactor SettingsAdvancedFragment to use compose view.")
    @Test
    fun `test that checkbox is enabled if online and root node exists`() {
        setInitialState(isOnline = MutableStateFlow(true), rootNodeExists = true)

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        verifyPreference(ViewMatchers.isEnabled())
    }

    @Ignore("These tests are not stable. Refactor SettingsAdvancedFragment to use compose view.")
    @Test
    fun `test that checkbox is not enabled if offline`() {
        setInitialState(MutableStateFlow(value = false), true)

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        verifyPreference(ViewMatchers.isNotEnabled())
    }

    @Ignore("These tests are not stable. Refactor SettingsAdvancedFragment to use compose view.")
    @Test
    fun `test that checkbox is not enabled if root node is null`() {
        setInitialState(rootNodeExists = false)

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        verifyPreference(ViewMatchers.isNotEnabled())
    }

    @Ignore("These tests are not stable. Refactor SettingsAdvancedFragment to use compose view.")
    @Test
    fun `test that checkbox becomes not enabled if connection is lost`() {
        val isOnline = MutableStateFlow(true)
        setInitialState(isOnline)

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        verifyPreference(ViewMatchers.isEnabled())

        isOnline.tryEmit(false)

        Thread.sleep(200)

        verifyPreference(ViewMatchers.isNotEnabled())
    }

    @Ignore("These tests are not stable. Refactor SettingsAdvancedFragment to use compose view.")
    @Test
    fun `test that checkbox becomes enabled if connection established`() {
        val isOnline = MutableStateFlow(false)
        setInitialState(isOnline = isOnline)

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        verifyPreference(ViewMatchers.isNotEnabled())

        isOnline.tryEmit(true)

        Thread.sleep(200)

        verifyPreference(ViewMatchers.isEnabled())
    }

    @Ignore("These tests are not stable. Refactor SettingsAdvancedFragment to use compose view.")
    @Test
    fun `test that the set use case is called with true when checkbox is checked`() {
        setInitialState()

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        onPreferences().perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0,
                ViewActions.click()
            )
        )

        verifyPreference(ViewMatchers.isChecked())

        runBlocking { verify(TestSettingsAdvancedUseCases.setUseHttps).invoke(true) }
    }

    @Ignore("These tests are not stable. Refactor SettingsAdvancedFragment to use compose view.")
    @Test
    fun `test that the set use case is called with false when checkbox is unchecked`() {
        runBlocking { whenever(TestSettingsAdvancedUseCases.isUseHttpsEnabled()).thenReturn(true) }
        setInitialState()

        launchFragmentInHiltContainer<SettingsAdvancedFragment>()

        onPreferences().perform(
            RecyclerViewActions.actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0,
                ViewActions.click()
            )
        )

        verifyPreference(ViewMatchers.isNotChecked())

        runBlocking { verify(TestSettingsAdvancedUseCases.setUseHttps).invoke(false) }
    }

    private fun setInitialState(
        isOnline: StateFlow<Boolean> = MutableStateFlow(true),
        rootNodeExists: Boolean = true,
    ) {
        whenever(TestInitialiseUseCases.monitorConnectivity()).thenReturn(isOnline)
        runBlocking { whenever(TestInitialiseUseCases.rootNodeExists()).thenReturn(rootNodeExists) }
    }

    private fun verifyPreference(enabled: Matcher<View>?): ViewInteraction? {
        return onPreferences().check(
            RecyclerViewAssertions.withRowContaining(
                CoreMatchers.allOf(
                    ViewMatchers.hasDescendant(ViewMatchers.withText(R.string.setting_subtitle_use_https_only)),
                    ViewMatchers.hasSibling(ViewMatchers.hasDescendant(enabled))
                )
            )
        )
    }
}