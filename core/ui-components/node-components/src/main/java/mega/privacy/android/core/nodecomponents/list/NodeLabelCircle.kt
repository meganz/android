package mega.privacy.android.core.nodecomponents.list

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.nodecomponents.R
import mega.privacy.android.domain.entity.NodeLabel


/**
 * Composable function to display a circle with a color representing the node label
 *
 * @param label the label of the node
 * @param modifier the modifier to be applied to the circle
 */
@Composable
fun NodeLabelCircle(
    label: NodeLabel,
    modifier: Modifier = Modifier,
    size: Dp = 7.dp,
) {
    val color = when (label) {
        NodeLabel.RED -> R.color.label_red
        NodeLabel.ORANGE -> R.color.label_orange
        NodeLabel.YELLLOW -> R.color.label_yellow
        NodeLabel.GREEN -> R.color.label_green
        NodeLabel.BLUE -> R.color.label_blue
        NodeLabel.PURPLE -> R.color.label_purple
        NodeLabel.GREY -> R.color.label_purple
    }.let { colorResource(id = it) }
    Canvas(
        modifier = modifier.size(size),
        onDraw = {
            drawCircle(color = color)
        },
    )
}