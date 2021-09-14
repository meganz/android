package mega.privacy.android.app.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import mega.privacy.android.app.utils.ZoomUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZoomViewModel @Inject constructor(): ViewModel() {

    private val _zoom = MutableLiveData(ZoomUtil.ZOOM_DEFAULT)
    val zoom : LiveData<Int> = _zoom

    fun setZoom(zoom: Int) {
        _zoom.value = zoom
    }
}