package mega.privacy.android.app.presentation.cancelaccountplan.view

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme

@Composable
internal fun MegaTableRow(
    rowCells: List<TableCell>,
    rowHeight: Dp,
    rowPadding: Dp,
    modifier: Modifier = Modifier,
) {
    val totalColumns = rowCells.size
    LazyVerticalGrid(
        columns = GridCells.Fixed(totalColumns),
        modifier = modifier.padding(horizontal = rowPadding)
    ) {
        itemsIndexed(rowCells) { index, cell ->
            val columnIndex = index % totalColumns
            val widthFraction = if (columnIndex == 0) 0.4f else
                (0.6f / (totalColumns - 1).coerceAtLeast(1))

            MegaTableCell(
                cell = cell,
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .height(rowHeight)
                    .padding(horizontal = 1.dp)
                    .testTag(TABLE_CELL_TEST_TAG)
            )
        }
    }
}

@Composable
@CombinedThemePreviews
private fun TableRowPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        MegaTableRow(
            rowCells = listOf(
                TableCell.TextCell(
                    "Feature",
                    TableCell.TextCellStyle.Header,
                    TableCell.CellAlignment.Start
                ),
                TableCell.TextCell(
                    "Free Plan",
                    TableCell.TextCellStyle.Header,
                    TableCell.CellAlignment.Center
                ),
                TableCell.TextCell(
                    "Pro Plan",
                    TableCell.TextCellStyle.Header,
                    TableCell.CellAlignment.Center
                ),
            ),
            rowHeight = 50.dp,
            rowPadding = 8.dp
        )
    }
}

internal const val TABLE_CELL_TEST_TAG = "table_row:cell"