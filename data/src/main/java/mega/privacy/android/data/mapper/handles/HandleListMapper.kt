package mega.privacy.android.data.mapper.handles

import nz.mega.sdk.MegaHandleList
import javax.inject.Inject

/**
 * Handle List mapper
 */
internal class HandleListMapper @Inject constructor() {

    operator fun invoke(handleList: MegaHandleList) = with(handleList) {
        (0 until size())
            .map { get(it) }
    }
}