package mega.privacy.android.app.presentation.login

import android.app.Activity
import android.view.autofill.AutofillManager
import androidx.credentials.CreatePasswordRequest
import androidx.credentials.CredentialManager
import androidx.credentials.exceptions.CreateCredentialCancellationException
import mega.privacy.android.app.presentation.login.model.PasswordCredential
import timber.log.Timber
import kotlin.coroutines.cancellation.CancellationException

/**
 * Offers to save the credentials of a newly created account to the user's password manager
 * (e.g. Google Password Manager) via Credential Manager, reducing the risk of losing access
 * to an E2EE account because of a forgotten password.
 *
 * Only used for the first login after signup and email verification: no autofill session
 * exists at that point, so the platform cannot offer to save on its own. Regular logins rely
 * on the platform autofill save prompt instead, which also avoids provider-specific
 * re-prompts for already saved credentials (e.g. Samsung Pass).
 *
 * Requires an [Activity] receiver because Credential Manager needs an activity context
 * to host its save-password UI.
 *
 * Best effort: dismissal and failures are swallowed, as saving the password must never
 * interrupt the login flow.
 */
internal suspend fun Activity.savePasswordCredential(credential: PasswordCredential) {
    // A pending platform autofill session (started by typing into fields with autofill
    // semantics) would show its own save prompt when committed, duplicating this one.
    runCatching { getSystemService(AutofillManager::class.java)?.cancel() }
    runCatching {
        CredentialManager.create(this).createCredential(
            context = this,
            request = CreatePasswordRequest(
                id = credential.email,
                password = credential.password,
            ),
        )
        Timber.d("Password credential saved to the user's password manager")
    }.onFailure { e ->
        when (e) {
            // Coroutine cancellation (e.g. activity recreation mid-prompt) must propagate so
            // the event is not marked consumed and the prompt can fire again afterwards.
            is CancellationException -> throw e

            is CreateCredentialCancellationException ->
                Timber.d("Password credential saving declined by the user")

            else -> Timber.w(e, "Password credential saving failed: ${e::class.simpleName}")
        }
    }
}
