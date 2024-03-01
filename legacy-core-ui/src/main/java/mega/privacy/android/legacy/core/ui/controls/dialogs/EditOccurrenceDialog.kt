package mega.privacy.android.legacy.core.ui.controls.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.AlertDialog
import androidx.compose.material.LocalAbsoluteElevation
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.DialogProperties
import mega.privacy.android.shared.theme.MegaAppTheme
import mega.privacy.android.core.ui.controls.buttons.TextMegaButton
import mega.privacy.android.core.ui.controls.text.MegaSpannedText
import mega.privacy.android.core.ui.model.MegaSpanStyle
import mega.privacy.android.core.ui.model.SpanIndicator
import mega.privacy.android.core.ui.preview.CombinedThemePreviews
import mega.privacy.android.core.ui.theme.extensions.textColorSecondary
import mega.privacy.android.core.ui.theme.tokens.TextColor
import mega.privacy.android.core.ui.utils.composeLet

/**
 * Alert dialog to edit a Scheduled meeting occurrence
 */
@Composable
fun EditOccurrenceDialog(
    title: String,
    confirmButtonText: String,
    dateTitleText: String,
    dateText: String,
    startTimeTitleText: String,
    startTimeText: String,
    endTimeTitleText: String,
    endTimeText: String,
    freePlanLimitationWarningText: String,
    cancelButtonText: String?,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onDateTap: () -> Unit,
    onStartTimeTap: () -> Unit,
    onEndTimeTap: () -> Unit,
    onUpgradeNowClicked: () -> Unit,
    modifier: Modifier = Modifier,
    shouldShowFreePlanLimitWarning: Boolean = false,
    isConfirmButtonEnabled: Boolean = true,
    isDateEdited: Boolean = false,
    isStartTimeEdited: Boolean = false,
    isEndTimeEdited: Boolean = false,
    dismissOnClickOutside: Boolean = true,
    dismissOnBackPress: Boolean = true,
) {
    CompositionLocalProvider(LocalAbsoluteElevation provides 24.dp) {
        AlertDialog(
            title = {
                Text(
                    text = title,
                    style = MaterialTheme.typography.subtitle1.copy(
                        color = MaterialTheme.colors.onSurface,
                        fontWeight = FontWeight.Medium
                    )
                )
            },
            text = {
                Column {
                    Row(
                        modifier = modifier
                            .padding(bottom = 36.dp)
                            .fillMaxWidth()
                    ) {
                        Column {
                            Text(
                                modifier = modifier.padding(bottom = 6.dp),
                                text = dateTitleText,
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.secondary,
                            )
                            ClickableText(
                                text = AnnotatedString(dateText),
                                style = MaterialTheme.typography.subtitle1.copy(
                                    color = if (isDateEdited) MaterialTheme.colors.onSurface else MaterialTheme.colors.textColorSecondary,
                                ),
                                onClick = { onDateTap() }
                            )
                        }
                    }

                    Row(
                        modifier = modifier
                            .height(72.dp)
                            .fillMaxWidth()
                    ) {
                        Column(
                            modifier = modifier.weight(1f),
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                modifier = modifier
                                    .padding(bottom = 6.dp, top = 6.dp),
                                text = startTimeTitleText,
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.secondary,
                            )
                            ClickableText(
                                modifier = modifier
                                    .padding(bottom = 8.dp),
                                text = AnnotatedString(startTimeText),
                                style = MaterialTheme.typography.subtitle1.copy(
                                    color = if (isStartTimeEdited) MaterialTheme.colors.onSurface else MaterialTheme.colors.textColorSecondary,
                                ),
                                onClick = { onStartTimeTap() }
                            )
                        }
                        Column(
                            horizontalAlignment = Alignment.Start,
                            modifier = modifier
                                .wrapContentSize(Alignment.CenterEnd)
                                .padding(end = 40.dp)
                        ) {
                            Text(
                                modifier = modifier
                                    .padding(bottom = 6.dp, top = 6.dp),
                                text = endTimeTitleText,
                                style = MaterialTheme.typography.caption,
                                color = MaterialTheme.colors.secondary,
                            )
                            ClickableText(
                                modifier = modifier
                                    .padding(bottom = 8.dp),
                                text = AnnotatedString(endTimeText),
                                style = MaterialTheme.typography.subtitle1.copy(
                                    color = if (isEndTimeEdited) MaterialTheme.colors.onSurface else MaterialTheme.colors.textColorSecondary,
                                ),
                                onClick = { onEndTimeTap() }
                            )
                        }
                    }

                    if (shouldShowFreePlanLimitWarning) {
                        Row(
                            modifier = Modifier
                                .clickable { onUpgradeNowClicked() }
                                .height(72.dp)
                                .testTag(EDIT_OCCURRENCE_DIALOG_WARNING_TAG)
                                .fillMaxWidth()
                        ) {
                            MegaSpannedText(
                                value = freePlanLimitationWarningText,
                                baseStyle = MaterialTheme.typography.body2,
                                styles = mapOf(
                                    SpanIndicator('A') to MegaSpanStyle(
                                        SpanStyle(
                                            textDecoration = TextDecoration.Underline,
                                        )
                                    ),
                                ),
                                color = TextColor.Primary
                            )
                        }
                    }
                }
            },
            onDismissRequest = onDismiss,
            confirmButton = {
                TextMegaButton(
                    text = confirmButtonText,
                    enabled = isConfirmButtonEnabled,
                    onClick = onConfirm,
                )
            },
            dismissButton = cancelButtonText?.composeLet {
                TextMegaButton(
                    text = cancelButtonText,
                    onClick = onDismiss,
                )
            },
            properties = DialogProperties(
                dismissOnBackPress = dismissOnBackPress,
                dismissOnClickOutside = dismissOnClickOutside,
            ),
        )
    }
}

internal const val EDIT_OCCURRENCE_DIALOG_WARNING_TAG =
    "edit_occurrence_dialog:free_plan_limit_warning"

@CombinedThemePreviews
@Composable
private fun EditOccurrenceDialogPreview() {
    MegaAppTheme(isDark = isSystemInDarkTheme()) {
        Box(
            modifier = Modifier.padding(horizontal = 240.dp, vertical = 120.dp),
            content = {
                EditOccurrenceDialog(
                    cancelButtonText = "Cancel",
                    title = "Edit occurrence",
                    confirmButtonText = "Update",
                    dateTitleText = "Date",
                    dateText = "Monday, 5 June",
                    startTimeTitleText = "Start time",
                    startTimeText = "10:00",
                    endTimeTitleText = "End time",
                    endTimeText = "11:00",
                    freePlanLimitationWarningText = "Plans limitation",
                    onConfirm = {},
                    onDismiss = {},
                    onDateTap = {},
                    onStartTimeTap = {},
                    onEndTimeTap = {},
                    onUpgradeNowClicked = {}
                )
            },
        )
    }
}