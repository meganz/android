package mega.privacy.android.navigation

import android.content.Context
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.atomic.AtomicReference

/**
 * Extension function to get [MegaActivityResultContract] from the application context.
 *
 * @return [MegaActivityResultContract] instance.
 */
val Context.megaActivityResultContract: MegaActivityResultContract
    get() = MegaActivityResultContractProvider.get(this)

/**
 * This interface is needed to inject [MegaActivityResultContract] into classes that are not Hilt components.
 *
 * @property contract The [MegaActivityResultContract] instance.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface MegaActivityResultContractEntryPoint {
    val contract: MegaActivityResultContract
}

internal object MegaActivityResultContractProvider {
    private val contractRef = AtomicReference<MegaActivityResultContract?>(null)

    fun get(context: Context): MegaActivityResultContract {
        return contractRef.get() ?: run {
            val newContract = EntryPointAccessors.fromApplication(
                context.applicationContext,
                MegaActivityResultContractEntryPoint::class.java
            ).contract

            contractRef.set(newContract)
            newContract
        }
    }
}