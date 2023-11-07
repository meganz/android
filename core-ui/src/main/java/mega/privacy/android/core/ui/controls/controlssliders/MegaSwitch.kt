package mega.privacy.android.core.ui.controls.controlssliders

import android.content.Context
import android.util.AttributeSet
import android.widget.Checkable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.core.content.withStyledAttributes
import mega.privacy.android.core.R
import mega.privacy.android.core.ui.preview.BooleanProvider
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.AndroidTheme
import mega.privacy.android.core.ui.theme.MegaTheme


/**
 * Custom implementation of Switch View following our design
 * WARNING: This component is still not ready, as disabled state colors needs to be defined. Please don't use it until it's ready
 * @property checked
 * @property enabled
 * @property onCheckedChange
 */
internal class MegaSwitch : AbstractComposeView, Checkable {
    @get:JvmName("isCheckedKt")
    @set:JvmName("setCheckedKt")
    var checked by mutableStateOf(false)

    @get:JvmName("isEnabledKt")
    @set:JvmName("setEnabledKt")
    var enabled by mutableStateOf(true)
    var onCheckedChange by mutableStateOf<(Boolean) -> Unit>({ })

    /**
     * overridden getter to be sure it's not used by mistake or from java code
     */
    override fun isEnabled() = enabled

    /**
     * overridden setter to be sure it's not used by mistake or from java code
     */
    override fun setEnabled(enabled: Boolean) {
        this.enabled = enabled
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    ) {
        context.withStyledAttributes(attrs, R.styleable.MegaSwitch) {
            checked = getBoolean(R.styleable.MegaSwitch_mega_switch_checked, checked)
            enabled = getBoolean(R.styleable.MegaSwitch_mega_switch_checked, enabled)
        }
    }

    @Composable
    override fun Content() {
        AndroidTheme(isDark = isSystemInDarkTheme()) {
            MegaSwitch(checked = checked, onCheckedChange = {
                checked = it
                onCheckedChange(it)
            })
        }
    }

    override fun setChecked(checked: Boolean) {
        this.checked = checked
    }

    override fun isChecked() = checked

    override fun toggle() {
        checked = !checked
    }
}

/**
 * Custom implementation of Switch component following our design
 * WARNING: This component is still not ready, as disabled state colors needs to be defined. Please don't use it until it's ready
 * @param checked
 * @param modifier [Modifier]
 * @param enabled
 * @param onCheckedChange
 */
@Composable
internal fun MegaSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    val thumbOffsetRaw = remember(checked) {
        val offset = (trackWidth - trackHeight) / 2
        if (checked) offset else -offset
    }

    val thumbOffset by animateDpAsState(targetValue = thumbOffsetRaw, label = "thumb Offset")

    val trackAnimationSpecs = tween<Color>()
    val thumbAnimationSpecs = tween<Color>(easing = LinearOutSlowInEasing)

    val trackColor by animateColorAsState(
        targetValue = if (checked && enabled) {
            MegaTheme.colors.components.selectionControl
        } else if (!checked) {
            MegaTheme.colors.background.pageBackground
        } else {
            MegaTheme.colors.border.disabled
        },
        animationSpec = trackAnimationSpecs,
        label = "track color"
    )
    val borderColor by animateColorAsState(
        targetValue = if (enabled) {
            MegaTheme.colors.button.outline
        } else {
            MegaTheme.colors.border.disabled
        },
        animationSpec = trackAnimationSpecs,
        label = "border color"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (checked) {
            MegaTheme.colors.background.pageBackground
        } else if (enabled && isPressed) {
            MegaTheme.colors.button.outlinePressed
        } else if (enabled) {
            MegaTheme.colors.button.outline
        } else {
            MegaTheme.colors.border.disabled
        },
        animationSpec = thumbAnimationSpecs,
        label = "thumb color"
    )
    Box(
        modifier = modifier
            .clickable(
                enabled = enabled,
                onClick = { onCheckedChange(!checked) },
                interactionSource = interactionSource,
                indication = null,
            )
            .padding((rippleSize - trackHeight) / 2)
            .size(width = trackWidth, height = trackHeight)
            .background(trackColor, CircleShape)
            .border(borderWidth, borderColor, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .size(thumbSize)
                .offset(x = thumbOffset)
                .background(thumbColor, shape = CircleShape)
                .indication(
                    interactionSource,
                    rememberRipple(
                        bounded = false,
                        radius = rippleSize / 2,
                    )
                ),
            contentAlignment = Alignment.Center,
        ) {
            AnimatedContent(
                targetState = checked,
                label = "CrossFadeChecked",
                contentAlignment = Alignment.Center,
                transitionSpec = {
                    fadeIn(spring()).togetherWith(fadeOut(spring()))
                }
            ) { checked ->
                if (checked) {
                    Icon(
                        painter = painterResource(id = R.drawable.checked),
                        tint = trackColor,
                        contentDescription = "Checked",
                    )
                } else {
                    Spacer(
                        modifier
                            .size(width = 10.dp, height = 2.dp)
                            .background(trackColor, CircleShape)
                    )
                }
            }
        }
    }
}

private val borderWidth = 1.dp
private val thumbSize = 16.dp
private val trackHeight = 24.dp
private val trackWidth = 48.dp
private val rippleSize = 32.dp

@CombinedThemePreviews
@Composable
private fun MegaSwitchPreview(
    @PreviewParameter(BooleanProvider::class) initialChecked: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        var checked by remember {
            mutableStateOf(initialChecked)
        }
        MegaSwitch(checked) {
            checked = !checked
        }
    }
}

@CombinedThemePreviews
@Composable
private fun MegaSwitchDisabledPreview(
    @PreviewParameter(BooleanProvider::class) initialChecked: Boolean,
) {
    AndroidTheme(isDark = isSystemInDarkTheme()) {
        var checked by remember {
            mutableStateOf(initialChecked)
        }
        MegaSwitch(checked, enabled = false) {
            checked = !checked
        }
    }
}