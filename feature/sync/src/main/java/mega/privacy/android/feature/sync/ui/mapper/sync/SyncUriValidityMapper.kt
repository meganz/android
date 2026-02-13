package mega.privacy.android.feature.sync.ui.mapper.sync

import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.featuretoggle.DomainFeatures
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsMediaUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.usecase.GetLocalDCIMFolderPathUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.GetFolderPairsUseCase
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Mapper class for validating the URI of a sync folder.
 */
class SyncUriValidityMapper @Inject constructor(
    private val getFolderPairsUseCase: GetFolderPairsUseCase,
    private val getPathByDocumentContentUriUseCase: GetPathByDocumentContentUriUseCase,
    private val getLocalDCIMFolderPathUseCase: GetLocalDCIMFolderPathUseCase,
    private val getPrimaryFolderPathUseCase: GetPrimaryFolderPathUseCase,
    private val getSecondaryFolderPathUseCase: GetSecondaryFolderPathUseCase,
    private val isCameraUploadsEnabledUseCase: IsCameraUploadsEnabledUseCase,
    private val isMediaUploadsEnabledUseCase: IsMediaUploadsEnabledUseCase,
    private val getFeatureFlagValueUseCase: GetFeatureFlagValueUseCase,
) {

    suspend operator fun invoke(documentUri: String): SyncValidityResult {
        runCatching {
            val externalPath =
                getPathByDocumentContentUriUseCase(documentUri)
            externalPath?.let { path ->
                if (path.isEmpty() || path == "/" || path == File.separator) {
                    return SyncValidityResult.Invalid
                }
                val localDCIMFolderPath = getLocalDCIMFolderPathUseCase()
                val primaryFolderPath = getPrimaryFolderPathUseCase()
                val mediaUploadPath = getSecondaryFolderPathUseCase()
                val folderPairs = getFolderPairsUseCase()
                if (checkIfPathIsAlreadySynced(
                        path,
                        localDCIMFolderPath,
                        primaryFolderPath,
                        mediaUploadPath
                    )
                ) {
                    return SyncValidityResult.ShowSnackbar(
                        messageResId = sharedR.string.device_center_new_sync_select_local_device_folder_currently_synced_message
                    )
                }
                val matchDetails = findDetailedMatchingFolderPair(
                    folderPairs,
                    externalPath,
                )

                if (matchDetails != null) {
                    val (matchingFolderPair, _, relationship) = matchDetails // Destructuring declaration
                    val syncType = matchingFolderPair.syncType

                    val snackbarMessage = when (relationship) {
                        PathRelationship.EXACT_MATCH -> {
                            when (syncType) {
                                SyncType.TYPE_BACKUP -> sharedR.string.sync_local_device_folder_currently_backed_up_message
                                else -> sharedR.string.sync_local_device_folder_currently_synced_message
                            }
                        }

                        PathRelationship.LOCAL_CONTAINS_EXTERNAL -> { // externalPath is a subfolder of localPath
                            sharedR.string.general_sync_active_sync_below_path // Message implies "you're trying to sync something *within* an already synced broader folder"
                        }

                        PathRelationship.EXTERNAL_CONTAINS_LOCAL -> { // localPath is a subfolder of externalPath
                            sharedR.string.general_sync_message_folder_backup_issue_due_to_being_inside_another_backed_up_folder
                        }

                        PathRelationship.NO_MATCH -> {
                            null
                        }
                    }
                    snackbarMessage?.let {
                        return SyncValidityResult.ShowSnackbar(messageResId = snackbarMessage)
                    }
                }
                val folderName = extractFolderName(path)
                if (folderName.isEmpty()) {
                    return SyncValidityResult.Invalid
                }
                return SyncValidityResult.ValidFolderSelected(
                    localFolderUri = UriPath(documentUri),
                    folderName = folderName
                )
            }
        }.onFailure {
            Timber.e(it)
            return SyncValidityResult.Invalid
        }
        return SyncValidityResult.Invalid
    }


    private suspend fun checkIfPathIsAlreadySynced(
        path: String,
        localDCIMFolderPath: String,
        primaryFolderPath: String,
        mediaUploadPath: String,
    ): Boolean {
        val isNewDCIMLogicEnabled =
            getFeatureFlagValueUseCase(DomainFeatures.DCIMSelectionAsSyncBackup)

        val isCameraMatch = isCameraUploadsEnabledUseCase() &&
                primaryFolderPath.isNotEmpty() &&
                path.startsWith(primaryFolderPath)

        val isMediaMatch = isMediaUploadsEnabledUseCase() &&
                mediaUploadPath.isNotEmpty() &&
                path.startsWith(mediaUploadPath)

        val isLegacyDCIMMatch = !isNewDCIMLogicEnabled &&
                localDCIMFolderPath.isNotEmpty() &&
                path.startsWith(localDCIMFolderPath)

        return isCameraMatch || isMediaMatch || isLegacyDCIMMatch
    }

    private suspend fun findDetailedMatchingFolderPair(
        folderPairs: List<FolderPair>,
        externalPath: String,
    ): PathMatchDetails? {
        for (folderPair in folderPairs) {
            val localPath = getPathByDocumentContentUriUseCase(folderPair.localFolderPath)
                ?: continue // Skip if path can't be resolved

            val relationship = determinePathRelationship(localPath, externalPath)
            if (relationship != PathRelationship.NO_MATCH) {
                return PathMatchDetails(folderPair, localPath, relationship)
            }
        }
        return null
    }

    private fun determinePathRelationship(
        localPath: String,
        externalPath: String,
    ): PathRelationship {
        val normalizedLocal = localPath.trimEnd('/', File.separatorChar)
        val normalizedExternal = externalPath.trimEnd('/', File.separatorChar)

        return when {
            normalizedLocal == normalizedExternal -> PathRelationship.EXACT_MATCH
            UriPath(normalizedExternal).isSubPathOf(UriPath(normalizedLocal)) -> PathRelationship.LOCAL_CONTAINS_EXTERNAL

            UriPath(normalizedLocal).isSubPathOf(UriPath(normalizedExternal)) -> PathRelationship.EXTERNAL_CONTAINS_LOCAL

            else -> PathRelationship.NO_MATCH
        }
    }

    private fun extractFolderName(path: String): String {
        val normalizedPath = path.trimEnd('/', File.separatorChar)
        if (normalizedPath.isEmpty() || normalizedPath == "/" || normalizedPath == File.separator) {
            return ""
        }

        // Try both forward slash and system separator
        val parts = normalizedPath.split("/", File.separator)
        return parts.lastOrNull() ?: ""
    }
}

private data class PathMatchDetails(
    val folderPair: FolderPair,
    val localPath: String, // Assuming localPath will not be null for a valid match relevant here
    val relationship: PathRelationship,
)

private enum class PathRelationship {
    EXACT_MATCH,
    LOCAL_CONTAINS_EXTERNAL, // externalPath is a subpath of localPath
    EXTERNAL_CONTAINS_LOCAL, // localPath is a subpath of externalPath
    NO_MATCH // Or some other state if localPath can be null
}
