package mega.privacy.android.data.mapper

import nz.mega.sdk.MegaStringList
import javax.inject.Inject

/**
 * Map [MegaStringList] to [List]
 */
internal class StringListMapper @Inject constructor() {

    /**
     * Convert [MegaStringList] to [List]
     *
     * @param megaStringList [MegaStringList]
     * @return              [List]
     */
    operator fun invoke(
        megaStringList: MegaStringList,
    ): List<String> = buildList {
        for (i in 0 until megaStringList.size()) {
            add(megaStringList.get(i))
        }
    }
}
