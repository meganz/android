package mega.privacy.android.app.utils

import android.app.Activity
import android.content.Context

object ContextUtils {

    fun Context.isValid(): Boolean =
        !(this as Activity).isFinishing && !isDestroyed
}
