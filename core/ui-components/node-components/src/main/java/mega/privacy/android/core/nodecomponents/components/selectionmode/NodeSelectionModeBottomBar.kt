package mega.privacy.android.core.nodecomponents.components.selectionmode

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.toolbar.MegaFloatingToolbar
import mega.android.core.ui.model.TopAppBarAction
import mega.privacy.android.core.nodecomponents.model.NodeSelectionAction

@Composable
fun NodeSelectionModeBottomBar(
    count: Int,
    visible: Boolean,
    modifier: Modifier = Modifier,
    onActionPressed: (TopAppBarAction) -> Unit
) {
    AnimatedVisibility(
        visible = visible,
        enter = slideInVertically(
            animationSpec = tween(durationMillis = 300),
            initialOffsetY = { it }
        ),
        exit = ExitTransition.None
    ) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .systemBarsPadding()
                .padding(bottom = 16.dp)
        ) {
            MegaFloatingToolbar(
                modifier = Modifier.align(Alignment.BottomCenter),
                actions = listOf(
                    NodeSelectionAction.Download,
                    NodeSelectionAction.ShareLink(count = count),
                    NodeSelectionAction.Move,
                    NodeSelectionAction.RubbishBin,
                    NodeSelectionAction.More
                ),
                onActionPressed = onActionPressed
            )
        }
    }
}
