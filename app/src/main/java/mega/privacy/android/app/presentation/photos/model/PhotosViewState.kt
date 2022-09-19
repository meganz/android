package mega.privacy.android.app.presentation.photos.model

data class PhotosViewState(
    val tabs: List<PhotosTab> = PhotosTab.values().asList(),
    val selectedTab: PhotosTab = PhotosTab.Timeline,
    val isMenuShowing: Boolean = true,
)