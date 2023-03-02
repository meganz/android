package mega.privacy.android.data.mapper

import nz.mega.sdk.MegaHandleList

/**
 * Handle List mapper
 */
internal fun interface HandleListMapper {
    /**
     * Invoke
     *
     * @param handleList
     * @return List of handles
     */
    operator fun invoke(handleList: MegaHandleList): List<Long>
}