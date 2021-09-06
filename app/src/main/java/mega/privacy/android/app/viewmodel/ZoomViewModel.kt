package mega.privacy.android.app.viewmodel

import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import mega.privacy.android.app.MegaApplication

class ZoomViewModel() : ViewModel() {

    private val _zoom = MutableLiveData<Int>()
    val zoom : LiveData<Int> = _zoom

    fun setZoom(zoom: Int) {
        _zoom.value = zoom
    }
}