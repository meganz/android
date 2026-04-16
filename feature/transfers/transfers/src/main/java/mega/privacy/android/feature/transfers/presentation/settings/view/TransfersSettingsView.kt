package mega.privacy.android.feature.transfers.presentation.settings.view

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
    val enabled = uiState is TransfersSettingsUiState.Data
    val isLoading = uiState is TransfersSettingsUiState.Loading
    val selectedDownloadConnections =
        (uiState as? TransfersSettingsUiState.Data)?.maxDownloadConnections ?: 0
    val selectedUploadConnections =
        (uiState as? TransfersSettingsUiState.Data)?.maxUploadConnections ?: 0
    val transferConnections =
        (uiState as? TransfersSettingsUiState.Data)?.maxTransferConnectionsRange?.toList()
            ?: emptyList()

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
        Column(modifier = Modifier.padding(paddingValues)) {
            GenericListItem(
                enableClick = false,
                contentPadding = PaddingValues(
                    horizontal = 16.dp,
                    vertical = 12.dp
                ),
                title = {
                    MegaText(
                        text = "Transfer connections",
                        textColor = TextColor.Primary,
                        style = AppTheme.typography.bodyLarge,
                    )
                },
                subtitle = {
                    MegaText(
                        text = "Defines the number of parallel connections per file transfer. Higher values may improve speed but increase battery and data usage.",
                        textColor = TextColor.Secondary,
                        style = AppTheme.typography.bodyMedium,
                    )
                },
            )
            StrongDivider(modifier = Modifier.fillMaxWidth())
            SettingsOptionsItem(
                key = DOWNLOAD_CONNECTIONS_VIEW_TAG,
                title = "Download connections",
                values = transferConnections,
                valueToString = { value ->
                    if (isLoading) "" else value.toText(DEFAULT_DOWNLOAD_CONNECTIONS)
                },
                selectedValue = selectedDownloadConnections,
                enabled = enabled,
            ) { _, value -> onSetMaxDownloadConnections(value) }
            StrongDivider(modifier = Modifier.fillMaxWidth())
            SettingsOptionsItem(
                key = UPLOAD_CONNECTIONS_VIEW_TAG,
                title = "Upload connections",
                values = transferConnections,
                valueToString = { value ->
                    if (isLoading) "" else value.toText(DEFAULT_UPLOAD_CONNECTIONS)
                },
                selectedValue = selectedUploadConnections,
                enabled = enabled,
            ) { _, value -> onSetMaxUploadConnections(value) }
            StrongDivider(modifier = Modifier.fillMaxWidth())
        }
    }
}

internal fun Int.toText(default: Int): String = when (this) {
    1 -> "$this (Best for slow networks)"
    default -> "$this (Default)"
    8 -> "$this (Higher battery and data usage)"
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
internal const val DEFAULT_DOWNLOAD_CONNECTIONS = 4
internal const val DEFAULT_UPLOAD_CONNECTIONS = 3