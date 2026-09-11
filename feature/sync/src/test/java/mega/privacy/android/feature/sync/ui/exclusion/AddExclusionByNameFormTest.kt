package mega.privacy.android.feature.sync.ui.exclusion

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByNameFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import mega.privacy.android.feature.sync.ui.views.SYNC_SINGLE_SELECT_OPTION_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.shared.resources.R as sharedR

@RunWith(AndroidJUnit4::class)
internal class AddExclusionByNameFormTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val selectedTypes = mutableListOf<SyncExclusionTypeOption>()
    private val selectedConditions = mutableListOf<SyncExclusionCondition>()
    private val enteredValues = mutableListOf<String>()

    private fun setContent(state: AddExclusionByNameFormState) {
        composeTestRule.setContent {
            AddExclusionByNameForm(
                state = state,
                onTypeOptionSelected = selectedTypes::add,
                onConditionSelected = selectedConditions::add,
                onValueChanged = enteredValues::add,
            )
        }
    }

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    @Test
    fun `test that the condition chip is hidden when no type is picked`() {
        setContent(AddExclusionByNameFormState())

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_TYPE_CHIP_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_CONDITION_CHIP_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the condition chip is shown when a type is picked`() {
        setContent(AddExclusionByNameFormState(typeOption = SyncExclusionTypeOption.Files))

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_CONDITION_CHIP_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the description prompts for a choice when nothing is picked`() {
        setContent(AddExclusionByNameFormState())

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_DESCRIPTION_TAG)
            .assertTextEquals(string(sharedR.string.sync_exclusions_add_description_initial))
    }

    @Test
    fun `test that the description describes the picked type`() {
        setContent(AddExclusionByNameFormState(typeOption = SyncExclusionTypeOption.Folders))

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_DESCRIPTION_TAG)
            .assertTextEquals(string(sharedR.string.sync_exclusions_add_description_folders))
    }

    @Test
    fun `test that the description is hidden when the extensions type is picked`() {
        setContent(AddExclusionByNameFormState(typeOption = SyncExclusionTypeOption.Extensions))

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_DESCRIPTION_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the type chip shows its placeholder when nothing is picked`() {
        setContent(AddExclusionByNameFormState())

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_TYPE_CHIP_TAG)
            .assertContentDescriptionEquals(string(sharedR.string.sync_exclusions_add_type))
    }

    @Test
    fun `test that the condition chip shows its placeholder when only a type is picked`() {
        setContent(AddExclusionByNameFormState(typeOption = SyncExclusionTypeOption.Files))

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_CONDITION_CHIP_TAG)
            .assertContentDescriptionEquals(string(sharedR.string.sync_exclusions_add_condition))
    }

    @Test
    fun `test that the condition chip is hidden when the extensions type is picked`() {
        setContent(AddExclusionByNameFormState(typeOption = SyncExclusionTypeOption.Extensions))

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_CONDITION_CHIP_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that a chip reports its chosen state to accessibility services`() {
        setContent(AddExclusionByNameFormState(typeOption = SyncExclusionTypeOption.Files))

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_TYPE_CHIP_TAG).assertIsSelected()
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_CONDITION_CHIP_TAG).assertIsNotSelected()
    }

    @Test
    fun `test that the type sheet marks the picked option`() {
        setContent(AddExclusionByNameFormState(typeOption = SyncExclusionTypeOption.Folders))

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_TYPE_CHIP_TAG).performClick()

        val folders = string(sharedR.string.sync_exclusions_add_type_folders)
        composeTestRule.onNodeWithTag("$SYNC_SINGLE_SELECT_OPTION_TAG:$folders").assertIsSelected()
    }

    @Test
    fun `test that the type sheet lists every option in the designed order`() {
        setContent(AddExclusionByNameFormState())

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_TYPE_CHIP_TAG).performClick()

        composeTestRule.onNodeWithText(string(sharedR.string.sync_exclusions_add_type))
            .assertIsDisplayed()
        val expected = listOf(
            sharedR.string.sync_exclusions_add_type_files,
            sharedR.string.sync_exclusions_add_type_folders,
            sharedR.string.sync_exclusions_add_type_files_and_folders,
            sharedR.string.sync_exclusions_add_type_extensions,
        ).map(::string)
        expected.forEach {
            composeTestRule.onNodeWithTag("$SYNC_SINGLE_SELECT_OPTION_TAG:$it").assertExists()
        }
    }

    @Test
    fun `test that the condition sheet lists every option in the designed order`() {
        setContent(AddExclusionByNameFormState(typeOption = SyncExclusionTypeOption.Files))

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_CONDITION_CHIP_TAG).performClick()

        composeTestRule.onNodeWithText(string(sharedR.string.sync_exclusions_add_condition))
            .assertIsDisplayed()
        val expected = listOf(
            sharedR.string.sync_exclusions_add_condition_beginning_with,
            sharedR.string.sync_exclusions_add_condition_containing,
            sharedR.string.sync_exclusions_add_condition_ending_with,
            sharedR.string.sync_exclusions_add_condition_matching_exactly,
            sharedR.string.sync_exclusions_add_condition_matching_wildcard,
        ).map(::string)
        expected.forEach {
            composeTestRule.onNodeWithTag("$SYNC_SINGLE_SELECT_OPTION_TAG:$it").assertExists()
        }
    }

    @Test
    fun `test that the chips show the picked labels`() {
        setContent(
            AddExclusionByNameFormState(
                typeOption = SyncExclusionTypeOption.Folders,
                condition = SyncExclusionCondition.Containing,
            )
        )

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_TYPE_CHIP_TAG)
            .assertContentDescriptionEquals(string(sharedR.string.sync_exclusions_add_type_folders))
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_CONDITION_CHIP_TAG)
            .assertContentDescriptionEquals(string(sharedR.string.sync_exclusions_add_condition_containing))
    }

    @Test
    fun `test that tapping the type chip reports the picked option`() {
        setContent(AddExclusionByNameFormState())

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_TYPE_CHIP_TAG).performClick()
        val extensions = string(sharedR.string.sync_exclusions_add_type_extensions)
        composeTestRule.onNodeWithTag("$SYNC_SINGLE_SELECT_OPTION_TAG:$extensions").performClick()

        assertThat(selectedTypes).containsExactly(SyncExclusionTypeOption.Extensions)
    }

    @Test
    fun `test that tapping the condition chip reports the picked condition`() {
        setContent(AddExclusionByNameFormState(typeOption = SyncExclusionTypeOption.Files))

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_CONDITION_CHIP_TAG).performClick()
        val endingWith = string(sharedR.string.sync_exclusions_add_condition_ending_with)
        composeTestRule.onNodeWithTag("$SYNC_SINGLE_SELECT_OPTION_TAG:$endingWith").performClick()

        assertThat(selectedConditions).containsExactly(SyncExclusionCondition.EndingWith)
    }

    @Test
    fun `test that the wildcard helper is hidden for a condition that reads no wildcards`() {
        setContent(
            AddExclusionByNameFormState(
                typeOption = SyncExclusionTypeOption.Files,
                condition = SyncExclusionCondition.BeginningWith,
            )
        )

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_WILDCARD_HELPER_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the wildcard helper is shown for the matching wildcard condition`() {
        setContent(
            AddExclusionByNameFormState(
                typeOption = SyncExclusionTypeOption.Files,
                condition = SyncExclusionCondition.MatchingWildcard,
            )
        )

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_WILDCARD_HELPER_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that a validation error replaces the wildcard helper`() {
        setContent(
            AddExclusionByNameFormState(
                typeOption = SyncExclusionTypeOption.Files,
                condition = SyncExclusionCondition.MatchingWildcard,
                value = "*.tmp",
                errorRes = sharedR.string.sync_exclusions_add_name_duplicate_error,
            )
        )

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_WILDCARD_HELPER_TAG).assertDoesNotExist()
        composeTestRule
            .onNodeWithText(string(sharedR.string.sync_exclusions_add_name_duplicate_error))
            .assertIsDisplayed()
    }

    @Test
    fun `test that editing the name reports the new value`() {
        setContent(
            AddExclusionByNameFormState(
                typeOption = SyncExclusionTypeOption.Files,
                condition = SyncExclusionCondition.MatchingExactly,
                value = "notes.txt",
            )
        )

        composeTestRule.onNodeWithText("notes.txt").performTextReplacement("draft.txt")

        assertThat(enteredValues).containsExactly("draft.txt")
    }
}
