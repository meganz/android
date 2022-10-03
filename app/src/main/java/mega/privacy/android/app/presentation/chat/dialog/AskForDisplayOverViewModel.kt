package mega.privacy.android.app.presentation.chat.dialog

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import mega.privacy.android.app.DatabaseHandler
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.utils.IncomingCallNotification
import javax.inject.Inject

@HiltViewModel
class AskForDisplayOverViewModel @Inject constructor(
    val dbHandler: DatabaseHandler,
) : ViewModel() {

    private val _showDialog = MutableStateFlow(false)
    val showDialog: StateFlow<Boolean> = _showDialog.asStateFlow()

    private val _finish = MutableStateFlow(false)
    val finish: StateFlow<Boolean> get() = _finish

    private val _toSettings = MutableStateFlow(false)
    val toSettings: StateFlow<Boolean> get() = _toSettings

    private val _dismiss = MutableStateFlow(false)
    val dismiss: StateFlow<Boolean> get() = _dismiss

    /**
     * Method in charge of controlling whether the dialogue should be shown or not
     */
    fun onOpenDialog() {
        if (IncomingCallNotification.shouldNotify(MegaApplication.getInstance().applicationContext) && dbHandler.shouldAskForDisplayOver()) {
            _showDialog.value = true
        } else {
            _showDialog.value = false
            onFinishActivity()
        }
    }

    /**
     * Close the activity
     */
    fun onFinishActivity() {
        _finish.value = true
    }

    /**
     * Allow notifications for incoming calls
     */
    fun onAllow() {
        _showDialog.value = false
        _toSettings.value = true
    }

    /**
     * Dismiss dialog
     */
    fun onNotNow() {
        _showDialog.value = false
        _dismiss.value = true
    }

    /**
     * Method to register if the dialogue should not be shown again
     */
    fun onDestroy() {
        if (dbHandler.shouldAskForDisplayOver() && !_showDialog.value) {
            dbHandler.dontAskForDisplayOver()
        }
    }
}