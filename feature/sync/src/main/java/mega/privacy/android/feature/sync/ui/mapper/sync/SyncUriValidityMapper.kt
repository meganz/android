package mega.privacy.android.feature.sync.ui.mapper.sync

import mega.privacy.android.domain.entity.sync.SyncType
import mega.privacy.android.domain.entity.uri.UriPath
import mega.privacy.android.domain.usecase.camerauploads.GetPrimaryFolderPathUseCase
import mega.privacy.android.domain.usecase.camerauploads.GetSecondaryFolderPathUseCase
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
) {

    suspend operator fun invoke(documentUri: String): SyncUriValidityResult {
        val externalPath =
            getPathByDocumentContentUriUseCase(documentUri)
        val localDCIMFolderPath = getLocalDCIMFolderPathUseCase()
        val primaryFolderPathUseCase = getPrimaryFolderPathUseCase().removeSuffix("/")
        val mediaUploadPath = getSecondaryFolderPathUseCase().removeSuffix("/")
        val folderPairs = getFolderPairsUseCase()
        externalPath?.let { path ->
            Timber.d("ExternalPath: $path and localDCIMFolderPath: $localDCIMFolderPath MediaUploadsPath $mediaUploadPath")
            if ((localDCIMFolderPath.isNotEmpty() && path.contains(localDCIMFolderPath))
                || (mediaUploadPath.isNotEmpty() && path.contains(mediaUploadPath))
                || (primaryFolderPathUseCase.isNotEmpty() && path.contains(primaryFolderPathUseCase))
            ) {
                return SyncUriValidityResult.ShowSnackbar(
                    messageResId = sharedR.string.device_center_new_sync_select_local_device_folder_currently_synced_message
                )
            }
            runCatching {
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
                            Timber.d("No match found for the selected folder")
                            null
                        }
                    }
                    snackbarMessage?.let {
                        return SyncUriValidityResult.ShowSnackbar(messageResId = snackbarMessage)
                    }
                }
            }.onFailure {
                Timber.e(it)
                return SyncUriValidityResult.Invalid
            }

            val folderName = path.split(File.separator).last()
            Timber.d("FolderName: $folderName")
            return SyncUriValidityResult.ValidFolderSelected(
                localFolderUri = UriPath(documentUri),
                folderName = folderName
            )
        }
        return SyncUriValidityResult.Invalid
    }

    private suspend fun findDetailedMatchingFolderPair(
        folderPairs: List<FolderPair>,
        externalPath: String,
    ): PathMatchDetails? {
        for (folderPair in folderPairs) {
            val localPath = getPathByDocumentContentUriUseCase(folderPair.localFolderPath)
                ?: continue // Skip if path can't be resolved

            Timber.d("Comparing Local Path: $localPath with External Path: $externalPath")

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
        return when {
            localPath == externalPath -> PathRelationship.EXACT_MATCH
            localPath.contains(externalPath) -> PathRelationship.LOCAL_CONTAINS_EXTERNAL // externalPath is "under" localPath
            externalPath.contains(localPath) -> PathRelationship.EXTERNAL_CONTAINS_LOCAL // localPath is "under" externalPath
            else -> PathRelationship.NO_MATCH
        }
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
