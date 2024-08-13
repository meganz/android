package mega.privacy.android.app.meeting.fragments

import mega.privacy.android.icon.pack.R as IconR
import mega.privacy.android.shared.resources.R as SharedR
import android.content.Context
import android.util.AttributeSet
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.AbstractComposeView
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.core.content.ContextCompat
import mega.privacy.android.app.R
import mega.privacy.android.app.main.megachat.AppRTCAudioManager
import mega.privacy.android.app.meeting.fragments.MeetingActionButtonsTestTags.CAMERA_BUTTON
import mega.privacy.android.app.meeting.fragments.MeetingActionButtonsTestTags.END_CALL_BUTTON
import mega.privacy.android.app.meeting.fragments.MeetingActionButtonsTestTags.MIC_BUTTON
import mega.privacy.android.app.meeting.fragments.MeetingActionButtonsTestTags.MORE_BUTTON
import mega.privacy.android.app.meeting.fragments.MeetingActionButtonsTestTags.SPEAKER_BUTTON
import mega.privacy.android.app.meeting.fragments.MeetingActionButtonsTestTags.TOOLTIP
import mega.privacy.android.app.meeting.fragments.fab.OnOffFab
import mega.privacy.android.domain.entity.call.AudioDevice
import mega.privacy.android.legacy.core.ui.controls.tooltips.LegacyMegaTooltip
import mega.privacy.android.shared.original.core.ui.controls.text.MegaText
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme
import mega.privacy.android.shared.original.core.ui.theme.values.TextColor
import kotlin.random.Random


/**
 * MeetingActionButtonsView is a view that contains the buttons for the meeting.
 */
internal class MeetingsActionButtonsView : AbstractComposeView {

    var showMicWarning by mutableStateOf(false)

    var showCameraWarning by mutableStateOf(false)

    var isMicOn by mutableStateOf(false)

    var isCameraOn by mutableStateOf(false)
    var isSpeakerOn by mutableStateOf(false)
    private var isMoreOn by mutableStateOf(true)
    var buttonsEnabled by mutableStateOf(true)
    var isRaiseHandToolTipShown by mutableStateOf(false)
    var currentAudioDevice by mutableStateOf(AudioDevice.None)

    var onRaiseToRandTooltipDismissed by mutableStateOf<(() -> Unit)?>(null)

    var onMicClicked by mutableStateOf<((Boolean) -> Unit)?>(null)

    /**
     *   Callback for the camera button.
     */
    var onCamClicked by mutableStateOf<((Boolean) -> Unit)?>(null)

    /**
     * Callback for the speaker button.
     */
    var onSpeakerClicked by mutableStateOf<((Boolean) -> Unit)?>(null)

    /**
     * Callback for the hold button.
     */
    var onMoreClicked by mutableStateOf<((Boolean) -> Unit)?>(null)

    /**
     * Callback for the end call button.
     */
    var onEndClicked by mutableStateOf<((Boolean) -> Unit)?>(null)

    var backgroundTintAlpha by mutableFloatStateOf(0.0F)

    private var tooltipKey by mutableIntStateOf(Random.nextInt())

    fun setNewTooltipKey() {
        // set a new key for updating the position of the tooltip
        tooltipKey = Random.nextInt()
    }

    constructor(context: Context) : this(context, null)
    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)
    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) : super(
        context,
        attrs,
        defStyleAttr
    )

    @Composable
    override fun Content() {
        OriginalTempTheme(
            isDark = (isSystemInDarkTheme().not() && backgroundTintAlpha < 0.2F) || isSystemInDarkTheme()
        ) {
            MeetingsActionButtons(
                onMicClicked = onMicClicked,
                onCamClicked = onCamClicked,
                onSpeakerClicked = onSpeakerClicked,
                onMoreClicked = onMoreClicked,
                onEndClicked = onEndClicked,
                micEnabled = isMicOn,
                cameraEnabled = isCameraOn,
                speakerEnabled = isSpeakerOn,
                moreEnabled = isMoreOn,
                showMicWarning = showMicWarning,
                showCameraWarning = showCameraWarning,
                buttonsEnabled = buttonsEnabled,
                backgroundTintAlpha = backgroundTintAlpha,
                isRaiseHandToolTipShown = isRaiseHandToolTipShown,
                onRaiseToRandTooltipDismissed = onRaiseToRandTooltipDismissed,
                currentAudioDevice = currentAudioDevice,
                tooltipKey = tooltipKey
            )
        }
    }
}

/**
 * MeetingActionButtons  contains the buttons for the meeting.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun MeetingsActionButtons(
    onMicClicked: ((Boolean) -> Unit)?,
    onCamClicked: ((Boolean) -> Unit)?,
    onSpeakerClicked: ((Boolean) -> Unit)?,
    onMoreClicked: ((Boolean) -> Unit)?,
    onEndClicked: ((Boolean) -> Unit)?,
    onRaiseToRandTooltipDismissed: (() -> Unit)?,
    micEnabled: Boolean,
    cameraEnabled: Boolean,
    speakerEnabled: Boolean,
    moreEnabled: Boolean,
    showMicWarning: Boolean,
    showCameraWarning: Boolean,
    buttonsEnabled: Boolean,
    backgroundTintAlpha: Float,
    isRaiseHandToolTipShown: Boolean,
    tooltipKey: Int,
    currentAudioDevice: AudioDevice,
    modifier: Modifier = Modifier,
) {

    val context = LocalContext.current
    val textStyle = MaterialTheme.typography.caption.copy(fontSize = 10.sp)
    val backgroundTint by remember(backgroundTintAlpha) {
        derivedStateOf {
            val colorStart = Color(ContextCompat.getColor(context, R.color.grey_032_white_054))
            val colorEnd = Color(ContextCompat.getColor(context, R.color.grey_060_white_054))
            lerp(colorStart, colorEnd, backgroundTintAlpha)
        }
    }

    val isEnabled by rememberSaveable(buttonsEnabled) {
        mutableStateOf(buttonsEnabled)
    }
    Box(
        modifier = modifier
            .semantics { testTagsAsResourceId = true }
            .padding(horizontal = 24.dp)
            .padding(top = 8.dp)
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {

                // Microphone button
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ConstraintLayout {
                        val (fab, warning) = createRefs()
                        val isOn = rememberSaveable(micEnabled) { mutableStateOf(micEnabled) }

                        OnOffFab(
                            modifier = Modifier
                                .constrainAs(fab) {
                                    top.linkTo(parent.top)
                                    start.linkTo(parent.start)
                                }
                                .testTag(MIC_BUTTON),
                            isOn = isOn,
                            enabled = isEnabled,
                            onIcon = IconR.drawable.ic_mic,
                            offIcon = IconR.drawable.ic_mic_stop,
                            disableIcon = IconR.drawable.ic_mic_stop,
                            onBackgroundTint = backgroundTint,
                            onOff = onMicClicked
                        )
                        val showWarning by rememberSaveable(showMicWarning) {
                            mutableStateOf(
                                showMicWarning
                            )
                        }
                        if (showWarning) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_permission_warning),
                                contentDescription = null,
                                modifier = Modifier
                                    .constrainAs(warning) {
                                        top.linkTo(parent.top)
                                        end.linkTo(parent.end)
                                    }
                                    .clip(CircleShape)
                                    .shadow(3.dp)
                            )
                        }
                    }

                    MegaText(
                        text = stringResource(id = R.string.general_mic),
                        textColor = TextColor.Primary,
                        style = textStyle
                    )
                }

                // Camera button
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    ConstraintLayout {
                        val (fab, warning) = createRefs()
                        val isOn = rememberSaveable(cameraEnabled) { mutableStateOf(cameraEnabled) }
                        OnOffFab(
                            modifier = Modifier
                                .constrainAs(fab) {
                                    top.linkTo(parent.top)
                                    start.linkTo(parent.start)
                                }
                                .testTag(CAMERA_BUTTON),
                            isOn = isOn,
                            enabled = isEnabled,
                            onIcon = IconR.drawable.ic_video_on,
                            offIcon = IconR.drawable.ic_video_off,
                            disableIcon = IconR.drawable.ic_video_off,
                            onBackgroundTint = backgroundTint,
                            onOff = onCamClicked
                        )
                        val showWarning by rememberSaveable(showCameraWarning) {
                            mutableStateOf(
                                showCameraWarning
                            )
                        }
                        if (showWarning) {
                            Image(
                                painter = painterResource(id = R.drawable.ic_permission_warning),
                                contentDescription = null,
                                modifier = Modifier
                                    .constrainAs(warning) {
                                        top.linkTo(parent.top)
                                        end.linkTo(parent.end)
                                    }
                                    .clip(CircleShape)
                                    .shadow(3.dp)
                            )
                        }
                    }

                    MegaText(
                        text = stringResource(id = R.string.general_camera),
                        textColor = TextColor.Primary,
                        style = textStyle
                    )
                }

                // Speaker button
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isOn = rememberSaveable(speakerEnabled) { mutableStateOf(speakerEnabled) }
                    val (onIcon, stringId) =
                        if (currentAudioDevice == AudioDevice.WiredHeadset ||
                            currentAudioDevice == AudioDevice.Bluetooth
                        ) Pair(
                            R.drawable.ic_headphone,
                            R.string.general_headphone
                        ) else Pair(IconR.drawable.ic_volume_max, R.string.general_speaker)
                    OnOffFab(
                        modifier = Modifier.testTag(SPEAKER_BUTTON),
                        isOn = isOn,
                        enabled = if (currentAudioDevice == AudioDevice.None) false else isEnabled,
                        onIcon = onIcon,
                        offIcon = IconR.drawable.ic_volume_off,
                        disableIcon = IconR.drawable.ic_volume_off,
                        onBackgroundTint = backgroundTint,
                        onOff = onSpeakerClicked
                    )

                    MegaText(
                        text = stringResource(id = stringId),
                        textColor = TextColor.Primary,
                        style = textStyle
                    )
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isOn = rememberSaveable(moreEnabled) { mutableStateOf(moreEnabled) }
                    if (isRaiseHandToolTipShown || backgroundTintAlpha == 1.0F) {
                        OnOffFab(
                            modifier = Modifier.testTag(MORE_BUTTON),
                            isOn = isOn,
                            enabled = false,
                            onIcon = R.drawable.more_call_options_icon,
                            offIcon = R.drawable.more_call_options_icon,
                            disableIcon = R.drawable.more_call_options_icon,
                            onBackgroundTint = backgroundTint,
                            offBackgroundTint = backgroundTint,
                            onIconTint = Color.White,
                            offIconTint = Color.White,
                            disabledIconTint = Color.White,
                            onOff = onMoreClicked
                        )
                    } else {
                        LegacyMegaTooltip(
                            key = tooltipKey,
                            modifier = Modifier.testTag(TOOLTIP),
                            descriptionText = stringResource(id = SharedR.string.meetings_raised_hand_tooltip_title),
                            actionText = stringResource(R.string.button_permission_info),
                            showOnTop = true,
                            setDismissWhenTouchOutside = true,
                            content = {
                                OnOffFab(
                                    modifier = Modifier.testTag(MORE_BUTTON),
                                    isOn = isOn,
                                    enabled = false,
                                    onIcon = R.drawable.more_call_options_icon,
                                    offIcon = R.drawable.more_call_options_icon,
                                    disableIcon = R.drawable.more_call_options_icon,
                                    onBackgroundTint = backgroundTint,
                                    offBackgroundTint = backgroundTint,
                                    onIconTint = Color.White,
                                    offIconTint = Color.White,
                                    onOff = {
                                        onMoreClicked?.invoke(it)
                                        onRaiseToRandTooltipDismissed?.invoke()
                                    }
                                )
                            },
                            onDismissed = {
                                onRaiseToRandTooltipDismissed?.invoke()
                            }
                        )
                    }

                    MegaText(
                        text = stringResource(id = R.string.meetings_more_call_option_button),
                        textColor = TextColor.Primary,
                        style = textStyle
                    )
                }

                // End Call button
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isOn = rememberSaveable { mutableStateOf(true) }
                    OnOffFab(
                        modifier = Modifier.testTag(END_CALL_BUTTON),
                        isOn = isOn,
                        enabled = true,
                        onIcon = IconR.drawable.hang_call_icon,
                        offIcon = IconR.drawable.hang_call_icon,
                        disableIcon = IconR.drawable.hang_call_icon,
                        onBackgroundTint = Color(
                            ContextCompat.getColor(
                                context,
                                R.color.color_primary_400
                            )
                        ),
                        offBackgroundTint = Color(
                            ContextCompat.getColor(
                                context,
                                R.color.color_primary_400
                            )
                        ),
                        onIconTint = Color.White,
                        offIconTint = Color.White,
                        onOff = onEndClicked
                    )
                    MegaText(
                        text = stringResource(id = R.string.meeting_end),
                        textColor = TextColor.Primary,
                        style = textStyle
                    )
                }
            }
        }
    }
}


@CombinedThemePreviews
@Composable
private fun MeetingBottomFloatingPanelPreview() {
    OriginalTempTheme(isDark = isSystemInDarkTheme()) {
        MeetingsActionButtons(
            modifier = Modifier.padding(vertical = 8.dp),
            onMicClicked = {},
            onCamClicked = {},
            onSpeakerClicked = {},
            onMoreClicked = {},
            onEndClicked = {},
            micEnabled = true,
            cameraEnabled = true,
            speakerEnabled = true,
            moreEnabled = true,
            showMicWarning = false,
            buttonsEnabled = true,
            showCameraWarning = false,
            backgroundTintAlpha = 1.0F,
            isRaiseHandToolTipShown = false,
            onRaiseToRandTooltipDismissed = {},
            currentAudioDevice = AudioDevice.SpeakerPhone,
            tooltipKey = 0
        )
    }
}

internal object MeetingActionButtonsTestTags {
    private const val MEETING_ACTION_BUTTONS = "meeting_action_buttons"
    const val MIC_BUTTON = "$MEETING_ACTION_BUTTONS:mic"
    const val CAMERA_BUTTON = "$MEETING_ACTION_BUTTONS:camera"
    const val SPEAKER_BUTTON = "$MEETING_ACTION_BUTTONS:speaker"
    const val MORE_BUTTON = "$MEETING_ACTION_BUTTONS:more"
    const val TOOLTIP = "$MEETING_ACTION_BUTTONS:tooltip"
    const val END_CALL_BUTTON = "$MEETING_ACTION_BUTTONS:end_call"
}
