package mega.privacy.android.data.mapper.viewtype

import mega.privacy.android.domain.entity.preference.ViewType
import javax.inject.Inject

/**
 * Default implementation of [ViewTypeMapper]
 */
internal class ViewTypeMapperImpl @Inject constructor() : ViewTypeMapper {
    override fun invoke(state: Int?): ViewType? = ViewType(state)
}