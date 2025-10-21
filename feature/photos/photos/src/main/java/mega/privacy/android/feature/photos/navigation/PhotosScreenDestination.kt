package mega.privacy.android.feature.photos.navigation

import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.feature.photos.presentation.MediaMainRoute
import mega.privacy.android.navigation.contract.NavigationHandler

@Serializable
data object PhotosNavKey : NavKey

fun EntryProviderScope<NavKey>.photosScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<PhotosNavKey> {
        MediaMainRoute()
    }
}
