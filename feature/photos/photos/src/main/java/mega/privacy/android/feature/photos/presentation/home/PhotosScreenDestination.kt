package mega.privacy.android.feature.photos.presentation.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.android.core.ui.components.MegaText
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.NavigationHandler

@Serializable
data object PhotosNavKey : NavKey

fun EntryProviderScope<NavKey>.photosScreen(
    navigationHandler: NavigationHandler,
    onTransfer: (TransferTriggerEvent) -> Unit,
) {
    entry<PhotosNavKey> {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MegaText(text = "Photos Screen")
        }
    }
}
