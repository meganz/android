package mega.privacy.android.data.mapper.handles

import nz.mega.sdk.MegaHandleList
import javax.inject.Inject

/**
 * Mega handle list provider
 */
class MegaHandleListProvider @Inject constructor() {
    /**
     * Invoke
     *
     * @return new MegaHandleList
     */
    operator fun invoke(): MegaHandleList? = MegaHandleList.createInstance()
}
