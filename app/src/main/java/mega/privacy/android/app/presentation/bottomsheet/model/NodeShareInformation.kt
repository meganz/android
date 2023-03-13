package mega.privacy.android.app.presentation.bottomsheet.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Node share information
 *
 * @property user
 * @property isPending
 * @property isVerified
 */
@Parcelize
data class NodeShareInformation(
    val user: String?,
    val isPending: Boolean,
    val isVerified: Boolean,
) : Parcelable