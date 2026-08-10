package mega.privacy.android.app.utils

import android.content.Context
import androidx.annotation.Keep
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.listeners.ChatChangeVideoStreamListener
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.CapturerObserver
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import timber.log.Timber

/**
 * The class can call from JNI to manage the video capture devices.
 * Don't move package name and change class name
 */
@Keep
object VideoCaptureUtils {

    private var videoCapturer: VideoCapturer? = null

    // Cache to avoid re-enumerating Camera2 on every video frame; invalidated when device name changes
    @Volatile
    private var cachedDeviceName: String? = null

    @Volatile
    private var cachedIsFrontCamera: Boolean = false

    private fun context(): Context = MegaApplication.getInstance().applicationContext

    /**
     * Indicates if show video is allowed. The default value is TRUE, but this value will change to
     * FALSE meanwhile swapping between front and back cameras. The value will be TRUE again once
     * the camera swapping is completed.
     */
    private var isVideoAllowed = true

    /**
     * Check if show video is allowed.
     *
     * @return TRUE if show video is allowed or FALSE in other case.
     * @see VideoCaptureUtils.isVideoAllowed
     */
    @JvmStatic
    fun isVideoAllowed(): Boolean = isVideoAllowed

    /**
     * Set if show video is allowed.
     *
     * @param isVideoAllowed Value to indicate if show video is allowed.
     * @see VideoCaptureUtils.isVideoAllowed
     */
    @JvmStatic
    fun setIsVideoAllowed(isVideoAllowed: Boolean) {
        try {
            VideoCaptureUtils.isVideoAllowed = isVideoAllowed
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    private fun createCameraCapturer(
        enumerator: CameraEnumerator,
        deviceName: String?,
    ): VideoCapturer? {
        Timber.d("createCameraCapturer: %s", deviceName)
        try {
            return enumerator.createCapturer(deviceName, null)
        } catch (e: Exception) {
            Timber.e(e)
        }
        return null
    }

    /**
     * Get the video capture devices list.
     *
     * Need `@JvmStatic`: SDK native code looks it up via GetStaticMethodID (see megachat.cpp).
     *
     * @return The video capture devices list.
     */
    @JvmStatic
    private fun deviceList(): Array<String> {
        Timber.d("DeviceList")
        try {
            val enumerator: CameraEnumerator = Camera2Enumerator(context())
            return enumerator.deviceNames
        } catch (e: Exception) {
            Timber.e(e)
        }
        return emptyArray()
    }

    /**
     * Swap the current camera device to the opposite camera device.
     *
     * @param listener Camera swap listener.
     */
    @JvmStatic
    fun swapCamera(listener: ChatChangeVideoStreamListener?) {
        try {
            val megaChatApi = MegaApplication.getInstance().megaChatApi
            val currentCamera = megaChatApi.videoDeviceSelected
            val newCamera = if (isFrontCamera(currentCamera)) {
                getBackCamera()
            } else {
                getFrontCamera()
            }
            if (newCamera != null) {
                isVideoAllowed = false
                megaChatApi.setChatVideoInDevice(newCamera, listener)
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
    }

    /**
     * Get the front camera device.
     *
     * @return Front camera device.
     */
    @JvmStatic
    fun getFrontCamera(): String? {
        try {
            return getCameraDevice(true)
        } catch (e: Exception) {
            Timber.e(e)
        }
        return null
    }

    /**
     * Get the back camera device.
     *
     * @return Back camera device.
     */
    @JvmStatic
    fun getBackCamera(): String? {
        try {
            return getCameraDevice(false)
        } catch (e: Exception) {
            Timber.e(e)
        }
        return null
    }

    /**
     * Get a camera device (front or back).
     *
     * @param front Value to indicate the camera device to get (true: front / false: back).
     * @return The camera device (front or back) requested. NULL if the requested device does not exist.
     */
    private fun getCameraDevice(front: Boolean): String? {
        try {
            val enumerator: CameraEnumerator = Camera2Enumerator(context())
            val deviceList = deviceList()
            for (device in deviceList) {
                if ((front && enumerator.isFrontFacing(device)) || (!front && enumerator.isBackFacing(device))) {
                    return device
                }
            }
        } catch (e: Exception) {
            Timber.e(e)
        }
        return null
    }

    /**
     * Check if the camera device is the front camera.
     *
     * @param device Camera device to check.
     * @return True if device is front camera or false in other case.
     */
    @JvmStatic
    fun isFrontCamera(device: String?): Boolean {
        try {
            val enumerator: CameraEnumerator = Camera2Enumerator(context())
            return enumerator.isFrontFacing(device)
        } catch (e: Exception) {
            Timber.e(e)
        }
        return false
    }

    /**
     * Check if the camera device is the back camera.
     *
     * @param device Camera device to check.
     * @return True if device is back camera or false in other case.
     */
    @JvmStatic
    fun isBackCamera(device: String?): Boolean {
        try {
            val enumerator: CameraEnumerator = Camera2Enumerator(context())
            return enumerator.isBackFacing(device)
        } catch (e: Exception) {
            Timber.e(e)
        }
        return false
    }

    private fun isFrontCameraFromCache(deviceName: String): Boolean {
        if (deviceName == cachedDeviceName) {
            return cachedIsFrontCamera
        }
        synchronized(VideoCaptureUtils::class.java) {
            // Re-check inside the lock: another thread may have already updated the cache
            // between the outer check and acquiring the lock, making enumeration redundant.
            if (deviceName != cachedDeviceName) {
                cachedIsFrontCamera = isFrontCamera(deviceName)
                cachedDeviceName = deviceName
            }
            return cachedIsFrontCamera
        }
    }

    /**
     * Check if the front camera is the current video device in use.
     *
     * @return True if the front camera is in use or false in other case.
     */
    @JvmStatic
    fun isFrontCameraInUse(): Boolean {
        try {
            val megaChatApi = MegaApplication.getInstance().megaChatApi
            val deviceName = megaChatApi.videoDeviceSelected
            if (deviceName.isNullOrEmpty()) return false
            return isFrontCameraFromCache(deviceName)
        } catch (e: Exception) {
            Timber.e(e)
        }
        return false
    }

    @JvmStatic
    fun stopVideoCapture() {
        Timber.d("stopVideoCapture")
        videoCapturer?.let {
            try {
                it.stopCapture()
            } catch (e: Exception) {
                Timber.e(e)
            }
            videoCapturer = null
        }
    }

    @JvmStatic
    fun startVideoCapture(
        videoWidth: Int,
        videoHeight: Int,
        videoFps: Int,
        surfaceTextureHelper: SurfaceTextureHelper?,
        nativeAndroidVideoTrackSource: CapturerObserver?,
        deviceName: String?,
    ) {
        Timber.d("startVideoCapture: %s", deviceName)

        stopVideoCapture()

        try {
            val context = context()
            val capturer = createCameraCapturer(Camera2Enumerator(context), deviceName)
            videoCapturer = capturer

            if (capturer == null) {
                Timber.e("Unable to create video capturer")
                return
            }

            capturer.initialize(surfaceTextureHelper, context, nativeAndroidVideoTrackSource)

            // Start the capture!
            capturer.startCapture(videoWidth, videoHeight, videoFps)
            Timber.d("Start Capture")
        } catch (e: Exception) {
            Timber.e(e)
        }
    }
}
