package mega.privacy.android.app.audioplayer

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.components.attacher.NodeAttacher
import mega.privacy.android.app.components.saver.MegaNodeSaver
import mega.privacy.android.app.components.saver.OfflineNodeSaver
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.utils.Constants.*
import mega.privacy.android.app.utils.MegaNodeUtilKt

/**
 * ViewModel for main audio player UI logic.
 */
class AudioPlayerViewModel @ViewModelInject constructor(
    private val offlineNodeSaver: OfflineNodeSaver,
    private val megaNodeSaver: MegaNodeSaver,
    private val dbHandler: DatabaseHandler,
) : BaseRxViewModel() {

    private val _itemToRemove = MutableLiveData<Long>()
    val itemToRemove: LiveData<Long> = _itemToRemove

    private var nodeAttacher: NodeAttacher? = null

    /**
     * Save an offline node to device.
     *
     * @param handle node handle
     * @param activityStarter function to start activity
     */
    fun saveOfflineNode(handle: Long, activityStarter: (Intent, Int) -> Unit) {
        val node = dbHandler.findByHandle(handle) ?: return
        offlineNodeSaver.save(listOf(node), false, activityStarter)
    }

    /**
     * Save a mega node to device.
     *
     * @param handle node handle
     * @param isFolderLink if this node is a folder link node
     * @param activityStarter function to start activity
     */
    fun saveMegaNode(handle: Long, isFolderLink: Boolean, activityStarter: (Intent, Int) -> Unit) {
        megaNodeSaver.save(listOf(handle), false, isFolderLink, activityStarter)
    }

    fun attachNodeToChats(handle: Long, activityLauncher: ActivityLauncher) {
        nodeAttacher = NodeAttacher(activityLauncher)
        nodeAttacher?.attachNode(handle)
    }

    /**
     * Handle activity result launched by NodeSaver.
     *
     * @param requestCode requestCode of onActivityResult
     * @param resultCode resultCode of onActivityResult
     * @param data data of onActivityResult
     * @param snackbarShower interface to show snackbar
     * @param activityLauncher interface to start activity
     */
    fun handleActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        snackbarShower: SnackbarShower,
        activityLauncher: ActivityLauncher
    ) {
        if (offlineNodeSaver.handleActivityResult(requestCode, resultCode, data)
            || megaNodeSaver.handleActivityResult(requestCode, resultCode, data)
            || resultCode != RESULT_OK || data == null
        ) {
            return
        }

        when (requestCode) {
            REQUEST_CODE_SELECT_CHAT -> {
                nodeAttacher?.handleActivityResult(requestCode, resultCode, data, snackbarShower)
            }
            REQUEST_CODE_SELECT_MOVE_FOLDER -> {
                val handles = MegaNodeUtilKt.handleSelectMoveFolderResult(
                    requestCode, resultCode, data, snackbarShower
                )

                for (handle in handles) {
                    _itemToRemove.value = handle
                }
            }
            REQUEST_CODE_SELECT_COPY_FOLDER -> {
                MegaNodeUtilKt.handleSelectCopyFolderResult(
                    requestCode, resultCode, data, snackbarShower, activityLauncher
                )
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        offlineNodeSaver.destroy()
        megaNodeSaver.destroy()
    }
}
