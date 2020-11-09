package mega.privacy.android.app.audioplayer

import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.DefaultControlDispatcher
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Timeline

class AudioPlayerControlDispatcher : DefaultControlDispatcher(0, 0) {
    private val window = Timeline.Window()

    override fun dispatchPrevious(player: Player): Boolean {
        val timeline = player.currentTimeline
        if (timeline.isEmpty || player.isPlayingAd) {
            return true
        }
        val windowIndex = player.currentWindowIndex
        timeline.getWindow(windowIndex, window)
        val previousWindowIndex = player.previousWindowIndex
        if (previousWindowIndex != C.INDEX_UNSET) {
            player.seekTo(previousWindowIndex, C.TIME_UNSET)
        } else {
            player.seekTo(windowIndex, 0)
        }
        return true
    }
}
