package mega.privacy.android.domain.usecase.contact

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Monitor contact last green updates use case
 *
 * @property monitorChatPresenceLastGreenUpdatesUseCase
 */
class MonitorUserLastGreenUpdatesUseCase @Inject constructor(
    private val monitorChatPresenceLastGreenUpdatesUseCase: MonitorChatPresenceLastGreenUpdatesUseCase,
) {

    /**
     * Invoke.
     *
     * @return Flow of Int.
     */
    operator fun invoke(userHandle: Long): Flow<Int> =
        monitorChatPresenceLastGreenUpdatesUseCase()
            .filter { it.handle == userHandle }
            .map { it.lastGreen }
}