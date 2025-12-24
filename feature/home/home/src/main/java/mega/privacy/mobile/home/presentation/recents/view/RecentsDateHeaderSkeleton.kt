package mega.privacy.mobile.home.presentation.recents.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.theme.AppTheme

@Composable
fun RecentsDateHeaderSkeleton(
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        MegaText("", style = AppTheme.typography.labelMedium)
        Spacer(
            modifier = Modifier
                .width(80.dp)
                .height(16.dp)
                .shimmerEffect()
        )
    }
}