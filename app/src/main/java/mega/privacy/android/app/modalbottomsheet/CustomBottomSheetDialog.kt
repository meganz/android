package mega.privacy.android.app.modalbottomsheet

import android.content.Context
import android.content.res.Configuration.ORIENTATION_PORTRAIT
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.core.content.ContextCompat
import com.google.android.material.bottomsheet.BottomSheetDialog
import mega.privacy.android.app.R
import mega.privacy.android.app.utils.Util

class CustomBottomSheetDialog(context: Context, theme: Int) : BottomSheetDialog(context, theme) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val resources = context.resources
        val width = resources.displayMetrics.widthPixels
        val height = resources.displayMetrics.heightPixels
        val isPortraitMode = resources.configuration.orientation == ORIENTATION_PORTRAIT

        window?.setLayout(if (isPortraitMode) width else height, MATCH_PARENT)

        // Set navigation bar background with light grey to make navigation buttons visible under light mode.
        if (!Util.isDarkMode(context)) {
            window?.navigationBarColor = ContextCompat.getColor(context, R.color.white_alpha_070)
        }
    }
}