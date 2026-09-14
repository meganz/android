package mega.privacy.android.feature.sync.ui.exclusion

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionByExtensionFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionExtensionError
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncExclusionTypeOption
import mega.privacy.android.feature.sync.ui.views.SYNC_SINGLE_SELECT_OPTION_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.shared.resources.R as sharedR

@RunWith(AndroidJUnit4::class)
internal class AddExclusionByExtensionFormTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val selectedTypes = mutableListOf<SyncExclusionTypeOption>()
    private val enteredValues = mutableListOf<String>()

    private fun setContent(state: AddExclusionByExtensionFormState) {
        composeTestRule.setContent {
            AddExclusionByExtensionForm(
                state = state,
                onTypeOptionSelected = selectedTypes::add,
                onValueChanged = enteredValues::add,
            )
        }
    }

    private fun string(id: Int) = composeTestRule.activity.getString(id)

    @Test
    fun `test that the description names what an extension rule does`() {
        setContent(AddExclusionByExtensionFormState())

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_EXTENSION_DESCRIPTION_TAG)
            .assertTextEquals(string(sharedR.string.sync_exclusions_add_description_extensions))
    }

    @Test
    fun `test that the type chip reads Extensions and is selected`() {
        setContent(AddExclusionByExtensionFormState())

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_EXTENSION_TYPE_CHIP_TAG)
            .assertContentDescriptionEquals(string(sharedR.string.sync_exclusions_add_type_extensions))
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_EXTENSION_TYPE_CHIP_TAG).assertIsSelected()
    }

    @Test
    fun `test that no condition is offered`() {
        setContent(AddExclusionByExtensionFormState())

        composeTestRule
            .onNodeWithContentDescription(string(sharedR.string.sync_exclusions_add_condition))
            .assertDoesNotExist()
        composeTestRule
            .onNodeWithText(string(sharedR.string.sync_exclusions_add_condition))
            .assertDoesNotExist()
    }

    @Test
    fun `test that the helper tells the user to separate extensions with a comma`() {
        setContent(AddExclusionByExtensionFormState(value = "log"))

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_EXTENSION_HELPER_TAG)
            .assertTextEquals(string(sharedR.string.sync_exclusions_add_extension_helper))
    }

    @Test
    fun `test that the helper is shown before anything is typed`() {
        setContent(AddExclusionByExtensionFormState())

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_EXTENSION_HELPER_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the helper gives way to an invalid error`() {
        setContent(
            AddExclusionByExtensionFormState(
                value = "my doc",
                error = AddExclusionExtensionError.Invalid,
            )
        )

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_EXTENSION_HELPER_TAG).assertDoesNotExist()
        composeTestRule
            .onNodeWithText(string(sharedR.string.sync_exclusions_add_extension_invalid_error))
            .assertIsDisplayed()
    }

    @Test
    fun `test that the duplicate error names the offending extension`() {
        setContent(
            AddExclusionByExtensionFormState(
                value = "log, mp3",
                error = AddExclusionExtensionError.Duplicate("mp3"),
            )
        )

        composeTestRule
            .onNodeWithText(
                composeTestRule.activity.getString(
                    sharedR.string.sync_exclusions_add_extension_duplicate_error,
                    "mp3",
                )
            )
            .assertIsDisplayed()
    }

    @Test
    fun `test that the field shows the typed value`() {
        setContent(AddExclusionByExtensionFormState(value = "mp4, zip"))

        composeTestRule.onNodeWithText("mp4, zip").assertIsDisplayed()
    }

    @Test
    fun `test that typing reports the raw value unparsed`() {
        setContent(AddExclusionByExtensionFormState(value = "log"))

        composeTestRule.onNodeWithText("log").performTextReplacement("log, mp4")

        assertThat(enteredValues).containsExactly("log, mp4")
    }

    @Test
    fun `test that the type chip opens the type sheet with Extensions already marked`() {
        setContent(AddExclusionByExtensionFormState())

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_EXTENSION_TYPE_CHIP_TAG).performClick()

        composeTestRule.onNodeWithText(string(sharedR.string.sync_exclusions_add_type))
            .assertIsDisplayed()
        val extensions = string(sharedR.string.sync_exclusions_add_type_extensions)
        composeTestRule.onNodeWithTag("$SYNC_SINGLE_SELECT_OPTION_TAG:$extensions")
            .assertIsSelected()
    }

    @Test
    fun `test that picking a type from the sheet reports it`() {
        setContent(AddExclusionByExtensionFormState())

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_EXTENSION_TYPE_CHIP_TAG).performClick()
        val files = string(sharedR.string.sync_exclusions_add_type_files)
        composeTestRule.onNodeWithTag("$SYNC_SINGLE_SELECT_OPTION_TAG:$files").performClick()

        assertThat(selectedTypes).containsExactly(SyncExclusionTypeOption.Files)
    }

    @Test
    fun `test that picking a type closes the type sheet`() {
        setContent(AddExclusionByExtensionFormState())

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_EXTENSION_TYPE_CHIP_TAG).performClick()
        val files = string(sharedR.string.sync_exclusions_add_type_files)
        composeTestRule.onNodeWithTag("$SYNC_SINGLE_SELECT_OPTION_TAG:$files").performClick()

        composeTestRule.onNodeWithTag("$SYNC_SINGLE_SELECT_OPTION_TAG:$files").assertDoesNotExist()
    }

    @Test
    fun `test that re-picking Extensions closes the type sheet`() {
        setContent(AddExclusionByExtensionFormState())

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_EXTENSION_TYPE_CHIP_TAG).performClick()
        val extensions = string(sharedR.string.sync_exclusions_add_type_extensions)
        composeTestRule.onNodeWithTag("$SYNC_SINGLE_SELECT_OPTION_TAG:$extensions").performClick()

        composeTestRule
            .onNodeWithTag("$SYNC_SINGLE_SELECT_OPTION_TAG:$extensions")
            .assertDoesNotExist()
        assertThat(selectedTypes).containsExactly(SyncExclusionTypeOption.Extensions)
    }
}
