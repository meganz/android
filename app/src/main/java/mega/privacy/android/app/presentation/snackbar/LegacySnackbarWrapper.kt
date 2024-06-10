package mega.privacy.android.app.presentation.snackbar

import android.content.Context
import androidx.compose.material.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.utils.Util

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
    LaunchedEffect(snackbarHostState.currentSnackbarData) {
        snackbarHostState.currentSnackbarData?.message?.let {
            Util.showSnackbar(context, it)
        }
        snackbarHostState.currentSnackbarData?.dismiss()
    }
}