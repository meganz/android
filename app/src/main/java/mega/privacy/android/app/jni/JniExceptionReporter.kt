package mega.privacy.android.app.jni

import androidx.annotation.Keep


/***************************************************************************************************************************
 * These classes are referenced from the SWIG code and should not be moved without updating the .i files in the SDK project!      *
 *_-^- _-^-_-^-_-^- _-^-_-^-_-^- _-^-_-^-_-^- _-^-_-^-_-^- _-^-_-^-_-^- _-^-_-^-_-^- _-^-_-^-_-^- _-^-_-^-_-^- _-^-_-^-_-^- _-^-_-^-_-^- *
 ***************************************************************************************************************************/

/**
 * Interface for receiving exceptions that occur inside JNI code,
 * such as during callbacks from C++ into Java/Kotlin.
 */
@Keep
interface JniExceptionHandler {
    fun onJniException(location: String, message: String, stacktrace: String)
}


/**
 * Singleton container used by native code to report JNI exceptions
 * back into the managed layer.
 */
@Keep
object JniExceptionReporter {
    @JvmStatic
    var handler: JniExceptionHandler? = null
}