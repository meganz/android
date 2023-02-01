package test.mega.privacy.android.app.fragments.settingsFragments.download

import android.os.Build.VERSION_CODES
import android.view.View
import androidx.test.espresso.matcher.ViewMatchers.hasDescendant
import androidx.test.espresso.matcher.ViewMatchers.hasSibling
import androidx.test.espresso.matcher.ViewMatchers.isChecked
import androidx.test.espresso.matcher.ViewMatchers.withText
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import mega.privacy.android.app.R
import mega.privacy.android.app.fragments.settingsFragments.download.DownloadSettingsFragment
import org.hamcrest.CoreMatchers.allOf
import org.hamcrest.Matcher
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.whenever
import org.robolectric.annotation.Config
import test.mega.privacy.android.app.RecyclerViewAssertions.Companion.withNoRowContaining
import test.mega.privacy.android.app.RecyclerViewAssertions.Companion.withRowContaining
import test.mega.privacy.android.app.di.TestDownloadSettingsUseCases.getDownloadLocationPath
import test.mega.privacy.android.app.di.TestDownloadSettingsUseCases.getStorageDownloadAskAlways
import test.mega.privacy.android.app.launchFragmentInHiltContainer
import test.mega.privacy.android.app.presentation.settings.onPreferences

@OptIn(ExperimentalCoroutinesApi::class)
@HiltAndroidTest
@Config(sdk = [VERSION_CODES.Q])
@RunWith(AndroidJUnit4::class)
class DownloadSettingsFragmentTest {
    @get:Rule
    val hiltRule = HiltAndroidRule(this)

    @Before
    fun setUp() {
        hiltRule.inject()
    }

    @Test
    fun `test that download ask always checkbox initial state should follow getStorageDownloadAskAlways use case`() {
        fun verifyIsChecked(isChecked: Boolean) = runTest {
            whenever(getStorageDownloadAskAlways()).thenReturn(isChecked)

            launchFragmentInHiltContainer<DownloadSettingsFragment>()

            val matcher = if (isChecked) isChecked() else not(isChecked())

            verifyRecyclerViewContains(
                viewMatcher = allOf(
                    hasDescendant(withText(R.string.settings_storage_ask_me_always)),
                    hasSibling(hasDescendant(matcher)),
                )
            )
        }

        verifyIsChecked(true)
        verifyIsChecked(false)
    }

    @Test
    fun `test that download location summary initial path should follow getDownloadLocationPath`() =
        runTest {
            val fakePath = "location/path"

            whenever(getDownloadLocationPath()).thenReturn(fakePath)

            launchFragmentInHiltContainer<DownloadSettingsFragment>()

            verifyRecyclerViewContains(
                viewMatcher = allOf(
                    hasDescendant(withText(R.string.settings_storage_download_location)),
                    hasDescendant(withText(fakePath)),
                )
            )
        }

    @Test
    fun `test that default download location preference will show hide based on always ask location checkbox state`() {
        fun verifyCheckboxState(isChecked: Boolean) = runTest {
            val fakePath = "location/path"

            whenever(getStorageDownloadAskAlways()).thenReturn(isChecked)
            whenever(getDownloadLocationPath()).thenReturn(fakePath)

            launchFragmentInHiltContainer<DownloadSettingsFragment>()

            val expectedView = allOf(
                hasDescendant(withText(R.string.settings_storage_download_location)),
                hasDescendant(withText(fakePath))
            )
            val preferenceCheckAssertion =
                if (isChecked) withNoRowContaining(expectedView) else withRowContaining(
                    expectedView
                )

            onPreferences().check(preferenceCheckAssertion)
        }

        verifyCheckboxState(true)
        verifyCheckboxState(false)
    }

    private fun verifyRecyclerViewContains(viewMatcher: Matcher<View?>) {
        onPreferences().check(withRowContaining(viewMatcher))
    }
}