package mega.privacy.android.app.utils.wrapper

import android.graphics.Bitmap
import android.graphics.BitmapFactory

/**
 * define methods relate to BitmapFactory
 */
interface BitmapFactoryWrapper {
    /**
     * decode file
     *
     * @param pathName
     * @param opts
     */
    fun decodeFile(pathName: String?, opts: BitmapFactory.Options): Bitmap?
}