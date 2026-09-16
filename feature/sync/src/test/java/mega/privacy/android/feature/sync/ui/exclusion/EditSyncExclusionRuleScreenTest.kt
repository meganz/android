package mega.privacy.android.feature.sync.ui.exclusion

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByNameFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.EditSyncExclusionRuleUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import mega.privacy.android.feature.sync.ui.views.SYNC_SINGLE_SELECT_OPTION_TAG
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.shared.resources.R as sharedR

@RunWith(AndroidJUnit4::class)
internal class EditSyncExclusionRuleScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private var deleted = 0
    private var saved = 0
    private var dismissed = 0

    private val prefilled = EditSyncExclusionRuleUiState(
        byName = AddExclusionByNameFormState(
            typeOption = SyncExclusionTypeOption.Folders,
            condition = SyncExclusionCondition.Containing,
            value = "temp",
        ),
    )

    private fun setContent(uiState: EditSyncExclusionRuleUiState = prefilled) {
        composeTestRule.setContent {
            EditSyncExclusionRuleScreen(
                uiState = uiState,
                onTypeOptionSelected = {},
                onConditionSelected = {},
                onValueChanged = {},
                onDelete = { deleted++ },
                onSave = { saved++ },
                onDismiss = { dismissed++ },
            )
        }
    }

    private fun string(resId: Int) = composeTestRule.activity.getString(resId)

    @Test
    fun `test that the screen shows the edit chrome`() {
        setContent()

        composeTestRule.onNodeWithTag(EDIT_EXCLUSION_TOP_BAR_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(sharedR.string.sync_exclusions_edit_title))
            .assertIsDisplayed()
        composeTestRule.onNodeWithTag(EDIT_EXCLUSION_BUTTON_GROUP_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(sharedR.string.general_dialog_cancel_button))
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(sharedR.string.general_action_save))
            .assertIsDisplayed()
    }

    @Test
    fun `test that the screen shows the chosen rule in the form`() {
        setContent()

        composeTestRule
            .onNodeWithTag(ADD_EXCLUSION_TYPE_CHIP_TAG)
            .assertContentDescriptionEquals(string(sharedR.string.sync_exclusions_add_type_folders))
        composeTestRule
            .onNodeWithTag(ADD_EXCLUSION_CONDITION_CHIP_TAG)
            .assertContentDescriptionEquals(
                string(sharedR.string.sync_exclusions_add_condition_containing),
            )
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_NAME_FIELD_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithText("temp").assertIsDisplayed()
    }

    @Test
    fun `test that the screen shows no tabs`() {
        setContent()

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_TAB_ROW_TAG).assertDoesNotExist()
        composeTestRule
            .onNodeWithText(string(sharedR.string.sync_exclusions_add_tab_by_size))
            .assertDoesNotExist()
    }

    @Test
    fun `test that the trash is shown as a trailing action and is spoken`() {
        setContent()

        composeTestRule.onNodeWithTag(CommonMenuAction.Delete.testTag).assertIsDisplayed()
        // the description sits on the icon the action tag wraps, which is the node TalkBack reads
        composeTestRule
            .onNodeWithContentDescription(string(sharedR.string.general_delete))
            .assertIsDisplayed()
    }

    @Test
    fun `test that a rule with no type shows its whole pattern and no condition`() {
        setContent(
            EditSyncExclusionRuleUiState(
                byName = AddExclusionByNameFormState(
                    typeOption = null,
                    condition = SyncExclusionCondition.MatchingWildcard,
                    value = "draft*",
                ),
            )
        )

        composeTestRule.onNodeWithText("draft*").assertIsDisplayed()
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_CONDITION_CHIP_TAG).assertDoesNotExist()
        composeTestRule
            .onNodeWithTag(ADD_EXCLUSION_TYPE_CHIP_TAG)
            .assertContentDescriptionEquals(string(sharedR.string.sync_exclusions_add_type))
    }

    @Test
    fun `test that the trash reports a deletion without confirming first`() {
        setContent()

        composeTestRule.onNodeWithTag(CommonMenuAction.Delete.testTag).performClick()

        assertThat(deleted).isEqualTo(1)
        assertThat(saved).isEqualTo(0)
        assertThat(dismissed).isEqualTo(0)
    }

    @Test
    fun `test that save reports the edit`() {
        setContent()

        composeTestRule
            .onNodeWithText(string(sharedR.string.general_action_save))
            .performClick()

        assertThat(saved).isEqualTo(1)
        assertThat(deleted).isEqualTo(0)
        assertThat(dismissed).isEqualTo(0)
    }

    @Test
    fun `test that cancel dismisses without saving`() {
        setContent()

        composeTestRule
            .onNodeWithText(string(sharedR.string.general_dialog_cancel_button))
            .performClick()

        assertThat(dismissed).isEqualTo(1)
        assertThat(saved).isEqualTo(0)
        assertThat(deleted).isEqualTo(0)
    }

    @Test
    fun `test that backing out dismisses without saving`() {
        setContent()

        composeTestRule
            .onNodeWithContentDescription(TOP_BAR_NAVIGATION_ICON_DESCRIPTION)
            .performClick()

        assertThat(dismissed).isEqualTo(1)
        assertThat(saved).isEqualTo(0)
        assertThat(deleted).isEqualTo(0)
    }

    @Test
    fun `test that the type picker does not offer extensions`() {
        setContent()

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_TYPE_CHIP_TAG).performClick()

        listOf(
            sharedR.string.sync_exclusions_add_type_files,
            sharedR.string.sync_exclusions_add_type_folders,
            sharedR.string.sync_exclusions_add_type_files_and_folders,
        ).forEach {
            // the sheet opens partially expanded, so the last option exists without being drawn
            composeTestRule
                .onNodeWithTag("$SYNC_SINGLE_SELECT_OPTION_TAG:${string(it)}")
                .assertExists()
        }
        val extensions = string(sharedR.string.sync_exclusions_add_type_extensions)
        composeTestRule
            .onNodeWithTag("$SYNC_SINGLE_SELECT_OPTION_TAG:$extensions")
            .assertDoesNotExist()
    }
}

private const val TOP_BAR_NAVIGATION_ICON_DESCRIPTION = "Navigation Icon"
