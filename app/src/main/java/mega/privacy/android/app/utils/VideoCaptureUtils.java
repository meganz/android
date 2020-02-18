package mega.privacy.android.app.utils;

import android.content.Context;

import org.webrtc.Camera1Enumerator;
import org.webrtc.CameraEnumerator;
import org.webrtc.CapturerObserver;
import org.webrtc.SurfaceTextureHelper;
import org.webrtc.VideoCapturer;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.lollipop.megachat.calls.ChatCallActivity;
import nz.mega.sdk.MegaChatApiAndroid;
import static mega.privacy.android.app.utils.LogUtil.*;

public class VideoCaptureUtils{

    static private VideoCapturer createCameraCapturer(CameraEnumerator enumerator, String deviceName) {
        logDebug("createCameraCapturer: " + deviceName);
        return enumerator.createCapturer(deviceName, null);
    }

    static private String[] deviceList() {
        logDebug("DeviceList");
        CameraEnumerator enumerator = new Camera1Enumerator(true);
        return enumerator.getDeviceNames();
    }

    public static void swapCamera(MegaChatApiAndroid megaChatApi, Context context){
        String currentCamera = megaChatApi.getVideoDeviceSelected();
        String newCamera;
        if(isFrontCamera(currentCamera)){
            newCamera = VideoCaptureUtils.getBackCamera();
        }else {
            newCamera = VideoCaptureUtils.getFrontCamera();
        }
        if (newCamera != null) {
            MegaApplication.setIsAllowedToShowVideo(false, isFrontCamera(newCamera));
            megaChatApi.setChatVideoInDevice(newCamera, ((ChatCallActivity)context));
        }
    }

    /**
     * Get the front camera device.
     * @return Front camera device.
     */
    static public String getFrontCamera() {
        return getCameraDevice(true);
    }

    /**
     * Get the back camera device.
     * @return Back camera device.
     */
    static public String getBackCamera() {
        return getCameraDevice(false);
    }

    /**
     * Get a camera device (front or back).
     * @param front Value to indicate the camera device to get (true: front / false: back).
     * @return The camera device (front or back) requested.
     */
    static private String getCameraDevice(boolean front) {
        CameraEnumerator enumerator = new Camera1Enumerator(true);
        String[] deviceList = deviceList();
        for (String device : deviceList) {
            if ((front && enumerator.isFrontFacing(device)) || (!front && enumerator.isBackFacing(device))) {
                return device;
            }
        }
        return null;
    }

    /**
     * Check if the camera device is the front camera.
     * @param device Camera device to check.
     * @return True if device is front camera or false in other case.
     */
    static public boolean isFrontCamera(String device) {
        CameraEnumerator enumerator = new Camera1Enumerator(true);
        return enumerator.isFrontFacing(device);
    }

    /**
     * Check if the camera device is the back camera.
     * @param device Camera device to check.
     * @return True if device is back camera or false in other case.
     */
    static public boolean isBackCamera(String device) {
        CameraEnumerator enumerator = new Camera1Enumerator(true);
        return enumerator.isBackFacing(device);
    }

    static private VideoCapturer videoCapturer = null;

    static public void stopVideoCapture() {
        logDebug("stopVideoCapture");

        if (videoCapturer != null) {
            try {
                videoCapturer.stopCapture();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            videoCapturer = null;
        }
    }

    static public void startVideoCapture(int videoWidth, int videoHeight, int videoFps, SurfaceTextureHelper surfaceTextureHelper, CapturerObserver nativeAndroidVideoTrackSource, String deviceName) {
        logDebug("startVideoCapture: " + deviceName);

        stopVideoCapture();
        Context context = MegaApplication.getInstance().getApplicationContext();

        videoCapturer = createCameraCapturer(new Camera1Enumerator(true), deviceName);

        if (videoCapturer == null) {
            logError("Unable to create video capturer");
            return;
        }

        videoCapturer.initialize(surfaceTextureHelper, context, nativeAndroidVideoTrackSource);

        // Start the capture!
        videoCapturer.startCapture(videoWidth, videoHeight, videoFps);
        logDebug("Start Capture");
    }

}
