package mega.privacy.android.data.mapper.handles

import nz.mega.sdk.MegaChatApiJava
import nz.mega.sdk.MegaHandleList
import javax.inject.Inject

/**
 * Handle List mapper
 */
internal class HandleListMapper @Inject constructor() {

    operator fun invoke(handleList: MegaHandleList) = with(handleList) {
        (0 until size()).filter { it != MegaChatApiJava.MEGACHAT_INVALID_HANDLE }
            .map { get(it) }
    }
}