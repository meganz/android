package mega.privacy.android.app.nav

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import mega.privacy.android.navigation.MegaNavigator

/**
 * Extension function to get [MegaNavigator] from the application context.
 *
 * This is useful for classes that are not Hilt components and need access to the navigator.
 *
 * @return [MegaNavigator] instance.
 */
val Context.megaNavigator: MegaNavigator
    get() {
        return EntryPointAccessors.fromApplication(
            this.applicationContext,
            MegaNavigatorEntryPoint::class.java
        ).megaNavigator
    }

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