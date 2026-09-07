package mega.privacy.android.feature.sync.ui.views

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@Config(qualifiers = "fr-rFr-w1080dp-h1920dp")
@RunWith(AndroidJUnit4::class)
internal class SyncSingleSelectBottomSheetTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private enum class SizeUnit { Bytes, Kilobytes, Megabytes }

    private val options = listOf(
        SyncSingleSelectOption(SizeUnit.Bytes, LABEL_BYTES),
        SyncSingleSelectOption(SizeUnit.Kilobytes, LABEL_KILOBYTES),
        SyncSingleSelectOption(SizeUnit.Megabytes, LABEL_MEGABYTES),
    )

    private val selections = mutableListOf<SizeUnit>()
    private var dismissCount = 0

    private fun setContent(
        selectedValue: SizeUnit? = SizeUnit.Kilobytes,
        subtitle: String? = null,
    ) {
        composeTestRule.setContent {
            SyncSingleSelectSheetContent(
                title = TITLE,
                options = options,
                selectedValue = selectedValue,
                onOptionSelected = { selections += it },
                subtitle = subtitle,
            )
        }
        composeTestRule.waitForIdle()
    }

    private fun optionTag(label: String) = "$SYNC_SINGLE_SELECT_OPTION_TAG:$label"

    private fun radioTag(label: String) = "$SYNC_SINGLE_SELECT_RADIO_TAG:$label"

    @Test
    fun `test that SyncSingleSelectSheetContent displays the title and every option label`() {
        setContent()

        composeTestRule.onNodeWithText(TITLE).assertIsDisplayed()
        composeTestRule.onNodeWithText(LABEL_BYTES).assertIsDisplayed()
        composeTestRule.onNodeWithText(LABEL_KILOBYTES).assertIsDisplayed()
        composeTestRule.onNodeWithText(LABEL_MEGABYTES).assertIsDisplayed()
    }

    @Test
    fun `test that SyncSingleSelectSheetContent displays the subtitle when one is provided`() {
        setContent(subtitle = SUBTITLE)

        composeTestRule.onNodeWithText(SUBTITLE).assertIsDisplayed()
    }

    @Test
    fun `test that SyncSingleSelectSheetContent marks only the labelled selected option as selected`() {
        setContent(selectedValue = SizeUnit.Kilobytes)

        composeTestRule.onNodeWithTag(optionTag(LABEL_KILOBYTES)).assertIsSelected()
        composeTestRule.onNodeWithTag(optionTag(LABEL_BYTES)).assertIsNotSelected()
        composeTestRule.onNodeWithTag(optionTag(LABEL_MEGABYTES)).assertIsNotSelected()
    }

    @Test
    fun `test that SyncSingleSelectSheetContent marks no option as selected when selectedValue is null`() {
        setContent(selectedValue = null)

        composeTestRule.onNodeWithTag(optionTag(LABEL_BYTES)).assertIsNotSelected()
        composeTestRule.onNodeWithTag(optionTag(LABEL_KILOBYTES)).assertIsNotSelected()
        composeTestRule.onNodeWithTag(optionTag(LABEL_MEGABYTES)).assertIsNotSelected()
    }

    @Test
    fun `test that SyncSingleSelectSheetContent announces the option state only on the labelled row`() {
        setContent(selectedValue = SizeUnit.Kilobytes)

        composeTestRule.onNodeWithTag(optionTag(LABEL_KILOBYTES)).assertIsSelected()
        composeTestRule.onNodeWithTag(radioTag(LABEL_KILOBYTES)).assertDoesNotExist()
    }

    @Test
    fun `test that SyncSingleSelectSheetContent reports the value when an option row is clicked`() {
        setContent()

        composeTestRule.onNodeWithText(LABEL_MEGABYTES).performClick()

        assertThat(selections).containsExactly(SizeUnit.Megabytes)
    }

    @Test
    fun `test that SyncSingleSelectSheetContent reports the value when an option radio button is clicked`() {
        setContent()

        // the radio is cleared from the merged tree so its state is announced on the row instead,
        // which is why it can only be reached unmerged
        composeTestRule.onNodeWithTag(radioTag(LABEL_BYTES), useUnmergedTree = true)
            .performClick()

        assertThat(selections).containsExactly(SizeUnit.Bytes)
    }

    @Test
    fun `test that SyncSingleSelectSheetContent reports the value when the already selected option is clicked`() {
        setContent(selectedValue = SizeUnit.Kilobytes)

        composeTestRule.onNodeWithText(LABEL_KILOBYTES).performClick()

        assertThat(selections).containsExactly(SizeUnit.Kilobytes)
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Test
    fun `test that SyncSingleSelectBottomSheet reports the value and then dismisses when an option is clicked`() {
        composeTestRule.setContent {
            SyncSingleSelectBottomSheet(
                title = TITLE,
                options = options,
                selectedValue = SizeUnit.Kilobytes,
                onOptionSelected = { selections += it },
                onDismissRequest = { dismissCount++ },
                sheetState = rememberModalBottomSheetState(),
            )
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(LABEL_MEGABYTES).performClick()
        composeTestRule.waitUntil(timeoutMillis = 5_000) { dismissCount == 1 }

        assertThat(selections).containsExactly(SizeUnit.Megabytes)
        assertThat(dismissCount).isEqualTo(1)
    }

    private companion object {
        const val TITLE = "Unit"
        const val SUBTITLE = "Choose a unit"
        const val LABEL_BYTES = "B"
        const val LABEL_KILOBYTES = "KB"
        const val LABEL_MEGABYTES = "MB"
    }
}
