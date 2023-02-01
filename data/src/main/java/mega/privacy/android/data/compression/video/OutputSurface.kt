package mega.privacy.android.data.compression.video

import android.graphics.SurfaceTexture
import android.opengl.EGL14
import android.os.Handler
import android.os.HandlerThread
import android.view.Surface
import timber.log.Timber
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay
import javax.microedition.khronos.egl.EGLSurface


/**
 * Holds state associated with a Surface used for MediaCodec decoder output.
 *
 *
 * The (width,height) constructor for this class will prepare GL, create a SurfaceTexture,
 * and then create a Surface for that SurfaceTexture.  The Surface can be passed to
 * MediaCodec.configure() to receive decoder output.  When a frame arrives, we latch the
 * texture with updateTexImage, then render the texture with GL to a pbuffer.
 *
 *
 * The no-arg constructor skips the GL preparation step and doesn't allocate a pbuffer.
 * Instead, it just creates the Surface and SurfaceTexture, and when a frame arrives
 * we just draw it on whatever surface is current.
 *
 *
 * By default, the Surface will be using a BufferQueue in asynchronous mode, so we
 * can potentially drop frames.
 */
class OutputSurface : SurfaceTexture.OnFrameAvailableListener {
    private var mEGL: EGL10? = null
    private var mEGLDisplay: EGLDisplay? = null
    private var mEGLContext: EGLContext? = null
    private var mEGLSurface: EGLSurface? = null
    private var mSurfaceTexture: SurfaceTexture? = null

    /**
     * Returns the Surface that we draw onto.
     */
    var surface: Surface? = null
        private set
    private val mFrameSyncObject = Object() // guards mFrameAvailable
    private var mFrameAvailable = false
    private var mTextureRender: TextureRender? = null
    private lateinit var mHandlerThread: HandlerThread
    private var mHandler: Handler? = null

    /**
     * Creates an OutputSurface backed by a pbuffer with the specifed dimensions.  The new
     * EGL context and surface will be made current.  Creates a Surface that can be passed
     * to MediaCodec.configure().
     */
    constructor(width: Int, height: Int) {
        require(!(width <= 0 || height <= 0))
        eglSetup(width, height)
        makeCurrent()
        setup()
    }

    /**
     * Creates an OutputSurface using the current EGL context.  Creates a Surface that can be
     * passed to MediaCodec.configure().
     */
    constructor() {
        setup()
    }

    /**
     * Creates instances of TextureRender and SurfaceTexture, and a Surface associated
     * with the SurfaceTexture.
     */
    private fun setup() {
        mTextureRender = TextureRender()
        mTextureRender?.let {
            it.surfaceCreated()
            mHandlerThread = HandlerThread("callback-thread")
            mHandlerThread.start()
            mHandler = Handler(mHandlerThread.looper)

            // Even if we don't access the SurfaceTexture after the constructor returns, we
            // still need to keep a reference to it.  The Surface doesn't retain a reference
            // at the Java level, so if we don't either then the object can get GCed, which
            // causes the native finalizer to run.
            if (VERBOSE) Timber.tag(TAG).d("textureID=${mTextureRender?.textureId}")
            mSurfaceTexture = SurfaceTexture(it.textureId)
            // This doesn't work if OutputSurface is created on the thread that CTS started for
            // these test cases.
            //
            // The CTS-created thread has a Looper, and the SurfaceTexture constructor will
            // create a Handler that uses it.  The "frame available" message is delivered
            // there, but since we're not a Looper-based thread we'll never see it.  For
            // this to do anything useful, OutputSurface must be created on a thread without
            // a Looper, so that SurfaceTexture uses the main application Looper instead.
            //
            // Java language note: passing "this" out of a constructor is generally unwise,
            // but we should be able to get away with it here.
            mSurfaceTexture?.setOnFrameAvailableListener(this, mHandler)
            surface = Surface(mSurfaceTexture)
        }
    }

    /**
     * Prepares EGL.  We want a GLES 2.0 context and a surface that supports pbuffer.
     */
    private fun eglSetup(width: Int, height: Int) {
        mEGL = EGLContext.getEGL() as EGL10
        mEGLDisplay = mEGL?.eglGetDisplay(EGL10.EGL_DEFAULT_DISPLAY)
        if (mEGL?.eglInitialize(mEGLDisplay, null) == false) {
            throw RuntimeException("unable to initialize EGL10")
        }
        // Configure EGL for pbuffer and OpenGL ES 2.0.  We want enough RGB bits
        // to be able to tell if the frame is reasonable.
        val attribList = intArrayOf(
            EGL10.EGL_RED_SIZE, 8,
            EGL10.EGL_GREEN_SIZE, 8,
            EGL10.EGL_BLUE_SIZE, 8,
            EGL10.EGL_SURFACE_TYPE, EGL10.EGL_PBUFFER_BIT,
            EGL10.EGL_RENDERABLE_TYPE, EGL_OPENGL_ES2_BIT,
            EGL10.EGL_NONE
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfigs = IntArray(1)
        if (mEGL?.eglChooseConfig(mEGLDisplay, attribList, configs, 1, numConfigs) == false) {
            throw RuntimeException("unable to find RGB888+pbuffer EGL config")
        }
        // Configure context for OpenGL ES 2.0.
        val attributeList = intArrayOf(
            EGL14.EGL_CONTEXT_CLIENT_VERSION, 2,
            EGL10.EGL_NONE
        )
        mEGLContext = mEGL?.eglCreateContext(
            mEGLDisplay,
            configs[0], EGL10.EGL_NO_CONTEXT, attributeList
        )
        checkEglError("eglCreateContext")
        if (mEGLContext == null) {
            throw RuntimeException("null context")
        }
        // Create a pbuffer surface.  By using this for output, we can use glReadPixels
        // to test values in the output.
        val surfaceAttributes = intArrayOf(
            EGL10.EGL_WIDTH, width,
            EGL10.EGL_HEIGHT, height,
            EGL10.EGL_NONE
        )
        mEGLSurface = mEGL?.eglCreatePbufferSurface(mEGLDisplay, configs[0], surfaceAttributes)
        checkEglError("eglCreatePbufferSurface")
        if (mEGLSurface == null) {
            throw RuntimeException("surface was null")
        }
    }

    /**
     * Discard all resources held by this class, notably the EGL context.
     */
    fun release() {
        mEGL?.let {
            if (it.eglGetCurrentContext() == mEGLContext) {
                // Clear the current context and surface to ensure they are discarded immediately.
                it.eglMakeCurrent(
                    mEGLDisplay,
                    EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_SURFACE,
                    EGL10.EGL_NO_CONTEXT
                )
            }
            it.eglDestroySurface(mEGLDisplay, mEGLSurface)
            it.eglDestroyContext(mEGLDisplay, mEGLContext)
            //mEGL.eglTerminate(mEGLDisplay);
        }
        surface?.release()
        // this causes a bunch of warnings that appear harmless but might confuse someone:
        //  W BufferQueue: [unnamed-3997-2] cancelBuffer: BufferQueue has been abandoned!
        //mSurfaceTexture.release();
        // null everything out so future attempts to use this object will cause an NPE
        mEGLDisplay = null
        mEGLContext = null
        mEGLSurface = null
        mEGL = null
        mTextureRender = null
        surface = null
        mSurfaceTexture = null
    }

    /**
     * Makes our EGL context and surface current.
     */
    private fun makeCurrent() {
        if (mEGL == null) {
            throw RuntimeException("not configured for makeCurrent")
        }
        checkEglError("before makeCurrent")
        if (mEGL?.eglMakeCurrent(mEGLDisplay, mEGLSurface, mEGLSurface, mEGLContext) == false) {
            throw RuntimeException("eglMakeCurrent failed")
        }
    }

    /**
     * Replaces the fragment shader.
     */
    fun changeFragmentShader(fragmentShader: String) {
        mTextureRender?.changeFragmentShader(fragmentShader)
    }

    /**
     * Latches the next buffer into the texture.  Must be called from the thread that created
     * the OutputSurface object, after the onFrameAvailable callback has signaled that new
     * data is available.
     */
    fun awaitNewImage() {
        synchronized(mFrameSyncObject) {
            while (!mFrameAvailable) {
                try {
                    // Wait for onFrameAvailable() to signal us.  Use a timeout to avoid
                    // stalling the test if it doesn't arrive.
                    mFrameSyncObject.wait(500)
                    if (!mFrameAvailable) {
                        //if "spurious wakeup", continue while loop
                        throw RuntimeException("Surface frame wait timed out")
                    }
                } catch (ie: InterruptedException) {
                    // shouldn't happen
                    throw RuntimeException(ie)
                }
            }
            mFrameAvailable = false
        }
        // Latch the data.
        mTextureRender?.checkGlError("before updateTexImage")
        mSurfaceTexture?.updateTexImage()
    }

    /**
     * Draws the data from SurfaceTexture onto the current EGL surface.
     */
    fun drawImage() {
        mSurfaceTexture?.let {
            mTextureRender?.drawFrame(it)
        }
    }

    /**
     * onFrameAvailable
     */
    override fun onFrameAvailable(st: SurfaceTexture) {
        if (VERBOSE) Timber.d("new frame available")
        synchronized(mFrameSyncObject) {
            if (mFrameAvailable) {
                throw RuntimeException("mFrameAvailable already set, frame could be dropped")
            }
            mFrameAvailable = true
            mFrameSyncObject.notifyAll()
        }
    }

    /**
     * Checks for EGL errors.
     */
    private fun checkEglError(msg: String) {
        var failed = false
        var error = 0
        while (mEGL?.eglGetError()?.also { error = it } != EGL10.EGL_SUCCESS) {
            Timber.e("$msg : EGL error: 0x ${Integer.toHexString(error)}")
            failed = true
        }
        if (failed) {
            throw RuntimeException("EGL error encountered (see log)")
        }
    }

    private companion object {
        const val TAG = "OutputSurface"
        const val VERBOSE = false
        const val EGL_OPENGL_ES2_BIT = 4
    }
}
