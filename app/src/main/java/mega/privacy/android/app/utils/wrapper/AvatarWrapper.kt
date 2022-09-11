package mega.privacy.android.app.utils.wrapper

import android.graphics.Bitmap

/**
 * avatar wrapper
 */
interface AvatarWrapper {
    /**
     * get dominant color from bitmap
     */
    fun getDominantColor(bimap: Bitmap): Int

    /**
     * get specific avatar color
     *
     * @param typeColor type of color
     */
    fun getSpecificAvatarColor(typeColor: String): Int
}