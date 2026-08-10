package mega.privacy.android.feature.transfers.presentation.settings.view

import android.content.res.Resources
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import mega.android.core.ui.components.settings.SettingsNavigationItem
import mega.android.core.ui.components.settings.SettingsOptionsModal
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.extensions.LaunchedOnceEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.analytics.Analytics
import mega.privacy.android.feature.transfers.presentation.settings.model.TransfersSettingsUiState
import mega.privacy.android.shared.resources.R as sharedR
import mega.privacy.mobile.analytics.event.DownloadConnectionsChangedEvent
import mega.privacy.mobile.analytics.event.DownloadConnectionsDialogEvent
import mega.privacy.mobile.analytics.event.TransfersSettingsScreenEvent
import mega.privacy.mobile.analytics.event.UploadConnectionsChangedEvent
import mega.privacy.mobile.analytics.event.UploadConnectionsDialogEvent

@Composable
fun TransfersSettingsView(
    uiState: TransfersSettingsUiState,
    onSetMaxDownloadConnections: (Int) -> Unit,
    onSetMaxUploadConnections: (Int) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val resources = LocalResources.current

    LaunchedOnceEffect(Unit) {
        Analytics.tracker.trackEvent(TransfersSettingsScreenEvent)
    }

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
    var showDownloadDialog by rememberSaveable { mutableStateOf(false) }
    var showUploadDialog by rememberSaveable { mutableStateOf(false) }

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
        SettingsNavigationItem(
            key = DOWNLOAD_CONNECTIONS_VIEW_TAG,
            title = stringResource(sharedR.string.settings_transfer_download_connections),
            enabled = enabled,
            subtitle = downloadValueToString(selectedDownloadConnections),
        ) {
            Analytics.tracker.trackEvent(DownloadConnectionsDialogEvent)
            showDownloadDialog = true
        }
        StrongDivider(modifier = Modifier.fillMaxWidth())
        SettingsNavigationItem(
            key = UPLOAD_CONNECTIONS_VIEW_TAG,
            title = stringResource(sharedR.string.settings_transfer_upload_connections),
            enabled = enabled,
            subtitle = uploadValueToString(selectedUploadConnections),
        ) {
            Analytics.tracker.trackEvent(UploadConnectionsDialogEvent)
            showUploadDialog = true
        }
        StrongDivider(modifier = Modifier.fillMaxWidth())
    }

    if (showDownloadDialog) {
        SettingsOptionsModal(
            key = DOWNLOAD_CONNECTIONS_VIEW_TAG,
            content = {
                addHeader(
                    title = stringResource(sharedR.string.settings_transfer_download_connections)
                )
                transferConnections.forEach { value ->
                    addItem(
                        isSelected = value == selectedDownloadConnections,
                        value = value,
                        valueToString = downloadValueToString,
                    )
                }
            },
            onDismiss = { showDownloadDialog = false },
        ) { value ->
            Analytics.tracker.trackEvent(
                DownloadConnectionsChangedEvent(
                    previousValue = selectedDownloadConnections,
                    newValue = value,
                )
            )
            onSetMaxDownloadConnections(value)
        }
    }

    if (showUploadDialog) {
        SettingsOptionsModal(
            key = UPLOAD_CONNECTIONS_VIEW_TAG,
            content = {
                addHeader(
                    title = stringResource(sharedR.string.settings_transfer_upload_connections)
                )
                transferConnections.forEach { value ->
                    addItem(
                        isSelected = value == selectedUploadConnections,
                        value = value,
                        valueToString = uploadValueToString,
                    )
                }
            },
            onDismiss = { showUploadDialog = false },
        ) { value ->
            Analytics.tracker.trackEvent(
                UploadConnectionsChangedEvent(
                    previousValue = selectedUploadConnections,
                    newValue = value,
                )
            )
            onSetMaxUploadConnections(value)
        }
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