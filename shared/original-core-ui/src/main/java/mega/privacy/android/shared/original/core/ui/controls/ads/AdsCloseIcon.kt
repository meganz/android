package mega.privacy.android.shared.original.core.ui.controls.ads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Close icon for ads.
 *
 * @param modifier Modifier.
 * @param onClick Click listener.
 */
@Composable
fun AdsCloseIcon(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    Icon(
        painter = painterResource(id = R.drawable.ic_universal_close),
        contentDescription = "Close Icon",
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(4.dp)
            .size(16.dp),
        tint = MegaOriginalTheme.colors.button.primary
    )
}

@CombinedThemePreviews
@Composable
private fun AdsCloseIconPreview() {
    OriginalTempTheme(isSystemInDarkTheme()) {
        AdsCloseIcon {}
    }
}