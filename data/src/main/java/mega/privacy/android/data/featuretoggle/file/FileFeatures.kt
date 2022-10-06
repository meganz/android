package mega.privacy.android.data.featuretoggle.file

import com.google.gson.annotations.SerializedName

/**
 * File features
 *
 * @property name
 * @property defaultValue
 */
data class FileFeatures(
    @SerializedName("name")
    val name: String,
    @SerializedName("value")
    val defaultValue: Boolean
)