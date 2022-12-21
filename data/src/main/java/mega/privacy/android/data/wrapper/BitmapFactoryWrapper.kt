package mega.privacy.android.data.wrapper

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * define methods relate to BitmapFactory
 */
internal interface BitmapFactoryWrapper {
    /**
     * decode file
     *
     * @param pathName
     * @param opts
     */
    fun decodeFile(pathName: String?, opts: BitmapFactory.Options): Bitmap?
}