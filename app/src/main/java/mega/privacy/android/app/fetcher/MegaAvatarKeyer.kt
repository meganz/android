package mega.privacy.android.app.fetcher

import coil.key.Keyer
import coil.request.Options
import mega.privacy.android.domain.entity.user.ContactAvatar

/**
 * Mega thumbnail keyer to build the key for MegaNode thumbnail in the memory cache
 *
 */
internal object MegaAvatarKeyer : Keyer<ContactAvatar> {
    override fun key(data: ContactAvatar, options: Options): String =
        "${data.id}-${options.size}"
}