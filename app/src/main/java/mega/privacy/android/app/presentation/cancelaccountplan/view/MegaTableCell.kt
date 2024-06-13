package mega.privacy.android.app.presentation.cancelaccountplan.view

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import mega.privacy.android.app.presentation.twofactorauthentication.extensions.drawableId
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.extensions.body2medium
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor

@Composable
private fun getTextAlignment(cellAlignment: TableCell.CellAlignment): TextAlign {
    return when (cellAlignment) {
        TableCell.CellAlignment.Start -> TextAlign.Start
        TableCell.CellAlignment.Center -> TextAlign.Center
        TableCell.CellAlignment.End -> TextAlign.End
    }
}

@Composable
private fun getCellAlignment(cellAlignment: TableCell.CellAlignment): Alignment {
    return when (cellAlignment) {
        TableCell.CellAlignment.Start -> Alignment.CenterStart
        TableCell.CellAlignment.Center -> Alignment.Center
        TableCell.CellAlignment.End -> Alignment.CenterEnd
    }
}

@Composable
internal fun MegaTableCell(
    cell: TableCell,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
    ) {
        if (cell is TableCell.TextCell) {
            val textAlignment = getTextAlignment(cellAlignment = cell.cellAlignment)
            val cellAlignment = getCellAlignment(cellAlignment = cell.cellAlignment)
            when (cell.style) {
                TableCell.TextCellStyle.Header -> {
                    MegaText(
                        text = cell.text,
                        textColor = TextColor.Primary,
                        style = MaterialTheme.typography.body2medium,
                        textAlign = textAlignment,
                        modifier = Modifier
                            .align(cellAlignment)
                            .wrapContentSize()
                            .testTag(TABLE_TEXT_CELL_TEST_TAG),
                    )
                }

                TableCell.TextCellStyle.SubHeader -> {
                    MegaText(
                        text = cell.text,
                        textColor = TextColor.Primary,
                        style = MaterialTheme.typography.caption.copy(fontWeight = FontWeight.SemiBold),
                        textAlign = textAlignment,
                        modifier = Modifier
                            .align(cellAlignment)
                            .wrapContentSize()
                            .testTag(TABLE_TEXT_CELL_TEST_TAG),
                    )
                }

                TableCell.TextCellStyle.Normal -> {
                    MegaText(
                        text = cell.text,
                        textColor = TextColor.Secondary,
                        style = MaterialTheme.typography.caption,
                        textAlign = textAlignment,
                        modifier = Modifier
                            .align(cellAlignment)
                            .wrapContentSize()
                            .testTag(TABLE_TEXT_CELL_TEST_TAG),
                    )
                }
            }
        } else if (cell is TableCell.IconCell) {
            Image(
                painter = painterResource(id = cell.iconResId),
                contentDescription = "",
                modifier = Modifier
                    .align(Alignment.Center)
                    .wrapContentSize()
                    .testTag(TABLE_ICON_CELL_TEST_TAG)
                    .semantics { drawableId = cell.iconResId }

            )
        }
    }
}

@Composable
@CombinedThemePreviews
private fun CellPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MegaTableCell(
            cell = TableCell.TextCell(
                "Hello there",
                TableCell.TextCellStyle.Header,
                TableCell.CellAlignment.Start
            )
        )
    }
}

internal const val TABLE_ICON_CELL_TEST_TAG = "mega_table:icon_cell_image"
internal const val TABLE_TEXT_CELL_TEST_TAG = "mega_table:text_cell_text"