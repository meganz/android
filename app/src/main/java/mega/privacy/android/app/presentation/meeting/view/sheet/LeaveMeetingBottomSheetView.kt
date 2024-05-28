package mega.privacy.android.app.presentation.meeting.view.sheet

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.MaterialTheme
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.Text
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import mega.privacy.android.app.R
import mega.privacy.android.app.presentation.meeting.model.InMeetingUiState
import mega.privacy.android.shared.original.core.ui.controls.buttons.OutlinedWithoutBackgroundMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultErrorMegaButton
import mega.privacy.android.shared.original.core.ui.controls.buttons.RaisedDefaultMegaButton
import mega.privacy.android.shared.original.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.shared.original.core.ui.theme.black
import mega.privacy.android.shared.original.core.ui.theme.extensions.textColorPrimary
import mega.privacy.android.shared.original.core.ui.theme.OriginalTempTheme

/**
 * Leave meeting bottom sheet view
 */
@OptIn(ExperimentalMaterialApi::class)
@Composable
internal fun LeaveMeetingBottomSheetView(
    state: InMeetingUiState,
    scrimColor: Color = black.copy(alpha = 0.32f),
    sheetGesturesEnabled: Boolean = true,
    content: (@Composable () -> Unit)? = null,
    onAssignAndLeaveClick: () -> Unit = {},
    onLeaveAnywayClick: () -> Unit = {},
    onEndForAllClick: () -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    if (state.showEndMeetingAsOnlyHostBottomPanel) {
        val coroutineScope = rememberCoroutineScope()
        val modalSheetState =
            rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Expanded,
                confirmValueChange = {
                    if (it == ModalBottomSheetValue.Hidden) {
                        onDismiss()
                    }
                    true
                })

        val roundedCornerRadius = 12.dp
        ModalBottomSheetLayout(
            modifier = Modifier
                .navigationBarsPadding()
                .testTag(BOTTOM_SHEET_CONTAINER),
            sheetShape = RoundedCornerShape(
                topStart = roundedCornerRadius,
                topEnd = roundedCornerRadius
            ),
            sheetState = modalSheetState,
            sheetGesturesEnabled = sheetGesturesEnabled,
            scrimColor = scrimColor,
            sheetContent = {
                Row(
                    modifier = Modifier
                        .testTag(BOTTOM_SHEET_HEADER)
                        .fillMaxWidth()
                        .height(IntrinsicSize.Min)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 24.dp, top = 20.dp, end = 24.dp)
                    ) {
                        Text(
                            text = stringResource(id = R.string.meeting_call_screen_title_of_bottom_panel_when_only_host_leave_the_meeting),
                            style = MaterialTheme.typography.h6.copy(color = MaterialTheme.colors.textColorPrimary),
                        )
                        Text(
                            modifier = Modifier.padding(top = 16.dp),
                            text = stringResource(id = R.string.meeting_call_screen_description_of_bottom_panel_when_only_host_leave_the_meeting),
                            style = MaterialTheme.typography.subtitle1,
                            color = MaterialTheme.colors.textColorPrimary,
                        )
                    }
                }

                Column(
                    modifier = Modifier
                        .padding(top = 24.dp)
                ) {
                    RaisedDefaultMegaButton(
                        modifier = Modifier
                            .padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                            .testTag(BOTTOM_SHEET_ASSIGN_AND_LEAVE_BUTTON)
                            .fillMaxWidth(),
                        textId = R.string.meeting_call_screen_bottom_panel_assign_and_leave_option,
                        onClick = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onAssignAndLeaveClick()
                        },
                        enabled = true
                    )

                    OutlinedWithoutBackgroundMegaButton(
                        modifier = Modifier
                            .padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                            .testTag(BOTTOM_SHEET_LEAVE_ANYWAY_BUTTON)
                            .fillMaxWidth(),
                        text = stringResource(id = R.string.meeting_call_screen_bottom_panel_leave_option),
                        onClick = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onLeaveAnywayClick()
                        },
                        rounded = false,
                        enabled = true,
                        iconId = null
                    )

                    RaisedDefaultErrorMegaButton(
                        modifier = Modifier
                            .padding(start = 24.dp, end = 24.dp, bottom = 8.dp)
                            .testTag(BOTTOM_SHEET_END_FOR_ALL_BUTTON)
                            .fillMaxWidth(),
                        textId = R.string.calls_call_screen_bottom_panel_end_call_for_all_option,
                        onClick = {
                            coroutineScope.launch { modalSheetState.hide() }
                            onEndForAllClick()
                        },
                        enabled = true
                    )
                }

            },
        ) {
            content?.invoke()
        }
    }
}

/**
 * Test Tags for the Leave Meeting Bottom Sheet
 */
internal const val BOTTOM_SHEET_CONTAINER =
    "leave_meeting_bottom_sheet:menu_action_bottom_sheet_container"

internal const val BOTTOM_SHEET_HEADER =
    "leave_meeting_bottom_sheet:menu_action_node_header"

internal const val BOTTOM_SHEET_LEAVE_ANYWAY_BUTTON =
    "leave_meeting_bottom_sheet:menu_action_node_leave_anyway_button"

internal const val BOTTOM_SHEET_ASSIGN_AND_LEAVE_BUTTON =
    "leave_meeting_bottom_sheet:menu_action_node_assign_and_leave_button"

internal const val BOTTOM_SHEET_END_FOR_ALL_BUTTON =
    "leave_meeting_bottom_sheet:menu_action_node_end_for_all_button"

/**
 * A Preview Composable that displays the Leave meeting Bottom Sheet with its Options
 */
@CombinedThemePreviews
@Composable
private fun PreviewLeaveMeetingBottomSheetView() {
    OriginalTempTheme(isDark = true) {
        LeaveMeetingBottomSheetView(
            state = InMeetingUiState(),
            onAssignAndLeaveClick = {},
            onLeaveAnywayClick = {},
            onEndForAllClick = {},
            onDismiss = {},
        )
    }
}