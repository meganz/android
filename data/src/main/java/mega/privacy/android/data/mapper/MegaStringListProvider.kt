package mega.privacy.android.data.mapper

import nz.mega.sdk.MegaStringList
import javax.inject.Inject

/**
 * Mega string list provider
 */
class MegaStringListProvider @Inject constructor() {
    /**
     * Invoke
     *
     * @return new MegaStringList
     */
    operator fun invoke(): MegaStringList? = MegaStringList.createInstance()
}
