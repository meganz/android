package mega.privacy.android.feature.sync.ui.mapper.sync

import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsCameraUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.camerauploads.IsMediaUploadsEnabledUseCase
import mega.privacy.android.domain.usecase.file.GetPathByDocumentContentUriUseCase
import mega.privacy.android.feature.sync.domain.entity.FolderPair
import mega.privacy.android.feature.sync.domain.usecase.GetLocalDCIMFolderPathUseCase
import mega.privacy.android.feature.sync.domain.usecase.sync.GetFolderPairsUseCase
import mega.privacy.android.shared.resources.R as sharedR
import timber.log.Timber
import java.io.File
import javax.inject.Inject

/**
 * Interface for validating the URI of a sync folder.
 */
sealed interface SyncUriValidityResult {
    data class ShowSnackbar(val messageResId: Int) : SyncUriValidityResult
    data class ValidFolderSelected(val localFolderUri: UriPath, val folderName: String) :
        SyncUriValidityResult

    object Invalid : SyncUriValidityResult
}

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
) {

    suspend operator fun invoke(documentUri: String): SyncUriValidityResult {
        runCatching {
            val externalPath =
                getPathByDocumentContentUriUseCase(documentUri)
            externalPath?.let { path ->
                if (path.isEmpty() || path == "/" || path == File.separator) {
                    return SyncUriValidityResult.Invalid
                }
                val localDCIMFolderPath = getLocalDCIMFolderPathUseCase()
                val primaryFolderPathUseCase = getPrimaryFolderPathUseCase().removeSuffix("/")
                val mediaUploadPath = getSecondaryFolderPathUseCase().removeSuffix("/")
                val folderPairs = getFolderPairsUseCase()
                if (checkIfPathIsAlreadySynced(
                        path,
                        localDCIMFolderPath,
                        primaryFolderPathUseCase,
                        mediaUploadPath
                    )
                ) {
                    return SyncUriValidityResult.ShowSnackbar(
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
                        return SyncUriValidityResult.ShowSnackbar(messageResId = snackbarMessage)
                    }
                }
                val folderName = extractFolderName(path)
                if (folderName.isEmpty()) {
                    return SyncUriValidityResult.Invalid
                }
                return SyncUriValidityResult.ValidFolderSelected(
                    localFolderUri = UriPath(documentUri),
                    folderName = folderName
                )
            }
        }.onFailure {
            Timber.e(it)
            return SyncUriValidityResult.Invalid
        }
        return SyncUriValidityResult.Invalid
    }

    private suspend fun checkIfPathIsAlreadySynced(
        path: String,
        localDCIMFolderPath: String,
        primaryFolderPathUseCase: String,
        mediaUploadPath: String,
    ): Boolean {
        return (localDCIMFolderPath.isNotEmpty() && path.contains(localDCIMFolderPath))
                || (isCameraUploadsEnabledUseCase() && (primaryFolderPathUseCase.isNotEmpty() && path.contains(
            primaryFolderPathUseCase
        )))
                || (isMediaUploadsEnabledUseCase() && (mediaUploadPath.isNotEmpty() && path.contains(
            mediaUploadPath
        )))
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
            isSubPath(
                normalizedExternal,
                normalizedLocal
            ) -> PathRelationship.LOCAL_CONTAINS_EXTERNAL

            isSubPath(
                normalizedLocal,
                normalizedExternal
            ) -> PathRelationship.EXTERNAL_CONTAINS_LOCAL

            else -> PathRelationship.NO_MATCH
        }
    }

    private fun isSubPath(childPath: String, parentPath: String): Boolean {
        val normalizedParent = parentPath.trimEnd('/', File.separatorChar)
        return childPath.startsWith("$normalizedParent${File.separator}") ||
                childPath.startsWith("$normalizedParent/")
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
