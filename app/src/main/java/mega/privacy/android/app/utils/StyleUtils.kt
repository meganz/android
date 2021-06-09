package mega.privacy.android.app.utils

import android.content.Context
import android.os.Build
import android.widget.TextView
import androidx.core.content.ContextCompat
import mega.privacy.android.app.utils.ColorUtils.getThemeColor


object StyleUtils {

    @JvmStatic
    fun setTextStyle(
        context: Context,
        textView: TextView,
        textAppearance: Int,
        textColor: Int,
        isTextAttrColor: Boolean
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            textView.setTextAppearance(textAppearance)
        } else {
            textView.setTextColor(
                if (isTextAttrColor) getThemeColor(context, textColor)
                else ContextCompat.getColor(context, textColor)
            )
        }
    }
}