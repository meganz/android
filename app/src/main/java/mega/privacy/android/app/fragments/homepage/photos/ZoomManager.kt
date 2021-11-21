package mega.privacy.android.app.fragments.homepage.photos

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_DEFAULT
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_IN_1X
import mega.privacy.android.app.utils.ZoomUtil.ZOOM_OUT_2X

/**
 * ZoomManager is used for encapsulate zoom functions by a composition way
 */
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

    /**
     * Applies a zoom in and checks if can zoom in after it.
     *
     * @return True if can zoom in, false otherwise.
     */
    fun zoomIn(): Boolean {
        if (currentZoom < ZOOM_IN_1X) {
            // Don't use currentZoom++, shouldn't change the value of currentZoom here.
            setZoom(currentZoom + 1)
        }
        return canZoomIn()
    }

    /**
     * Check can zoom in.
     *
     * @return True if can zoom in, false otherwise.
     */
    fun canZoomIn(): Boolean {
        return currentZoom != ZOOM_IN_1X
    }


    /**
     * Applies a zoom out and checks if can zoom out after it.
     *
     * @return True if can zoom out, false otherwise.
     */
    fun zoomOut(): Boolean {
        if (currentZoom > ZOOM_OUT_2X) {
            // Don't use currentZoom--, shouldn't change the value of currentZoom here.
            setZoom(currentZoom - 1)
        }
        return canZoomOut()
    }

    /**
     * Check can zoom out.
     *
     * @return True if can zoom out, false otherwise.
     */
    fun canZoomOut(): Boolean {
        return currentZoom != ZOOM_OUT_2X
    }

    fun restoreDefaultZoom() {
        setZoom(ZOOM_DEFAULT)
    }
}