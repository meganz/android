package mega.privacy.android.app.components.saver

import android.content.Context

abstract class Saving(
    val totalSize: Long,
    val highPriority: Boolean
) {
    var unsupportedFileName = ""
        protected set

    /**
     * Check if there is any unsupported file in this Saving.
     *
     * @param context Android context
     */
    abstract fun hasUnsupportedFile(context: Context): Boolean

    companion object {
        val NOTHING = object : Saving(0, false) {
            override fun hasUnsupportedFile(context: Context): Boolean = false
        }
    }
}
