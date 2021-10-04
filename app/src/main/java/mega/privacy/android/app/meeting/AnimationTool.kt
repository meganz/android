package mega.privacy.android.app.meeting

import android.view.View
import android.view.animation.TranslateAnimation
import androidx.core.view.isVisible
import kotlinx.coroutines.ExperimentalCoroutinesApi
import mega.privacy.android.app.meeting.fragments.InMeetingFragment

object AnimationTool {

    @ExperimentalCoroutinesApi
    fun View.fadeInOut(dy: Float, duration: Long = InMeetingFragment.ANIMATION_DURATION, toTop: Boolean) {
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

    @ExperimentalCoroutinesApi
    fun View.moveY(dy: Float, duration: Long = InMeetingFragment.ANIMATION_DURATION) {
        animate().y(dy).setDuration(duration).start()
    }

    @ExperimentalCoroutinesApi
    fun View.moveX(dx: Float, duration: Long = InMeetingFragment.ANIMATION_DURATION) {
        animate().x(dx).setDuration(duration).start()
    }

    fun View.clearAnimationAndGone() {
        clearAnimation().also { isVisible = false }
    }
}

