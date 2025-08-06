package mega.privacy.android.navigation

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent

/**
 * Extension function to get [MegaActivityResultContract] from the application context.
 *
 * @return [MegaActivityResultContract] instance.
 */
val Context.megaActivityResultContract: MegaActivityResultContract
    get() {
        return EntryPointAccessors.fromApplication(
            this.applicationContext,
            MegaActivityResultContractEntryPoint::class.java
        ).contract
    }

@EntryPoint
@InstallIn(SingletonComponent::class)
interface MegaActivityResultContractEntryPoint {
    val contract: MegaActivityResultContract
}