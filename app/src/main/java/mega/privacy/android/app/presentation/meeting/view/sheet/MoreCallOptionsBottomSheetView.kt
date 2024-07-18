package mega.privacy.android.app.presentation.meeting.view.sheet

import mega.privacy.android.icon.pack.R as iconPackR
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.meeting.activity.MeetingActivityViewModel
import mega.privacy.android.app.meeting.fragments.InMeetingViewModel
import mega.privacy.android.app.presentation.meeting.model.InMeetingUiState
import mega.privacy.android.app.presentation.meeting.model.MeetingState
import mega.privacy.android.shared.original.core.ui.theme.black
import mega.privacy.android.domain.entity.call.ChatCall
import mega.privacy.android.domain.entity.call.CallOnHoldType

/**
 * Recurring Meeting Occurrence bottom sheet view
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun MoreCallOptionsBottomSheetView(
    inMeetingViewModel: InMeetingViewModel = hiltViewModel(),
    meetingViewModel: MeetingActivityViewModel = hiltViewModel(),
    scrimColor: Color = black.copy(alpha = 0.32f),
    sheetGesturesEnabled: Boolean = true,
    content: (@Composable () -> Unit)? = null,
) {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState =
        rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Expanded,
            confirmValueChange = {
                if (it == ModalBottomSheetValue.Hidden) {
                    inMeetingViewModel.moreCallOptionsBottomPanelDismiss()
                }
                true
            })
    val roundedCornerRadius = 12.dp

    val uiState by inMeetingViewModel.state.collectAsStateWithLifecycle()
    val meetingState by meetingViewModel.state.collectAsStateWithLifecycle()

    if (uiState.showCallOptionsBottomSheet) {
        ModalBottomSheetLayout(
            modifier = Modifier
                .navigationBarsPadding()
                .testTag(CALL_OPTIONS_BOTTOM_SHEET_CONTAINER),
            sheetState = modalSheetState,
            sheetShape = RoundedCornerShape(
                topStart = roundedCornerRadius,
                topEnd = roundedCornerRadius
            ),
            sheetGesturesEnabled = sheetGesturesEnabled,
            scrimColor = scrimColor,
            sheetContent = {
                BottomSheetContent(
                    modalSheetState = modalSheetState,
                    coroutineScope = coroutineScope,
                    uiState = uiState,
                    meetingState = meetingState,
                    onPutCallOnHold = inMeetingViewModel::onClickOnHold,
                    onRaiseHand = inMeetingViewModel::raiseHandToSpeak,
                    onLowerHand = inMeetingViewModel::lowerHandToStopSpeak
                )
            }
        ) {
            content?.invoke()
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun BottomSheetContent(
    modalSheetState: ModalBottomSheetState,
    coroutineScope: CoroutineScope,
    uiState: InMeetingUiState,
    meetingState: MeetingState,
    onPutCallOnHold: () -> Unit = {},
    onRaiseHand: () -> Unit = {},
    onLowerHand: () -> Unit = {},
) = with(uiState) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
    ) {
        ConstraintLayout(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {

            Column(modifier = Modifier.verticalScroll(rememberScrollState()))
            {
                if (isRaiseToSpeakFeatureFlagEnabled && !isOneToOneCall) {
                    BottomSheetMenuItemView(
                        modifier = Modifier.testTag(CALL_OPTIONS_BOTTOM_SHEET_RAISE_HAND_BUTTON),
                        res = R.drawable.raise_hand_icon,
                        text = when {
                            meetingState.isMyHandRaisedToSpeak -> R.string.meetings_lower_hand_option_button
                            else -> R.string.meetings_raise_hand_option_button
                        },
                        description = "Raise or lower hand",
                        tintRed = false,
                        onClick = {
                            coroutineScope.launch { modalSheetState.hide() }
                            when {
                                meetingState.isMyHandRaisedToSpeak -> onLowerHand()
                                else -> onRaiseHand()
                            }
                        }
                    )
                }

                BottomSheetMenuItemView(
                    modifier = Modifier.testTag(CALL_OPTIONS_BOTTOM_SHEET_PUT_CALL_ON_HOLD_BUTTON),
                    res = when (getButtonTypeToShow) {
                        CallOnHoldType.ResumeCall -> R.drawable.resume_call_icon
                        CallOnHoldType.PutCallOnHold -> iconPackR.drawable.putt_call_on_hold_icon
                        CallOnHoldType.SwapCalls -> iconPackR.drawable.ic_arrows_swap
                    },
                    text = when (getButtonTypeToShow) {
                        CallOnHoldType.ResumeCall -> R.string.meetings_resume_call_option_button
                        CallOnHoldType.PutCallOnHold -> R.string.meetings_put_call_on_hold_option_button
                        CallOnHoldType.SwapCalls -> R.string.meetings_swap_calls_option_button
                    },
                    description = "Contact info",
                    tintRed = false,
                    onClick = {
                        coroutineScope.launch { modalSheetState.hide() }
                        onPutCallOnHold()
                    })
            }
        }
    }
}

@OptIn(ExperimentalMaterialApi::class)
@Preview
@Composable
private fun MoreCallOptionsBottomSheetViewWithoutCallOnHoldPreview() {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Expanded,
        skipHalfExpanded = false,
    )
    BottomSheetContent(
        modalSheetState = modalSheetState,
        coroutineScope = coroutineScope,
        uiState = InMeetingUiState(
            showCallOptionsBottomSheet = true,
            isRaiseToSpeakFeatureFlagEnabled = false,
            anotherCall = null, call = ChatCall(
                chatId = 123L,
                callId = 456L,
                isOnHold = false
            )
        ),
        meetingState = MeetingState(),
        onPutCallOnHold = { },
        onRaiseHand = { },
        onLowerHand = {}
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Preview
@Composable
private fun MoreCallOptionsBottomSheetViewWithCallOnHoldPreview() {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Expanded,
        skipHalfExpanded = false,
    )
    BottomSheetContent(
        modalSheetState = modalSheetState,
        coroutineScope = coroutineScope,
        uiState = InMeetingUiState(
            showCallOptionsBottomSheet = true,
            isRaiseToSpeakFeatureFlagEnabled = false,
            anotherCall = null, call = ChatCall(
                chatId = 123L,
                callId = 456L,
                isOnHold = true
            )
        ),
        meetingState = MeetingState(),
        onPutCallOnHold = { },
        onRaiseHand = { },
        onLowerHand = {}
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Preview
@Composable
private fun MoreCallOptionsBottomSheetViewWithSwapCallsPreview() {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Expanded,
        skipHalfExpanded = false,
    )
    BottomSheetContent(
        modalSheetState = modalSheetState,
        coroutineScope = coroutineScope,
        uiState = InMeetingUiState(
            showCallOptionsBottomSheet = true,
            isRaiseToSpeakFeatureFlagEnabled = false,
            anotherCall = ChatCall(
                chatId = 123L,
                callId = 456L,
                isOnHold = false
            ), call = ChatCall(
                chatId = 123L,
                callId = 456L,
                isOnHold = true
            )
        ),
        meetingState = MeetingState(),
        onPutCallOnHold = { },
        onRaiseHand = { },
        onLowerHand = {}
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Preview
@Composable
private fun MoreCallOptionsBottomSheetViewWithRaiseHandPreview() {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Expanded,
        skipHalfExpanded = false,
    )
    BottomSheetContent(
        modalSheetState = modalSheetState,
        coroutineScope = coroutineScope,
        uiState = InMeetingUiState(
            showCallOptionsBottomSheet = true,
            isRaiseToSpeakFeatureFlagEnabled = true,
            anotherCall = ChatCall(
                chatId = 123L,
                callId = 456L,
                isOnHold = false
            ), call = ChatCall(
                chatId = 123L,
                callId = 456L,
                isOnHold = true
            )
        ),
        meetingState = MeetingState(),
        onPutCallOnHold = { },
        onRaiseHand = { },
        onLowerHand = {}
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Preview
@Composable
private fun MoreCallOptionsBottomSheetViewWithLowerHandPreview() {
    val coroutineScope = rememberCoroutineScope()
    val modalSheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Expanded,
        skipHalfExpanded = false,
    )
    BottomSheetContent(
        modalSheetState = modalSheetState,
        coroutineScope = coroutineScope,
        uiState = InMeetingUiState(
            showCallOptionsBottomSheet = true,
            isRaiseToSpeakFeatureFlagEnabled = true,
        ),
        meetingState = MeetingState(),
        onPutCallOnHold = { },
        onRaiseHand = { },
        onLowerHand = {}
    )
}

/**
 * Test Tags for the Call options Bottom Sheet
 */
internal const val CALL_OPTIONS_BOTTOM_SHEET_CONTAINER =
    "more_call_options_bottom_sheet:menu_action_bottom_sheet_container"
internal const val CALL_OPTIONS_BOTTOM_SHEET_RAISE_HAND_BUTTON =
    "more_call_options_bottom_sheet:menu_action_bottom_sheet_raise_hand_button"
internal const val CALL_OPTIONS_BOTTOM_SHEET_PUT_CALL_ON_HOLD_BUTTON =
    "more_call_options_bottom_sheet:menu_action_bottom_sheet_put_call_on_hold_button"


