package mega.privacy.android.data.mapper.meeting

import nz.mega.sdk.MegaIntegerList
import javax.inject.Inject

/**
 * Mapper to convert list of int to [MegaIntegerList]
 */
internal class MegaIntegerListMapper @Inject constructor() {
    operator fun invoke(list: List<Int>?): MegaIntegerList {
        val integerList: MegaIntegerList = MegaIntegerList.createInstance()
        list?.forEach {
            integerList.add(it.toLong())
        }
        return integerList
    }
}