package mega.privacy.android.app.components.saver

import android.content.Context

abstract class Saving(
    val totalSize: Long,
    val highPriority: Boolean
) {
    var unsupportedFileName = ""
        protected set

    abstract fun hasUnsupportedFile(context: Context): Boolean

    companion object {
        val NOTHING = object : Saving(0, false) {
            override fun hasUnsupportedFile(context: Context): Boolean = false
        }
    }
}
