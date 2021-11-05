package mega.privacy.android.app.fragments.homepage.photos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_DEFAULT
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_IN_1X
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_OUT_2X
import javax.inject.Singleton

class ZoomManager {

    private var currentZoom = ZOOM_DEFAULT
    private val _zoom = MutableLiveData(ZOOM_DEFAULT)

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
    fun zoomIn(): Boolean {
        if (currentZoom < ZOOM_IN_1X) {
            // Don't use currentZoom++, shouldn't change the value of currentZoom here.
            setZoom(currentZoom + 1)
        }
        return canZoomIn()
    }

    /**
     * return a Boolean [true] indicate can zoom in, otherwise, false.
     */
    fun canZoomIn(): Boolean {
        return currentZoom != ZOOM_IN_1X
    }


    /**
     * return a Boolean [true] Zoom in the max, otherwise, false.
     */
    fun zoomOut(): Boolean {
        if (currentZoom > ZOOM_OUT_2X) {
            // Don't use currentZoom--, shouldn't change the value of currentZoom here.
            setZoom(currentZoom - 1)
        }
        return canZoomOut()
    }

    /**
     * return a Boolean [true] indicate can zoom out, so we can from this flag to handle UI logic, otherwise, false.
     */
    fun canZoomOut(): Boolean {
        return currentZoom != ZOOM_OUT_2X
    }

    fun restoreDefaultZoom() {
        setZoom(ZOOM_DEFAULT)
    }
}