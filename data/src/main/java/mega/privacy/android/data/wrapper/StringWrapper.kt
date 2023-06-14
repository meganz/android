package mega.privacy.android.data.wrapper

/**
 * The interface for wrapping the static method regarding String
 */
interface StringWrapper {


    /**
     *Get localized progress size
     */
    fun getProgressSize(progress: Long, size: Long): String
}
