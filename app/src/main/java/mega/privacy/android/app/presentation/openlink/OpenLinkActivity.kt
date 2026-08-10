package mega.privacy.android.app.presentation.openlink

import android.content.Intent
import android.content.Intent.ACTION_MAIN
import android.content.Intent.ACTION_VIEW
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import dagger.hilt.android.AndroidEntryPoint
import mega.privacy.android.app.activities.PasscodeActivity
import mega.privacy.android.app.appstate.MegaActivity
import mega.privacy.android.app.appstate.MegaActivity.Companion.ACTION_DEEP_LINKS
import mega.privacy.android.app.usecase.orientation.enableAdaptiveLayout
import timber.log.Timber

/**
 * Entry point for external links that immediately forwards the intent to [MegaActivity].
 *
 * Do not remove this class yet: it is declared with `singleTask` launch mode in the manifest
 * and forwards links to [MegaActivity] with `FLAG_ACTIVITY_SINGLE_TOP` and
 * `FLAG_ACTIVITY_CLEAR_TOP` to prevent duplicate [MegaActivity] instances from accumulating
 * in the back stack. It should only be removed once all screens have been migrated to the
 * single-activity architecture.
 */
@AndroidEntryPoint
class OpenLinkActivity : PasscodeActivity() {

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        this.intent = intent
        forwardIntentToSingleActivity()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()

        // Set orientation before super.onCreate() to ensure it takes effect
        enableAdaptiveLayout { old, new ->
            Timber.d("On size change in OpenLinkActivity from $old to $new")
        }

        super.onCreate(savedInstanceState)
        forwardIntentToSingleActivity()
    }

    private fun forwardIntentToSingleActivity() {
        val isFromHistory =
            (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0
        MegaActivity.getIntent(
            this,
            action = if (isFromHistory) ACTION_MAIN else ACTION_DEEP_LINKS,
        ).also { megaActivityIntent ->
            if (!isFromHistory) {
                megaActivityIntent.data = intent.data
            }
            startActivity(megaActivityIntent)
        }
        intent = Intent(intent).apply {
            action = ACTION_VIEW
            data = null
            replaceExtras(Bundle())
        }
        finish()
    }
}
