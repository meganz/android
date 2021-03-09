package mega.privacy.android.app.mediaplayer

import android.app.Activity.RESULT_OK
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_FOLDER_TO_COPY
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_FOLDER_TO_MOVE
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
            REQUEST_CODE_SELECT_FOLDER_TO_MOVE -> {
                val handles = MegaNodeUtilKt.handleSelectFolderToMoveResult(
                    requestCode, resultCode, data, snackbarShower
                )

                for (handle in handles) {
                    _itemToRemove.value = handle
                }
            }
            REQUEST_CODE_SELECT_FOLDER_TO_COPY -> {
                MegaNodeUtilKt.handleSelectFolderToCopyResult(
                    requestCode, resultCode, data, snackbarShower, activityLauncher
                )
            }
        }
    }
}
