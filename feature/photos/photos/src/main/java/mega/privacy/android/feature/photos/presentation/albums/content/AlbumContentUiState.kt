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
import mega.privacy.android.feature.photos.presentation.albums.content.model.AlbumContentSelectionAction
import mega.privacy.android.feature.photos.presentation.albums.model.AlbumUiState
import mega.privacy.android.navigation.destination.AlbumContentPreviewNavKey

/**
 * State of the Album Content screen.
 * @property isLoading True if the screen is loading, false otherwise.
 * @property isAddingPhotos True if the screen is adding photos, false otherwise.
 * @property totalAddedPhotos Total added photos.
 * @property isRemovingPhotos True if the screen is removing photos, false otherwise.
 * @property totalRemovedPhotos Total removed photos.
 * @property showRemoveLinkConfirmation True if the remove link confirmation is shown, false otherwise.
 * @property uiAlbum The UIAlbum.
 * @property photos The list of photos.
 * @property selectedPhotos The set of selected photos.
 * @property currentMediaType The current media type.
 * @property accountType The account type.
 * @property isHiddenNodesOnboarded True if the hidden nodes are onboarded, false otherwise.
 * @property isBusinessAccountExpired True if the business account is expired, false otherwise.
 * @property hiddenNodeEnabled True if the hidden node is enabled, false otherwise.
 * @property visibleBottomBarActions The list of visible bottom bar actions based on current selection.
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
    val currentMediaType: FilterMediaType = FilterMediaType.ALL_MEDIA,
    val accountType: AccountType? = null,
    val isHiddenNodesOnboarded: Boolean? = null,
    val isBusinessAccountExpired: Boolean = false,
    val hiddenNodeEnabled: Boolean = false,
    val showProgressMessage: Boolean = false,
    val savePhotosToDeviceEvent: StateEventWithContent<List<TypedNode>> = consumed(),
    val sharePhotosEvent: StateEventWithContent<List<TypedNode>> = consumed(),
    val sendPhotosToChatEvent: StateEventWithContent<List<TypedNode>> = consumed(),
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
    val addMoreItemsEvent: StateEvent = consumed,
    val visibleBottomBarActions: ImmutableList<AlbumContentSelectionAction> = persistentListOf(),
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