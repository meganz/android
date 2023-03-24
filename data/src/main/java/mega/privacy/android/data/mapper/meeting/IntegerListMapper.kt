package mega.privacy.android.data.mapper.meeting

import nz.mega.sdk.MegaIntegerList
import javax.inject.Inject

/**
 * Mapper to convert [MegaIntegerList] to list of int
 */
internal class IntegerListMapper @Inject constructor() {
    operator fun invoke(integerList: MegaIntegerList): List<Int> = mutableListOf<Int>().apply {
        if (integerList.size() > 0) {
            for (i in 0..integerList.size()) {
                add(integerList.get(i).toInt())
            }
        }
    }
}