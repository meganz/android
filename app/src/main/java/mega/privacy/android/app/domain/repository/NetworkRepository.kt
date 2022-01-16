package mega.privacy.android.app.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.app.domain.entity.ConnectivityState

interface NetworkRepository {
    fun getCurrentConnectivityState(): ConnectivityState
    fun monitorConnectivityChanges() : Flow<ConnectivityState>
}