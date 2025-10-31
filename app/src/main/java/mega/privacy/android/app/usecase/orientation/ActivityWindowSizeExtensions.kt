package mega.privacy.android.app.usecase.orientation

import android.app.Activity
import android.content.res.Configuration
import android.os.Parcelable
import android.view.ViewTreeObserver
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.parcelize.Parcelize
import mega.privacy.android.app.BaseActivity
import timber.log.Timber

/**
 * Enum representing the width size class for window dimensions.
 * Follows Material Design 3 window size class guidelines.
 */
enum class WindowWidthSizeClass { Compact, Medium, Expanded }

/**
 * Enum representing the height size class for window dimensions.
 * Follows Material Design 3 window size class guidelines.
 */
enum class WindowHeightSizeClass { Compact, Medium, Expanded }

/**
 * Data class representing the window size class for both width and height dimensions.
 * Follows Material Design 3 window size class guidelines.
 */
@Parcelize
data class WindowSizeClass(
    val widthSizeClass: WindowWidthSizeClass,
    val heightSizeClass: WindowHeightSizeClass,
) : Parcelable

typealias WSC = WindowSizeClass

/**
 * Calculates the window size class based on the activity's screen dimensions and orientation.
 * Uses Material Design 3 breakpoints for classification.
 */
fun Activity.calculateWindowSizeClass(): WindowSizeClass {
    val displayMetrics = resources.displayMetrics
    val configuration = resources.configuration

    // Convert pixels to density-independent pixels
    val widthDp = displayMetrics.widthPixels / displayMetrics.density
    val heightDp = displayMetrics.heightPixels / displayMetrics.density

    Timber.v("calculateWindowSizeClass: Raw dimensions - widthDp: $widthDp, heightDp: $heightDp, density: ${displayMetrics.density}")

    // Handle orientation - swap dimensions for landscape
    val actualWidth = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        maxOf(widthDp, heightDp) else minOf(widthDp, heightDp)
    val actualHeight = if (configuration.orientation == Configuration.ORIENTATION_LANDSCAPE)
        minOf(widthDp, heightDp) else maxOf(widthDp, heightDp)

    Timber.v("calculateWindowSizeClass: Orientation: ${configuration.orientation}, actualWidth: $actualWidth, actualHeight: $actualHeight")

    // Determine width size class based on Material Design 3 breakpoints
    val widthSizeClass = when {
        actualWidth < 600 -> WindowWidthSizeClass.Compact
        actualWidth < 840 -> WindowWidthSizeClass.Medium
        else -> WindowWidthSizeClass.Expanded
    }

    // Determine height size class based on Material Design 3 breakpoints
    val heightSizeClass = when {
        actualHeight < 480 -> WindowHeightSizeClass.Compact
        actualHeight < 900 -> WindowHeightSizeClass.Medium
        else -> WindowHeightSizeClass.Expanded
    }

    val result = WindowSizeClass(widthSizeClass, heightSizeClass)
    Timber.d("calculateWindowSizeClass: Result - $result")
    return result
}

/**
 * Call from onCreate() to enable adaptive layout monitoring.
 * This extension function sets up automatic window size class monitoring
 * and triggers the callback when the window size changes.
 *
 * @param onSizeChanged Callback triggered when window size changes
 */
fun AppCompatActivity.enableAdaptiveLayout(
    onSizeChanged: ((old: WindowSizeClass?, new: WindowSizeClass) -> Unit)? = null,
) {
    Timber.d("enableAdaptiveLayout called for ${this::class.java.simpleName}")
    var previous: WSC? = null

    windowSizeClassFlow()
        .onEach { newSize ->
            Timber.d("Window size changed: $previous -> $newSize")
            onSizeChanged?.invoke(previous, newSize)
            previous = newSize
        }
        .launchIn(lifecycleScope)
}

/**
 * Gets the current window size class for this activity.
 * @return The current WindowSizeClass based on screen dimensions and orientation
 */
fun Activity.getCurrentWindowSize(): WindowSizeClass {
    val sizeClass = calculateWindowSizeClass()
    Timber.v("getCurrentWindowSize: $sizeClass")
    return sizeClass
}

/**
 * Creates a Flow that emits window size class changes.
 * Uses ViewTreeObserver to detect layout changes and emits distinct values.
 * @return Flow of WindowSizeClass that emits when window size changes
 */
fun Activity.windowSizeClassFlow(): Flow<WindowSizeClass> =
    callbackFlow<WindowSizeClass> {
        Timber.d("windowSizeClassFlow: Starting flow for ${this::class.java.simpleName}")
        val view = window.decorView
        val listener = ViewTreeObserver.OnGlobalLayoutListener {
            Timber.v("ViewTreeObserver: Layout changed, calculating new size")
            trySend(getCurrentWindowSize())
        }

        view.viewTreeObserver.addOnGlobalLayoutListener(listener)
        Timber.d("ViewTreeObserver listener added")

        awaitClose {
            Timber.d("windowSizeClassFlow: Cleaning up ViewTreeObserver listener")
            view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
        }
    }
        .onStart {
            // Emit initial value when flow starts
            emit(getCurrentWindowSize())
        }
        .distinctUntilChanged()
