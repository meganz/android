package mega.privacy.android.app.meeting

import android.view.View
import android.view.animation.TranslateAnimation
import androidx.core.view.isVisible

object AnimationTool {

    fun View.fadeInOut(dy: Float = 300f, duration: Long = 500, toTop: Boolean = false) {
      val animation = if (isVisible) TranslateAnimation(
            0f,
            0f,
            0f,
            if (toTop) -dy else dy
        ) else TranslateAnimation(
            0f,
            0f,
            if (toTop) -dy else dy,
            0f
        )

        animation.duration = duration
        animation.fillAfter = false
        startAnimation(animation)
        isVisible = !isVisible
    }

    fun View.moveY(dy: Float, duration: Long = 500) {
        animate().y(dy).setDuration(duration).start()
    }
}

