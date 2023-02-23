package mega.privacy.android.data.mapper.viewtype

import mega.privacy.android.domain.entity.preference.ViewType

/**
 * Mapper to convert the integer value received from DataStore into [ViewType]
 */
internal fun interface ViewTypeMapper {

    /**
     * Invocation function
     *
     * @param state The [Int] received from DataStore
     *
     * @return The corresponding [ViewType]
     */
    operator fun invoke(state: Int?): ViewType?
}