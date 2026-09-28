package mega.privacy.android.domain.usecase.viewtype

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapNotNull
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.repository.ViewTypeRepository
import javax.inject.Inject

/**
 * Implementing class of [MonitorViewType]
 *
 * @property viewTypeRepository
 */
class DefaultMonitorViewType @Inject constructor(
    private val viewTypeRepository: ViewTypeRepository,
) : MonitorViewType {
    override fun invoke(): Flow<ViewType> = viewTypeRepository.monitorViewType()
        .mapNotNull { it ?: ViewType.LIST }
}