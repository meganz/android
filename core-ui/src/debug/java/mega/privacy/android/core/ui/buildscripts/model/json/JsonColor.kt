package mega.privacy.android.core.ui.buildscripts.model.json

import androidx.compose.ui.graphics.Color
import com.google.gson.annotations.SerializedName
import mega.privacy.android.core.ui.buildscripts.kotlingenerator.getPropertyName

/**
 * Color following json structure
 */
internal data class JsonColor(
    override var name: String?,
    @SerializedName("\$value")
    val value: Color?,
) : JsonCoreUiObject, JsonLeaf {
    override fun getPropertyName(groupParentName: String?) =
        name.getPropertyName("Color", groupParentName)


    override fun getPropertyClass() = Color::class

    override fun getPropertyInitializer() =
        if (value == null) "Undefined" else {
            "Color(${value.red.toBase255()}, ${value.green.toBase255()}, ${value.blue.toBase255()}, ${value.alpha.toBase255()})"
        }
}

private fun Float.toBase255() = (this * 255f).toInt()
