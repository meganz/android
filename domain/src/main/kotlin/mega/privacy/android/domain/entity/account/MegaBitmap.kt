package mega.privacy.android.domain.entity.account

/**
 * Domain entity for bitmap data.
 */
data class MegaBitmap(
    /**
     * Bitmap width
     */
    val width: Int = 0,
    /**
     * Bitmap height
     */
    val height: Int = 0,
    /**
     * ARGB pixels of a bitmap
     */
    val pixels: IntArray? = null,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MegaBitmap

        if (width != other.width) return false
        if (height != other.height) return false
        if (pixels != null) {
            if (other.pixels == null) return false
            if (!pixels.contentEquals(other.pixels)) return false
        } else if (other.pixels != null) return false

        return true
    }

    override fun hashCode(): Int {
        var result = width
        result = 31 * result + height
        result = 31 * result + (pixels?.contentHashCode() ?: 0)
        return result
    }
}