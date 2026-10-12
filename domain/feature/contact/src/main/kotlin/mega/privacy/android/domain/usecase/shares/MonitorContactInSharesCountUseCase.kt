package mega.privacy.android.domain.usecase.shares

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import mega.privacy.android.domain.entity.node.NodeChanges
import mega.privacy.android.domain.usecase.node.MonitorNodeUpdatesUseCase
import javax.inject.Inject

/**
 * Monitor the number of nodes a contact shares with the user, emitting the current count first
 * and then refreshing it on every node update that affects incoming shares.
 */
class MonitorContactInSharesCountUseCase @Inject constructor(
    private val getInSharesUseCase: GetInSharesUseCase,
    private val monitorNodeUpdatesUseCase: MonitorNodeUpdatesUseCase,
) {
    /**
     * Invoke.
     *
     * @param email Email of the contact.
     * @return Flow of the number of nodes shared by the contact.
     */
    operator fun invoke(email: String): Flow<Int> =
        monitorNodeUpdatesUseCase()
            .filter { update ->
                update.changes.keys.any { it.isIncomingShare } ||
                        update.changes.values.any { NodeChanges.Remove in it }
            }
            .conflate()
            .map { getInSharesUseCase(email).size }
            .onStart { emit(getInSharesUseCase(email).size) }
}
