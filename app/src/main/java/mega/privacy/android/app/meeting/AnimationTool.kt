package mega.privacy.android.app.meeting

import android.view.View
import android.view.animation.TranslateAnimation

object AnimationTool {

    fun View.fadeInOut(duration: Long = 500, toTop: Boolean = false) {
        val isVisible = visibility == View.VISIBLE
        val y = 300f

        val animation = if (isVisible) TranslateAnimation(
            0f,
            0f,
            0f,
            if (toTop) -y else y
        ) else TranslateAnimation(
            0f,
            0f,
            if (toTop) -y else y,
            0f)

        animation.duration = duration
        animation.fillAfter = true
        startAnimation(animation)
        visibility = if (isVisible) View.GONE else View.VISIBLE
    }
}

