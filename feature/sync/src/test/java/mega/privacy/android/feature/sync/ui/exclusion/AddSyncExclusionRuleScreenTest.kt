package mega.privacy.android.feature.sync.ui.exclusion

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByNameFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionTab
import mega.privacy.android.feature.sync.ui.model.exclusion.AddSyncExclusionRuleUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.shared.resources.R as sharedR

@RunWith(AndroidJUnit4::class)
internal class AddSyncExclusionRuleScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val selectedTabs = mutableListOf<AddExclusionTab>()
    private var confirmed = 0
    private var dismissed = 0

    private fun setContent(uiState: AddSyncExclusionRuleUiState) {
        composeTestRule.setContent {
            AddSyncExclusionRuleScreen(
                uiState = uiState,
                onTabSelected = selectedTabs::add,
                onTypeOptionSelected = {},
                onConditionSelected = {},
                onNameValueChanged = {},
                onExtensionValueChanged = {},
                onConfirm = { confirmed++ },
                onDismiss = { dismissed++ },
            )
        }
    }

    private fun string(resId: Int) = composeTestRule.activity.getString(resId)

    /**
     * The screen title and the primary button carry the same words, so the button has to be found
     * by where it sits rather than by what it reads.
     */
    private fun confirmButton() = composeTestRule
        .onAllNodesWithText(string(sharedR.string.sync_exclusions_add_confirm))
        .filterToOne(hasAnyAncestor(hasTestTag(ADD_EXCLUSION_BUTTON_GROUP_TAG)))

    @Test
    fun `test that the screen shows the shared chrome both tabs use`() {
        setContent(AddSyncExclusionRuleUiState())

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_TOP_BAR_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_TAB_ROW_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(sharedR.string.sync_exclusions_add_tab_by_name))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(sharedR.string.sync_exclusions_add_tab_by_size))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(sharedR.string.general_dialog_cancel_button))
            .assertIsDisplayed()
        confirmButton().assertIsDisplayed()
    }

    @Test
    fun `test that the screen opens on the by name tab`() {
        setContent(AddSyncExclusionRuleUiState())

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_DESCRIPTION_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(sharedR.string.sync_exclusions_add_tab_by_name))
            .assertIsSelected()
    }

    @Test
    fun `test that the screen leaves the by name body when the size tab is selected`() {
        setContent(AddSyncExclusionRuleUiState(selectedTab = AddExclusionTab.BySize))

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_DESCRIPTION_TAG).assertDoesNotExist()
        composeTestRule
            .onNodeWithText(string(sharedR.string.sync_exclusions_add_tab_by_size))
            .assertIsSelected()
    }

    @Test
    fun `test that tapping a tab reports it`() {
        setContent(AddSyncExclusionRuleUiState())

        composeTestRule
            .onNodeWithText(string(sharedR.string.sync_exclusions_add_tab_by_size))
            .performClick()

        assertThat(selectedTabs).containsExactly(AddExclusionTab.BySize)
    }

    @Test
    fun `test that the by name tab swaps to extension entry when extensions is the type`() {
        setContent(
            AddSyncExclusionRuleUiState(
                byName = AddExclusionByNameFormState(
                    typeOption = SyncExclusionTypeOption.Extensions,
                ),
            )
        )

        composeTestRule
            .onNodeWithTag(ADD_EXCLUSION_EXTENSION_DESCRIPTION_TAG)
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_DESCRIPTION_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the size tab never shows extension entry`() {
        setContent(
            AddSyncExclusionRuleUiState(
                selectedTab = AddExclusionTab.BySize,
                byName = AddExclusionByNameFormState(
                    typeOption = SyncExclusionTypeOption.Extensions,
                ),
            )
        )

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_EXTENSION_DESCRIPTION_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_DESCRIPTION_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that cancel dismisses without confirming`() {
        setContent(AddSyncExclusionRuleUiState())

        composeTestRule
            .onNodeWithText(string(sharedR.string.general_dialog_cancel_button))
            .performClick()

        assertThat(dismissed).isEqualTo(1)
        assertThat(confirmed).isEqualTo(0)
    }

    @Test
    fun `test that closing dismisses without confirming`() {
        setContent(AddSyncExclusionRuleUiState())

        composeTestRule
            .onNodeWithContentDescription(TOP_BAR_NAVIGATION_ICON_DESCRIPTION)
            .performClick()

        assertThat(dismissed).isEqualTo(1)
        assertThat(confirmed).isEqualTo(0)
    }

    @Test
    fun `test that the primary button stays enabled on an empty form and confirms on tap`() {
        setContent(AddSyncExclusionRuleUiState())

        confirmButton().assertIsEnabled().performClick()

        assertThat(confirmed).isEqualTo(1)
        assertThat(dismissed).isEqualTo(0)
    }
}

/** The description core-ui gives every top bar navigation icon, whichever type it draws. */
private const val TOP_BAR_NAVIGATION_ICON_DESCRIPTION = "Navigation Icon"
