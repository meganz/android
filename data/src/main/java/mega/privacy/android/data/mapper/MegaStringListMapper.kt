package mega.privacy.android.data.mapper

import nz.mega.sdk.MegaStringList
import javax.inject.Inject

/**
 * Map [MegaStringList] to [List]
 */
internal class MegaStringListMapper @Inject constructor() {

    /**
     * Convert [MegaStringList] to [List]
     *
     * @param megaStringList [MegaStringList]
     * @return              [List]
     */
    operator fun invoke(
        megaStringList: MegaStringList,
    ): List<String> =
        mutableListOf<String>().apply {
            for (i in 0 until megaStringList.size()) {
                add(megaStringList.get(i))
            }
        }

    /**
     * Convert [List] to [MegaStringList]
     *
     * @param list   [List]
     * @return      [MegaStringList]
     */
    operator fun invoke(list: List<String>): MegaStringList =
        MegaStringList.createInstance().apply {
            list.forEach { value ->
                add(value)
            }
        }
}
