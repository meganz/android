package mega.privacy.android.app.mediaplayer

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_COPY_FOLDER
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_MOVE_FOLDER
import mega.privacy.android.app.utils.MegaNodeUtilKt

/**
 * ViewModel for main audio player UI logic.
 */
class MediaPlayerViewModel @ViewModelInject constructor() : BaseRxViewModel() {

    private val _itemToRemove = MutableLiveData<Long>()
    val itemToRemove: LiveData<Long> = _itemToRemove

    /**
     * Handle activity result.
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
        if (resultCode != RESULT_OK || data == null) {
            return
        }

        when (requestCode) {
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
}
