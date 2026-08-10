package mega.privacy.android.feature.documentscanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.theme.values.TextColor
import mega.android.core.ui.tokens.theme.DSTokens
import mega.privacy.android.shared.resources.R as sharedR

/**
 * Top chrome for the scanner: back (left) + the auto-capture toggle (right).
 *
 * The "switch to old scanner" fallback lives in [ScannerControlBar] (low
 * prominence), not here.
 *
 * @param isAutoOn Whether auto-capture is on; drives the AUTO toggle style.
 * @param onToggleAutoCapture Flip AUTO ↔ MANUAL.
 * @param onClose Close the scanner.
 */
@Composable
fun ScannerTopBar(
    isAutoOn: Boolean,
    onToggleAutoCapture: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.x8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        ScannerCloseButton(onClose = onClose)
        AutoCaptureToggle(isOn = isAutoOn, onToggle = onToggleAutoCapture)
    }
}

@Composable
private fun AutoCaptureToggle(
    isOn: Boolean,
    onToggle: () -> Unit,
) {
    val spacing = LocalSpacing.current
    val description = stringResource(
        if (isOn) sharedR.string.document_scanner_auto_capture_on
        else sharedR.string.document_scanner_auto_capture_off
    )
    val stateModifier = if (isOn) {
        Modifier.background(DSTokens.colors.background.blur)
    } else {
        Modifier.border(
            width = 1.dp,
            color = DSTokens.colors.icon.onColor.copy(alpha = 0.6f),
            shape = CircleShape,
        )
    }
    MegaText(
        text = stringResource(sharedR.string.document_scanner_auto_capture_label),
        textColor = TextColor.OnColor,
        style = AppTheme.typography.labelLarge,
        modifier = Modifier
            .clip(CircleShape)
            .then(stateModifier)
            .clickable(onClick = onToggle)
            .semantics { contentDescription = description }
            .padding(horizontal = spacing.x20, vertical = spacing.x8),
    )
}
