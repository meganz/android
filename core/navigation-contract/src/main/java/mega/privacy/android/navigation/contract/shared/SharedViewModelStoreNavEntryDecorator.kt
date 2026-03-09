package mega.privacy.android.navigation.contract.shared

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.SAVED_STATE_REGISTRY_OWNER_KEY
import androidx.lifecycle.SavedStateViewModelFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.enableSavedStateHandles
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation3.runtime.NavEntryDecorator
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.compose.LocalSavedStateRegistryOwner
import mega.privacy.android.navigation.contract.metadata.NavEntryMetadataScope

/**
 * CompositionLocal providing the shared [ViewModelStoreOwner] set by
 * [SharedViewModelStoreNavEntryDecorator] when the entry's metadata contains a
 * [NavEntryMetadataScope.withSharedViewModelStoreKey] value.
 *
 * `null` when the current entry has no shared ViewModel store configured.
 *
 * @see sharedViewModel
 */
val LocalSharedViewModelStoreOwner =
    staticCompositionLocalOf<ViewModelStoreOwner?> { null }

/**
 * Returns a [SharedViewModelStoreNavEntryDecorator] that is remembered across recompositions.
 *
 * @param [viewModelStoreOwner] The [ViewModelStoreOwner] that provides the [ViewModelStore] to
 *   NavEntries
 * @param [removeViewModelStoreOnPop] A lambda that returns a Boolean for whether the store for a
 *   [NavEntry] should be removed when the [NavEntry] is popped from the backStack. If true, the
 *   entry's ViewModelStore will be removed.
 */
@Composable
fun <T : Any> rememberSharedViewModelStoreNavEntryDecorator(
    viewModelStoreOwner: ViewModelStoreOwner =
        checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        },
    removeViewModelStoreOnPop: () -> Boolean = { true },
): SharedViewModelStoreNavEntryDecorator<T> {
    val currentRemoveViewModelStoreOnPop = rememberUpdatedState(removeViewModelStoreOnPop)
    return remember(viewModelStoreOwner, currentRemoveViewModelStoreOnPop) {
        SharedViewModelStoreNavEntryDecorator(
            viewModelStoreOwner.viewModelStore,
            removeViewModelStoreOnPop,
        )
    }
}

/**
 * Provides each [NavEntry] with its own entry-scoped [ViewModelStoreOwner] exposed through
 * [LocalViewModelStoreOwner]. When the entry's metadata contains a shared store key
 * (via [NavEntryMetadataScope.withSharedViewModelStoreKey] or
 * [NavEntryMetadataScope.provideSharedViewModelScope]), a second [ViewModelStoreOwner] backed
 * by the shared [ViewModelStore] is also provided via [LocalSharedViewModelStoreOwner].
 *
 * This mirrors the Fragment pattern:
 * - `hiltViewModel()` returns an **entry-scoped** ViewModel (like `viewModels()` in Fragment)
 * - [sharedViewModel] returns a **shared-scoped** ViewModel (like `activityViewModels()` in
 *   Fragment)
 *
 * For **cross-module** sharing where the child module cannot reference the parent's NavKey,
 * use the named-scope pattern: the parent calls [NavEntryMetadataScope.provideSharedViewModelScope]
 * and the child calls [NavEntryMetadataScope.withSharedViewModelStoreKey] with the same scope
 * name string. The named scope store is cleaned up when the provider entry is popped.
 *
 * @see [NavEntryMetadataScope.provideSharedViewModelScope]
 * @see [NavEntryMetadataScope.withSharedViewModelStoreKey]
 * @see [sharedViewModel]
 *
 * This requires the usage of [androidx.navigation3.runtime.SaveableStateHolderNavEntryDecorator] to
 * ensure that the [NavEntry] scoped [ViewModel]s can properly provide access to
 * [androidx.lifecycle.SavedStateHandle]s
 *
 * @param [viewModelStore] The [ViewModelStore] that provides to NavEntries
 * @param [removeViewModelStoreOnPop] A lambda that returns a Boolean for whether the store for a
 *   [NavEntry] should be cleared when the [NavEntry] is popped from the backStack. If true, the
 *   entry's ViewModelStore will be removed.
 * @see NavEntryDecorator.onPop for more details on when this callback is invoked
 */
class SharedViewModelStoreNavEntryDecorator<T : Any>(
    viewModelStore: ViewModelStore,
    removeViewModelStoreOnPop: () -> Boolean,
) : NavEntryDecorator<T>(
    onPop = ({ key ->
        if (removeViewModelStoreOnPop()) {
            val evm = viewModelStore.getEntryViewModel()
            evm.clearViewModelStoreOwnerForKey(key)
            evm.clearOwnedScopes(key)
        }
    }),
    decorate = { entry ->
        val entryViewModel = viewModelStore.getEntryViewModel()

        val entryViewModelStore =
            entryViewModel.viewModelStoreForKey(entry.contentKey)

        val sharedContentKey = entry.metadata[SHARED_VIEWMODEL_STORE_KEY]
        val sharedViewModelStore = sharedContentKey?.let {
            entryViewModel.viewModelStoreForKey(it)
        }

        if (entry.metadata[SHARED_VIEWMODEL_SCOPE_OWNER_KEY] == true && sharedContentKey != null) {
            entryViewModel.registerScopeOwner(entry.contentKey, sharedContentKey)
        }

        val savedStateRegistryOwner = LocalSavedStateRegistryOwner.current

        val entryOwner = rememberViewModelStoreOwner(
            viewModelStore = entryViewModelStore,
            savedStateRegistryOwner = savedStateRegistryOwner,
        )

        val sharedOwner: ViewModelStoreOwner? = if (sharedViewModelStore != null) {
            rememberViewModelStoreOwner(
                viewModelStore = sharedViewModelStore,
                savedStateRegistryOwner = savedStateRegistryOwner,
            )
        } else {
            null
        }

        if (sharedOwner != null) {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides entryOwner,
                LocalSharedViewModelStoreOwner provides sharedOwner,
            ) {
                entry.Content()
            }
        } else {
            CompositionLocalProvider(
                LocalViewModelStoreOwner provides entryOwner,
            ) {
                entry.Content()
            }
        }
    },
) {

    companion object {
        internal const val SHARED_VIEWMODEL_STORE_KEY = "shared_viewmodel_store_key"
        internal const val SHARED_VIEWMODEL_SCOPE_OWNER_KEY = "shared_viewmodel_scope_owner_key"

        /**
         * Creates metadata that associates this entry with a shared [ViewModelStore] identified
         * by [contentKey]. The shared store is accessible inside the entry via
         * [sharedViewModel], while regular `hiltViewModel()` calls remain entry-scoped.
         *
         * Prefer [NavEntryMetadataScope.withSharedViewModelStoreKey] or
         * [NavEntryMetadataScope.provideSharedViewModelScope] inside
         * [mega.privacy.android.navigation.contract.metadata.buildMetadata] for new code.
         */
        fun sharedViewModelStoreKey(contentKey: Any) =
            mapOf(SHARED_VIEWMODEL_STORE_KEY to contentKey)
    }

}

/**
 * Registers a shared [ViewModelStore] key so this entry can access [ViewModel]s from the
 * store identified by [contentKey] via [sharedViewModel], while retaining its own entry-scoped
 * store for regular `hiltViewModel()` calls.
 *
 * [contentKey] can be either a parent entry's content key (same-module usage) or a string
 * scope name that matches a [provideSharedViewModelScope] call (cross-module usage).
 *
 * Use inside [mega.privacy.android.navigation.contract.metadata.buildMetadata]:
 * ```
 * metadata = buildMetadata {
 *     withSharedViewModelStoreKey(SharedScopes.PAYMENT_FLOW)
 * }
 * ```
 *
 * @param contentKey The content key or scope name of the shared [ViewModelStore]
 * @see provideSharedViewModelScope
 */
fun NavEntryMetadataScope.withSharedViewModelStoreKey(contentKey: Any) {
    set(SharedViewModelStoreNavEntryDecorator.SHARED_VIEWMODEL_STORE_KEY, contentKey)
}

/**
 * Marks this entry as the **owner** of a named shared [ViewModelStore] scope and makes that
 * scope available via [LocalSharedViewModelStoreOwner].
 *
 * Use this on the "parent" entry that creates the shared [ViewModel]s. Consumer entries in
 * other modules use [withSharedViewModelStoreKey] with the same [scopeName] to access the
 * shared store without a compile-time dependency on the provider's [NavKey][androidx.navigation3.runtime.NavKey].
 *
 * When this entry is popped, the named scope store is cleaned up automatically.
 *
 * Usage:
 * ```
 * // Provider (parent module):
 * entry<PaymentScreen>(
 *     metadata = buildMetadata {
 *         provideSharedViewModelScope(SharedScopes.PAYMENT_FLOW)
 *     }
 * ) {
 *     val sharedVm = sharedViewModel<PaymentFlowViewModel>()
 *     val localVm = hiltViewModel<PaymentScreenViewModel>()
 * }
 *
 * // Consumer (child module — no dependency on parent):
 * entry<PaymentDetailsScreen>(
 *     metadata = buildMetadata {
 *         withSharedViewModelStoreKey(SharedScopes.PAYMENT_FLOW)
 *     }
 * ) {
 *     val sharedVm = sharedViewModel<PaymentFlowViewModel>()  // same instance
 *     val localVm = hiltViewModel<PaymentDetailsViewModel>()
 * }
 * ```
 *
 * @param scopeName A string identifier for the scope. Both provider and consumer must use the
 *   same value, typically defined as a constant in a shared module.
 */
fun NavEntryMetadataScope.provideSharedViewModelScope(scopeName: String) {
    set(SharedViewModelStoreNavEntryDecorator.SHARED_VIEWMODEL_STORE_KEY, scopeName)
    set(SharedViewModelStoreNavEntryDecorator.SHARED_VIEWMODEL_SCOPE_OWNER_KEY, true)
}

/**
 * Returns a `@HiltViewModel`-annotated [ViewModel] scoped to the shared [ViewModelStore]
 * provided by [SharedViewModelStoreNavEntryDecorator].
 *
 * This is analogous to `activityViewModels()` in the Fragment world: the ViewModel lives in the
 * shared store (typically owned by a parent entry) and survives until that parent entry is popped.
 * Meanwhile, a regular `hiltViewModel()` call returns an entry-scoped ViewModel that is cleaned up
 * when the current entry is popped.
 *
 * Usage:
 * ```
 * // Provider (parent):
 * entry<ParentScreen>(
 *     metadata = buildMetadata {
 *         provideSharedViewModelScope(SharedScopes.MY_FLOW)
 *     },
 * ) {
 *     val sharedVm = sharedViewModel<SharedViewModel>()  // shared-scoped
 *     val localVm = hiltViewModel<ParentViewModel>()     // entry-scoped
 * }
 *
 * // Consumer (child — can be in a different module):
 * entry<ChildScreen>(
 *     metadata = buildMetadata {
 *         withSharedViewModelStoreKey(SharedScopes.MY_FLOW)
 *     },
 * ) {
 *     val sharedVm = sharedViewModel<SharedViewModel>()  // same instance
 *     val localVm = hiltViewModel<ChildViewModel>()      // entry-scoped
 * }
 * ```
 *
 * @throws IllegalStateException if no shared [ViewModelStoreOwner] is available (i.e. the
 *   entry's metadata does not include [withSharedViewModelStoreKey] or
 *   [provideSharedViewModelScope])
 */
@Composable
inline fun <reified VM : ViewModel> sharedViewModel(
    key: String? = null,
): VM {
    val owner = checkNotNull(LocalSharedViewModelStoreOwner.current) {
        "No shared ViewModelStoreOwner found. " +
                "Set withSharedViewModelStoreKey() in the entry metadata."
    }
    return hiltViewModel(
        viewModelStoreOwner = owner,
        key = key,
    )
}

@Composable
private fun rememberViewModelStoreOwner(
    viewModelStore: ViewModelStore,
    savedStateRegistryOwner: SavedStateRegistryOwner,
): ViewModelStoreOwner = remember {
    NavEntryViewModelStoreOwner(
        viewModelStore = viewModelStore,
        delegate = savedStateRegistryOwner,
        enableSavedState = true,
    )
}

private class NavEntryViewModelStoreOwner(
    override val viewModelStore: ViewModelStore,
    delegate: SavedStateRegistryOwner,
    enableSavedState: Boolean,
) : ViewModelStoreOwner,
    SavedStateRegistryOwner by delegate,
    HasDefaultViewModelProviderFactory {

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = SavedStateViewModelFactory()

    override val defaultViewModelCreationExtras: CreationExtras
        get() =
            MutableCreationExtras().also {
                it[SAVED_STATE_REGISTRY_OWNER_KEY] = this
                it[VIEW_MODEL_STORE_OWNER_KEY] = this
            }

    init {
        if (enableSavedState) {
            require(this.lifecycle.currentState == Lifecycle.State.INITIALIZED) {
                "The Lifecycle state is already beyond INITIALIZED. The " +
                        "SharedViewModelStoreNavEntryDecorator requires adding the " +
                        "SavedStateNavEntryDecorator to ensure support for " +
                        "SavedStateHandles."
            }
            enableSavedStateHandles()
        }
    }
}

private class EntryViewModel : ViewModel() {
    private val owners = mutableMapOf<Any, ViewModelStore>()
    private val scopeOwners = mutableMapOf<Any, Any>()

    fun viewModelStoreForKey(key: Any): ViewModelStore = owners.getOrPut(key) { ViewModelStore() }

    fun clearViewModelStoreOwnerForKey(key: Any) {
        owners.remove(key)?.clear()
    }

    fun registerScopeOwner(contentKey: Any, scopeKey: Any) {
        scopeOwners[contentKey] = scopeKey
    }

    fun clearOwnedScopes(contentKey: Any) {
        scopeOwners.remove(contentKey)?.let { scopeKey ->
            owners.remove(scopeKey)?.clear()
        }
    }

    override fun onCleared() {
        owners.forEach { (_, store) -> store.clear() }
        scopeOwners.clear()
    }
}


private fun ViewModelStore.getEntryViewModel(): EntryViewModel {
    val provider =
        ViewModelProvider.create(
            store = this,
            factory = viewModelFactory { initializer { EntryViewModel() } },
        )
    return provider[EntryViewModel::class]
}
