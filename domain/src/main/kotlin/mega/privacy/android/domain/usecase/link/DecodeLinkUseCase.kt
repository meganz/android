package mega.privacy.android.domain.usecase.link

import java.net.URLDecoder
import javax.inject.Inject

/**
 * Decode link use case
 *
 */
class DecodeLinkUseCase @Inject constructor() {
    /**
     * Invoke the use case
     *
     * @param link Link to decode
     * @return Decoded link
     */
    operator fun invoke(link: String): String {
        var url = link
        try {
            url = URLDecoder.decode(url, "UTF-8")
        } catch (ignore: Exception) {
        }

        url = url.replace(' ', '+')

        if (url.startsWith("mega://")) {
            url = url.replaceFirst("mega://", "https://mega.nz/")
        } else if (url.startsWith("mega.")) {
            url = url.replaceFirst("mega.", "https://mega.")
        }

        if (url.startsWith("https://www.mega.co.nz")) {
            url = url.replaceFirst("https://www.mega.co.nz", "https://mega.co.nz")
        }

        if (url.startsWith("https://www.mega.nz")) {
            url = url.replaceFirst("https://www.mega.nz", "https://mega.nz")
        }

        if (url.endsWith("/")) {
            url = url.substring(0, url.length - 1)
        }

        return url
    }
}