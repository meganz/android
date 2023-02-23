package mega.privacy.android.data.repository

import kotlinx.coroutines.flow.map
import mega.privacy.android.data.gateway.preferences.UIPreferencesGateway
import mega.privacy.android.data.mapper.viewtype.ViewTypeMapper
import mega.privacy.android.domain.entity.preference.ViewType
import mega.privacy.android.domain.repository.ViewTypeRepository
import javax.inject.Inject

/**
 * Implementing class of [ViewTypeRepository]
 *
 * @property viewTypeMapper
 * @property uiPreferencesGateway
 */
internal class ViewTypeRepositoryImpl @Inject constructor(
    private val viewTypeMapper: ViewTypeMapper,
    private val uiPreferencesGateway: UIPreferencesGateway,
) : ViewTypeRepository {
    override fun monitorViewType() = uiPreferencesGateway.monitorViewType()
        .map { viewTypeMapper(it) }

    override suspend fun setViewType(viewType: ViewType) {
        uiPreferencesGateway.setViewType(viewType.id)
    }
}