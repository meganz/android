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
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionBySizeFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.EditSyncSizeExclusionUiState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeBoundInput
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit
import mega.privacy.android.navigation.contract.menu.CommonMenuAction
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.shared.resources.R as sharedR

@RunWith(AndroidJUnit4::class)
internal class EditSyncSizeExclusionScreenTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private var deleted = 0
    private var saved = 0
    private var dismissed = 0

    private val prefilled = EditSyncSizeExclusionUiState(
        bySize = AddExclusionBySizeFormState(
            condition = SyncSizeCondition.BiggerThan,
            firstBound = SyncSizeBoundInput(value = "100", unit = SyncSizeUnit.Megabytes),
        ),
    )

    private fun setContent(uiState: EditSyncSizeExclusionUiState = prefilled) {
        composeTestRule.setContent {
            EditSyncSizeExclusionScreen(
                uiState = uiState,
                onConditionSelected = {},
                onFirstBoundValueChanged = {},
                onFirstBoundUnitSelected = {},
                onSecondBoundValueChanged = {},
                onSecondBoundUnitSelected = {},
                onDelete = { deleted++ },
                onSave = { saved++ },
                onDismiss = { dismissed++ },
            )
        }
    }

    private fun string(resId: Int) = composeTestRule.activity.getString(resId)

    @Test
    fun `test that the screen shows the same edit chrome as the name screen`() {
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
        composeTestRule.onNodeWithTag(CommonMenuAction.Delete.testTag).assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription(string(sharedR.string.general_delete))
            .assertIsDisplayed()
    }

    @Test
    fun `test that the screen shows the size form and not the name form`() {
        setContent()

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_DESCRIPTION_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_DESCRIPTION_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_TAB_ROW_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the screen shows the declared bounds in the form`() {
        setContent()

        composeTestRule
            .onNodeWithTag(ADD_EXCLUSION_SIZE_CONDITION_CHIP_TAG)
            .assertContentDescriptionEquals(
                string(sharedR.string.sync_exclusions_add_size_condition_bigger_than),
            )
        composeTestRule.onNodeWithText("100").assertIsDisplayed()
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_SECOND_VALUE_TAG).assertDoesNotExist()
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
    fun `test that save is refused while the bounds leave out every file`() {
        setContent(
            EditSyncSizeExclusionUiState(
                bySize = AddExclusionBySizeFormState(
                    condition = SyncSizeCondition.NotBetween,
                    firstBound = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Kilobytes),
                    secondBound = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Kilobytes),
                ),
            )
        )

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_RANGE_ERROR_TAG).assertIsDisplayed()
        composeTestRule
            .onNodeWithText(string(sharedR.string.general_action_save))
            .performClick()

        assertThat(saved).isEqualTo(0)
    }

    @Test
    fun `test that the trash reports a deletion without confirming first`() {
        setContent()

        composeTestRule.onNodeWithTag(CommonMenuAction.Delete.testTag).performClick()

        assertThat(deleted).isEqualTo(1)
        assertThat(saved).isEqualTo(0)
        assertThat(dismissed).isEqualTo(0)
    }

    /**
     * The trash carries no bound, so it can only mean the whole exclusion. What is worth pinning
     * is that a drawn second bound does not change the action: a condition-scoped trash would
     * leave a range half deleted.
     */
    @Test
    fun `test that the trash reports the same deletion under a range condition`() {
        setContent(
            EditSyncSizeExclusionUiState(
                bySize = AddExclusionBySizeFormState(
                    condition = SyncSizeCondition.NotBetween,
                    firstBound = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Kilobytes),
                    secondBound = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Gigabytes),
                ),
            )
        )

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_SECOND_VALUE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(CommonMenuAction.Delete.testTag).performClick()

        assertThat(deleted).isEqualTo(1)
    }

    @Test
    fun `test that save is refused while a bound is not a size a rule can use`() {
        setContent(
            EditSyncSizeExclusionUiState(
                bySize = AddExclusionBySizeFormState(
                    condition = SyncSizeCondition.BiggerThan,
                    firstBound = SyncSizeBoundInput(value = "0", unit = SyncSizeUnit.Bytes),
                ),
            )
        )

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_RANGE_ERROR_TAG).assertDoesNotExist()
        composeTestRule
            .onNodeWithText(string(sharedR.string.general_action_save))
            .performClick()

        assertThat(saved).isEqualTo(0)
    }

    @Test
    fun `test that the trash is not refused while the bounds leave out every file`() {
        setContent(
            EditSyncSizeExclusionUiState(
                bySize = AddExclusionBySizeFormState(
                    condition = SyncSizeCondition.NotBetween,
                    firstBound = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Kilobytes),
                    secondBound = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Kilobytes),
                ),
            )
        )

        composeTestRule.onNodeWithTag(CommonMenuAction.Delete.testTag).performClick()

        assertThat(deleted).isEqualTo(1)
    }

    @Test
    fun `test that cancel dismisses without saving`() {
        setContent()

        composeTestRule
            .onNodeWithText(string(sharedR.string.general_dialog_cancel_button))
            .performClick()

        assertThat(dismissed).isEqualTo(1)
        assertThat(saved).isEqualTo(0)
    }

    @Test
    fun `test that backing out dismisses without saving`() {
        setContent()

        composeTestRule
            .onNodeWithContentDescription(TOP_BAR_NAVIGATION_ICON_DESCRIPTION)
            .performClick()

        assertThat(dismissed).isEqualTo(1)
        assertThat(saved).isEqualTo(0)
    }
}

private const val TOP_BAR_NAVIGATION_ICON_DESCRIPTION = "Navigation Icon"
