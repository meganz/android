package mega.privacy.android.app.main

import android.graphics.Bitmap
import android.os.Parcelable
import androidx.annotation.ColorRes
import kotlinx.parcelize.Parcelize

/**
 * Invitation contact info ui model
 *
 * @property id id of the contact
 * @property name name of the contact
 * @property type type of the contact info
 * @property filteredContactInfos Phone numbers and emails which don't exist on MEGA
 * @property displayInfo display info of the contact
 * @property avatarColorResId Contact's avatar color ID
 * @property handle the contact handle
 * @property bitmap the contact bitmap
 * @property isHighlighted indicates whether the contact is selected or not
 */
@Parcelize
data class InvitationContactInfo @JvmOverloads constructor(
    val id: Long = 0L,
    private val name: String = "",
    val type: Int = 0,
    val filteredContactInfos: List<String> = emptyList(),
    var displayInfo: String = "",
    @ColorRes val avatarColorResId: Int = 0,
    val handle: String? = null,
    var bitmap: Bitmap? = null,
    var isHighlighted: Boolean = false,
) : Parcelable, Cloneable {

    /**
     * Check if the selected contact has more than 1 info
     */
    fun hasMultipleContactInfos(): Boolean = filteredContactInfos.size > 1

    /**
     * Get the contact name
     */
    fun getContactName(): String = name.ifEmpty { displayInfo }

    /**
     * Check whether the contact is an email contact
     */
    fun isEmailContact(): Boolean = displayInfo.contains(AT_SIGN)

    /**
     * Creates and returns a copy of this object.
     *
     * Need to override this as public because clone is protected hence it will be a private subclass in Kotlin.
     */
    @Throws(CloneNotSupportedException::class)
    public override fun clone(): Any = super.clone()

    override fun toString(): String =
        "\n{id=$id, isHighlighted=$isHighlighted, type=$type, bitmap=$bitmap, name='$name', displayInfo='$displayInfo', handle='$handle', avatarColorResId='$avatarColorResId'}"

    companion object {
        /**
         * Contact header
         */
        const val TYPE_PHONE_CONTACT_HEADER = 1

        /**
         * Contact items
         */
        const val TYPE_PHONE_CONTACT = 3

        /**
         * Invitation via email
         */
        const val TYPE_MANUAL_INPUT_EMAIL = 4

        /**
         * Invitation via phone number
         */
        const val TYPE_MANUAL_INPUT_PHONE = 5

        private const val AT_SIGN = "@"

        /**
         * Create the invitation contact info model from the user input
         */
        @JvmStatic
        fun createManualInput(
            inputString: String,
            type: Int,
            @ColorRes avatarColorResId: Int,
        ) = InvitationContactInfo(
            id = inputString.hashCode().toLong(),
            type = type,
            displayInfo = inputString,
            avatarColorResId = avatarColorResId
        )
    }
}
