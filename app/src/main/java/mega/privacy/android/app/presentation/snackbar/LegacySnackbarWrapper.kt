package mega.privacy.android.app.presentation.snackbar

import android.content.Context
import androidx.compose.material.SnackbarDuration
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.core.content.ContextCompat.getString
import kotlinx.coroutines.delay
import mega.privacy.android.app.R
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.interfaces.showSnackbar
import mega.privacy.android.app.utils.Constants.OPEN_FILE_SNACKBAR_TYPE
import timber.log.Timber

/**
 * Utility function to show a snackbar in legacy views where we still don't have scaffold.
 * Snackbar host state should be attached to snackbar host in the scaffold, but in some cases we
 * don't have a scaffold yet because the fragment is inside an activity with the toolbar or
 * viewpager or other higher level components not in compose
 * NOTE: [snackbarHostState] action is not supported and will be ignored
 * @param snackbarHostState to be used to show the snackbar, the action will be ignored.
 * @param context, it should be an instance of [SnackbarShower], otherwise won't show anything
 */
@Composable
fun LegacySnackBarWrapper(snackbarHostState: SnackbarHostState, context: Context?) {
    val snackbarShower = context as? SnackbarShower ?: run {
        Timber.w("Unable to show snack bar, view does not exist or context is not instance of SnackbarShower")
        return
    }

    snackbarHostState.currentSnackbarData?.let { snackbarData ->
        LaunchedEffect(snackbarData) {
            snackbarData.message.let { message ->
                snackbarData.actionLabel?.let { actionLabel ->
                    when (actionLabel) {
                        getString(context, R.string.general_confirmation_open) -> {
                            snackbarShower.showSnackbar(
                                OPEN_FILE_SNACKBAR_TYPE,
                                message
                            ) {
                                snackbarData.performAction()
                            }
                            delay(snackbarData.duration.toMillis())
                            snackbarData.dismiss()
                        }

                        else -> null
                    }
                } ?: run {
                    snackbarShower.showSnackbar(message)
                    snackbarData.dismiss()
                }
            }
        }
    }
}

private fun SnackbarDuration.toMillis() = when (this) {
    SnackbarDuration.Indefinite -> Long.MAX_VALUE
    SnackbarDuration.Long -> 10000L
    SnackbarDuration.Short -> 4000L
}