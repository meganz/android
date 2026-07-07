package mega.privacy.android.feature.documentscanner.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
 * Bottom chrome for the scanner: the manual shutter, with the low-prominence
 * "switch to old scanner" fallback button below it.
 *
 * @param onManualShutter Invoked when the shutter is tapped. The capture pipeline
 *   (frame grab + feedback) is wired by the caller.
 * @param onSwitchToLegacy Route to the legacy ML Kit scanner (fallback).
 */
@Composable
fun ScannerControlBar(
    onManualShutter: () -> Unit,
    onSwitchToLegacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = spacing.x48, bottom = spacing.x24),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        ShutterButton(onClick = onManualShutter)
        LegacyScannerButton(
            onClick = onSwitchToLegacy,
            modifier = Modifier.padding(top = spacing.x16),
        )
    }
}

@Composable
private fun ShutterButton(onClick: () -> Unit) {
    val description = stringResource(sharedR.string.document_scanner_capture_button)
    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .border(width = 4.dp, color = DSTokens.colors.icon.onColor, shape = CircleShape)
            .clickable(onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(DSTokens.colors.icon.onColor),
        )
    }
}

/** Low-prominence fallback: plain on-camera-white text; legible on the black
 *  bottom panel without needing an outline. */
@Composable
private fun LegacyScannerButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    MegaText(
        text = stringResource(sharedR.string.document_scanner_download_dialog_use_old_scanner_button),
        textColor = TextColor.OnColor,
        style = AppTheme.typography.labelLarge,
        modifier = modifier
            .clickable(onClick = onClick)
            .padding(horizontal = spacing.x16, vertical = spacing.x8),
    )
}
