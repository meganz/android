package mega.privacy.android.feature.sync.ui.exclusion

import androidx.activity.ComponentActivity
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.filterToOne
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextReplacement
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import mega.privacy.android.feature.sync.ui.model.exclusion.AddExclusionBySizeFormState
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeBoundInput
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeCondition
import mega.privacy.android.feature.sync.ui.model.exclusion.SyncSizeUnit
import mega.privacy.android.feature.sync.ui.model.exclusion.labelRes
import mega.privacy.android.feature.sync.ui.views.SYNC_SINGLE_SELECT_OPTION_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import mega.privacy.android.shared.resources.R as sharedR

@RunWith(AndroidJUnit4::class)
internal class AddExclusionBySizeFormTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val selectedConditions = mutableListOf<SyncSizeCondition>()
    private val firstValues = mutableListOf<String>()
    private val firstUnits = mutableListOf<SyncSizeUnit>()
    private val secondValues = mutableListOf<String>()
    private val secondUnits = mutableListOf<SyncSizeUnit>()

    private fun setContent(state: AddExclusionBySizeFormState) {
        composeTestRule.setContent {
            AddExclusionBySizeForm(
                state = state,
                onConditionSelected = selectedConditions::add,
                onFirstBoundValueChanged = firstValues::add,
                onFirstBoundUnitSelected = firstUnits::add,
                onSecondBoundValueChanged = secondValues::add,
                onSecondBoundUnitSelected = secondUnits::add,
            )
        }
    }

    private fun string(resId: Int) = composeTestRule.activity.getString(resId)

    /** The tagged node is the field container; only its editable child accepts text. */
    private fun editableFieldIn(tag: String) = composeTestRule
        .onAllNodes(hasSetTextAction())
        .filterToOne(hasAnyAncestor(hasTestTag(tag)))

    @Test
    fun `test that the form hides the bounds when no condition is picked`() {
        setContent(AddExclusionBySizeFormState())

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_CONDITION_CHIP_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_FIRST_VALUE_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_SECOND_VALUE_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the form shows one bound when a single bound condition is picked`() {
        setContent(AddExclusionBySizeFormState(condition = SyncSizeCondition.BiggerThan))

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_FIRST_VALUE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_FIRST_UNIT_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_SECOND_VALUE_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_CONJUNCTION_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the form shows both bounds and the conjunction when the condition is a range`() {
        setContent(AddExclusionBySizeFormState(condition = SyncSizeCondition.NotBetween))

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_FIRST_VALUE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_CONJUNCTION_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_SECOND_VALUE_TAG).assertIsDisplayed()
        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_SECOND_UNIT_TAG).assertIsDisplayed()
    }

    @Test
    fun `test that the condition sheet offers exactly the three drawn conditions`() {
        setContent(AddExclusionBySizeFormState())

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_CONDITION_CHIP_TAG).performClick()

        composeTestRule
            .onNodeWithText(string(sharedR.string.sync_exclusions_add_size_condition_title))
            .assertIsDisplayed()
        SyncSizeCondition.entries.forEach { condition ->
            val label = string(condition.labelRes)
            composeTestRule.onNodeWithTag("$SYNC_SINGLE_SELECT_OPTION_TAG:$label").assertExists()
        }
        composeTestRule.onAllNodes(isSingleSelectOption).assertCountEquals(3)
    }

    @Test
    fun `test that picking a condition reports it`() {
        setContent(AddExclusionBySizeFormState())

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_CONDITION_CHIP_TAG).performClick()
        composeTestRule
            .onNodeWithTag(
                "$SYNC_SINGLE_SELECT_OPTION_TAG:" +
                        string(sharedR.string.sync_exclusions_add_size_condition_not_between)
            )
            .performClick()

        assertThat(selectedConditions).containsExactly(SyncSizeCondition.NotBetween)
    }

    @Test
    fun `test that the first bound opens a unit sheet writing only to the first bound`() {
        setContent(
            AddExclusionBySizeFormState(
                condition = SyncSizeCondition.NotBetween,
                firstBound = SyncSizeBoundInput(unit = SyncSizeUnit.Kilobytes),
                secondBound = SyncSizeBoundInput(unit = SyncSizeUnit.Gigabytes),
            )
        )

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_FIRST_UNIT_TAG).performClick()
        composeTestRule
            .onNodeWithTag(
                "$SYNC_SINGLE_SELECT_OPTION_TAG:" +
                        string(sharedR.string.sync_exclusions_add_size_unit_kilobytes)
            )
            .assertIsSelected()
        composeTestRule
            .onNodeWithTag(
                "$SYNC_SINGLE_SELECT_OPTION_TAG:" +
                        string(sharedR.string.sync_exclusions_add_size_unit_megabytes)
            )
            .performClick()

        assertThat(firstUnits).containsExactly(SyncSizeUnit.Megabytes)
        assertThat(secondUnits).isEmpty()
    }

    @Test
    fun `test that the condition chip shows the picked condition as selected`() {
        setContent(AddExclusionBySizeFormState(condition = SyncSizeCondition.SmallerThan))

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_CONDITION_CHIP_TAG).performClick()

        composeTestRule
            .onNodeWithTag(
                "$SYNC_SINGLE_SELECT_OPTION_TAG:" +
                        string(sharedR.string.sync_exclusions_add_size_condition_smaller_than)
            )
            .assertIsSelected()
    }

    @Test
    fun `test that the second bound opens a unit sheet writing only to the second bound`() {
        setContent(
            AddExclusionBySizeFormState(
                condition = SyncSizeCondition.NotBetween,
                firstBound = SyncSizeBoundInput(unit = SyncSizeUnit.Kilobytes),
                secondBound = SyncSizeBoundInput(unit = SyncSizeUnit.Gigabytes),
            )
        )

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_SECOND_UNIT_TAG).performClick()
        composeTestRule
            .onNodeWithTag(
                "$SYNC_SINGLE_SELECT_OPTION_TAG:" +
                        string(sharedR.string.sync_exclusions_add_size_unit_gigabytes)
            )
            .assertIsSelected()
        composeTestRule
            .onNodeWithTag(
                "$SYNC_SINGLE_SELECT_OPTION_TAG:" +
                        string(sharedR.string.sync_exclusions_add_size_unit_bytes)
            )
            .performClick()

        assertThat(secondUnits).containsExactly(SyncSizeUnit.Bytes)
        assertThat(firstUnits).isEmpty()
    }

    @Test
    fun `test that the value field reports only digits`() {
        setContent(AddExclusionBySizeFormState(condition = SyncSizeCondition.BiggerThan))

        editableFieldIn(ADD_EXCLUSION_SIZE_FIRST_VALUE_TAG).performTextReplacement("1.5 MB")

        assertThat(firstValues.last()).isEqualTo("15")
    }

    @Test
    fun `test that the form shows the range error when the bounds are the wrong way round`() {
        setContent(
            AddExclusionBySizeFormState(
                condition = SyncSizeCondition.NotBetween,
                firstBound = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Gigabytes),
                secondBound = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Kilobytes),
            )
        )

        composeTestRule
            .onNodeWithText(string(sharedR.string.sync_exclusions_add_size_range_error))
            .assertIsDisplayed()
    }

    @Test
    fun `test that the form hides the range error when the range reads upwards`() {
        setContent(
            AddExclusionBySizeFormState(
                condition = SyncSizeCondition.NotBetween,
                firstBound = SyncSizeBoundInput(value = "1", unit = SyncSizeUnit.Kilobytes),
                secondBound = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Gigabytes),
            )
        )

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_RANGE_ERROR_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the form does not blame the order when a bound is not a usable size`() {
        setContent(
            AddExclusionBySizeFormState(
                condition = SyncSizeCondition.NotBetween,
                firstBound = SyncSizeBoundInput(value = "5", unit = SyncSizeUnit.Gigabytes),
                secondBound = SyncSizeBoundInput(value = "0", unit = SyncSizeUnit.Bytes),
            )
        )

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_RANGE_ERROR_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that the form hides the range error while the second bound is still empty`() {
        setContent(
            AddExclusionBySizeFormState(
                condition = SyncSizeCondition.NotBetween,
                firstBound = SyncSizeBoundInput(value = "2", unit = SyncSizeUnit.Gigabytes),
            )
        )

        composeTestRule.onNodeWithTag(ADD_EXCLUSION_SIZE_RANGE_ERROR_TAG).assertDoesNotExist()
    }

    @Test
    fun `test that each bound field reports against its own bound`() {
        setContent(AddExclusionBySizeFormState(condition = SyncSizeCondition.NotBetween))

        editableFieldIn(ADD_EXCLUSION_SIZE_SECOND_VALUE_TAG).performTextReplacement("2")

        assertThat(secondValues).containsExactly("2")
        assertThat(firstValues).isEmpty()
    }
}

/** Matches the option rows of the single-select sheet, whose tags carry their label. */
private val isSingleSelectOption = SemanticsMatcher("is a single select option") { node ->
    node.config.getOrNull(SemanticsProperties.TestTag)
        ?.startsWith("$SYNC_SINGLE_SELECT_OPTION_TAG:") == true
}
