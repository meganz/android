package mega.privacy.android.domain.repository

import kotlinx.coroutines.flow.Flow
import mega.privacy.android.domain.entity.preference.ViewType

/**
 * The Repository for all view type operations
 */
interface ViewTypeRepository {

    /**
     * Monitors the [ViewType]
     *
     * @return a [Flow] to observe any changes to the [ViewType]
     */
    fun monitorViewType(): Flow<ViewType?>


    /**
     * Sets the new View type
     * @param viewType The new View type
     */
    suspend fun setViewType(viewType: ViewType)
}