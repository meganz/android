package mega.privacy.android.data.mapper.handles

import nz.mega.sdk.MegaHandleList
import javax.inject.Inject

/**
 * Mapper for converting data into a [MegaHandleList]
 */
internal class MegaHandleListMapper @Inject constructor(
    private val megaHandleListProvider: MegaHandleListProvider
) {

    operator fun invoke(handleList: List<Long>): MegaHandleList? =
        megaHandleListProvider()?.also { megaHandleList ->
            handleList.map { megaHandleList.addMegaHandle(it) }
        }
}