package mega.privacy.android.app.appstate.content.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.ContentTransform
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.ui.NavDisplay
import mega.android.core.ui.components.LocalSnackBarHostState
import mega.android.core.ui.components.snackbar.SnackbarLifetimeController
import mega.privacy.android.app.appstate.content.transfer.AppTransferViewModel
import mega.privacy.android.app.presentation.transfers.starttransfer.view.StartTransferComponent
import mega.privacy.android.domain.entity.transfer.event.TransferTriggerEvent
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.NavigationHandler
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.bottomsheet.BottomSheetSceneStrategy
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import mega.privacy.android.navigation.contract.shared.rememberSharedViewModelStoreNavEntryDecorator
import mega.privacy.android.navigation.contract.transition.fadeTransition
import kotlin.reflect.KClass

/**
 * Shared scaffolding for activities that host their own back stack outside the
 * [mega.privacy.android.app.appstate.MegaActivity] single-activity shell. Wraps the body in
 * [container], builds a persisted [NavigationHandler] via [rememberLegacyActivityNavigation],
 * sets up snackbar/transfer/dialog/bottom-sheet plumbing, and renders a [NavDisplay] that
 * registers [featureDestinations] (minus [excludeOwnDestination]) and [appDialogDestinations]
 * alongside the activity's own entries from [entryContent].
 *
 * @param container wraps the scaffold body. Typically a `MegaAppContainer` or a custom container
 * stack.
 * @param initialKey root destination used as the back stack root.
 * @param navigationResultManager shared result bus used by [NavigationHandler].
 * @param featureDestinations feature graphs to register alongside the activity's own entries.
 * @param appDialogDestinations app-level dialog graphs to register.
 * @param onEmptyBackStack invoked when a back op would leave the back stack empty.
 * @param excludeOwnDestination feature destination class whose `navigationGraph` should NOT be
 * registered — the activity provides its own entry in [entryContent].
 * @param overlayContent composed inside the [LocalSnackBarHostState] provider, after (i.e. on
 * top of) the `NavDisplay`. Use it for activity-level side-effects that need the snackbar host,
 * or to render UI that should overlay the navigation content (e.g. a snackbar host).
 * @param transitionSpec transition used for forward navigation. Defaults to [fadeTransition].
 * @param popTransitionSpec transition used for back/pop navigation. Defaults to [fadeTransition].
 * @param entryContent block that registers the activity's own `entry<...>` blocks; receives
 * the shared [NavigationHandler] and [TransferHandler].
 */
@Composable
fun LegacyActivityScaffold(
    container: @Composable (content: @Composable () -> Unit) -> Unit,
    initialKey: NavKey,
    navigationResultManager: NavigationResultManager,
    featureDestinations: Set<FeatureDestination> = emptySet(),
    appDialogDestinations: Set<AppDialogDestinations> = emptySet(),
    onEmptyBackStack: () -> Unit = {},
    excludeOwnDestination: KClass<out FeatureDestination>? = null,
    overlayContent: @Composable () -> Unit = {},
    transitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = { fadeTransition },
    popTransitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform = { fadeTransition },
    entryContent: EntryProviderScope<NavKey>.(NavigationHandler, TransferHandler) -> Unit,
) {
    container {
        val (backStack, navigationHandler) = rememberLegacyActivityNavigation(
            initialKey = initialKey,
            navigationResultManager = navigationResultManager,
            onEmptyBackStack = onEmptyBackStack,
        )
        // Activity-scoped (composed at the scaffold root, outside any NavDisplay entry) so
        // transfer events outlive individual destinations.
        val appTransferViewModel = hiltViewModel<AppTransferViewModel>()
        val transferState by appTransferViewModel.state.collectAsStateWithLifecycle()
        val transferHandler = remember(appTransferViewModel) {
            object : TransferHandler {
                override fun setTransferEvent(event: TransferTriggerEvent) {
                    appTransferViewModel.setTransferEvent(event)
                }
            }
        }
        val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }
        val bottomSheetStrategy = remember { BottomSheetSceneStrategy<NavKey>() }
        val snackbarHostState = remember { SnackbarHostState() }

        CompositionLocalProvider(LocalSnackBarHostState provides snackbarHostState) {
            SnackbarLifetimeController()
            NavDisplay(
                backStack = backStack,
                onBack = { navigationHandler.back() },
                sceneStrategies = listOf(dialogStrategy, bottomSheetStrategy),
                entryDecorators = listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    rememberSharedViewModelStoreNavEntryDecorator(),
                ),
                transitionSpec = transitionSpec,
                popTransitionSpec = popTransitionSpec,
                predictivePopTransitionSpec = { popTransitionSpec() },
                entryProvider = entryProvider {
                    entryContent(navigationHandler, transferHandler)
                    featureDestinations
                        .filterNot { destination ->
                            excludeOwnDestination?.isInstance(destination) == true
                        }
                        .forEach { destination ->
                            destination.navigationGraph(
                                this,
                                navigationHandler,
                                transferHandler,
                            )
                        }
                    appDialogDestinations.forEach { destination ->
                        destination.navigationGraph(
                            this,
                            navigationHandler,
                            {},
                        )
                    }
                },
            )
            StartTransferComponent(
                event = transferState.transferEvent,
                onConsumeEvent = appTransferViewModel::consumedTransferEvent,
            )
            overlayContent()
        }
    }
}
