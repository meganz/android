package mega.privacy.android.data.wrapper

import android.content.Context
import nz.mega.sdk.MegaStringMap
import nz.mega.sdk.MegaUser

/**
 * Contact wrapper
 *
 */
interface ContactWrapper {
    /**
     * Notify first name update
     *
     * @param context
     * @param userHandle
     */
    fun notifyFirstNameUpdate(context: Context, userHandle: Long)

    /**
     * Notify last name update
     *
     * @param context
     * @param userHandle
     */
    fun notifyLastNameUpdate(context: Context, userHandle: Long)

    /**
     * Update db nickname
     *
     * @param contacts
     * @param context
     * @param map
     */
    fun updateDBNickname(contacts: List<MegaUser>, context: Context, map: MegaStringMap)
}