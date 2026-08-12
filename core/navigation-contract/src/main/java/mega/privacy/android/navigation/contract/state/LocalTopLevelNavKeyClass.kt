package mega.privacy.android.navigation.contract.state

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.navigation3.runtime.NavKey
import kotlin.reflect.KClass

/**
 * The [NavKey] class of the current top-level (navigation bar) destination of the main
 * navigation display, or null when the composition is not hosted inside it.
 */
val LocalTopLevelNavKeyClass = compositionLocalOf<KClass<out NavKey>?> { null }

/**
 * Returns whether the main nav item screen rooted at [rootNavKeyClass] is displayed pushed on
 * top of another section's back stack, e.g. opened from the Menu while hidden from the
 * navigation bar, in which case its top bar should show a back affordance.
 */
@Composable
fun isPushedMainNavScreen(rootNavKeyClass: KClass<out NavKey>): Boolean =
    LocalTopLevelNavKeyClass.current?.let { it != rootNavKeyClass } == true
