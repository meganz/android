package mega.privacy.android.data.mapper

import nz.mega.sdk.MegaHandleList
import javax.inject.Inject

/**
 * Handle list mapper impl
 */
internal class HandleListMapperImpl @Inject constructor() : HandleListMapper {
    override fun invoke(handleList: MegaHandleList) =
        mutableListOf<Long>().apply {
            for (i in 0..handleList.size()) {
                add(handleList[i])
            }
        }
}