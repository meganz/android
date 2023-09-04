package mega.privacy.android.core.ui.buildscripts.model.json

import com.google.gson.annotations.SerializedName

/**
 * Color that is referencing another color by its token name
 */
internal data class JsonColorRef(
    override var name: String?,
    @SerializedName("\$value")
    val tokenName: JsonTokenName?,
) : JsonCoreUiObject