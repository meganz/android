package mega.privacy.android.navigation

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.atomic.AtomicReference

/**
 * Extension function to get [MegaNavigator] from the application context.
 *
 * This is useful for classes that are not Hilt components and need access to the navigator.
 *
 * @return [MegaNavigator] instance.
 */
val Context.megaNavigator: MegaNavigator
    get() = MegaNavigatorProvider.get(this)

/**
 * This interface is needed to inject [MegaNavigator] into classes that are not Hilt components.
 *
 * @property megaNavigator The [MegaNavigator] instance.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MegaNavigatorEntryPoint {
    val megaNavigator: MegaNavigator
}

internal object MegaNavigatorProvider {
    private val navigatorRef = AtomicReference<MegaNavigator?>(null)

    fun get(context: Context): MegaNavigator {
        return navigatorRef.get() ?: run {
            val newNavigator = EntryPointAccessors.fromApplication(
                context.applicationContext,
                MegaNavigatorEntryPoint::class.java
            ).megaNavigator

            navigatorRef.set(newNavigator)
            newNavigator
        }
    }
}