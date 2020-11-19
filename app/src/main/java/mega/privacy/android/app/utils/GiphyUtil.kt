package mega.privacy.android.app.utils

import android.net.Uri
import mega.privacy.android.app.services.GiphyService.BASE_URL
import mega.privacy.android.app.services.GiphyService.GIPHY_URL
import mega.privacy.android.app.services.GiphyService.TEST_URL
import mega.privacy.android.app.utils.LogUtil.logError

class GiphyUtil {

    companion object {
        /**
         * Gets the original src of a Giphy by replacing GIPHY_URL to the endpoint.
         *
         * @param giphyUri Uri of a Giphy with the GIPHY_URL beginning.
         * @return The final src with real endpoint.
         */
        @JvmStatic
        fun getOriginalGiphySrc(giphyUri: String?): Uri? {
            if (!TextUtil.isTextEmpty(giphyUri) && giphyUri?.contains(GIPHY_URL) == true) {
                return Uri.parse(giphyUri.replace(GIPHY_URL, BASE_URL))
            }

            logError("Wrong giphyUri: $giphyUri")
            return if (TextUtil.isTextEmpty(giphyUri)) null else Uri.parse(giphyUri)
        }

        /**
         * Modifies the original src of a Giphy by replacing the endpoint to GIPHY_URL.
         *
         * @param originalSrc   Original src of a Giphy with the original endpoint.
         * @return The final src with GIPHY_URL.
         */
        @JvmStatic
        fun getGiphySrc(originalSrc: String?): String? {
            if (originalSrc?.contains(BASE_URL) == true) {
                return originalSrc.replace(BASE_URL, GIPHY_URL)
            } else if (originalSrc?.contains(TEST_URL) == true) {
                return originalSrc.replace(TEST_URL, GIPHY_URL)
            }

            return originalSrc
        }
    }
}