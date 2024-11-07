package mega.privacy.android.app.meeting.fragments.fab

import mega.privacy.android.icon.pack.R as IconR
import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.PreviewParameter
import mega.privacy.android.shared.original.core.ui.controls.chat.attachpanel.CellButton
import mega.privacy.android.shared.original.core.ui.controls.chat.attachpanel.CellButtonType
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * OnOffFab is a FloatingActionButton that can be toggled on and off.
 */
@Composable
fun OnOffFab(
    itemName: String,
    isOn: Boolean,
    enabled: Boolean,
    onIcon: Int,
    offIcon: Int,
    disableIcon: Int,
    modifier: Modifier = Modifier,
    onOff: ((Boolean) -> Unit)? = null,
) {
    CellButton(
        iconId = when {
            !enabled -> disableIcon
            isOn -> onIcon
            else -> offIcon
        },
        itemName = itemName,
        onItemClick = {
            if (enabled) {
                onOff?.invoke(isOn)
            }
        },
        modifier = modifier,
        type = if (isOn) CellButtonType.On else CellButtonType.Off,
        enabled = enabled,
    )
}

@SuppressLint("UnrememberedMutableState")
@CombinedThemePreviews
@Composable
private fun OnOffFabPreviewOff(
    @PreviewParameter(BooleanProvider::class) value: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        OnOffFab(
            itemName = "Mic",
            isOn = true,
            enabled = true,
            onIcon = IconR.drawable.ic_mic,
            offIcon = IconR.drawable.ic_mic_stop,
            disableIcon = IconR.drawable.ic_mic_stop,
            onOff = {},
        )
    }
}
