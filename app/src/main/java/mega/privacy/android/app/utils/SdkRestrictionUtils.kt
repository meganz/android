package mega.privacy.android.app.utils

import android.os.Build

object SdkRestrictionUtils {

    /**
     * Check if current Android Version is compatible with Save To Gallery functionality.
     *
     * @return  True if it's compatible, false otherwise.
     */
    fun isSaveToGalleryCompatible(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.R
}
