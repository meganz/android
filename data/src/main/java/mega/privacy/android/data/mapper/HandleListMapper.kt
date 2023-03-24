package mega.privacy.android.data.mapper

import nz.mega.sdk.MegaHandleList
import nz.mega.sdk.MegaChatApiJava
import javax.inject.Inject

/**
 * Handle List mapper
 */
internal class HandleListMapper @Inject constructor() {
    operator fun invoke(handleList: MegaHandleList): List<Long> = mutableListOf<Long>().apply {
        if (handleList.size() > 0) {
            for (i in 0..handleList.size()) {
                if (handleList[i] != MegaChatApiJava.MEGACHAT_INVALID_HANDLE) {
                    add(handleList[i])
                }
            }
        }
    }
}