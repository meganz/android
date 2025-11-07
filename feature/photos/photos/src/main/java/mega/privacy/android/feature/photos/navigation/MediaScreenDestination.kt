package mega.privacy.android.feature.photos.navigation

import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.photos.presentation.MediaMainRoute
import mega.privacy.android.feature.photos.presentation.albums.content.AlbumContentScreen
import mega.privacy.android.feature.photos.presentation.albums.content.AlbumContentViewModel
import mega.privacy.android.navigation.contract.NavigationHandler

@Serializable
data object MediaMainNavKey : NavKey

fun EntryProviderScope<NavKey>.mediaMainRoute(
    navigationHandler: NavigationHandler,
) {
    entry<MediaMainNavKey> {
        MediaMainRoute(navigateToAlbumContent = navigationHandler::navigate)
    }
}

@Serializable
data class AlbumContentNavKey(val id: Long?, val type: String?) : NavKey

fun EntryProviderScope<NavKey>.albumContentScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit
) {
    entry<AlbumContentNavKey> { args ->
        val viewModel = hiltViewModel<AlbumContentViewModel, AlbumContentViewModel.Factory>(
            creationCallback = { it.create(args) }
        )
        AlbumContentScreen(
            navigationHandler = navigationHandler,
            onTransfer = onTransfer,
            viewModel = viewModel
        )
    }
}
