package mega.privacy.android.app.appstate

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import mega.privacy.android.app.utils.Constants

/**
 * Builds intents that target the non-exported [MegaActivity] alias.
 *
 * The exported [MegaActivity] ignores [LAUNCH_INTENT] to prevent other apps from using
 * it as a trampoline into non-exported MEGA activities while the passcode is locked.
 * Internal code that needs to set [LAUNCH_INTENT] must build intents through this
 * helper so they reach the non-exported alias instead.
 */
object MegaActivityInternalLauncher {

    /**
     * Fully-qualified class name of the non-exported activity-alias declared in
     * AndroidManifest.xml. Only callers within this app process can reach it.
     */
    const val INTERNAL_ENTRY_COMPONENT =
        "mega.privacy.android.app.appstate.MegaActivityInternal"

    /**
     * Intent-extra key used to carry a nested [Intent] that [MegaActivity] should start
     * after its root node is ready. Honored only when the outer intent targets
     * [INTERNAL_ENTRY_COMPONENT]; the exported [MegaActivity] ignores it.
     */
    const val LAUNCH_INTENT: String = "LAUNCH_INTENT"

    /**
     * Build an intent targeting the non-exported [MegaActivity] alias.
     *
     * @param context Context used to resolve the alias [ComponentName].
     * @param action Optional intent action.
     * @param warningMessage Optional warning message to display once [MegaActivity] starts.
     */
    fun getIntent(
        context: Context,
        action: String? = null,
        warningMessage: String? = null,
    ): Intent = Intent().apply {
        component = ComponentName(context, INTERNAL_ENTRY_COMPONENT)
        this.action = action
        putExtra(Constants.INTENT_EXTRA_WARNING_MESSAGE, warningMessage)
    }
}
