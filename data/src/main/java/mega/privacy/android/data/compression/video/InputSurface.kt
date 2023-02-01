package mega.privacy.android.data.compression.video

import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLExt
import android.opengl.EGLSurface
import android.view.Surface
import timber.log.Timber


/**
 * Holds state associated with a Surface used for MediaCodec encoder input.
 *
 *
 * The constructor takes a Surface obtained from MediaCodec.createInputSurface(), and uses that
 * to create an EGL window surface.  Calls to eglSwapBuffers() cause a frame of data to be sent
 * to the video encoder.
 */
class InputSurface(private var surface: Surface?) {
    private var mEGLDisplay: EGLDisplay? = null
    private var mEGLContext: EGLContext? = null
    private var mEGLSurface: EGLSurface? = null

    /**
     * Returns the Surface that the MediaCodec receives buffers from.
     */

    /**
     * Creates an InputSurface from a Surface.
     */
    init {
        if (surface == null) throw NullPointerException()
        eglSetup()
    }

    /**
     * Prepares EGL.  We want a GLES 2.0 context and a surface that supports recording.
     */
    private fun eglSetup() {
        mEGLDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        if (mEGLDisplay === EGL14.EGL_NO_DISPLAY) {
            throw RuntimeException("unable to get EGL14 display")
        }
        val version = IntArray(2)
        if (!EGL14.eglInitialize(mEGLDisplay, version, 0, version, 1)) {
            mEGLDisplay = null
            throw RuntimeException("unable to initialize EGL14")
        }
        // Configure EGL for pbuffer and OpenGL ES 2.0.  We want enough RGB bits
        // to be able to tell if the frame is reasonable.
        val attribList = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL_RECORDABLE_ANDROID, 1,
            EGL14.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (!EGL14.eglChooseConfig(
                mEGLDisplay,
                attribList,
                0,
                configs,
                0,
                configs.size,
                numConfigs,
                0
            )
        ) {
            throw RuntimeException("unable to find RGB888+recordable ES2 EGL config")
        }
        // Configure context for OpenGL ES 2.0.
        val attributeList = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL14.EGL_NONE
        )
        mEGLContext = EGL14.eglCreateContext(
            mEGLDisplay,
            configs[0], EGL14.EGL_NO_CONTEXT, attributeList, 0
        )
        checkEglError("eglCreateContext")
        if (mEGLContext == null) {
            throw RuntimeException("null context")
        }
        // Create a window surface, and attach it to the Surface we received.
        val surfaceAttributes = intArrayOf(
            EGL14.EGL_NONE
        )
        mEGLSurface = EGL14.eglCreateWindowSurface(
            mEGLDisplay, configs[0],
            surface, surfaceAttributes, 0
        )
        checkEglError("eglCreateWindowSurface")
        if (mEGLSurface == null) {
            throw RuntimeException("surface was null")
        }
    }

    /**
     * Discard all resources held by this class, notably the EGL context.  Also releases the
     * Surface that was passed to our constructor.
     */
    fun release() {
        if (EGL14.eglGetCurrentContext() == mEGLContext) {
            // Clear the current context and surface to ensure they are discarded immediately.
            EGL14.eglMakeCurrent(
                mEGLDisplay,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_SURFACE,
                EGL14.EGL_NO_CONTEXT
            )
        }
        EGL14.eglDestroySurface(mEGLDisplay, mEGLSurface)
        EGL14.eglDestroyContext(mEGLDisplay, mEGLContext)
        //EGL14.eglTerminate(mEGLDisplay);
        surface?.release()
        // null everything out so future attempts to use this object will cause an NPE
        mEGLDisplay = null
        mEGLContext = null
        mEGLSurface = null
        surface = null
    }

    /**
     * Makes our EGL context and surface current.
     */
    fun makeCurrent() {
        if (!EGL14.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext)) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    /**
     * Calls eglSwapBuffers.  Use this to "publish" the current frame.
     */
    fun swapBuffers(): Boolean {
        return EGL14.eglSwapBuffers(mEGLDisplay, mEGLSurface)
    }

    /**
     * Sends the presentation time stamp to EGL.  Time is expressed in nanoseconds.
     */
    fun setPresentationTime(nsecs: Long) {
        EGLExt.eglPresentationTimeANDROID(mEGLDisplay, mEGLSurface, nsecs)
    }

    /**
     * Checks for EGL errors.
     */
    private fun checkEglError(msg: String) {
        var failed = false
        var error: Int
        while (EGL14.eglGetError().also { error = it } != EGL14.EGL_SUCCESS) {
            Timber.tag(TAG).e("$msg : EGL error: 0x ${Integer.toHexString(error)}")
            failed = true
        }
        if (failed) {
            throw RuntimeException("EGL error encountered (see log)")
        }
    }

    private companion object {
        const val TAG = "InputSurface"
        const val EGL_RECORDABLE_ANDROID = 0x3142
        const val EGL_OPENGL_ES2_BIT = 4
    }
}
