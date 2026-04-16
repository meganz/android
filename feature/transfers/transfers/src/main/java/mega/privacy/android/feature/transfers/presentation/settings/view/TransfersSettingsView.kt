package mega.privacy.android.feature.transfers.presentation.settings.view

import android.content.res.Resources
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.unit.dp
import mega.android.core.ui.components.MegaScaffold
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.divider.StrongDivider
import mega.android.core.ui.components.list.GenericListItem
import mega.android.core.ui.components.settings.SettingsOptionsItem
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.feature.transfers.presentation.settings.model.TransfersSettingsUiState
import mega.privacy.android.shared.resources.R as sharedR

@Composable
fun TransfersSettingsView(
    uiState: TransfersSettingsUiState,
    onSetMaxDownloadConnections: (Int) -> Unit,
    onSetMaxUploadConnections: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current

    MegaScaffold(
        modifier = modifier
            .testTag(TRANSFERS_SETTINGS_VIEW_TAG)
            .fillMaxSize()
            .semantics { testTagsAsResourceId = true },
        topBar = {
            MegaTopAppBar(
                title = stringResource(id = sharedR.string.general_section_transfers),
                navigationType = AppBarNavigationType.Back(onNavigateBack)
            )
        }
    ) { paddingValues ->
        val modifier = Modifier.padding(paddingValues)
        when (uiState) {
            is TransfersSettingsUiState.Data -> TransfersSettingsViewContent(
                modifier = modifier,
                transferConnections = uiState.maxTransferConnectionsRange.toList(),
                selectedDownloadConnections = uiState.maxDownloadConnections,
                selectedUploadConnections = uiState.maxUploadConnections,
                downloadValueToString = { value ->
                    value.transferConnectionsValueToString(resources, DEFAULT_DOWNLOAD_CONNECTIONS)
                },
                uploadValueToString = { value ->
                    value.transferConnectionsValueToString(resources, DEFAULT_UPLOAD_CONNECTIONS)
                },
                onSetMaxDownloadConnections = onSetMaxDownloadConnections,
                onSetMaxUploadConnections = onSetMaxUploadConnections,
                enabled = true
            )

            TransfersSettingsUiState.Loading ->
                TransfersSettingsViewContent(
                    modifier = modifier,
                    transferConnections = emptyList(),
                    selectedDownloadConnections = 0,
                    selectedUploadConnections = 0,
                    downloadValueToString = { "" },
                    uploadValueToString = { "" },
                    onSetMaxDownloadConnections = onSetMaxDownloadConnections,
                    onSetMaxUploadConnections = onSetMaxUploadConnections,
                    enabled = false
                )
        }
    }
}

@Composable
private fun TransfersSettingsViewContent(
    transferConnections: List<Int>,
    selectedDownloadConnections: Int,
    selectedUploadConnections: Int,
    downloadValueToString: (Int) -> String,
    uploadValueToString: (Int) -> String,
    modifier: Modifier,
    onSetMaxDownloadConnections: (Int) -> Unit,
    onSetMaxUploadConnections: (Int) -> Unit,
    enabled: Boolean,
) {
    Column(modifier = modifier) {
        GenericListItem(
            enableClick = false,
            contentPadding = PaddingValues(
                horizontal = 16.dp,
                vertical = 12.dp
            ),
            title = {
                MegaText(
                    text = stringResource(sharedR.string.settings_transfer_connections_title),
                    textColor = TextColor.Primary,
                    style = AppTheme.typography.bodyLarge,
                )
            },
            subtitle = {
                MegaText(
                    text = stringResource(sharedR.string.settings_transfer_connections_text),
                    textColor = TextColor.Secondary,
                    style = AppTheme.typography.bodyMedium,
                )
            },
        )
        StrongDivider(modifier = Modifier.fillMaxWidth())
        SettingsOptionsItem(
            key = DOWNLOAD_CONNECTIONS_VIEW_TAG,
            title = stringResource(sharedR.string.settings_transfer_download_connections),
            values = transferConnections,
            valueToString = downloadValueToString,
            selectedValue = selectedDownloadConnections,
            enabled = enabled,
        ) { _, value -> onSetMaxDownloadConnections(value) }
        StrongDivider(modifier = Modifier.fillMaxWidth())
        SettingsOptionsItem(
            key = UPLOAD_CONNECTIONS_VIEW_TAG,
            title = stringResource(sharedR.string.settings_transfer_upload_connections),
            values = transferConnections,
            valueToString = uploadValueToString,
            selectedValue = selectedUploadConnections,
            enabled = enabled,
        ) { _, value -> onSetMaxUploadConnections(value) }
        StrongDivider(modifier = Modifier.fillMaxWidth())
    }
}

internal fun Int.transferConnectionsValueToString(resources: Resources, default: Int): String =
    when (this) {
        BEST_FOR_SLOW_NETWORKS -> resources.getString(
            sharedR.string.settings_transfer_connections_slow_networs,
            BEST_FOR_SLOW_NETWORKS
        )

        default -> resources.getString(
            sharedR.string.settings_transfer_connections_default,
            default
        )

        DEFAULT_UPLOAD_AND_DATA_USAGE -> resources.getString(
            sharedR.string.settings_transfer_connections_higher_usage,
            DEFAULT_UPLOAD_AND_DATA_USAGE
        )

        else -> this.toString()
    }


@Composable
@CombinedThemePreviews
private fun TransfersSettingsViewLoadingPreview() {
    AndroidThemeForPreviews {
        TransfersSettingsView(
            uiState = TransfersSettingsUiState.Loading,
            onSetMaxDownloadConnections = {},
            onSetMaxUploadConnections = {},
            onNavigateBack = {},
        )
    }
}

@Composable
@CombinedThemePreviews
private fun TransfersSettingsViewDataPreview() {
    AndroidThemeForPreviews {
        TransfersSettingsView(
            uiState = TransfersSettingsUiState.Data(
                maxDownloadConnections = 4,
                maxUploadConnections = 3,
                maxTransferConnectionsRange = 1..8,
            ),
            onSetMaxDownloadConnections = {},
            onSetMaxUploadConnections = {},
            onNavigateBack = {},
        )
    }
}

internal const val TRANSFERS_SETTINGS_VIEW_TAG = "transfers_settings_view"
internal const val DOWNLOAD_CONNECTIONS_VIEW_TAG =
    "$TRANSFERS_SETTINGS_VIEW_TAG:download_connections"
internal const val UPLOAD_CONNECTIONS_VIEW_TAG = "$TRANSFERS_SETTINGS_VIEW_TAG:upload_connections"
internal const val BEST_FOR_SLOW_NETWORKS = 1
internal const val DEFAULT_DOWNLOAD_CONNECTIONS = 4
internal const val DEFAULT_UPLOAD_CONNECTIONS = 3
internal const val DEFAULT_UPLOAD_AND_DATA_USAGE = 8