package mega.privacy.android.feature.photos.presentation.albums.importlink

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import mega.privacy.android.domain.entity.StorageState
import mega.privacy.android.domain.entity.photos.Album
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.photos.model.PhotoUiState

data class AlbumImportState(
    val isNetworkConnected: Boolean = false,
    val isLogin: Boolean = false,
    val showInputDecryptionKeyDialog: Boolean = false,
    val link: String? = null,
    val isLocalAlbumsLoaded: Boolean = false,
    val album: Album.UserAlbum? = null,
    val photos: List<PhotoUiState> = listOf(),
    val selectedPhotos: Set<PhotoUiState> = setOf(),
    val showErrorAccessDialog: Boolean = false,
    val showRenameAlbumDialog: Boolean = false,
    val isRenameAlbumValid: Boolean = false,
    val renameAlbumErrorMessage: String? = null,
    val isImportConstraintValid: Boolean = false,
    val showImportAlbumDialog: Boolean = false,
    val importAlbumMessage: String? = null,
    val isAvailableStorageCollected: Boolean = false,
    val showStorageExceededDialog: Boolean = false,
    val isBackToHome: Boolean = false,
    val downloadEvent: StateEventWithContent<TransferTriggerEvent> = consumed(),
    val folderSubHandle: String? = null,
    val openFileNodeEvent: StateEventWithContent<PhotoUiState> = consumed(),
    val storageState: StorageState = StorageState.Unknown,
    val addToCloudDriveFinishedEvent: StateEvent = consumed,
)
