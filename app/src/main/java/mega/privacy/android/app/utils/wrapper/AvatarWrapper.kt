package mega.privacy.android.app.utils.wrapper

/**
 * avatar wrapper
 */
interface AvatarWrapper {
    /**
     * get specific avatar color
     *
     * @param typeColor type of color
     */
    fun getSpecificAvatarColor(typeColor: String): Int
}