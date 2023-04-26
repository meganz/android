package mega.privacy.android.app.di

import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import mega.privacy.android.data.gateway.MegaLocalRoomGateway
import mega.privacy.android.domain.qualifier.ApplicationScope

/**
 * Room entry point
 *
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DatabaseEntryPoint {

    /**
     * Local room gateway
     */
    val localRoomGateway: MegaLocalRoomGateway

    /**
     * Application scope
     */
    @ApplicationScope
    fun applicationScope(): CoroutineScope
}