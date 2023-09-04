package mega.privacy.android.core.ui.buildscripts.model.json

import kotlin.reflect.KClass

/**
 * Group of core-ui objects following json structure
 */
internal data class JsonGroup(
    override var name: String?,
    val children: List<JsonCoreUiObject>,
) : JsonCoreUiObject {
    fun hasChildOfType(clazz: KClass<*>): Boolean =
        children.any {
            it::class == clazz
                    || (it as? JsonGroup)?.hasChildOfType(clazz) == true
        }
}
