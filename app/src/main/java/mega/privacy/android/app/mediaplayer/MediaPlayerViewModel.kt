package mega.privacy.android.app.mediaplayer

import android.app.Activity.RESULT_OK
import android.content.Context
import android.content.Intent
import androidx.hilt.lifecycle.ViewModelInject
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.arch.BaseRxViewModel
import mega.privacy.android.app.interfaces.ActivityLauncher
import mega.privacy.android.app.interfaces.SnackbarShower
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_FOLDER_TO_COPY
import mega.privacy.android.app.utils.Constants.REQUEST_CODE_SELECT_FOLDER_TO_MOVE
import mega.privacy.android.app.utils.MegaNodeUtil.handleSelectFolderToCopyResult
import mega.privacy.android.app.utils.MegaNodeUtil.handleSelectFolderToMoveResult

/**
 * ViewModel for main audio player UI logic.
 */
class MediaPlayerViewModel @ViewModelInject constructor() : BaseRxViewModel() {

    private val _itemToRemove = MutableLiveData<Long>()
    val itemToRemove: LiveData<Long> = _itemToRemove

    /**
     * Handle activity result.
     *
     * @param context          Current Context.
     * @param requestCode      RequestCode of onActivityResult
     * @param resultCode       ResultCode of onActivityResult
     * @param data             Data of onActivityResult
     * @param snackbarShower   Interface to show snackbar
     * @param activityLauncher Interface to start activity
     */
    fun handleActivityResult(
        context: Context,
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
                val handles = handleSelectFolderToMoveResult(
                    context, requestCode, resultCode, data, snackbarShower
                )

                for (handle in handles) {
                    _itemToRemove.value = handle
                }
            }
            REQUEST_CODE_SELECT_FOLDER_TO_COPY -> {
                handleSelectFolderToCopyResult(
                    context, requestCode, resultCode, data, snackbarShower, activityLauncher
                )
            }
        }
    }
}
