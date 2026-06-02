package mega.privacy.android.app.appstate.content.navigation

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.activity.ComponentDialog
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.os.bundleOf
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.serialization.NavKeySerializer
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import mega.privacy.android.app.R
import mega.privacy.android.app.appstate.content.navigation.LegacyOverlayDialogFragment.Companion.collectResultAndDismiss
import mega.privacy.android.app.appstate.content.navigation.LegacyOverlayDialogFragment.Companion.dismiss
import mega.privacy.android.app.appstate.content.navigation.LegacyOverlayDialogFragment.Companion.show
import mega.privacy.android.app.presentation.container.MegaAppContainer
import mega.privacy.android.domain.entity.ThemeMode
import mega.privacy.android.domain.usecase.MonitorThemeModeUseCase
import mega.privacy.android.navigation.contract.FeatureDestination
import mega.privacy.android.navigation.contract.dialog.AppDialogDestinations
import javax.inject.Inject

/**
 * Reusable full-screen [DialogFragment] that hosts a Compose / Nav3 flow rooted at
 * [LegacyActivityScaffold]. Intended for cloud-explorer-style flows (share-to-chat, copy, move, …)
 * launched from legacy Activities or Fragments that cannot adopt the single-activity shell.
 *
 * Why a DialogFragment: the host [FragmentManager] owns the fragment's lifecycle and
 * [androidx.savedstate.SavedStateRegistry], so the inner back stack (`rememberSerializable`) and
 * every `rememberSaveable` inside the flow survive configuration changes without any custom
 * persistence plumbing.
 *
 * Use [show]/[dismiss]/[collectResultAndDismiss] from the host. Result delivery still flows
 * through [NavigationResultManager]; the helper collects the result, clears it, and dismisses the
 * overlay so each call site stays a single block.
 */
@AndroidEntryPoint
class LegacyOverlayDialogFragment : DialogFragment() {

    @Inject
    lateinit var navigationResultManager: NavigationResultManager

    @Inject
    lateinit var monitorThemeModeUseCase: MonitorThemeModeUseCase

    @Inject
    lateinit var featureDestinations: Set<@JvmSuppressWildcards FeatureDestination>

    @Inject
    lateinit var appDialogDestinations: Set<@JvmSuppressWildcards AppDialogDestinations>

    private val initialKey: NavKey by lazy(LazyThreadSafetyMode.NONE) {
        val json = requireNotNull(requireArguments().getString(ARG_INITIAL_KEY)) {
            "LegacyOverlayDialogFragment requires '$ARG_INITIAL_KEY'; use show() to construct it."
        }
        Json.decodeFromString(NavKeySerializer(), json)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The theme sets `windowIsFloating=false` + transparent background so the dialog window
        // fills the host (same visual as the previous decor-view overlay).
        setStyle(STYLE_NORMAL, R.style.Theme_Mega_LegacyOverlayDialog)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        // DialogFragment defaults to a plain `Dialog`, which does NOT propagate
        // OnBackPressedDispatcherOwner / NavigationEventDispatcherOwner via the view tree — so
        // NavDisplay can't register its back callback and the dialog's default back-to-dismiss
        // kicks in instead. ComponentDialog implements both owners and wires them on its decor
        // view, letting Compose's BackHandler / NavDisplay intercept back inside the flow.
        return ComponentDialog(requireContext(), theme)
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View = ComposeView(requireContext()).apply {
        setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        setContent {
            val theme by monitorThemeModeUseCase()
                .collectAsStateWithLifecycle(initialValue = ThemeMode.System)
            // ComponentDialog sets itself as the OnBackPressedDispatcherOwner /
            // NavigationEventDispatcherOwner on its decor view, so NavDisplay finds the dialog's
            // dispatcher automatically — no need to override the CompositionLocal here.
            LegacyActivityScaffold(
                container = { content ->
                    MegaAppContainer(themeMode = theme, content = content)
                },
                initialKey = initialKey,
                navigationResultManager = navigationResultManager,
                featureDestinations = featureDestinations,
                appDialogDestinations = appDialogDestinations,
                onEmptyBackStack = { dismissAllowingStateLoss() },
            ) { _, _ -> }
        }
    }

    companion object {
        /**
         * Default tag for the single-overlay case. Pass a distinct tag to [show]/[dismiss] when
         * multiple overlays of this type may coexist on the same [FragmentManager].
         */
        const val TAG = "LegacyOverlayDialogFragment"
        private const val ARG_INITIAL_KEY = "initialKey"

        /**
         * Show the overlay with [initialKey] as the root destination of its back stack. No-op if a
         * fragment with [tag] is already attached to [fragmentManager]. [initialKey] must be a
         * `@Serializable` NavKey (kotlinx.serialization).
         */
        fun show(
            fragmentManager: FragmentManager,
            initialKey: NavKey,
            tag: String = TAG,
        ) {
            if (fragmentManager.findFragmentByTag(tag) != null) return
            LegacyOverlayDialogFragment().apply {
                arguments = bundleOf(
                    ARG_INITIAL_KEY to Json.encodeToString(NavKeySerializer(), initialKey),
                )
            }.show(fragmentManager, tag)
        }

        /** Dismiss the overlay tagged [tag] if currently shown. Safe to call when not shown. */
        fun dismiss(fragmentManager: FragmentManager, tag: String = TAG) {
            (fragmentManager.findFragmentByTag(tag) as? LegacyOverlayDialogFragment)
                ?.dismissAllowingStateLoss()
        }

        /**
         * Convenience: collect non-null results emitted on [resultKey] from [navigationResultManager],
         * forward each value to [onResult], clear the result, and dismiss the overlay tagged [tag].
         * Cancels with [lifecycle] (`repeatOnLifecycle(STARTED)`). Replaces the host's manual
         * collector + dismiss boilerplate.
         */
        fun <T> collectResultAndDismiss(
            fragmentManager: FragmentManager,
            scope: CoroutineScope,
            lifecycle: Lifecycle,
            navigationResultManager: NavigationResultManager,
            resultKey: String,
            tag: String = TAG,
            onResult: (T) -> Unit,
        ) {
            scope.launch {
                lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
                    navigationResultManager
                        .monitorResult<T>(resultKey)
                        .filterNotNull()
                        .collect { value ->
                            onResult(value)
                            navigationResultManager.clearResult(resultKey)
                            dismiss(fragmentManager, tag)
                        }
                }
            }
        }
    }
}
