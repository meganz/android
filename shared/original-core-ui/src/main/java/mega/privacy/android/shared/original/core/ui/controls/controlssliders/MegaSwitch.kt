package mega.privacy.android.shared.original.core.ui.controls.controlssliders

import android.content.Context
import android.util.AttributeSet
import android.view.View
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Icon
import androidx.compose.material.minimumInteractiveComponentSize
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.core.content.withStyledAttributes
import mega.privacy.android.core.R
import mega.privacy.android.shared.original.core.ui.preview.BooleanProvider
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.MegaOriginalTheme
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.PreviewWithTempAndNewCoreColorTokens


/**
 * Custom implementation of Switch View following our design
 * @property checked
 * @property enabled
 * * @property clickable
 * @property onCheckedChange
 * @property onClick
 */
class MegaSwitch : AbstractComposeView, Checkable {
    @get:JvmName("isCheckedKt")
    @set:JvmName("setCheckedKt")
    var checked by mutableStateOf(false)

    @get:JvmName("isEnabledKt")
    @set:JvmName("setEnabledKt")
    var enabled by mutableStateOf(true)

    @get:JvmName("isClickableKt")
    @set:JvmName("setClickableKt")
    var clickable by mutableStateOf(true)
    var onCheckedChange by mutableStateOf<((MegaSwitch, Boolean) -> Unit)?>(null)
    var onClick by mutableStateOf<(() -> Unit)?>(null)

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
        OriginalTempTheme(isDark = isSystemInDarkTheme()) {
            MegaSwitch(
                checked = checked,
                onCheckedChange = if (clickable) {
                    {
                        val changed = checked != it
                        checked = it
                        onCheckedChange?.takeIf { changed }?.invoke(this, it)
                    }
                } else null,
                onClickListener = onClick?.takeIf { clickable }
            )
        }
    }

    override fun setChecked(checked: Boolean) {
        val changed = checked != this.checked
        this.checked = checked
        onCheckedChange?.takeIf { changed }?.invoke(this, checked)
    }

    override fun isChecked() = checked

    override fun toggle() {
        checked = !checked
        onCheckedChange?.invoke(this, checked)
    }

    fun setOnCheckedChangeListener(listener: ((MegaSwitch, Boolean) -> Unit)?) {
        onCheckedChange = listener
    }

    fun setOnClickListener(listener: (View) -> Unit) {
        super.setOnClickListener(listener)
        this.onClick = { listener(this) }
    }

    override fun setClickable(clickable: Boolean) {
        super.setClickable(clickable)
        this.clickable = clickable
    }
}

/**
 * Custom implementation of Switch component following our design
 * @param checked
 * @param modifier [Modifier]
 * @param enabled
 * @param onCheckedChange
 */
@Composable
fun MegaSwitch(
    checked: Boolean,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onClickListener: (() -> Unit)? = null,
    onCheckedChange: ((Boolean) -> Unit)?,
) {
    val isPressed by interactionSource.collectIsPressedAsState()

    val thumbOffsetRaw = remember(checked) {
        val offset = (trackWidth - trackHeight) / 2
        if (checked) offset else -offset
    }

    val thumbOffset by animateDpAsState(targetValue = thumbOffsetRaw, label = "thumb Offset")

    val trackAnimationSpecs = tween<Color>()
    val thumbAnimationSpecs = tween<Color>(easing = LinearOutSlowInEasing)

    val trackColor by animateColorAsState(
        targetValue = getColorForTrack(checked = checked, pressed = isPressed, enabled = enabled),
        animationSpec = trackAnimationSpecs,
        label = "track color"
    )
    val borderColor by animateColorAsState(
        targetValue = getColorForBorder(pressed = isPressed, enabled = enabled),
        animationSpec = trackAnimationSpecs,
        label = "border color"
    )
    val thumbColor by animateColorAsState(
        targetValue = getColorForThumb(checked = checked, pressed = isPressed, enabled = enabled),
        animationSpec = thumbAnimationSpecs,
        label = "thumb color"
    )
    Box(
        modifier = modifier
            .minimumInteractiveComponentSize()
            .then(
                if (onCheckedChange != null) {
                    Modifier.toggleable(
                        value = checked,
                        onValueChange = {
                            onCheckedChange(it)
                            onClickListener?.invoke()
                        },
                        enabled = enabled,
                        role = Role.Switch,
                        interactionSource = interactionSource,
                        indication = null
                    )
                } else if (onClickListener != null) {
                    Modifier.clickable(
                        enabled = enabled,
                        role = Role.Switch,
                        interactionSource = interactionSource,
                        onClick = onClickListener,
                        indication = null
                    )
                } else Modifier
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
                        modifier = Modifier.testTag(CHECKED_TAG),
                        painter = painterResource(id = R.drawable.checked),
                        tint = trackColor,
                        contentDescription = "Checked",
                    )
                } else {
                    Spacer(
                        Modifier
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
internal const val CHECKED_TAG = "mega_switch:icon_checked"

@Composable
private fun getColorForThumb(checked: Boolean, pressed: Boolean, enabled: Boolean) =
    if (enabled) {
        when {
            !checked && !pressed -> MegaOriginalTheme.colors.components.selectionControl
            !checked && pressed -> MegaOriginalTheme.colors.button.outlinePressed
            else /*checked*/ -> MegaOriginalTheme.colors.background.surface1
        }
    } else {
        if (checked) MegaOriginalTheme.colors.background.pageBackground else MegaOriginalTheme.colors.border.disabled
    }

@Composable
private fun getColorForTrack(checked: Boolean, pressed: Boolean, enabled: Boolean) =
    getColorForThumb(
        checked = !checked,
        pressed = pressed,
        enabled = enabled
    )

@Composable
private fun getColorForBorder(pressed: Boolean, enabled: Boolean) =
    getColorForThumb(
        checked = false,
        pressed = pressed,
        enabled = enabled
    )

@CombinedThemePreviews
@Composable
private fun MegaSwitchPreview(
    @PreviewParameter(BooleanProvider::class) initialChecked: Boolean,
) {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
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
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        var checked by remember {
            mutableStateOf(initialChecked)
        }
        MegaSwitch(checked, enabled = false) {
            checked = !checked
        }
    }
}

@CombinedThemePreviews
@Composable
private fun MegaSwitchTempAndNewPreview(
    @PreviewParameter(BooleanProvider::class) initialChecked: Boolean,
) {
    PreviewWithTempAndNewCoreColorTokens(isDark = isSystemInDarkTheme()) {
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
private fun MegaSwitchDisabledTempAndNewPreview(
    @PreviewParameter(BooleanProvider::class) initialChecked: Boolean,
) {
    PreviewWithTempAndNewCoreColorTokens(isDark = isSystemInDarkTheme()) {
        var checked by remember {
            mutableStateOf(initialChecked)
        }
        MegaSwitch(checked, enabled = false) {
            checked = !checked
        }
    }
}