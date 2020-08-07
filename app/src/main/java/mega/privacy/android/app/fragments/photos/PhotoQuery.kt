package mega.privacy.android.app.fragments.photos

import nz.mega.sdk.MegaApiJava

data class PhotoQuery(var order: Int = MegaApiJava.ORDER_NONE, var searchDate: LongArray)  {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as PhotoQuery

        if (order != other.order) return false
        if (!searchDate.contentEquals(other.searchDate)) return false

        return true
        return false
    }

    override fun hashCode(): Int {
        var result = order
        result = 31 * result + searchDate.contentHashCode()
        return result
    }
}