package mega.privacy.android.app.meeting.fragments.fab

import mega.privacy.android.icon.pack.R as IconR
import android.annotation.SuppressLint
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
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
    isOn: MutableState<Boolean>,
    enabled: Boolean,
    onIcon: Int,
    offIcon: Int,
    disableIcon: Int,
    modifier: Modifier = Modifier,
    onOff: ((Boolean) -> Unit)? = null,
) {
    val icon = when {
        !enabled -> disableIcon
        isOn.value -> onIcon
        else -> offIcon
    }
    CellButton(
        iconId = icon,
        itemName = itemName,
        onItemClick = {
            if (enabled) {
                onOff?.invoke(isOn.value)
                isOn.value = !isOn.value
            }
        },
        modifier = modifier,
        type = if (isOn.value) CellButtonType.On else CellButtonType.Off,
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
        val isOn = mutableStateOf(value)
        OnOffFab(
            itemName = "Mic",
            isOn = isOn,
            enabled = true,
            onIcon = IconR.drawable.ic_mic,
            offIcon = IconR.drawable.ic_mic_stop,
            disableIcon = IconR.drawable.ic_mic_stop,
            onOff = {
                isOn.value = !isOn.value
            },
        )
    }
}
