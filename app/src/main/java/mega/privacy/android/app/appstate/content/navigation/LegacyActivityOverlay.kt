package mega.privacy.android.app.appstate.content.navigation

import android.app.Activity
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.navigation3.runtime.NavKey
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.domain.usecase.featureflag.GetFeatureFlagValueUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import javax.inject.Inject

/**
 * Hosts a Compose / Nav3 flow (via [LegacyActivityScaffold]) as a window-level overlay above a
 * legacy Activity or Fragment, so legacy hosts can launch a [FeatureDestination] flow without a
 * dedicated Activity. The counterpart of [LegacyActivityScaffold] for hosts that must keep their
 * own existing UI: it manages the [ComposeView] lifecycle, attaches it to the host activity's decor
 * view, and bridges results back via [collectResult]. Each host should inject its own instance and
 * pair every [show] with cleanup via [hide] (typically from `onDestroy`/`onDestroyView`).
 */
class LegacyActivityOverlay @Inject constructor(
    private val navigationResultManager: NavigationResultManager,
    private val featureDestinations: Set<@JvmSuppressWildcards FeatureDestination>,
    private val appDialogDestinations: Set<@JvmSuppressWildcards AppDialogDestinations>,
) {
    private var overlay: ComposeView? = null
    private var onHidden: (() -> Unit)? = null

    /**
     * Attach the overlay to the host activity's decor view, hosting the flow rooted at [initialKey].
     * No-op if already shown. Clears any stale [resultKey] result so a previous flow's result can't
     * be consumed by this one.
     *
     * @param wrapContent Optional wrapping composable for the overlay content (e.g. Fragment hosts
     *   may provide their own [LocalNavigationEventDispatcherOwner]).
     * @param onHidden Invoked when the overlay is dismissed for any reason (result collected, back
     *   press, empty back stack, explicit [hide] call).
     */
    fun show(
        activity: Activity,
        lifecycleOwner: LifecycleOwner,
        viewModelStoreOwner: ViewModelStoreOwner,
        savedStateRegistryOwner: SavedStateRegistryOwner,
        themeMode: Flow<ThemeMode>,
        initialKey: NavKey,
        resultKey: String,
        wrapContent: @Composable (@Composable () -> Unit) -> Unit = { it() },
        onHidden: () -> Unit = {},
    ) {
        if (overlay != null) return
        this.onHidden = onHidden
        navigationResultManager.clearResult(resultKey)
        val composeView = ComposeView(activity).apply {
            setViewTreeLifecycleOwner(lifecycleOwner)
            setViewTreeViewModelStoreOwner(viewModelStoreOwner)
            setViewTreeSavedStateRegistryOwner(savedStateRegistryOwner)
            // Auto-dispose the composition when the host's lifecycle owner is destroyed,
            // so the overlay can't outlive its lifecycle if cleanup is missed.
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                val theme by themeMode.collectAsStateWithLifecycle(initialValue = ThemeMode.System)
                BackHandler { hide() }
                val scaffold: @Composable () -> Unit = {
                    wrapContent {
                        LegacyActivityScaffold(
                            container = { content ->
                                MegaAppContainer(themeMode = theme, content = content)
                            },
                            initialKey = initialKey,
                            navigationResultManager = navigationResultManager,
                            featureDestinations = featureDestinations,
                            appDialogDestinations = appDialogDestinations,
                            onEmptyBackStack = { hide() },
                        ) { _, _ -> }
                    }
                }
                // A ComposeView attached to the decor view doesn't inherit the activity's
                // NavigationEventDispatcherOwner, so NavDisplay can't handle device back. Provide it
                // from the host activity (Fragment hosts may also supply their own via wrapContent).
                val dispatcherOwner = activity as? NavigationEventDispatcherOwner
                if (dispatcherOwner != null) {
                    CompositionLocalProvider(
                        LocalNavigationEventDispatcherOwner provides dispatcherOwner,
                    ) { scaffold() }
                } else {
                    scaffold()
                }
            }
        }
        // Fade the overlay in so it doesn't pop over the host's existing UI.
        composeView.alpha = 0f
        (activity.window.decorView as ViewGroup).addView(
            composeView,
            FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT,
            ),
        )
        composeView.animate().alpha(1f).setDuration(FADE_DURATION_MS).start()
        overlay = composeView
    }

    /** Fade out, then detach and dispose the overlay if currently shown. */
    fun hide() {
        val view = overlay ?: return
        overlay = null
        val callback = onHidden
        onHidden = null
        view.animate().alpha(0f).setDuration(FADE_DURATION_MS).withEndAction {
            (view.parent as? ViewGroup)?.removeView(view)
            // Removing the view only detaches it; with DisposeOnViewTreeLifecycleDestroyed the
            // composition would otherwise survive and keep its back callbacks registered on the
            // host, hijacking back presses after the overlay is gone. Dispose it explicitly.
            view.disposeComposition()
            callback?.invoke()
        }.start()
    }

    private companion object {
        const val FADE_DURATION_MS = 200L
    }

    /**
     * Collect the [resultKey] result from the hosted flow on the host's STARTED lifecycle. [onResult]
     * is invoked with each non-null emission; the overlay is then hidden and the result cleared from
     * the bus.
     */
    fun <T> collectResult(
        scope: CoroutineScope,
        lifecycleOwner: LifecycleOwner,
        resultKey: String,
        onResult: (T) -> Unit,
    ) {
        scope.launch {
            lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                navigationResultManager
                    .monitorResult<T>(resultKey)
                    .collect { value ->
                        if (value != null) onResult(value)
                        navigationResultManager.clearResult(resultKey)
                        hide()
                    }
            }
        }
    }
}

/**
 * Hilt entry point so non-injectable hosts (e.g. manually constructed handlers) can obtain a
 * [LegacyActivityOverlay] and the use cases needed to drive it from an Android context.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface LegacyActivityOverlayEntryPoint {
    fun legacyActivityOverlay(): LegacyActivityOverlay
    fun monitorThemeModeUseCase(): MonitorThemeModeUseCase
    fun getFeatureFlagValueUseCase(): GetFeatureFlagValueUseCase
}
