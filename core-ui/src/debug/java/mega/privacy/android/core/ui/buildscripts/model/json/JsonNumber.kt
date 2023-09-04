package mega.privacy.android.core.ui.buildscripts.model.json

import com.google.gson.annotations.SerializedName

/**
 * Number following json structure
 */
internal data class JsonNumber(
    override var name: String?,
    @SerializedName("\$value")
    val value: Int,
) : JsonCoreUiObject