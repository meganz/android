package mega.privacy.android.core.ui.buildscripts.model.json

import androidx.compose.ui.graphics.Color
import com.google.gson.annotations.SerializedName

/**
 * Color following json structure
 */
internal data class JsonColor(
    override var name: String?,
    @SerializedName("\$value")
    val value: Color?,
) : JsonCoreUiObject
