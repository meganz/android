package mega.privacy.android.app.audioplayer

import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.components.saver.MegaNodeSaver
import mega.privacy.android.app.components.saver.OfflineNodeSaver

class AudioPlayerViewModel @ViewModelInject constructor(
    private val offlineNodeSaver: OfflineNodeSaver,
    private val megaNodeSaver: MegaNodeSaver,
) : BaseRxViewModel() {

    fun saveOfflineNode(handle: Long, activityStarter: (Intent, Int) -> Unit) {
        offlineNodeSaver.save(handle, false, activityStarter)
    }

    fun saveMegaNode(handle: Long, isFolderLink: Boolean, activityStarter: (Intent, Int) -> Unit) {
        megaNodeSaver.save(listOf(handle), false, isFolderLink, activityStarter)
    }

    fun handleActivityResult(requestCode: Int, resultCode: Int, data: Intent?): Boolean {
        return offlineNodeSaver.handleActivityResult(requestCode, resultCode, data)
                || megaNodeSaver.handleActivityResult(requestCode, resultCode, data)
    }

    override fun onCleared() {
        super.onCleared()
        offlineNodeSaver.destroy()
        megaNodeSaver.destroy()
    }
}
