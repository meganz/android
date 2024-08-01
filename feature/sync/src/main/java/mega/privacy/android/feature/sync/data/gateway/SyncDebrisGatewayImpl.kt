package mega.privacy.android.feature.sync.data.gateway

import mega.privacy.android.feature.sync.domain.entity.SyncDebris
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Gateway to store sync debris in the runtime memory.
 */
@Singleton
internal class SyncDebrisGatewayImpl @Inject constructor() : SyncDebrisGateway {

    private val debris = mutableListOf<SyncDebris>()

    override fun set(debris: List<SyncDebris>) {
        this.debris.clear()
        this.debris.addAll(debris)
    }

    override fun get(): List<SyncDebris> = debris
}