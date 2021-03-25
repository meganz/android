package mega.privacy.android.app.utils

import androidx.annotation.IdRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment

@Suppress("UNCHECKED_CAST")
fun <F : Fragment> AppCompatActivity.getFragmentFromNavHost(
    @IdRes navHostId: Int,
    fragmentClass: Class<F>
): F? {
    val navHostFragment = supportFragmentManager.findFragmentById(navHostId) ?: return null
    for (fragment in navHostFragment.childFragmentManager.fragments) {
        if (fragment.javaClass == fragmentClass) {
            return fragment as F
        }
    }
    return null
}
