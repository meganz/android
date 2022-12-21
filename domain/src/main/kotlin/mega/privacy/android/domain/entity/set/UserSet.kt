package mega.privacy.android.domain.entity.set

/**
 * This interface defines the MegaSet entity from SDK
 */
interface UserSet {

    /**
     * The Set ID
     */
    val id: Long

    /**
     * The Set name
     */
    val name: String

    /**
     * The Set cover
     */
    val cover: Long?

    /**
     * The Set modification time
     */
    val modificationTime: Long
}