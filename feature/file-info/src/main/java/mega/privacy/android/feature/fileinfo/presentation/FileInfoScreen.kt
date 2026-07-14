package mega.privacy.android.feature.fileinfo.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLocale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavKey
import mega.android.core.ui.components.MegaScaffoldWithTopAppBarScrollBehavior
import mega.android.core.ui.components.MegaText
import mega.android.core.ui.components.surface.BoxSurface
import mega.android.core.ui.components.surface.SurfaceColor
import mega.android.core.ui.components.toolbar.AppBarNavigationType
import mega.android.core.ui.components.toolbar.MegaTopAppBar
import mega.android.core.ui.modifiers.shimmerEffect
import mega.android.core.ui.preview.CombinedThemePreviews
import mega.android.core.ui.theme.AndroidThemeForPreviews
import mega.android.core.ui.theme.AppTheme
import mega.android.core.ui.theme.values.TextColor
import mega.privacy.android.core.formatter.formatFileSize
import mega.privacy.android.core.formatter.formatModifiedDate
import mega.privacy.android.domain.entity.node.NodeSourceType
import mega.privacy.android.domain.entity.shares.AccessPermission
import mega.privacy.android.feature.fileinfo.presentation.model.FileInfoUiState
import mega.privacy.android.feature.fileinfo.presentation.view.FileInfoDetailRow
import mega.privacy.android.feature.fileinfo.presentation.view.FileInfoMapView
import mega.privacy.android.feature.fileinfo.presentation.view.PermissionsRow
import mega.privacy.android.feature.fileinfo.presentation.view.TagsSection
import mega.privacy.android.icon.pack.IconPack
import mega.privacy.android.icon.pack.R as iconPackR
import mega.privacy.android.navigation.destination.ContactInfoNavKey
import mega.privacy.android.navigation.destination.FileContactInfoNavKey
import mega.privacy.android.navigation.destination.TagsNavKey
import mega.privacy.android.shared.nodes.components.NodeDescriptionField
import mega.privacy.android.shared.nodes.components.NodeThumbnailView
import mega.privacy.android.shared.nodes.components.ThumbnailLayoutType
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
    modifier: Modifier = Modifier,
) {
    MegaScaffoldWithTopAppBarScrollBehavior(
        modifier = modifier.testTag(FILE_INFO_SCREEN_TAG),
        topBar = {
            MegaTopAppBar(
                modifier = Modifier.testTag(FILE_INFO_APP_BAR_TAG),
                // TODO extract to a localized string resource
                title = "Info",
                navigationType = AppBarNavigationType.Close(onBack),
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
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val locale = LocalLocale.current.platformLocale
    val typeLabel = if (uiState.isFile) {
        uiState.fileTypeExtension?.uppercase()
    } else {
        // TODO extract to a localized string resource
        "Folder"
    }
    val subtitle = buildList {
        if (uiState.isOutgoingShare) {
            // TODO extract to a localized string resource
            add("Outgoing share")
        }
        if (uiState.isIncomingShare) {
            // TODO extract to a localized string resource
            add("Incoming share")
        }
        typeLabel?.let { add(it) }
        if (uiState.isFile && uiState.sizeInBytes > 0) {
            add(formatFileSize(uiState.sizeInBytes, context))
        }
    }.joinToString(separator = " • ")

    Column(
        modifier = modifier
            .fillMaxSize()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BoxSurface(
            surfaceColor = SurfaceColor.Surface1,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .clip(RoundedCornerShape(16.dp))
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
        }

        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            MegaText(
                modifier = Modifier.testTag(FILE_INFO_NAME_TAG),
                text = uiState.title,
                textColor = TextColor.Primary,
                overflow = TextOverflow.Ellipsis,
                maxLines = 2,
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
                // TODO extract to localized string resources
                label = "Shared with",
                value = "${uiState.sharedContactCount} contacts",
                trailingIcon = IconPack.Medium.Thin.Outline.ChevronRight,
                onClick = {
                    onNavigate(
                        FileContactInfoNavKey(
                            folderHandle = nodeHandle,
                            // The shared-recipients screen uses folderName as its title
                            // TODO extract to a localized string resource
                            folderName = "Shared with",
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
                    // TODO extract to a localized string resource
                    label = "Owner",
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

        uiState.creationTime?.let { added ->
            FileInfoDetailRow(
                // TODO extract to a localized string resource
                label = "Added",
                value = formatModifiedDate(locale, added),
                modifier = Modifier.testTag(FILE_INFO_ADDED_TAG),
            )
        }

        uiState.modificationTime?.let { modified ->
            FileInfoDetailRow(
                // TODO extract to a localized string resource
                label = "Last modified",
                value = formatModifiedDate(locale, modified),
                modifier = Modifier.testTag(FILE_INFO_LAST_MODIFIED_TAG),
            )
        }

        locationRootLabel(uiState.nodeSourceType)?.let { rootLabel ->
            val location = if (uiState.locationFolders.isEmpty()) {
                rootLabel
            } else {
                uiState.locationFolders.joinToString(separator = " > ", prefix = "$rootLabel > ")
            }
            FileInfoDetailRow(
                // TODO extract to a localized string resource
                label = "Location",
                value = location,
                trailingIcon = IconPack.Medium.Thin.Outline.FolderSearch,
                onClick = onLocationClick,
                modifier = Modifier.testTag(FILE_INFO_LOCATION_TAG),
            )
        }

        uiState.mapCoordinates?.let { coordinates ->
            FileInfoMapView(
                coordinates = coordinates,
                caption = uiState.locationCaption,
            )
        }

        if (uiState.canEditDescription || uiState.descriptionText.isNotBlank()) {
            NodeDescriptionField(
                description = uiState.descriptionText,
                isEditable = uiState.canEditDescription,
                // TODO extract to localized string resources
                label = "Description",
                placeholder = "Add description",
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

@Composable
private fun FileInfoLoading(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .testTag(FILE_INFO_LOADING_TAG)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp)
                .shimmerEffect(shape = RoundedCornerShape(16.dp)),
        )
        Box(
            modifier = Modifier
                .width(200.dp)
                .height(24.dp)
                .shimmerEffect(),
        )
    }
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
                fileTypeExtension = "pdf",
                sizeInBytes = 10L * 1024 * 1024,
                creationTime = 1_749_000_000L,
                modificationTime = 1_749_500_000L,
                nodeSourceType = NodeSourceType.CLOUD_DRIVE,
                locationFolders = listOf("Documents", "Marketing"),
                descriptionText = "This is test description",
                tags = listOf("marketing", "2024", "confidential"),
                accessPermission = AccessPermission.OWNER
            ),
            nodeHandle = 0L,
            onBack = {},
            onLocationClick = {},
            onNavigate = {},
            onDescriptionChange = {},
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
            ),
            nodeHandle = 0L,
            onBack = {},
            onLocationClick = {},
            onNavigate = {},
            onDescriptionChange = {},
        )
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
            ),
            nodeHandle = 0L,
            onBack = {},
            onLocationClick = {},
            onNavigate = {},
            onDescriptionChange = {},
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@CombinedThemePreviews
@Composable
private fun FileInfoScreenLoadingPreview() {
    AndroidThemeForPreviews {
        FileInfoScreen(
            uiState = FileInfoUiState(isLoading = true),
            nodeHandle = 0L,
            onBack = {},
            onLocationClick = {},
            onNavigate = {},
            onDescriptionChange = {},
        )
    }
}

internal const val FILE_INFO_SCREEN_TAG = "file_info_screen:scaffold"
internal const val FILE_INFO_APP_BAR_TAG = "file_info_screen:app_bar"
internal const val FILE_INFO_HEADER_TAG = "file_info_screen:header"
internal const val FILE_INFO_NAME_TAG = "file_info_screen:name"
internal const val FILE_INFO_SUBTITLE_TAG = "file_info_screen:subtitle"
internal const val FILE_INFO_ADDED_TAG = "file_info_screen:added"
internal const val FILE_INFO_LAST_MODIFIED_TAG = "file_info_screen:last_modified"
internal const val FILE_INFO_LOCATION_TAG = "file_info_screen:location"
internal const val FILE_INFO_SHARED_WITH_TAG = "file_info_screen:shared_with"
internal const val FILE_INFO_OWNER_TAG = "file_info_screen:owner"
internal const val FILE_INFO_PERMISSIONS_TAG = "file_info_screen:permissions"
internal const val FILE_INFO_LOADING_TAG = "file_info_screen:loading"
internal const val FILE_INFO_DESCRIPTION_TAG = "file_info_screen:description"
internal const val FILE_INFO_TAGS_TAG = "file_info_screen:tags"
