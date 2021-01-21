package mega.privacy.android.app.utils

import android.util.DisplayMetrics
import androidx.fragment.app.Fragment
import mega.privacy.android.app.lollipop.ManagerActivityLollipop

fun <T> Fragment.callManager(call: (manager: ManagerActivityLollipop) -> T): T? {
    val hostActivity = this.activity
    return if (hostActivity is ManagerActivityLollipop) {
        call(hostActivity)
    } else {
        null
    }
}

fun Fragment.displayMetrics(): DisplayMetrics {
    return requireContext().resources.displayMetrics
}
