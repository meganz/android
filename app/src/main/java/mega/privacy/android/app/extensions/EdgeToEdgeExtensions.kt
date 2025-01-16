package mega.privacy.android.app.extensions

import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import androidx.fragment.app.Fragment

/**
 * Enable edge to edge and consume insets for the activity used XML layout.
 *
 */
@JvmOverloads
fun ComponentActivity.enableEdgeToEdgeAndConsumeInsets(
    type: Int = WindowInsetsCompat.Type.systemBars(),
    handleWindowInsets: (WindowInsetsCompat) -> Unit = {},
) {
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

        handleWindowInsets(windowInsets)

        WindowInsetsCompat.CONSUMED
    }
}

/**
 * Consume insets for the activity with custom toolbar used XML layout.
 */
@JvmOverloads
fun ComponentActivity.consumeInsetsWithToolbar(
    type: Int = WindowInsetsCompat.Type.systemBars(),
    customToolbar: View,
) {
    // we need condition to check if the device running Android 15 when we target sdk to 35
    // because it will enable edge to edge by default
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, windowInsets ->
        val insets = windowInsets.getInsets(type)
        v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = insets.left
            bottomMargin = insets.bottom
            rightMargin = insets.right
        }

        customToolbar.updatePadding(top = insets.top)

        WindowInsetsCompat.CONSUMED
    }
}

/**
 * Consume insets for the activity used XML layout.
 *
 * @param type The type of insets to consume. default is systemBars.
 * @param topInset Whether to consume top inset. default is true.
 * @param bottomInset Whether to consume bottom inset. default is true.
 * @param handleWindowInsets A lambda to handle window insets. default is empty.
 */
@JvmOverloads
fun ComponentActivity.consumeParentInsets(
    type: Int = WindowInsetsCompat.Type.systemBars(),
    topInset: Boolean = true,
    bottomInset: Boolean = true,
    handleWindowInsets: (WindowInsetsCompat) -> Unit = {},
) {
    ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content)) { v, windowInsets ->
        val insets = windowInsets.getInsets(type)
        v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
            leftMargin = insets.left
            rightMargin = insets.right
            if (topInset) {
                topMargin = insets.top
            }
            if (bottomInset) {
                bottomMargin = insets.bottom
            }
        }

        handleWindowInsets(windowInsets)

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
