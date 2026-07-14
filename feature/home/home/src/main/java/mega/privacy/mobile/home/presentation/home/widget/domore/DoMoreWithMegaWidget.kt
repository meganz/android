package mega.privacy.mobile.home.presentation.home.widget.domore

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.sharedcomponents.button.DoMoreWithMegaItemButton
import mega.privacy.android.domain.entity.Feature
import mega.privacy.android.domain.entity.navigation.Flagged
import mega.privacy.android.domain.featuretoggle.ApiFeatures
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.featureflag.FeatureFlagGate
import mega.privacy.android.navigation.contract.home.HomeWidget
import mega.privacy.android.navigation.contract.home.HomeWidgetOrder
import mega.privacy.android.navigation.contract.navkey.ContinuousScanNavKey
import mega.privacy.android.navigation.destination.CameraBackupPermissionsNavKey
import mega.privacy.android.navigation.destination.CreateScheduledMeetingNavKey
import mega.privacy.android.navigation.destination.InviteContactNavKey
import mega.privacy.android.navigation.destination.LegacySettingsCameraUploadsActivityNavKey
import mega.privacy.android.navigation.destination.SyncPromotionNavKey
import mega.privacy.android.shared.resources.R as sharedR
import javax.inject.Inject

/**
 * "Do more with MEGA" home section.
 *
 * Renders a horizontally-scrollable row of circular shortcut buttons. Each button is a
 * [DoMoreWithMegaItem] contributed via Dagger `@IntoSet`, so the section is fully
 * extensible without modifying this widget. The whole section is gated behind the
 * [mega.privacy.android.domain.featuretoggle.ApiFeatures.DoMoreWithMEGA] feature flag.
 */
class DoMoreWithMegaWidget @Inject constructor() : HomeWidget, Flagged {

    override val identifier: String = "DoMoreWithMegaWidgetProvider"
    override val defaultOrder: HomeWidgetOrder = HomeWidgetOrder.DoMoreWithMega
    override val canDelete: Boolean = false
    override val isConfigurable: Boolean = true
    override val isDraggable: Boolean = true
    override suspend fun getWidgetName() =
        LocalizedText.StringRes(sharedR.string.home_do_more_with_mega_title)

    @Composable
    override fun DisplayWidget(
        modifier: Modifier,
        navigationHandler: NavigationHandler,
        transferHandler: TransferHandler,
    ) {
        FeatureFlagGate(feature) {
            val viewModel = hiltViewModel<DoMoreWithMegaWidgetViewModel>()
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()
            if (uiState.items.isNotEmpty()) {
                DoMoreWithMegaSection(
                    items = uiState.items,
                    onItemClick = { item ->
                        navigationHandler.navigateForItem(
                            identifier = item.identifier,
                            isCameraUploadsEnabled = uiState.isCameraUploadsEnabled,
                            hasPreviouslyEnabledCameraUploads = uiState.hasPreviouslyEnabledCameraUploads,
                        )
                    },
                    modifier = modifier,
                )
            }
        }
    }

    override val feature: Feature = ApiFeatures.DoMoreWithMEGA
}

/**
 * Resolves the navigation for a "Do more with MEGA" shortcut. Handling the click here (rather than
 * on the item) keeps each item a pure descriptor and keeps all navigation logic in one place, where
 * the [NavigationHandler] lives and destinations can be built with whatever parameters they need.
 */
private fun NavigationHandler.navigateForItem(
    identifier: DoMoreWithMegaItem.Identifier,
    isCameraUploadsEnabled: Boolean,
    hasPreviouslyEnabledCameraUploads: Boolean,
) {
    when (identifier) {
        DoMoreWithMegaItem.Identifier.CameraUploads ->
            if (isCameraUploadsEnabled || hasPreviouslyEnabledCameraUploads) {
                navigate(LegacySettingsCameraUploadsActivityNavKey())
            } else {
                navigate(CameraBackupPermissionsNavKey)
            }

        DoMoreWithMegaItem.Identifier.AddSync ->
            navigate(SyncPromotionNavKey)

        DoMoreWithMegaItem.Identifier.ScanDocument ->
            navigate(ContinuousScanNavKey)


        DoMoreWithMegaItem.Identifier.CreateAlbum -> {
            // TODO wire navigation for Do More with MEGA
        }

        DoMoreWithMegaItem.Identifier.AddContact ->
            navigate(InviteContactNavKey())

        DoMoreWithMegaItem.Identifier.ScheduleMeeting ->
            navigate(CreateScheduledMeetingNavKey)
    }
}

@Composable
private fun DoMoreWithMegaSection(
    items: List<DoMoreWithMegaItem>,
    onItemClick: (DoMoreWithMegaItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag(DO_MORE_SECTION_TEST_TAG),
    ) {
        MegaText(
            text = stringResource(sharedR.string.home_do_more_with_mega_title),
            textColor = TextColor.Primary,
            style = AppTheme.typography.titleMedium.copy(fontSize = 18.sp),
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 12.dp)
                .testTag(DO_MORE_TITLE_TEST_TAG),
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Spacer(modifier = Modifier.width(4.dp))
            items.forEach { item ->
                DoMoreWithMegaItemButton(
                    icon = item.icon,
                    label = stringResource(item.labelRes),
                    onClick = { onItemClick(item) },
                )
            }
            Spacer(modifier = Modifier.width(4.dp))
        }
    }
}

internal const val DO_MORE_SECTION_TEST_TAG = "do_more_with_mega_widget:section"
internal const val DO_MORE_TITLE_TEST_TAG = "do_more_with_mega_widget:title"

@CombinedThemePreviews
@Composable
private fun DoMoreWithMegaSectionPreview() {
    AndroidThemeForPreviews {
        LazyColumn {
            item {
                DoMoreWithMegaSection(
                    items = listOf(
                        previewItem(
                            IconPack.Medium.Thin.Outline.Camera,
                            sharedR.string.home_do_more_with_mega_camera_uploads,
                        ),
                        previewItem(
                            IconPack.Medium.Thin.Outline.Sync01,
                            sharedR.string.home_do_more_with_mega_add_sync,
                        ),
                        previewItem(
                            IconPack.Medium.Thin.Outline.FileScan,
                            sharedR.string.home_do_more_with_mega_scan_document,
                        ),
                        previewItem(
                            IconPack.Medium.Thin.Outline.Image01,
                            sharedR.string.home_do_more_with_mega_create_album,
                        ),
                        previewItem(
                            IconPack.Medium.Thin.Outline.UserPlus,
                            sharedR.string.home_do_more_with_mega_add_contact,
                        ),
                    ),
                    onItemClick = {},
                )
            }
        }
    }
}

private fun previewItem(icon: ImageVector, labelRes: Int) = object : DoMoreWithMegaItem {
    override val identifier: DoMoreWithMegaItem.Identifier =
        DoMoreWithMegaItem.Identifier.CameraUploads
    override val icon: ImageVector = icon
    override val labelRes: Int = labelRes
}
