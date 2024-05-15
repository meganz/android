package mega.privacy.android.app.presentation.photos.model

/**
 * UI State class of the Photos section
 *
 * @property tabs The list of [PhotosTab]
 * @property selectedTab The selected [PhotosTab]
 * @property isMenuShowing true if the menu is showing
 */
data class PhotosViewState(
    val tabs: List<PhotosTab> = PhotosTab.entries,
    val selectedTab: PhotosTab = PhotosTab.Timeline,
    val isMenuShowing: Boolean = true,
)