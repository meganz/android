package mega.privacy.android.app.presentation.psa

import androidx.compose.runtime.DisposableEffect
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable
import mega.privacy.android.app.presentation.psa.model.PsaState
import mega.privacy.android.app.presentation.psa.view.InfoPsaScreen
import mega.privacy.android.app.presentation.psa.view.StandardPsaScreen
import mega.privacy.android.app.presentation.psa.view.WebPsaScreen
import mega.privacy.android.navigation.contract.bottomsheet.bottomSheetMetadata
import mega.privacy.android.navigation.contract.transparent.transparentMetadata
import mega.privacy.android.navigation.destination.WebSiteNavKey

@Serializable
data class StandardPsaBottomSheet(val psa: PsaState.StandardPsa) : NavKey

@Serializable
data class InfoPsaBottomSheet(val psa: PsaState.InfoPsa) : NavKey

@Serializable
data class WebPsaScreen(val psa: PsaState.WebPsa) : NavKey

internal fun EntryProviderScope<NavKey>.standardPsaBottomSheetDestination(
    onNavigate: (NavKey) -> Unit,
    closePsaScreen: (NavKey) -> Unit,
    removeDestination: (NavKey) -> Unit,
) {
    entry<StandardPsaBottomSheet>(
        metadata = bottomSheetMetadata(
            dismissOnBack = false,
            dismissOnOutsideClick = false
        )
    ) { key ->
        val viewModel = hiltViewModel<PsaScreenViewModel>()
        StandardPsaScreen(
            state = key.psa,
            markAsSeen = {
                viewModel.markAsSeen(it)
                closePsaScreen(key)
            },
            navigateToPsaPage = { url -> onNavigate(WebSiteNavKey(url)) }
        )

        DisposableEffect(key) {
            onDispose { removeDestination(key) }
        }
    }
}

internal fun EntryProviderScope<NavKey>.infoPsaBottomSheetDestination(
    closePsaScreen: (NavKey) -> Unit,
    removeDestination: (NavKey) -> Unit,
) {
    entry<InfoPsaBottomSheet>(
        metadata = bottomSheetMetadata(
            dismissOnBack = false,
            dismissOnOutsideClick = false
        )
    ) { key ->
        val viewModel = hiltViewModel<PsaScreenViewModel>()
        InfoPsaScreen(
            state = key.psa,
            markAsSeen = {
                viewModel.markAsSeen(it)
                closePsaScreen(key)
            },
        )

        DisposableEffect(key) {
            onDispose { removeDestination(key) }
        }
    }
}

internal fun EntryProviderScope<NavKey>.webPsaDestination(
    closePsaScreen: (NavKey) -> Unit,
    removeDestination: (NavKey) -> Unit,
) {
    entry<WebPsaScreen>(metadata = transparentMetadata()) { key ->
        val viewModel = hiltViewModel<PsaScreenViewModel>()
        WebPsaScreen(
            state = key.psa,
            markAsSeen = {
                viewModel.markAsSeen(it)
                closePsaScreen(key)
            },
        )

        DisposableEffect(key) {
            onDispose { removeDestination(key) }
        }
    }
}