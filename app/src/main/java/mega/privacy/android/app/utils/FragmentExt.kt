package mega.privacy.android.app.utils

import android.util.DisplayMetrics
import androidx.fragment.app.Fragment
import mega.privacy.android.app.main.ManagerActivity

fun <T> Fragment.callManager(call: (manager: ManagerActivity) -> T): T? {
    val hostActivity = this.activity
    return if (hostActivity is ManagerActivity) {
        call(hostActivity)
    } else {
        null
    }
}

fun Fragment.displayMetrics(): DisplayMetrics {
    return requireContext().resources.displayMetrics
}
