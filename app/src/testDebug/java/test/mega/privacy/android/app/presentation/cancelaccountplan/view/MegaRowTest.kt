package test.mega.privacy.android.app.presentation.cancelaccountplan.view

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.presentation.cancelaccountplan.view.TableCell
import mega.privacy.android.app.presentation.cancelaccountplan.view.MegaTableRow
import mega.privacy.android.app.presentation.cancelaccountplan.view.TABLE_CELL_TEST_TAG
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class MegaRowTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that all row cells are displayed correctly`() {

        val cells = listOf(
            TableCell.TextCell(
                "Feature",
                style = TableCell.TextCellStyle.Header,
                cellAlignment = TableCell.CellAlignment.Start,
            ),
            TableCell.TextCell(
                "Free",
                style = TableCell.TextCellStyle.Header,
                cellAlignment = TableCell.CellAlignment.Center,
            ),
            TableCell.TextCell(
                text = "Pro Plan", style = TableCell.TextCellStyle.Header,
                cellAlignment = TableCell.CellAlignment.Center,
            ),
        )

        composeTestRule.setContent {
            MegaTableRow(
                rowCells = cells,
                rowHeight = 50.dp,
                rowPadding = 8.dp
            )
        }
        composeTestRule.onAllNodesWithTag(TABLE_CELL_TEST_TAG).assertCountEquals(cells.size)
    }
}