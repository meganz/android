package mega.privacy.android.data.mapper

import nz.mega.sdk.MegaChatApiJava.MEGACHAT_INVALID_HANDLE
import nz.mega.sdk.MegaHandleList
import javax.inject.Inject

/**
 * Handle list mapper impl
 */
internal class HandleListMapperImpl @Inject constructor() : HandleListMapper {
    override fun invoke(handleList: MegaHandleList) =
        mutableListOf<Long>().apply {
            if (handleList.size() > 0) {
                for (i in 0..handleList.size()) {
                    if (handleList[i] != MEGACHAT_INVALID_HANDLE) {
                        add(handleList[i])
                    }
                }
            }
        }
}