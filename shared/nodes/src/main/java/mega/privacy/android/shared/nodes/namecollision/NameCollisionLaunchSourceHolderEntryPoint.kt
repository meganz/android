package mega.privacy.android.shared.nodes.namecollision

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Resolves the process-wide [NameCollisionLaunchSourceHolder] from any [Context], for composables
 * that have no Hilt injection point of their own.
 */
val Context.nameCollisionLaunchSourceHolder: NameCollisionLaunchSourceHolder
    get() = EntryPointAccessors.fromApplication(
        applicationContext,
        NameCollisionLaunchSourceHolderEntryPoint::class.java,
    ).nameCollisionLaunchSourceHolder

/**
 * Hilt entry point exposing the [NameCollisionLaunchSourceHolder] singleton to non-Hilt classes.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface NameCollisionLaunchSourceHolderEntryPoint {
    val nameCollisionLaunchSourceHolder: NameCollisionLaunchSourceHolder
}
