package mega.privacy.android.domain.usecase.viewtype

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * Returns a [Flow] to monitor view type changes in the app
 */
fun interface MonitorViewType {

    /**
     * Invocation method
     * @return A [Flow] that returns a specific [ViewType]
     */
    operator fun invoke(): Flow<ViewType>
}