package mega.privacy.android.data.mapper

import nz.mega.sdk.MegaStringList
import javax.inject.Inject

/**
 * Map [List] to [MegaStringList]
 */
internal class MegaStringListMapper @Inject constructor(
    private val megaStringListProvider: MegaStringListProvider,
) {

    /**
     * Convert [List] to [MegaStringList]
     *
     * @param list   [List]
     * @return      [MegaStringList]
     */
    operator fun invoke(list: List<String>): MegaStringList? =
        megaStringListProvider()?.also { megaStringList ->
            list.forEach { megaStringList.add(it) }
        }
}
