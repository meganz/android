package mega.privacy.android.shared.original.core.ui.controls.sheets

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme

/**
 * MegaBottomSheetContainer
 *
 * Should be used in only androidx based BottomSheetDialogFragment
 *
 * @param modifier Modifier
 * @param content content composable
 */
@Composable
fun MegaBottomSheetContainer(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .clip(
                RoundedCornerShape(
                    topStart = 8.dp,
                    topEnd = 8.dp
                )
            )
            .background(MegaOriginalTheme.colors.background.surface1),
    ) {
        content()
    }
}