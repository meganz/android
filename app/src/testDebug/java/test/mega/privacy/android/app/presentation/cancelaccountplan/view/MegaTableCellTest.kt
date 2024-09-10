package mega.privacy.android.app.presentation.cancelaccountplan.view

import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.hasDrawable
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class MegaTableCellTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that text cell is showing text correctly`() {

        val expectedCellText = "Feature"
        val textCell = TableCell.TextCell(
            expectedCellText,
            style = TableCell.TextCellStyle.Header,
            cellAlignment = TableCell.CellAlignment.Start,
        )

        composeTestRule.setContent { MegaTableCell(cell = textCell) }
        composeTestRule.onNodeWithTag(TABLE_TEXT_CELL_TEST_TAG).assertIsDisplayed()
            .assert(hasText(expectedCellText))
    }

    @Test
    fun `test that icon cell is showing icon correctly`() {

        val expectedIconResId = mega.privacy.android.icon.pack.R.drawable.ic_not_available
        val iconCell = TableCell.IconCell(
            iconResId = expectedIconResId,
        )

        composeTestRule.setContent { MegaTableCell(cell = iconCell) }
        composeTestRule.onNodeWithTag(TABLE_ICON_CELL_TEST_TAG).assertIsDisplayed()
        composeTestRule.onNode(hasDrawable(expectedIconResId)).assertIsDisplayed()
    }
}