package mega.privacy.android.app.presentation.manager

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/**
 * View model associated to [mega.privacy.android.app.main.ManagerActivity]
 * to hold data shared across fragments attached to the Activity
 */
@HiltViewModel
class ManagerDataViewModel @Inject constructor(
) : ViewModel() {

    /**
     * Dispatch the new rubbishBinParentHandle value to observers
     */
    private val _onRubbishBinParentHandleChanged: MutableLiveData<Long> = MutableLiveData(-1L)
    val onRubbishBinParentHandleChanged: LiveData<Long> = _onRubbishBinParentHandleChanged

    /**
     * Dispatch the new browserParentHandle value to observers
     */
    private val _onBrowserParentHandleChanged: MutableLiveData<Long> = MutableLiveData(-1L)
    val onBrowserParentHandleChanged: LiveData<Long> = _onBrowserParentHandleChanged

    /**
     * Set the current rubbish bin parent handle
     */
    fun setRubbishBinParentHandle(parentHandle: Long) {
        _onRubbishBinParentHandleChanged.value = parentHandle
    }

    /**
     * Get the current rubbish bin parent handle
     */
    fun getRubbishBinParentHandle(): Long {
        return onRubbishBinParentHandleChanged.value ?: -1L
    }

    /**
     * Set the current browser parent handle
     */
    fun setBrowserParentHandle(parentHandle: Long) {
        _onBrowserParentHandleChanged.value = parentHandle
    }

    /**
     * Get the current browser parent handle
     */
    fun getBrowserParentHandle(): Long {
        return _onBrowserParentHandleChanged.value ?: -1L
    }
}
