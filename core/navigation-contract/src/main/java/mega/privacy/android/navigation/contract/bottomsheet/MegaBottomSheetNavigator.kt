@file:OptIn(ExperimentalMaterial3Api::class)

package mega.privacy.android.navigation.contract.bottomsheet

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.util.fastForEach
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.FloatingWindow
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavOptions
import androidx.navigation.Navigator
import androidx.navigation.NavigatorState
import androidx.navigation.compose.LocalOwnersProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.transform
import kotlinx.coroutines.launch
import mega.android.core.ui.components.sheets.MegaModalBottomSheet
import mega.android.core.ui.components.sheets.MegaModalBottomSheetBackground
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

/**
 * Wrapper class that provides access to the underlying SheetState properties
 * This allows external components to check the sheet's visibility and state
 * without directly accessing the internal SheetState
 */
class BottomSheetNavigatorSheetState(private val sheetState: SheetState) {
    /**
     * @see SheetState.isVisible
     */
    val isVisible: Boolean
        get() = sheetState.isVisible

    /**
     * @see SheetState.currentValue
     */
    val currentValue: SheetValue
        get() = sheetState.currentValue

    /**
     * @see SheetState.targetValue
     */
    val targetValue: SheetValue
        get() = sheetState.targetValue
}

/**
 * Composable function that creates and remembers a MegaBottomSheetNavigator instance
 * This is the main entry point for using the bottom sheet navigator
 *
 * @param skipPartiallyExpanded Whether to skip the partially expanded state
 * @param confirmValueChange Callback to confirm sheet value changes
 * @return MegaBottomSheetNavigator instance
 */
@Composable
fun rememberBottomSheetNavigator(
    skipPartiallyExpanded: Boolean = false,
    confirmValueChange: (SheetValue) -> Boolean = { true },
): MegaBottomSheetNavigator {
    val sheetState = rememberModalBottomSheetState(
        skipPartiallyExpanded = skipPartiallyExpanded,
        confirmValueChange = confirmValueChange
    )

    return remember(sheetState) {
        MegaBottomSheetNavigator(sheetState)
    }
}

/**
 * Main provider component that sets up the bottom sheet navigation system
 *
 * @param megaBottomSheetNavigator The navigator instance
 * @param modifier Modifier for the container
 * @param content The main content that will be shown behind the sheet
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MegaBottomSheetNavigationProvider(
    megaBottomSheetNavigator: MegaBottomSheetNavigator,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier = modifier) {
        megaBottomSheetNavigator.initialize()
        content()
    }

    if (megaBottomSheetNavigator.sheetVisible) {
        MegaModalBottomSheet(
            modifier = Modifier.statusBarsPadding(),
            sheetState = megaBottomSheetNavigator.sheetState,
            onDismissRequest = {
                megaBottomSheetNavigator.onDismissRequest()
            },
            bottomSheetBackground = MegaModalBottomSheetBackground.Surface1
        ) {
            megaBottomSheetNavigator.sheetContent(this)
        }
    }
}

/**
 * Custom Navigator implementation for bottom sheet navigation
 * This extends the standard Navigator to handle bottom sheet-specific navigation logic
 */
@Navigator.Name("bottomSheet")
class MegaBottomSheetNavigator(
    internal val sheetState: SheetState, // The underlying sheet state from Material3
) : Navigator<MegaBottomSheetNavigator.BottomSheetDestination>() {

    /**
     * Controls whether the bottom sheet should be visible
     * This is managed internally by the navigator based on navigation state
     */
    internal var sheetVisible by mutableStateOf(false)
        private set

    /**
     * Tracks whether this navigator has been attached to the navigation system
     * This prevents accessing navigation state before the navigator is ready
     */
    private var attached = false

    /**
     * Cached empty flows to avoid creating new instances on every getter call
     */
    private val emptyBackStackFlow = MutableStateFlow(emptyList<NavBackStackEntry>())
    private val emptyTransitionsFlow = MutableStateFlow(emptySet<NavBackStackEntry>())

    /**
     * Provides access to the current navigation back stack
     * Returns an empty flow if the navigator isn't attached yet to prevent crashes
     *
     * This is used to monitor navigation changes and update the sheet accordingly
     */
    private val navBackStackEntries: StateFlow<List<NavBackStackEntry>>
        get() = if (attached) {
            state.backStack
        } else {
            emptyBackStackFlow
        }

    /**
     * Provides access to navigation transitions in progress
     * Returns an empty flow if the navigator isn't attached yet
     *
     * This is used to mark transitions as complete when the sheet becomes visible
     */
    private val transitionsInProgress: StateFlow<Set<NavBackStackEntry>>
        get() = if (attached) {
            state.transitionsInProgress
        } else {
            emptyTransitionsFlow
        }

    /**
     * Public interface for accessing sheet state properties
     * This allows external components to check sheet visibility without direct access
     */
    val navigatorSheetState: BottomSheetNavigatorSheetState =
        BottomSheetNavigatorSheetState(sheetState)

    /**
     * The content that will be rendered inside the bottom sheet
     * This is set dynamically based on the current navigation entry
     */
    internal var sheetContent: @Composable ColumnScope.() -> Unit = {}

    /**
     * Callback that handles sheet dismissal
     * This is called when the user dismisses the sheet (e.g., by tapping outside)
     */
    internal var onDismissRequest: () -> Unit = {}

    /**
     * Function that handles animated dismissal of the sheet
     * This provides smooth dismissal animation before calling onDismissRequest
     */
    private var animateToDismiss: () -> Unit = {}

    /**
     * Core initialization logic for the bottom sheet navigation system
     * This composable function handles:
     * - Monitoring navigation back stack changes
     * - Managing sheet visibility state
     * - Setting up sheet content and dismissal handlers
     * - Handling back button integration
     */
    internal val initialize: @Composable () -> Unit = {
        val saveableStateHolder = rememberSaveableStateHolder()
        val transitionsInProgressEntries by transitionsInProgress.collectAsStateWithLifecycle()

        /**
         * Tracks the current navigation entry that should be shown in the sheet
         * This entry is "retained" until the sheet is completely hidden to prevent
         * content from disappearing while the sheet is still animating
         *
         * The produceState monitors back stack changes and:
         * 1. Hides the sheet when navigation changes
         * 2. Emits the new entry to be shown
         * 3. Handles cancellation gracefully
         */
        val currentNavEntry by produceState<NavBackStackEntry?>(
            initialValue = null,
            key1 = navBackStackEntries
        ) {
            navBackStackEntries
                .transform { backStackEntries ->
                    val newEntry = backStackEntries.lastOrNull()

                    // Only hide the sheet if we're popping (going from having an entry to no entry)
                    // This prevents the sheet from being hidden when navigating to a new sheet
                    if (value != null && newEntry == null) {
                        try {
                            sheetVisible = false
                        } catch (_: CancellationException) {
                            // Handle cancellation gracefully
                        }
                    }

                    emit(newEntry)
                }
                .collect {
                    value = it
                }
        }

        // Only set up sheet logic if we have a navigation entry to show
        if (currentNavEntry != null) {
            BackHandler {
                animateToDismiss()
            }

            /**
             * Function that marks navigation transitions as complete
             * This is called when the sheet becomes visible to ensure proper
             * navigation state synchronization
             */
            val onCurrentSheetVisible by rememberUpdatedState {
                {
                    runCatching {
                        transitionsInProgressEntries.forEach(state::markTransitionComplete)
                    }.onFailure {
                        Timber.e(it, "MegaBottomSheetNavigator: Error marking transitions complete")
                    }
                }
            }
            val coroutineScope = rememberCoroutineScope()

            /**
             * Monitors sheet visibility changes and marks transitions complete
             * This ensures that navigation transitions are properly completed
             * when the sheet becomes visible
             */
            LaunchedEffect(sheetState, currentNavEntry) {
                snapshotFlow { sheetState.isVisible }
                    .distinctUntilChanged()
                    // Skip first value, we only care about changes
                    .drop(1)
                    .collect { visible ->
                        if (visible) {
                            onCurrentSheetVisible()
                        }
                    }
            }

            /**
             * Sets up the sheet content and dismissal handlers when the retained entry changes
             * This is the main logic that:
             * 1. Enables the sheet
             * 2. Sets up the content rendering function
             * 3. Configures dismissal handlers
             * 4. Sets up back button integration
             */
            LaunchedEffect(key1 = currentNavEntry) {
                sheetVisible = true

                sheetContent = {
                    runCatching {
                        currentNavEntry?.let { entry ->
                            if (!sheetVisible) {
                                return@let
                            }

                            // Use LocalOwnersProvider to ensure proper state restoration
                            entry.LocalOwnersProvider(saveableStateHolder) {
                                val content =
                                    (entry.destination as BottomSheetDestination).content
                                content(entry)
                            }
                        }
                    }.onFailure {
                        Timber.e(
                            it,
                            "MegaBottomSheetNavigator: Error while rendering bottom sheet content: ${currentNavEntry?.destination?.route}, sheetEnabled: $sheetVisible"
                        )
                    }
                }

                onDismissRequest = {
                    sheetVisible = false
                    currentNavEntry?.let { entry ->
                        try {
                            state.pop(popUpTo = entry, saveState = false)
                        } catch (e: IllegalStateException) {
                            // Entry was already destroyed, just hide the sheet
                        }
                    }
                }
                animateToDismiss = {
                    coroutineScope
                        .launch {
                            sheetState.hide()
                        }
                        .invokeOnCompletion {
                            onDismissRequest()
                        }
                }
            }
        } else {
            LaunchedEffect(key1 = Unit) {
                sheetContent = {}
                onDismissRequest = {}
                animateToDismiss = {}
            }
        }
    }

    /**
     * Called when the navigator is attached to the navigation system
     * This marks the navigator as ready to access navigation state
     */
    override fun onAttach(state: NavigatorState) {
        super.onAttach(state)
        attached = true
    }

    /**
     * Creates a new destination for this navigator
     * This is called by the navigation system when creating navigation destinations
     */
    override fun createDestination(): BottomSheetDestination =
        BottomSheetDestination(navigator = this, content = {})

    /**
     * Handles navigation to new destinations
     * This method:
     * 1. Compares current and new entries to determine if dismissal is needed
     * 2. Dismisses current sheet if navigating to a different destination
     * 3. Pushes new entries to the back stack
     */
    override fun navigate(
        entries: List<NavBackStackEntry>,
        navOptions: NavOptions?,
        navigatorExtras: Extras?,
    ) {
        val currentSheetEntry = navBackStackEntries.value.lastOrNull()
        val newSheetEntry = entries.firstOrNull()

        // Compare routes instead of entry objects to prevent infinite loops
        val currentRoute = currentSheetEntry?.destination?.route
        val newRoute = newSheetEntry?.destination?.route

        if (currentRoute != newRoute) {
            onDismissRequest()

            entries.fastForEach { entry ->
                state.push(entry)
            }
        }
        // Don't push the entry if it's the same route to prevent duplicates
    }

    /**
     * Handles popping entries from the back stack
     * This is called by the navigation system when navigating back
     */
    override fun popBackStack(popUpTo: NavBackStackEntry, savedState: Boolean) {
        state.pop(popUpTo, savedState)
    }

    /**
     * Custom navigation destination for bottom sheets
     * This extends NavDestination and implements FloatingWindow to indicate
     * that this destination should be shown as a floating window (bottom sheet)
     */
    @NavDestination.ClassType(Composable::class)
    class BottomSheetDestination(
        navigator: MegaBottomSheetNavigator,
        internal val content: @Composable ColumnScope.(NavBackStackEntry) -> Unit,
    ) : NavDestination(navigator), FloatingWindow
}