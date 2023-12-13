package mega.privacy.android.data.wrapper

import android.content.Context
import nz.mega.sdk.MegaUser

/**
 * Contact wrapper
 *
 */
interface ContactWrapper {

    /**
     * Retrieves the Mega User Name from the database
     *
     * @param megaUser A potentially nullable [MegaUser]
     * @return The Mega User Name from the database, or null if it cannot be found
     */
    fun getMegaUserNameDB(megaUser: MegaUser?): String?

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
     * Notify nickname update
     *
     * @param context
     * @param userHandle
     */
    fun notifyNicknameUpdate(context: Context, userHandle: Long)
}