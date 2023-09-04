package mega.privacy.android.core.ui.buildscripts.model.json

/**
 * represents an unknown core ui json element
 */
internal data class JsonCoreUiObjectUnknown(
    override var name: String? = null,
) : JsonCoreUiObject {
    override fun toString() = "unknown"
}