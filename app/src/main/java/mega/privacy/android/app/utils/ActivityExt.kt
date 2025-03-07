package mega.privacy.android.app.utils

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import android.util.DisplayMetrics
import android.view.WindowInsets
import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

@Suppress("UNCHECKED_CAST")
fun <F : Fragment> AppCompatActivity.getFragmentFromNavHost(
    @IdRes navHostId: Int,
    fragmentClass: Class<F>,
): F? {
    val navHostFragment = supportFragmentManager.findFragmentById(navHostId) ?: return null
    for (fragment in navHostFragment.childFragmentManager.fragments) {
        if (fragment.javaClass == fragmentClass) {
            return fragment as F
        }
    }
    return null
}

/**
 * Get screen Height
 * @return screen height
 */
fun Activity.getScreenHeight(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = windowManager.currentWindowMetrics
        val insets =
            windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        windowMetrics.bounds.height() - insets.top - insets.bottom
    } else {
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        displayMetrics.heightPixels
    }
}

/**
 * Get screen Width
 * @return screen width
 */
fun Activity.getScreenWidth(): Int {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val windowMetrics = windowManager.currentWindowMetrics
        val insets =
            windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
        windowMetrics.bounds.width() - insets.left - insets.right
    } else {
        val displayMetrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        windowManager.defaultDisplay.getMetrics(displayMetrics)
        displayMetrics.widthPixels
    }
}


/**
 * Checks if intent can be handled by 3rd-party apps installed on the device
 */
fun Activity.canHandleIntent(intent: Intent) =
    packageManager.queryIntentActivities(intent, 0).isNotEmpty()


/**
 * Create intent to view a folder in 3rd-party file manager
 */
fun Activity.createViewFolderIntent(folderContentUri: Uri, path: String?): Intent? {
    val intent = Intent(Intent.ACTION_VIEW).apply {
        putExtra("android.provider.extra.INITIAL_URI", folderContentUri)
        putExtra("org.openintents.extra.ABSOLUTE_PATH", path)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    intent.setDataAndType(folderContentUri, "resource/folder")
    if (canHandleIntent(intent)) return intent

    intent.setDataAndType(folderContentUri, DocumentsContract.Document.MIME_TYPE_DIR)
    if (canHandleIntent(intent)) return intent
    return null
}