package mega.privacy.android.shared.nodes.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import mega.android.core.ui.components.label.Label
import mega.android.core.ui.components.label.LabelColor
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
) {
    val color = when (label) {
        NodeLabel.RED -> LabelColor.Red
        NodeLabel.ORANGE -> LabelColor.Orange
        NodeLabel.YELLOW -> LabelColor.Yellow
        NodeLabel.GREEN -> LabelColor.Green
        NodeLabel.BLUE -> LabelColor.Blue
        NodeLabel.PURPLE -> LabelColor.Purple
        NodeLabel.GREY -> LabelColor.Grey
    }
    Label(
        color = color,
        modifier = modifier,
    )
}