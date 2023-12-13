package mega.privacy.android.app.presentation.bottomsheet.model

import android.os.Parcelable
import androidx.annotation.DrawableRes
import kotlinx.parcelize.Parcelize

/**
 * Data class that holds specific information about a Device Center Node, which is then displayed
 * in the Legacy Node Options Bottom Sheet header
 *
 * @property name The Device Center Node Name
 * @property status The Device Center Node Status
 * @property statusColorInt an optional Text Color for the Node Status, represented as an [Int]
 * @property icon The specific Icon of the Device Center Node, represented as an [Int]
 * @property statusIcon An optional Icon shown beside the Device Center Node Status, represented as
 * an [Int]
 */
@Parcelize
data class NodeDeviceCenterInformation(
    val name: String,
    val status: String,
    val statusColorInt: Int? = null,
    @DrawableRes val icon: Int,
    @DrawableRes val statusIcon: Int? = null,
) : Parcelable
