package mega.privacy.android.app.utils

import android.view.View
import android.view.ViewTreeObserver.OnGlobalLayoutListener

object ViewUtils {

    fun View.waitForLayout(runnable: Runnable) {
        viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                viewTreeObserver.removeOnGlobalLayoutListener(this)
                runnable.run()
            }
        })
    }
}
