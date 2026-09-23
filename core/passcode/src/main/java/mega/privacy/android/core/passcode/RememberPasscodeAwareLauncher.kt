package mega.privacy.android.core.passcode

import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

/**
 * Wraps an [ActivityResultContract] with [PasscodeAwareContract] so that launching external
 * activities (file pickers, document scanner, etc.) automatically suppresses the passcode
 * prompt on return.
 */
fun <I, O> ActivityResultContract<I, O>.withPasscodeAwareness(): ActivityResultContract<I, O> =
    PasscodeAwareContract(this)

/**
 * A passcode-aware version of [rememberLauncherForActivityResult].
 *
 * Use this instead of [rememberLauncherForActivityResult] when the [contract] may launch an
 * external activity (e.g. file picker, folder picker, document scanner). It wraps the contract
 * with [PasscodeAwareContract] to automatically suppress the passcode prompt when the user
 * returns from the external activity.
 *
 * @param contract the activity result contract
 * @param onResult callback for the result
 */
@Composable
fun <I, O> rememberPasscodeAwareLauncher(
    contract: ActivityResultContract<I, O>,
    onResult: (O) -> Unit,
): ManagedActivityResultLauncher<I, O> {
    val wrapped = remember(contract) { contract.withPasscodeAwareness() }
    return rememberLauncherForActivityResult(wrapped, onResult)
}
