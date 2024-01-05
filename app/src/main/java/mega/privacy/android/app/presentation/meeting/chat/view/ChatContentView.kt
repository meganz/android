package mega.privacy.android.app.presentation.meeting.chat.view

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
internal fun ChatContentView(
    modifier: Modifier = Modifier,
    topViews: @Composable () -> Unit = {},
    listView: @Composable (bottomPadding: Dp) -> Unit = {},
    bottomViews: @Composable () -> Unit = {},
) = Box(
    modifier = modifier
) {
    var bottomButtonsHeight by remember { mutableStateOf(12.dp) }
    val density = LocalDensity.current
    listView(bottomButtonsHeight)
    Box(
        modifier = Modifier.align(Alignment.TopCenter)
    ) {
        topViews()
    }
    Box(modifier = Modifier
        .align(Alignment.BottomCenter)
        .onGloballyPositioned {
            bottomButtonsHeight = with(density) { it.size.height.toDp() }
        }) {
        bottomViews()
    }

}

