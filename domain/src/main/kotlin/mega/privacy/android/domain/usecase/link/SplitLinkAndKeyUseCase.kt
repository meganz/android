package mega.privacy.android.domain.usecase.link

import mega.privacy.android.domain.entity.link.LinkAndKey
import javax.inject.Inject

/**
 * Splits a full public link into its link-without-key and decryption-key parts.
 *
 * Old file/folder links (`#!` / `#F!`) carry the key after a `!` separator; new links carry it
 * after a `#`. Returns a [LinkAndKey] with null parts when the link cannot be split.
 */
class SplitLinkAndKeyUseCase @Inject constructor() {

    /**
     * @param linkWithKey The full public link including its decryption key.
     * @return The [linkWithKey] split into its link-without-key and key parts.
     */
    operator fun invoke(linkWithKey: String): LinkAndKey {
        if (linkWithKey.contains("#!") || linkWithKey.contains("#F!")) {
            val parts = linkWithKey.split("!")
            if (parts.size == 3) {
                return LinkAndKey(
                    linkWithoutKey = "${parts[0]}!${parts[1]}",
                    key = parts[2],
                )
            }
        } else {
            val parts = linkWithKey.split("#")
            if (parts.size == 2) {
                return LinkAndKey(
                    linkWithoutKey = parts[0],
                    key = parts[1],
                )
            }
        }
        return LinkAndKey(linkWithoutKey = null, key = null)
    }
}
