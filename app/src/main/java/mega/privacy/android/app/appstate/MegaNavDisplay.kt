package mega.privacy.android.app.appstate

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.scene.DialogSceneStrategy
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.ui.NavDisplay
import de.palm.composestateevents.NavigationEventEffect
import mega.privacy.android.analytics.decorator.rememberAnalyticNavEntryDecorator
import mega.privacy.android.app.appstate.content.model.NavigationGraphState
import mega.privacy.android.app.appstate.content.navigation.PendingBackStack
import mega.privacy.android.app.appstate.content.navigation.PendingBackStackNavigationHandler
import mega.privacy.android.app.appstate.global.event.QueueEventViewModel
import mega.privacy.android.app.presentation.login.LoginViewModel
import mega.privacy.android.app.presentation.login.loginEntryProvider
import mega.privacy.android.app.presentation.logout.LogoutConfirmationDialog
import mega.privacy.android.core.passcode.presentation.navigation.passcodeView
import mega.privacy.android.navigation.contract.TransferHandler
import mega.privacy.android.navigation.contract.bottomsheet.BottomSheetSceneStrategy
import mega.privacy.android.navigation.contract.queue.NavigationQueueEvent
import mega.privacy.android.navigation.contract.queue.QueueEvent
import mega.privacy.android.navigation.contract.queue.dialog.AppDialogEvent
import mega.privacy.android.navigation.contract.shared.rememberSharedViewModelStoreNavEntryDecorator
import mega.privacy.android.navigation.contract.suppression.OverlaySuppressionState
import mega.privacy.android.navigation.contract.suppression.rememberOverlaySuppressionNavEntryDecorator
import mega.privacy.android.navigation.contract.transition.fadeTransition
import mega.privacy.android.navigation.contract.transparent.TransparentSceneStrategy

/**
 * Mega nav display
 *
 * @param backStack
 * @param navigationHandler
 * @param graphstate
 * @param transferHandler
 * @param loginViewModel
 * @param emitNavigationEvent
 * @param onFinish
 */
@Composable
internal fun MegaNavDisplay(
    backStack: PendingBackStack<NavKey>,
    navigationHandler: PendingBackStackNavigationHandler,
    graphstate: NavigationGraphState.Data,
    transferHandler: TransferHandler,
    loginViewModel: LoginViewModel,
    emitNavigationEvent: suspend (QueueEvent) -> Unit,
    onFinish: () -> Unit,
) {
    val transparentStrategy = remember { TransparentSceneStrategy<NavKey>() }

    val suppressionState = remember { OverlaySuppressionState() }
    val deferredEvents = remember { mutableStateListOf<QueueEvent>() }
    val isSuppressing by suppressionState.isSuppressing.collectAsStateWithLifecycle()

    val dialogStrategy = remember { DialogSceneStrategy<NavKey>() }
    val bottomSheetStrategy = remember { BottomSheetSceneStrategy<NavKey>() }
    val navigationEventViewModel = hiltViewModel<QueueEventViewModel>()
    val navigationEvents by navigationEventViewModel.navigationEvents.collectAsStateWithLifecycle()

    NavDisplay(
        backStack = backStack,
        onBack = { navigationHandler.back() },
        sceneStrategy = transparentStrategy.chain(dialogStrategy)
            .chain(bottomSheetStrategy),
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberSharedViewModelStoreNavEntryDecorator(),
            rememberAnalyticNavEntryDecorator(),
            rememberOverlaySuppressionNavEntryDecorator(
                suppressionState
            ),
        ),
        entryProvider = entryProvider {
            graphstate.featureDestinations
                .forEach { destination ->
                    destination.navigationGraph(
                        this,
                        navigationHandler,
                        transferHandler
                    )
                }

            graphstate.appDialogDestinations.forEach { destination ->
                destination.navigationGraph(
                    this,
                    navigationHandler,
                    navigationEventViewModel::eventHandled
                )
            }

            loginEntryProvider(
                navigationHandler = navigationHandler,
                loginViewModel = loginViewModel,
                onFinish = onFinish
            )

            passcodeView(
                logoutConfirmationDialog = { onDismissed ->
                    LogoutConfirmationDialog(onDismissed = onDismissed)
                },
            )
        },
        transitionSpec = { fadeTransition },
        popTransitionSpec = { fadeTransition },
        predictivePopTransitionSpec = { fadeTransition }
    )


    NavigationEventEffect(
        event = navigationEvents,
        onConsumed = navigationEventViewModel::eventDisplayed
    ) { queueEvent: QueueEvent ->

        if (isSuppressing && queueEvent.isSuppressable) {
            deferredEvents.add(queueEvent)
            if (queueEvent is AppDialogEvent) {
                navigationEventViewModel.eventHandled()
            }
        } else {
            when (queueEvent) {
                is NavigationQueueEvent -> {
                    navigationHandler.navigate(
                        queueEvent.keys,
                        queueEvent.navOptions
                    )
                }

                is AppDialogEvent -> {
                    navigationHandler.displayDialog(queueEvent.dialogDestination)
                }
            }
        }
    }

    LaunchedEffect(isSuppressing) {
        if (!isSuppressing && deferredEvents.isNotEmpty()) {
            val events = deferredEvents.toList()
            deferredEvents.clear()
            events.forEach { event ->
                emitNavigationEvent(event)
            }
        }
    }
}

private infix fun <T : Any> SceneStrategy<T>.chain(sceneStrategy: SceneStrategy<T>): SceneStrategy<T> =
    SceneStrategy { entries ->
        this@chain.run { calculateScene(entries) } ?: with(sceneStrategy) {
            calculateScene(
                entries
            )
        }
    }