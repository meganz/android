package mega.privacy.android.app.domain.usecase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import mega.privacy.android.app.domain.repository.NetworkRepository
import javax.inject.Inject

class DefaultIsOnline @Inject constructor(private val networkRepository: NetworkRepository) : IsOnline {
    override fun invoke(): Flow<Boolean> {
        return flow {
            emit(networkRepository.getCurrentConnectivityState().connected)
            emitAll(networkRepository.monitorConnectivityChanges().map { it.connected })
        }
    }
}