package mega.privacy.android.feature.photos.presentation.albums.content

import de.palm.composestateevents.StateEvent
import de.palm.composestateevents.StateEventWithContent
import de.palm.composestateevents.consumed
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import mega.privacy.android.domain.entity.AccountType
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.entity.media.MediaAlbum
import mega.privacy.android.domain.entity.node.SortDirection
import mega.privacy.android.domain.entity.node.TypedNode
import mega.privacy.android.domain.entity.photos.AlbumId
import mega.privacy.android.feature.photos.model.AlbumSortConfiguration
import mega.privacy.android.feature.photos.model.AlbumSortOption
import mega.privacy.android.feature.photos.model.FilterMediaType
import mega.privacy.android.feature.photos.model.PhotoUiState
import mega.privacy.android.feature.photos.model.Sort
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.navigation.destination.AlbumContentPreviewNavKey

/**
 * State of the Album Content screen.
 * @property isLoading True if the screen is loading, false otherwise.
 * @property isAddingPhotos True if the screen is adding photos, false otherwise.
 * @property totalAddedPhotos Total added photos.
 * @property isDeleteAlbum True if the screen is deleting the album, false otherwise.
 * @property isRemovingPhotos True if the screen is removing photos, false otherwise.
 * @property totalRemovedPhotos Total removed photos.
 * @property showRemoveLinkConfirmation True if the remove link confirmation is shown, false otherwise.
 * @property isLinkRemoved True if the link is removed, false otherwise.
 * @property uiAlbum The UIAlbum.
 * @property photos The list of photos.
 * @property selectedPhotos The set of selected photos.
 * @property currentSort The current sort.
 * @property currentMediaType The current media type.
 * @property snackBarMessage The snackbar message.
 * @property showRenameDialog True if the rename dialog is shown, false otherwise.
 * @property showSortByDialog True if the sort by dialog is shown, false otherwise.
 * @property showFilterDialog True if the filter dialog is shown, false otherwise.
 * @property showRemovePhotosDialog True if the remove photos dialog is shown, false otherwise.
 * @property isInputNameValid True if the input name is valid, false otherwise.
 * @property createDialogErrorMessage The create dialog error message.
 * @property newAlbumTitleInput The new album title input.
 * @property accountType The account type.
 * @property isHiddenNodesOnboarded True if the hidden nodes are onboarded, false otherwise.
 * @property isBusinessAccountExpired True if the business account is expired, false otherwise.
 * @property hiddenNodeEnabled True if the hidden node is enabled, false otherwise.
 */
data class AlbumContentUiState(
    val isLoading: Boolean = true,
    val isAddingPhotos: Boolean = false,
    val totalAddedPhotos: Int = 0,
    val isRemovingPhotos: Boolean = false,
    val totalRemovedPhotos: Int = 0,
    val uiAlbum: AlbumUiState? = null,
    val photos: ImmutableList<PhotoUiState> = persistentListOf(),
    val selectedPhotos: ImmutableSet<PhotoUiState> = persistentSetOf(),
    val currentSort: Sort = Sort.NEWEST,
    val currentMediaType: FilterMediaType = FilterMediaType.ALL_MEDIA,
    val snackBarMessage: String = "",
    val showRenameDialog: Boolean = false,
    val showSortByDialog: Boolean = false,
    val showFilterDialog: Boolean = false,
    val showRemovePhotosDialog: Boolean = false,
    val isInputNameValid: Boolean = true,
    val createDialogErrorMessage: Int? = null,
    val newAlbumTitleInput: String = "",
    val accountType: AccountType? = null,
    val isHiddenNodesOnboarded: Boolean? = null,
    val isBusinessAccountExpired: Boolean = false,
    val hiddenNodeEnabled: Boolean = false,
    val showProgressMessage: Boolean = false,
    val savePhotosToDeviceEvent: StateEventWithContent<List<TypedNode>> = consumed(),
    val sharePhotosEvent: StateEventWithContent<List<TypedNode>> = consumed(),
    val sendPhotosToChatEvent: StateEventWithContent<List<TypedNode>> = consumed(),
    val hidePhotosEvent: StateEventWithContent<List<TypedNode>> = consumed(),
    val deleteAlbumSuccessEvent: StateEvent = consumed,
    val showDeleteAlbumConfirmation: StateEvent = consumed,
    val updateAlbumNameErrorMessage: StateEventWithContent<String> = consumed(),
    val showUpdateAlbumName: StateEvent = consumed,
    val themeMode: ThemeMode = ThemeMode.System,
    val showRemoveLinkConfirmation: StateEvent = consumed,
    val linkRemovedSuccessEvent: StateEvent = consumed,
    val paywallEvent: StateEvent = consumed,
    val manageLinkEvent: StateEventWithContent<ManageLinkEvent?> = consumed(),
    val selectAlbumCoverEvent: StateEventWithContent<AlbumId?> = consumed(),
    val albumSortConfiguration: AlbumSortConfiguration = AlbumSortConfiguration(
        sortOption = AlbumSortOption.Modified,
        sortDirection = SortDirection.Descending
    ),
    val previewAlbumContentEvent: StateEventWithContent<AlbumContentPreviewNavKey> = consumed(),
    val addMoreItemsEvent: StateEvent = consumed
) {
    val isAddingPhotosProgressCompleted: Boolean
        get() = !isAddingPhotos && totalAddedPhotos > 0
    val isRemovingPhotosProgressCompleted: Boolean
        get() = !isRemovingPhotos && totalRemovedPhotos > 0
}

data class ManageLinkEvent(
    val album: MediaAlbum.User,
    val hasSensitiveContent: Boolean
)