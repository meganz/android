package mega.privacy.android.app.fragments.homepage.photos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import mega.privacy.android.app.utils.ZoomUtil
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ZoomViewModel @Inject constructor(): ViewModel() {

    private var currentZoom = ZoomUtil.ZOOM_DEFAULT
    private val _zoom = MutableLiveData(ZoomUtil.ZOOM_DEFAULT)

    val zoom: LiveData<Int>
        get() = _zoom

    fun setZoom(zoom: Int) {
        _zoom.value = zoom
    }

    fun setCurrentZoom(currentZoom: Int) {
        this.currentZoom = currentZoom
    }

    fun getCurrentZoom(): Int {
        return currentZoom
    }

    /**
     * return a Boolean [true] indicate can zoom in, so we can from this flag to handle UI logic, otherwise, false.
     */
    fun zoomIn() {
        if (currentZoom < ZoomUtil.ZOOM_IN_1X) {
            // Don't use currentZoom++, shouldn't change the value of currentZoom here.
            setZoom(currentZoom + 1)
        }
    }

    /**
     * return a Boolean [true] indicate can zoom in, otherwise, false.
     */
    fun canZoomIn() = currentZoom != ZoomUtil.ZOOM_IN_1X


    /**
     * return a Boolean [true] Zoom in the max, otherwise, false.
     */
    fun zoomOut() {
        if (currentZoom > ZoomUtil.ZOOM_OUT_2X) {
            // Don't use currentZoom--, shouldn't change the value of currentZoom here.
            setZoom(currentZoom - 1)
        }
    }

    /**
     * return a Boolean [true] indicate can zoom out, so we can from this flag to handle UI logic, otherwise, false.
     */
    fun canZoomOut() = currentZoom != ZoomUtil.ZOOM_OUT_2X

    fun restoreDefaultZoom() {
        setZoom(ZoomUtil.ZOOM_DEFAULT)
    }
}