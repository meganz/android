package mega.privacy.android.app.meeting.fragments.fab

import mega.privacy.android.icon.pack.R as IconR
import android.annotation.SuppressLint
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.FloatingActionButton
import androidx.compose.material.FloatingActionButtonDefaults
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews

/**
 * OnOffFab is a FloatingActionButton that can be toggled on and off.
 */
@Composable
fun OnOffFab(
    isOn: MutableState<Boolean>,
    enabled: Boolean,
    onIcon: Int,
    offIcon: Int,
    disableIcon: Int,
    modifier: Modifier = Modifier,
    onOff: ((Boolean) -> Unit)? = null,
    onBackgroundTint: Color = grey,
    offBackgroundTint: Color = Color.White,
    onIconTint: Color = Color.White,
    offIconTint: Color = n800,
    disabledIconTint: Color = Color.White.copy(alpha = 0.54f),
) {
    val icon = when {
        !enabled -> disableIcon
        isOn.value -> onIcon
        else -> offIcon
    }

    val iconTint = when {
        !enabled -> disabledIconTint
        isOn.value -> onIconTint
        else -> offIconTint
    }

    val backgroundTint = when {
        !enabled -> onBackgroundTint
        isOn.value -> onBackgroundTint
        else -> offBackgroundTint
    }

    FloatingActionButton(
        modifier = modifier
            .width(48.dp)
            .height(48.dp)
            .shadow(
                0.dp,
                shape = CircleShape
            ),
        onClick = {
            onOff?.invoke(isOn.value)
            isOn.value = !isOn.value
        },
        backgroundColor = backgroundTint,
        contentColor = iconTint,
        elevation = if (enabled && !isOn.value) FloatingActionButtonDefaults.elevation(7.dp) else FloatingActionButtonDefaults.elevation(
            0.dp
        )
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = null
        )
    }
}

@SuppressLint("UnrememberedMutableState")
@CombinedThemePreviews
@Composable
private fun OnOffFabPreviewOff(
    @PreviewParameter(BooleanProvider::class) value: Boolean,
) {
    val isOn = mutableStateOf(value)
    OnOffFab(
        isOn = isOn,
        enabled = true,
        onIcon = IconR.drawable.ic_mic,
        offIcon = IconR.drawable.ic_mic_stop,
        disableIcon = IconR.drawable.ic_mic_stop,
        onOff = {
            isOn.value = !isOn.value
        }
    )
}

private val n800: Color = Color(48, 50, 51, 255)
private val grey: Color = Color(0, 0, 0, 32)
