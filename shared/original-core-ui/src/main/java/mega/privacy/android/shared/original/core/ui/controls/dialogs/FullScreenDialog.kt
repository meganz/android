package mega.privacy.android.shared.original.core.ui.controls.dialogs

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.Window
import android.view.WindowManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.SecureFlagPolicy

/**
 * Full screen Dialog
 * @param onDismissRequest Executes when the user tries to dismiss the dialog.
 * @property dismissOnBackPress whether the dialog can be dismissed by pressing the back button.
 * If true, pressing the back button will call onDismissRequest.
 * @property securePolicy Policy for setting [WindowManager.LayoutParams.FLAG_SECURE] on the
 * dialog's window.
 * @param content The content to be displayed inside the dialog.
 */
@Composable
fun FullScreenDialog(
    onDismissRequest: () -> Unit,
    dismissOnBackPress: Boolean = true,
    securePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
    content: @Composable () -> Unit,
) {
    Dialog(
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = false, //there's no "outside" in FullScreenDialog
            securePolicy = securePolicy,
            usePlatformDefaultWidth = true,
            decorFitsSystemWindows = false,
        ),
        onDismissRequest = onDismissRequest,
    ) {
        Surface(
            modifier = Modifier.fillMaxSize()
        ) {
            val activityWindow = LocalView.current.context.getActivityWindow()
            val dialogWindow = (LocalView.current.parent as? DialogWindowProvider)?.window
            if (activityWindow != null && dialogWindow != null) {
                val attributes = WindowManager.LayoutParams()
                attributes.copyFrom(activityWindow.attributes)
                attributes.type = dialogWindow.attributes.type
                dialogWindow.attributes = attributes
            }
            content()
        }
    }
}

private tailrec fun Context.getActivityWindow(): Window? =
    when (this) {
        is Activity -> window
        is ContextWrapper -> baseContext.getActivityWindow()
        else -> null
    }