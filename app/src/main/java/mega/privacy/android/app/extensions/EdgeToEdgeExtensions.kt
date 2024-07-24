package mega.privacy.android.app.extensions

import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment

/**
 * Enable edge to edge and consume insets for the activity used XML layout.
 *
 */
@JvmOverloads
fun ComponentActivity.enableEdgeToEdgeAndConsumeInsets(type: Int = WindowInsetsCompat.Type.systemBars()) {
    // we need condition to check if the device running Android 15 when we target sdk to 35
    // because it will enable edge to edge by default
    enableEdgeToEdge()
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, windowInsets ->
        val insets = windowInsets.getInsets(type)
        v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = insets.left
            bottomMargin = insets.bottom
            rightMargin = insets.right
            topMargin = insets.top
        }

        WindowInsetsCompat.CONSUMED
    }
}

/**
 * Consume insets for the fragment used XML layout.
 *
 * @param type The type of insets to consume. default is systemBars.
 */
@JvmOverloads
fun Fragment.consumeInsets(type: Int = WindowInsetsCompat.Type.systemBars()) {
    // we need condition to check if the device running Android 15 when we target sdk to 35
    // because it will enable edge to edge by default
    ViewCompat.setOnApplyWindowInsetsListener(requireView()) { v, windowInsets ->
        val insets = windowInsets.getInsets(type)
        v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = insets.left
            bottomMargin = insets.bottom
            rightMargin = insets.right
            topMargin = insets.top
        }

        WindowInsetsCompat.CONSUMED
    }
}
