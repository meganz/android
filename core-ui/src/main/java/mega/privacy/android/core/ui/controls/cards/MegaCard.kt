package mega.privacy.android.core.ui.controls.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.ui.theme.MegaTheme
import mega.privacy.android.core.ui.theme.extensions.black_white

/**
 * Card to display info
 *
 * @param content   Content of the card
 * @param modifier  [Modifier]
 * @param onClicked Action to perform when tap/click on the card
 */
@Composable
fun MegaCard(
    content: @Composable ColumnScope.() -> Unit,
    modifier: Modifier = Modifier,
    onClicked: (() -> Unit)? = null,
) {
    val roundedCornersShape = RoundedCornerShape(6.dp)
    Column(
        modifier = modifier
            .shadow(
                elevation = 3.dp,
                shape = roundedCornersShape,
                spotColor = MaterialTheme.colors.black_white,
                ambientColor = MaterialTheme.colors.black_white,
            )
            .background(
                color = MegaTheme.colors.background.surface1,
                shape = roundedCornersShape,
            )
            .clickable {
                if (onClicked != null) {
                    onClicked()
                }
            },
    ) {
        content()
    }
}