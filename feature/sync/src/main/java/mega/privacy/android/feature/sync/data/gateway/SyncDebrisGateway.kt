package mega.privacy.android.feature.sync.data.gateway

import mega.privacy.android.feature.sync.domain.entity.SyncDebris

internal interface SyncDebrisGateway {

    fun set(debris: List<SyncDebris>)

    fun get(): List<SyncDebris>
}