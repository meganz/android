package mega.privacy.android.a11ytest.screen

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.a11ytest.ThemeReporters
import mega.privacy.android.domain.entity.preference.SortingPreference
import mega.privacy.android.domain.entity.preference.ViewModePreference
import mega.privacy.android.feature.settings.presentation.model.SortingAndViewModeSettingsUiState
import mega.privacy.android.feature.settings.presentation.view.SortingAndViewModeSettingsView
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Accessibility sweep of the Sorting and view mode settings screen from `:feature:settings`.
 *
 * The screen is mounted stateless with hand-built UI states, so the sweep exercises the real
 * production composition — scaffold, top app bar and settings rows — without a ViewModel or a
 * Hilt graph.
 */
@RunWith(AndroidJUnit4::class)
class SortingAndViewModeSettingsScreenAccessibilityTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val reporters = ThemeReporters("SortingAndViewModeSettingsScreenAccessibilityTest")

    @Test
    fun test_that_the_populated_settings_screen_passes_accessibility_checks() {
        val sweep = composeTestRule.setScreenContent(reporters) {
            SortingAndViewModeSettingsView(
                uiState = SortingAndViewModeSettingsUiState.Data(
                    sortingPreference = SortingPreference.PerFolder,
                    viewModePreference = ViewModePreference.AllFolders,
                ),
                onSetSortingPreference = {},
                onSetViewModePreference = {},
                onNavigateBack = {},
            )
        }

        sweep.checkInEachTheme()
    }

    @Test
    fun test_that_the_loading_settings_screen_passes_accessibility_checks() {
        val sweep = composeTestRule.setScreenContent(reporters) {
            SortingAndViewModeSettingsView(
                uiState = SortingAndViewModeSettingsUiState.Loading,
                onSetSortingPreference = {},
                onSetViewModePreference = {},
                onNavigateBack = {},
            )
        }

        sweep.checkInEachTheme()
    }
}
