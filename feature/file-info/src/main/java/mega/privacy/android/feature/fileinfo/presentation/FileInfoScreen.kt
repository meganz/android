package mega.privacy.android.feature.fileinfo.presentation

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.model.LocalizedText
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.devicetype.DeviceType
import mega.android.core.ui.theme.devicetype.LocalDeviceType
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.feature.fileinfo.presentation.model.FileInfoUiState
import mega.privacy.android.feature.fileinfo.presentation.view.DurationBadge
import mega.privacy.android.feature.fileinfo.presentation.view.FileInfoDetailRow
import mega.privacy.android.feature.fileinfo.presentation.view.FileInfoMapView
import mega.privacy.android.feature.fileinfo.presentation.view.PermissionsRow
import mega.privacy.android.feature.fileinfo.presentation.view.TagsSection
import mega.privacy.android.feature.fileinfo.presentation.view.TakenDownBanner
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.destination.ContactInfoNavKey
import mega.privacy.android.navigation.destination.FileContactInfoNavKey
import mega.privacy.android.navigation.destination.TagsNavKey
import mega.privacy.android.navigation.destination.VersionsFileNavKey
import mega.privacy.android.shared.nodes.components.NodeDescriptionField
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType
import mega.privacy.android.shared.nodes.model.NodeSubtitleText
import mega.privacy.android.shared.nodes.model.text
import mega.privacy.android.shared.resources.R as sharedR

/**
 * File Info screen showing the node thumbnail, name, type and size.
 *
 * @param uiState the current [FileInfoUiState]
 * @param nodeHandle the handle of the node being shown, used to build navigation keys
 * @param onBack invoked when the Close action is tapped
 * @param onLocationClick invoked when the location row is tapped (opens the containing folder)
 * @param onNavigate invoked with the destination [NavKey] for a forward navigation (tags, shares)
 * @param onDescriptionChange invoked with the new description when the description edit is committed
 * @param modifier modifier for the scaffold
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FileInfoScreen(
    uiState: FileInfoUiState,
    nodeHandle: Long,
    onBack: () -> Unit,
    onLocationClick: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDisputeTakedown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier.testTag(FILE_INFO_SCREEN_TAG),
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(FILE_INFO_APP_BAR_TAG),
                title = stringResource(sharedR.string.general_info),
                navigationType = AppBarNavigationType.Back(onBack),
            )
        },
    ) { contentPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            if (uiState.isLoading) {
                FileInfoLoading()
            } else {
                FileInfoContent(
                    uiState = uiState,
                    nodeHandle = nodeHandle,
                    onLocationClick = onLocationClick,
                    onNavigate = onNavigate,
                    onDescriptionChange = onDescriptionChange,
                    onDisputeTakedown = onDisputeTakedown,
                )
            }
        }
    }
}

@Composable
private fun FileInfoContent(
    uiState: FileInfoUiState,
    nodeHandle: Long,
    onLocationClick: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDisputeTakedown: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FileInfoResponsiveLayout(
        modifier = modifier,
        header = { headerModifier ->
            FileInfoHeader(uiState = uiState, modifier = headerModifier)
        },
        details = {
            FileInfoDetails(
                uiState = uiState,
                nodeHandle = nodeHandle,
                onLocationClick = onLocationClick,
                onNavigate = onNavigate,
                onDescriptionChange = onDescriptionChange,
                onDisputeTakedown = onDisputeTakedown,
            )
        },
    )
}

/**
 * Lays out the header and the scrollable details responsively: stacked in portrait, and a two-pane
 * row in landscape (constrained to a centered fraction of the width on tablets so neither pane
 * stretches across a wide screen). Shared by the loaded content and the loading skeleton so both
 * adapt identically.
 *
 * @param header receives the sizing modifier to apply to the header slot (full width in portrait,
 * an equal-weight column with the header aspect ratio in landscape)
 * @param details the scrollable detail rows, laid out in a column
 */
@Composable
internal fun FileInfoResponsiveLayout(
    modifier: Modifier = Modifier,
    header: @Composable (Modifier) -> Unit,
    details: @Composable ColumnScope.() -> Unit,
) {
    val isLandscape =
        LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isTablet = LocalDeviceType.current == DeviceType.Tablet

    if (isLandscape) {
        val contentWidthFraction = if (isTablet) TABLET_LANDSCAPE_WIDTH_FRACTION else 1f
        Box(
            modifier = modifier.fillMaxSize(),
            contentAlignment = Alignment.TopCenter,
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(contentWidthFraction)
                    .fillMaxHeight()
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top,
            ) {
                header(
                    Modifier
                        .weight(1f)
                        .aspectRatio(HEADER_ASPECT_RATIO),
                )
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .imePadding()
                        .verticalScroll(rememberScrollState())
                        .testTag(FILE_INFO_DETAILS_TAG),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    content = details,
                )
            }
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            header(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(HEADER_ASPECT_RATIO),
            )
            details()
        }
    }
}

// The header preview keeps the design's 380x200 container proportions across sizes.
private const val HEADER_ASPECT_RATIO = 1.9f
private const val TABLET_LANDSCAPE_WIDTH_FRACTION = 0.7f

@Composable
private fun FileInfoHeader(
    uiState: FileInfoUiState,
    modifier: Modifier = Modifier,
) {
    BoxSurface(
        surfaceColor = SurfaceColor.Surface1,
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .testTag(FILE_INFO_HEADER_TAG),
    ) {
        uiState.iconRes?.let { iconRes ->
            NodeThumbnailView(
                modifier = Modifier.align(Alignment.Center),
                data = uiState.thumbnailData,
                defaultImage = iconRes,
                contentDescription = uiState.title,
                contentScale = ContentScale.Crop,
                layoutType = ThumbnailLayoutType.FullSize,
            )
        }
        uiState.durationText?.let { duration ->
            DurationBadge(
                text = duration,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(8.dp)
                    .testTag(FILE_INFO_DURATION_BADGE_TAG),
            )
        }
    }
}

@Composable
private fun FileInfoDetails(
    uiState: FileInfoUiState,
    nodeHandle: Long,
    onLocationClick: () -> Unit,
    onNavigate: (NavKey) -> Unit,
    onDescriptionChange: (String) -> Unit,
    onDisputeTakedown: () -> Unit,
) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val folderContent = if (!uiState.isFile &&
        (uiState.numberOfFolders > 0 || uiState.numberOfFiles > 0)
    ) {
        NodeSubtitleText.FolderSubtitle(
            childFolderCount = uiState.numberOfFolders,
            childFileCount = uiState.numberOfFiles,
        ).text()
    } else {
        null
    }
    val subtitle = buildList {
        when {
            uiState.isOutgoingShare ->
                add(stringResource(sharedR.string.file_info_information_outgoing_share))

            uiState.isIncomingShare ->
                add(stringResource(sharedR.string.file_info_information_incoming_share))

            uiState.isFile -> uiState.fileTypeName?.text?.let(::add)
            else -> add(stringResource(sharedR.string.file_info_information_type_folder))
        }
        if (uiState.sizeInBytes > 0) {
            add(formatFileSize(uiState.sizeInBytes, context))
        }
        uiState.durationText?.let { add(it) }
        folderContent?.let { add(it) }
    }.joinToString(separator = " ⋅ ")

    val sharedWithLabel = stringResource(sharedR.string.file_info_information_shared_with_label)

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        if (uiState.isTakenDown) {
            TakenDownBanner(
                isFile = uiState.isFile,
                onDisputeClick = onDisputeTakedown,
                modifier = Modifier.testTag(FILE_INFO_TAKEN_DOWN_TAG),
            )
        }
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            MegaText(
                modifier = Modifier.testTag(FILE_INFO_NAME_TAG),
                text = uiState.title,
                textColor = TextColor.Primary,
                overflow = TextOverflow.Ellipsis,
                style = AppTheme.typography.titleMedium,
            )
            if (subtitle.isNotEmpty()) {
                MegaText(
                    modifier = Modifier.testTag(FILE_INFO_SUBTITLE_TAG),
                    text = subtitle,
                    textColor = TextColor.Secondary,
                    style = AppTheme.typography.bodyMedium,
                )
            }
        }

        if (uiState.isOutgoingShare) {
            FileInfoDetailRow(
                label = sharedWithLabel,
                value = pluralStringResource(
                    sharedR.plurals.file_info_information_num_contacts,
                    uiState.sharedContactCount,
                    uiState.sharedContactCount,
                ),
                trailingIcon = IconPack.Medium.Thin.Outline.ChevronRight,
                onClick = {
                    onNavigate(
                        FileContactInfoNavKey(
                            folderHandle = nodeHandle,
                            // The shared-recipients screen uses folderName as its title
                            folderName = sharedWithLabel,
                        )
                    )
                },
                modifier = Modifier.testTag(FILE_INFO_SHARED_WITH_TAG),
            )
        }

        if (uiState.isIncomingShare) {
            uiState.ownerEmail?.let { ownerEmail ->
                val ownerName = uiState.ownerName.orEmpty()
                FileInfoDetailRow(
                    label = stringResource(sharedR.string.file_info_information_owner_label),
                    value = if (ownerName.isNotBlank() && ownerName != ownerEmail) {
                        "$ownerName ($ownerEmail)"
                    } else {
                        ownerEmail
                    },
                    trailingIcon = IconPack.Medium.Thin.Outline.ChevronRight,
                    onClick = { onNavigate(ContactInfoNavKey(ownerEmail)) },
                    modifier = Modifier.testTag(FILE_INFO_OWNER_TAG),
                )
            }
            PermissionsRow(
                accessPermission = uiState.accessPermission,
                modifier = Modifier.testTag(FILE_INFO_PERMISSIONS_TAG),
            )
        }

        if (uiState.showFolderVersions) {
            FileInfoDetailRow(
                label = stringResource(sharedR.string.title_section_versions),
                value = pluralStringResource(
                    sharedR.plurals.file_info_information_num_versioned_files,
                    uiState.numberOfVersions,
                    uiState.numberOfVersions,
                ),
                modifier = Modifier.testTag(FILE_INFO_VERSIONS_TAG),
            )
            FileInfoDetailRow(
                label = stringResource(sharedR.string.file_info_information_current_versions_label),
                value = formatFileSize(uiState.currentVersionsSizeInBytes, context),
                modifier = Modifier.testTag(FILE_INFO_CURRENT_VERSIONS_TAG),
            )
            FileInfoDetailRow(
                label = stringResource(sharedR.string.file_info_information_previous_versions_label),
                value = formatFileSize(uiState.previousVersionsSizeInBytes, context),
                modifier = Modifier.testTag(FILE_INFO_PREVIOUS_VERSIONS_TAG),
            )
        }

        uiState.creationTime?.let { added ->
            FileInfoDetailRow(
                label = stringResource(sharedR.string.info_added),
                value = formatModifiedDate(locale, added),
                modifier = Modifier.testTag(FILE_INFO_ADDED_TAG),
            )
        }

        uiState.modificationTime?.let { modified ->
            FileInfoDetailRow(
                label = stringResource(sharedR.string.search_dropdown_chip_filter_type_last_modified),
                value = formatModifiedDate(locale, modified),
                modifier = Modifier.testTag(FILE_INFO_LAST_MODIFIED_TAG),
            )
        }

        if (uiState.showFileVersions) {
            FileInfoDetailRow(
                label = stringResource(sharedR.string.title_section_versions),
                value = pluralStringResource(
                    sharedR.plurals.file_info_information_num_versions,
                    uiState.versionCount,
                    uiState.versionCount,
                ),
                trailingIcon = IconPack.Medium.Thin.Outline.ChevronRight,
                onClick = { onNavigate(VersionsFileNavKey(nodeHandle)) },
                modifier = Modifier.testTag(FILE_INFO_VERSIONS_TAG),
            )
        }

        locationRootLabel(uiState.nodeSourceType)?.let { rootLabel ->
            val location = if (uiState.locationFolders.isEmpty()) {
                rootLabel
            } else {
                uiState.locationFolders.joinToString(separator = " > ", prefix = "$rootLabel > ")
            }
            FileInfoDetailRow(
                label = stringResource(sharedR.string.video_section_videos_location_filter_title),
                value = location,
                trailingIcon = IconPack.Medium.Thin.Outline.FolderSearch,
                onClick = onLocationClick,
                modifier = Modifier.testTag(FILE_INFO_LOCATION_TAG),
            )
        }

        if (uiState.showMapSection) {
            FileInfoMapView(
                coordinates = uiState.mapCoordinates,
                caption = uiState.locationCaption,
            )
        }

        if (uiState.canShowDescription) {
            NodeDescriptionField(
                description = uiState.descriptionText,
                isEditable = uiState.canEditDescription,
                label = stringResource(sharedR.string.file_info_information_description_label),
                placeholder = stringResource(sharedR.string.file_info_information_description_placeholder),
                onDescriptionChange = onDescriptionChange,
                modifier = Modifier.testTag(FILE_INFO_DESCRIPTION_TAG),
            )
        }

        if (uiState.canShowTags) {
            TagsSection(
                tags = uiState.tags,
                canEdit = uiState.canEditTags,
                onClick = { onNavigate(TagsNavKey(nodeHandle)) },
                modifier = Modifier.testTag(FILE_INFO_TAGS_TAG),
            )
        }
    }
}

/**
 * The localized root label for the location breadcrumb, or null when the node's source is unknown
 * (in which case the location row is hidden).
 */
@Composable
private fun locationRootLabel(sourceType: NodeSourceType?): String? = when (sourceType) {
    NodeSourceType.CLOUD_DRIVE -> stringResource(sharedR.string.general_section_cloud_drive)
    NodeSourceType.RUBBISH_BIN -> stringResource(sharedR.string.general_section_rubbish_bin)
    NodeSourceType.INCOMING_SHARES -> stringResource(sharedR.string.general_title_incoming_shares)
    else -> null
}


@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun FileInfoScreenFilePreview() {
    AndroidThemeForPreviews {
        FileInfoScreen(
            uiState = FileInfoUiState(
                isLoading = false,
                title = "Presentation.pdf",
                isFile = true,
                iconRes = iconPackR.drawable.ic_pdf_medium_solid,
                fileTypeName = LocalizedText.StringRes(sharedR.string.file_type_name_pdf_document),
                sizeInBytes = 10L * 1024 * 1024,
                creationTime = 1_749_000_000L,
                modificationTime = 1_749_500_000L,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                locationFolders = listOf("Documents", "Marketing"),
                descriptionText = "This is test description",
                tags = listOf("marketing", "2024", "confidential"),
                versionCount = 2,
                accessPermission = AccessPermission.OWNER
            ),
            nodeHandle = 0L,
            onBack = {},
            onLocationClick = {},
            onNavigate = {},
            onDescriptionChange = {},
            onDisputeTakedown = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun FileInfoScreenVideoPreview() {
    AndroidThemeForPreviews {
        FileInfoScreen(
            uiState = FileInfoUiState(
                isLoading = false,
                title = "housetour.mov",
                isFile = true,
                iconRes = iconPackR.drawable.ic_video_medium_solid,
                fileTypeName = LocalizedText.StringRes(sharedR.string.file_type_name_video),
                sizeInBytes = 4L * 1024 * 1024,
                durationText = "1:24",
                creationTime = 1_749_000_000L,
                modificationTime = 1_749_500_000L,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                accessPermission = AccessPermission.OWNER,
            ),
            nodeHandle = 0L,
            onBack = {},
            onLocationClick = {},
            onNavigate = {},
            onDescriptionChange = {},
            onDisputeTakedown = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun FileInfoScreenFolderPreview() {
    AndroidThemeForPreviews {
        FileInfoScreen(
            uiState = FileInfoUiState(
                isLoading = false,
                title = "Marketing",
                isFile = false,
                iconRes = iconPackR.drawable.ic_folder_medium_solid,
                creationTime = 1_749_000_000L,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                locationFolders = listOf("Documents"),
                sharedContactCount = 3,
                sizeInBytes = 21L * 1024 * 1024,
                numberOfFiles = 2,
            ),
            nodeHandle = 0L,
            onBack = {},
            onLocationClick = {},
            onNavigate = {},
            onDescriptionChange = {},
            onDisputeTakedown = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Landscape",
    device = "spec:width=880dp,height=400dp,orientation=landscape",
    showBackground = true,
)
@Composable
private fun FileInfoScreenLandscapePreview() {
    AndroidThemeForPreviews {
        FileInfoScreen(
            uiState = FileInfoUiState(
                isLoading = false,
                title = "New folder(1)",
                isFile = false,
                iconRes = iconPackR.drawable.ic_folder_medium_solid,
                creationTime = 1_749_000_000L,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                locationFolders = listOf("Documents", "Marketing", "2026"),
                sizeInBytes = 21L * 1024 * 1024,
                numberOfFiles = 2,
                accessPermission = AccessPermission.OWNER,
                descriptionText = "A collection of related files and assets organized for easy " +
                        "access and collaboration.",
                tags = listOf("marketing", "2026", "documentation"),
            ),
            nodeHandle = 0L,
            onBack = {},
            onLocationClick = {},
            onNavigate = {},
            onDescriptionChange = {},
            onDisputeTakedown = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview(
    name = "Tablet landscape",
    device = "spec:width=1280dp,height=800dp,orientation=landscape",
    showBackground = true,
)
@Composable
private fun FileInfoScreenTabletLandscapePreview() {
    AndroidThemeForPreviews {
        CompositionLocalProvider(LocalDeviceType provides DeviceType.Tablet) {
            FileInfoScreen(
                uiState = FileInfoUiState(
                    isLoading = false,
                    title = "New folder(1)",
                    isFile = false,
                    iconRes = iconPackR.drawable.ic_folder_medium_solid,
                    creationTime = 1_749_000_000L,
                    nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                    locationFolders = listOf("Documents", "Marketing", "2026"),
                    sizeInBytes = 21L * 1024 * 1024,
                    numberOfFiles = 2,
                    accessPermission = AccessPermission.OWNER,
                    descriptionText = "A collection of related files and assets organized for easy " +
                            "access and collaboration.",
                    tags = listOf("marketing", "2026", "documentation"),
                ),
                nodeHandle = 0L,
                onBack = {},
                onLocationClick = {},
                onNavigate = {},
                onDescriptionChange = {},
                onDisputeTakedown = {},
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun FileInfoScreenIncomingShareFolderPreview() {
    AndroidThemeForPreviews {
        FileInfoScreen(
            uiState = FileInfoUiState(
                isLoading = false,
                title = "Company Assets",
                isFile = false,
                iconRes = iconPackR.drawable.ic_folder_medium_solid,
                creationTime = 1_749_000_000L,
                nodeSourceType = NodeSourceType.INCOMING_SHARES,
                accessPermission = AccessPermission.FULL,
                ownerName = "John Doe",
                ownerEmail = "johndoe@mail.com",
                numberOfVersions = 91,
                currentVersionsSizeInBytes = 22_800_000_000L,
                previousVersionsSizeInBytes = 1_260_000_000L,
            ),
            nodeHandle = 0L,
            onBack = {},
            onLocationClick = {},
            onNavigate = {},
            onDescriptionChange = {},
            onDisputeTakedown = {},
        )
    }
}

internal const val FILE_INFO_SCREEN_TAG = "file_info_screen:scaffold"
internal const val FILE_INFO_APP_BAR_TAG = "file_info_screen:app_bar"
internal const val FILE_INFO_HEADER_TAG = "file_info_screen:header"
internal const val FILE_INFO_DETAILS_TAG = "file_info_screen:details"
internal const val FILE_INFO_DURATION_BADGE_TAG = "file_info_screen:duration_badge"
internal const val FILE_INFO_NAME_TAG = "file_info_screen:name"
internal const val FILE_INFO_SUBTITLE_TAG = "file_info_screen:subtitle"
internal const val FILE_INFO_ADDED_TAG = "file_info_screen:added"
internal const val FILE_INFO_LAST_MODIFIED_TAG = "file_info_screen:last_modified"
internal const val FILE_INFO_LOCATION_TAG = "file_info_screen:location"
internal const val FILE_INFO_SHARED_WITH_TAG = "file_info_screen:shared_with"
internal const val FILE_INFO_OWNER_TAG = "file_info_screen:owner"
internal const val FILE_INFO_PERMISSIONS_TAG = "file_info_screen:permissions"
internal const val FILE_INFO_VERSIONS_TAG = "file_info_screen:versions"
internal const val FILE_INFO_CURRENT_VERSIONS_TAG = "file_info_screen:current_versions"
internal const val FILE_INFO_PREVIOUS_VERSIONS_TAG = "file_info_screen:previous_versions"
internal const val FILE_INFO_DESCRIPTION_TAG = "file_info_screen:description"
internal const val FILE_INFO_TAGS_TAG = "file_info_screen:tags"
internal const val FILE_INFO_TAKEN_DOWN_TAG = "file_info_screen:taken_down"
