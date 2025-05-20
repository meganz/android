package mega.privacy.android.app.presentation.passcode.view

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTheme
import mega.privacy.android.shared.original.core.ui.utils.shimmerEffect

@Composable
internal fun PasscodeLoadingView() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background)
            .systemBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(
            modifier = Modifier
                .padding(bottom = 38.dp, top = 64.dp)
                .width(250.dp)
                .height(20.dp)
                .shimmerEffect()
        )
        Row(modifier = Modifier) {
            repeat(4) {
                Spacer(
                    modifier = Modifier
                        .padding(8.dp)
                        .size(width = 48.dp, height = 48.dp)
                        .shimmerEffect()
                )
            }
        }
    }
}

@CombinedThemePreviews
@Composable
private fun PasscodeLoadingViewPreview() {
    OriginalTheme(isDark = isSystemInDarkTheme()) {
        PasscodeLoadingView()
    }
}