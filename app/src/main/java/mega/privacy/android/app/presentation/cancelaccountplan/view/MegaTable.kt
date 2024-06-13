package mega.privacy.android.app.presentation.cancelaccountplan.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.controls.dividers.DividerType
import mega.privacy.android.shared.original.core.ui.controls.dividers.MegaDivider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.backgroundToken
import mega.privacy.android.shared.original.core.ui.theme.values.BackgroundColor

@Composable
internal fun MegaTable(
    numOfColumn: Int,
    tableCells: List<TableCell>,
    modifier: Modifier = Modifier,
    rowHeight: Dp = 50.dp,
    rowPadding: Dp = 8.dp,
) {
    Column(modifier = modifier) {
        tableCells.chunked(numOfColumn).forEachIndexed { index, sublist ->
            MegaTableRow(
                rowCells = sublist,
                modifier = Modifier
                    .testTag(TABLE_ROW_TEST_TAG)
                    .then(
                        if (index == 0) Modifier
                            .backgroundToken(BackgroundColor.Surface1) else Modifier
                    ),
                rowHeight = rowHeight,
                rowPadding = rowPadding
            )
            MegaDivider(
                dividerType = DividerType.FullSize,
                modifier = Modifier.testTag(TABLE_ROW_DIVIDER)
            )
        }
    }
}

@Composable
@CombinedThemePreviews
private fun MegaTablePreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MegaTable(
            numOfColumn = 2,
            tableCells = listOf(
                TableCell.TextCell(
                    "Column 1",
                    TableCell.TextCellStyle.Header, TableCell.CellAlignment.Start
                ),
                TableCell.TextCell(
                    "Column 2", TableCell.TextCellStyle.Header, TableCell.CellAlignment.Center
                ),
                TableCell.TextCell(
                    "Item 1", TableCell.TextCellStyle.SubHeader, TableCell.CellAlignment.Start
                ),
                TableCell.TextCell(
                    "Value 1", TableCell.TextCellStyle.Normal, TableCell.CellAlignment.Center
                ),
            )
        )
    }
}

internal const val TABLE_ROW_DIVIDER = "mega_table:divider"
internal const val TABLE_ROW_TEST_TAG = "mega_table:table_row"

