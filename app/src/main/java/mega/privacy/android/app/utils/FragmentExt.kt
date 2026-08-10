package mega.privacy.android.app.utils

import android.util.DisplayMetrics
import androidx.fragment.app.Fragment

fun Fragment.displayMetrics(): DisplayMetrics {
    return requireContext().resources.displayMetrics
}
