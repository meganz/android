package mega.privacy.android.feature.settings.presentation.view

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import mega.privacy.android.domain.entity.preference.SortingPreference
import mega.privacy.android.domain.entity.preference.ViewModePreference
import mega.privacy.android.feature.settings.presentation.model.SortingAndViewModeSettingsUiState
import mega.privacy.android.shared.resources.R as sharedR
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.robolectric.annotation.Config

@Config(sdk = [34])
@RunWith(AndroidJUnit4::class)
class SortingAndViewModeSettingsViewTest {

    @get:Rule
    var composeRule = createAndroidComposeRule<ComponentActivity>()

    private val onSetSortingPreference = mock<(SortingPreference) -> Unit>()
    private val onSetViewModePreference = mock<(ViewModePreference) -> Unit>()
    private val onNavigateBack = mock<() -> Unit>()

    private fun getString(resId: Int): String = composeRule.activity.getString(resId)

    private fun initComposeRuleContent(
        uiState: SortingAndViewModeSettingsUiState = SortingAndViewModeSettingsUiState.Loading,
    ) {
        composeRule.setContent {
            SortingAndViewModeSettingsView(
                uiState = uiState,
                onSetSortingPreference = onSetSortingPreference,
                onSetViewModePreference = onSetViewModePreference,
                onNavigateBack = onNavigateBack,
            )
        }
    }

    @Test
    fun `test that the skeleton is displayed when ui state is Loading`() {
        initComposeRuleContent(uiState = SortingAndViewModeSettingsUiState.Loading)

        composeRule.onNodeWithTag(SORTING_AND_VIEW_MODE_SETTINGS_VIEW_TAG).assertIsDisplayed()
        composeRule.onNodeWithTag(SORTING_AND_VIEW_MODE_SETTINGS_SKELETON_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the skeleton is not displayed when ui state is Data`() {
        initComposeRuleContent(uiState = DATA_STATE)

        composeRule.onNodeWithTag(SORTING_AND_VIEW_MODE_SETTINGS_SKELETON_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the top bar title is displayed`() {
        initComposeRuleContent()

        composeRule
            .onNodeWithText(getString(sharedR.string.settings_sorting_and_view_mode_title))
            .assertIsDisplayed()
    }

    @Test
    fun `test that onNavigateBack is invoked when the navigation icon is clicked`() {
        initComposeRuleContent()

        composeRule.onNodeWithContentDescription(
            label = "Navigation Icon",
            substring = true,
            ignoreCase = true,
            useUnmergedTree = true,
        ).performClick()

        verify(onNavigateBack).invoke()
    }

    @Test
    fun `test that both preference titles and descriptions are displayed when ui state is Data`() {
        initComposeRuleContent(uiState = DATA_STATE)

        composeRule.onNodeWithText(getString(sharedR.string.settings_sorting_preference_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(getString(sharedR.string.settings_sorting_preference_description))
            .assertIsDisplayed()
        composeRule.onNodeWithText(getString(sharedR.string.settings_view_mode_preference_title))
            .assertIsDisplayed()
        composeRule.onNodeWithText(getString(sharedR.string.settings_view_mode_preference_description))
            .assertIsDisplayed()
    }

    @Test
    fun `test that the preference rows are not displayed when ui state is Loading`() {
        initComposeRuleContent(uiState = SortingAndViewModeSettingsUiState.Loading)

        composeRule.onNodeWithTag(sortingItemTag).assertDoesNotExist()
        composeRule.onNodeWithTag(viewModeItemTag).assertDoesNotExist()
    }

    @Test
    fun `test that onSetSortingPreference is invoked when an option is selected in the sorting sheet`() {
        initComposeRuleContent(uiState = DATA_STATE)

        composeRule.onNodeWithTag(sortingItemTag).performClick()
        composeRule.waitForIdle()
        composeRule
            .onNode(hasText(getString(sharedR.string.settings_preference_option_all_folders)) and hasClickAction())
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        verify(onSetSortingPreference).invoke(SortingPreference.AllFolders)
        verifyNoInteractions(onSetViewModePreference)
    }

    @Test
    fun `test that onSetViewModePreference is invoked when an option is selected in the view mode sheet`() {
        initComposeRuleContent(uiState = DATA_STATE)

        composeRule.onNodeWithTag(viewModeItemTag).performClick()
        composeRule.waitForIdle()
        composeRule
            .onNode(hasText(getString(sharedR.string.settings_preference_option_all_folders)) and hasClickAction())
            .performSemanticsAction(SemanticsActions.OnClick)
        composeRule.waitForIdle()

        verify(onSetViewModePreference).invoke(ViewModePreference.AllFolders)
        verifyNoInteractions(onSetSortingPreference)
    }

    companion object {
        private val DATA_STATE = SortingAndViewModeSettingsUiState.Data(
            sortingPreference = SortingPreference.PerFolder,
            viewModePreference = ViewModePreference.PerFolder,
        )

        private val sortingItemTag = "settings_$SORTING_PREFERENCE_TAG:list_item"
        private val viewModeItemTag = "settings_$VIEW_MODE_PREFERENCE_TAG:list_item"
    }
}
