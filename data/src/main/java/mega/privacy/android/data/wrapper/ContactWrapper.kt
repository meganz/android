package mega.privacy.android.data.wrapper

import android.content.Context

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
}