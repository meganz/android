package mega.privacy.android.app.utils.wrapper

import org.webrtc.CameraEnumerator

/**
 * WebRTC Camera enumerator wrapper
 */
interface CameraEnumeratorWrapper {

    /**
     * Retrieve Camera Enumerator
     *
     * @return  [CameraEnumerator]
     */
    operator fun invoke(): CameraEnumerator
}
