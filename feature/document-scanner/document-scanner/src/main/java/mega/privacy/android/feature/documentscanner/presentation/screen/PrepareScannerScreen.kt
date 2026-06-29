package mega.privacy.android.feature.documentscanner.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import de.palm.composestateevents.EventEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.button.PrimaryFilledButton
import mega.android.core.ui.components.button.TextOnlyButton
import mega.android.core.ui.components.indicators.InfiniteProgressBarIndicator
import mega.android.core.ui.components.indicators.MegaAnimatedLinearProgressIndicator
import mega.android.core.ui.components.state.EmptyStateView
import mega.android.core.ui.components.text.SpannableText
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.spacing.LocalSpacing
import mega.android.core.ui.tokens.theme.DSTokens
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.documentscanner.domain.model.ScannerModelDownloadState
import mega.privacy.android.feature.documentscanner.presentation.PrepareScannerViewModel
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.shared.resources.R as sharedR
import kotlin.math.roundToInt

/**
 * Loading screen shown after the user confirms the one-time scanner-model download.
 *
 * It observes the background download and renders its progress. On success it
 * auto-navigates to the camera via [onModelReady]. On a recoverable failure it
 * offers a retry; on a permanent failure it only offers the legacy scanner.
 * "Use old scanner" ([onUseLegacy]) switches to the legacy ML Kit scanner; the
 * download keeps running in the background.
 *
 * @param onModelReady navigate to the camera once the model is downloaded.
 * @param onUseLegacy fall back to the legacy ML Kit scanner.
 */
@Composable
internal fun PrepareScannerScreen(
    onModelReady: () -> Unit,
    onUseLegacy: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PrepareScannerViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    PrepareScannerContent(
        downloadState = uiState.downloadState,
        onRetry = viewModel::onRetryDownload,
        onUseLegacy = onUseLegacy,
        modifier = modifier,
    )

    EventEffect(
        event = uiState.modelReadyEvent,
        onConsumed = viewModel::onModelReadyConsumed,
        action = onModelReady,
    )
}

@Composable
private fun PrepareScannerContent(
    downloadState: ScannerModelDownloadState,
    onRetry: () -> Unit,
    onUseLegacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(DSTokens.colors.background.pageBackground),
    ) {
        when (downloadState) {
            is ScannerModelDownloadState.Failed -> FailedContent(
                permanent = downloadState.permanent,
                onRetry = onRetry,
                onUseLegacy = onUseLegacy,
                modifier = Modifier.align(Alignment.Center),
            )

            else -> DownloadingContent(
                downloadState = downloadState,
                onUseLegacy = onUseLegacy,
                modifier = Modifier.align(Alignment.Center),
            )
        }
    }
}

@Composable
private fun DownloadingContent(
    downloadState: ScannerModelDownloadState,
    onUseLegacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val progress = (downloadState as? ScannerModelDownloadState.Downloading)?.progress

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = spacing.x24),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        MegaText(
            text = stringResource(sharedR.string.document_scanner_prepare_title),
            textColor = TextColor.Primary,
            style = AppTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(PREPARE_SCANNER_TITLE_TAG),
        )
        if (progress != null) {
            // Known progress: determinate bar + percentage.
            MegaAnimatedLinearProgressIndicator(
                indicatorProgress = progress,
                modifier = Modifier
                    .padding(top = spacing.x24)
                    .widthIn(max = 300.dp)
                    .testTag(PREPARE_SCANNER_PROGRESS_BAR_TAG),
            )
            MegaText(
                text = "${(progress * 100).roundToInt()}%",
                textColor = TextColor.Secondary,
                style = AppTheme.typography.bodySmall,
                modifier = Modifier
                    .padding(top = spacing.x8)
                    .testTag(PREPARE_SCANNER_PERCENTAGE_TAG),
            )
        } else {
            // No byte progress yet: indeterminate bar (same visual language).
            InfiniteProgressBarIndicator(
                modifier = Modifier
                    .padding(top = spacing.x24)
                    .widthIn(max = 300.dp)
                    .testTag(PREPARE_SCANNER_PROGRESS_BAR_TAG),
            )
        }
        MegaText(
            text = stringResource(sharedR.string.document_scanner_prepare_message),
            textColor = TextColor.Secondary,
            style = AppTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = spacing.x24)
                .testTag(PREPARE_SCANNER_MESSAGE_TAG),
        )
        TextOnlyButton(
            text = stringResource(sharedR.string.document_scanner_download_dialog_use_old_scanner_button),
            onClick = onUseLegacy,
            modifier = Modifier
                .padding(top = spacing.x16)
                .testTag(PREPARE_SCANNER_USE_OLD_SCANNER_BUTTON_TAG),
        )
    }
}

@Composable
private fun FailedContent(
    permanent: Boolean,
    onRetry: () -> Unit,
    onUseLegacy: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val useOldScannerText =
        stringResource(sharedR.string.document_scanner_download_dialog_use_old_scanner_button)
    EmptyStateView(
        modifier = modifier.testTag(PREPARE_SCANNER_ERROR_STATE_TAG),
        imagePainter = painterResource(iconPackR.drawable.ic_no_cloud),
        title = stringResource(sharedR.string.document_scanner_prepare_error_title),
        description = SpannableText(
            stringResource(
                if (permanent) {
                    sharedR.string.document_scanner_prepare_error_permanent_message
                } else {
                    sharedR.string.document_scanner_prepare_error_transient_message
                }
            )
        ),
        // A permanent failure (e.g. the model URL is gone) won't recover on retry, so
        // the only action there is the legacy scanner.
        primaryAction = if (permanent) {
            { LegacyButton(text = useOldScannerText, onClick = onUseLegacy) }
        } else {
            {
                PrimaryFilledButton(
                    text = stringResource(sharedR.string.document_scanner_prepare_retry_button),
                    onClick = onRetry,
                    modifier = Modifier.testTag(PREPARE_SCANNER_RETRY_BUTTON_TAG),
                )
            }
        },
        secondaryAction = if (permanent) {
            null
        } else {
            { LegacyButton(text = useOldScannerText, onClick = onUseLegacy) }
        },
    )
}

@Composable
private fun LegacyButton(text: String, onClick: () -> Unit) {
    TextOnlyButton(
        text = text,
        onClick = onClick,
        modifier = Modifier.testTag(PREPARE_SCANNER_USE_OLD_SCANNER_BUTTON_TAG),
    )
}

@Preview
@Composable
private fun PrepareScannerDownloadingPreview() {
    AndroidThemeForPreviews {
        PrepareScannerContent(
            downloadState = ScannerModelDownloadState.Downloading(
                bytesDownloaded = 45_000_000L,
                totalBytes = 97_867_228L,
            ),
            onRetry = {},
            onUseLegacy = {},
        )
    }
}

@Preview
@Composable
private fun PrepareScannerPendingPreview() {
    AndroidThemeForPreviews {
        PrepareScannerContent(
            downloadState = ScannerModelDownloadState.Pending,
            onRetry = {},
            onUseLegacy = {},
        )
    }
}

@Preview
@Composable
private fun PrepareScannerTransientFailurePreview() {
    AndroidThemeForPreviews {
        PrepareScannerContent(
            downloadState = ScannerModelDownloadState.Failed(permanent = false),
            onRetry = {},
            onUseLegacy = {},
        )
    }
}

@Preview
@Composable
private fun PrepareScannerPermanentFailurePreview() {
    AndroidThemeForPreviews {
        PrepareScannerContent(
            downloadState = ScannerModelDownloadState.Failed(permanent = true),
            onRetry = {},
            onUseLegacy = {},
        )
    }
}

internal const val PREPARE_SCANNER_TITLE_TAG = "prepare_scanner:title"
internal const val PREPARE_SCANNER_MESSAGE_TAG = "prepare_scanner:message"
internal const val PREPARE_SCANNER_PROGRESS_BAR_TAG = "prepare_scanner:progress_bar"
internal const val PREPARE_SCANNER_PERCENTAGE_TAG = "prepare_scanner:percentage"
internal const val PREPARE_SCANNER_ERROR_STATE_TAG = "prepare_scanner:error_state"
internal const val PREPARE_SCANNER_RETRY_BUTTON_TAG = "prepare_scanner:retry_button"
internal const val PREPARE_SCANNER_USE_OLD_SCANNER_BUTTON_TAG = "prepare_scanner:use_old_scanner_button"
