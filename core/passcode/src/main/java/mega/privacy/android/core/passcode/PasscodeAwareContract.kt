package mega.privacy.android.core.passcode

import android.content.Intent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.core.content.IntentCompat
import timber.log.Timber

/**
 * A decorator for [ActivityResultContract] that automatically suppresses the passcode prompt
 * when launching external activities (file pickers, document scanner, etc.).
 *
 * Inspects the intent produced by [createIntent] and calls
 * [PasscodeProcessLifecycleOwner.skipNextPasscodeCheck] when the intent targets an external
 * activity, preventing the passcode screen from appearing when the user returns.
 *
 * @param delegate the original contract to wrap
 */
class PasscodeAwareContract<I, O>(
    private val delegate: ActivityResultContract<I, O>,
) : ActivityResultContract<I, O>() {

    override fun createIntent(context: android.content.Context, input: I): Intent {
        val intent = delegate.createIntent(context, input)
        if (shouldSkipPasscode(context, intent)) {
            Timber.d("PasscodeAwareContract: skipping passcode for action=${intent.action}")
            PasscodeProcessLifecycleOwner.get().skipNextPasscodeCheck()
        }
        return intent
    }

    override fun parseResult(resultCode: Int, intent: Intent?): O =
        delegate.parseResult(resultCode, intent)

    override fun getSynchronousResult(
        context: android.content.Context,
        input: I,
    ): SynchronousResult<O>? = delegate.getSynchronousResult(context, input)

    private fun shouldSkipPasscode(context: android.content.Context, intent: Intent): Boolean {
        // Regular external intents (file/folder pickers, etc.)
        if (intent.action in EXTERNAL_ACTIONS) return true

        // IntentSender-based external launches (document scanner, in-app update, etc.)
        if (intent.action == StartIntentSenderForResult.ACTION_INTENT_SENDER_REQUEST) {
            val request = IntentCompat.getParcelableExtra(
                intent,
                StartIntentSenderForResult.EXTRA_INTENT_SENDER_REQUEST,
                IntentSenderRequest::class.java
            )
            return request?.intentSender?.creatorPackage == context.packageName
        }

        return false
    }

    companion object {
        /**
         * Intent actions that launch external activities (file/folder pickers, share targets, etc.)
         * and should suppress the passcode prompt on return.
         */
        val EXTERNAL_ACTIONS = setOf(
            Intent.ACTION_OPEN_DOCUMENT,
            Intent.ACTION_OPEN_DOCUMENT_TREE,
            Intent.ACTION_GET_CONTENT,
            Intent.ACTION_CREATE_DOCUMENT,
            Intent.ACTION_PICK,
            Intent.ACTION_CHOOSER,
            Intent.ACTION_SEND,
            Intent.ACTION_SEND_MULTIPLE,
        )
    }
}
