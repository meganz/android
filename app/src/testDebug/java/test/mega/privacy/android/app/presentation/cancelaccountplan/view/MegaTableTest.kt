package mega.privacy.android.app.presentation.cancelaccountplan.view

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import dagger.hilt.android.testing.HiltAndroidTest
import mega.privacy.android.app.presentation.cancelaccountplan.view.TableCell
import mega.privacy.android.app.presentation.cancelaccountplan.view.MegaTable
import mega.privacy.android.app.presentation.cancelaccountplan.view.TABLE_ROW_DIVIDER
import mega.privacy.android.app.presentation.cancelaccountplan.view.TABLE_ROW_TEST_TAG
import mega.privacy.android.shared.resources.R
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import mega.privacy.android.app.fromId

@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w720dp-h1280dp-xhdpi")
class MegaTableTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `test that all screen components are displayed correctly`() {

        val columns = 3
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
            TableCell.TextCell(
                text = fromId(id = R.string.account_cancel_account_screen_plan_links_with_expiry_dates),
                style = TableCell.TextCellStyle.SubHeader,
                cellAlignment = TableCell.CellAlignment.Start,
            ),
            TableCell.IconCell(iconResId = mega.privacy.android.icon.pack.R.drawable.ic_not_available),
            TableCell.IconCell(iconResId = mega.privacy.android.icon.pack.R.drawable.ic_available),
        )
        val expectedNumberRows = cells.chunked(columns).size

        composeTestRule.setContent {
            MegaTable(
                numOfColumn = columns,
                tableCells = cells
            )

        }
        composeTestRule.onAllNodesWithTag(TABLE_ROW_TEST_TAG).assertCountEquals(expectedNumberRows)
        composeTestRule.onAllNodesWithTag(TABLE_ROW_DIVIDER).assertCountEquals(expectedNumberRows)
    }
}