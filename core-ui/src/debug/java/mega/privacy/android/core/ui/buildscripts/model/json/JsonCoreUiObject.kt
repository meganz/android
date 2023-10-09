package mega.privacy.android.core.ui.buildscripts.model.json

import kotlin.reflect.KClass

/**
 * Generic interface for core-ui elements represented in a json
 */
internal sealed interface JsonCoreUiObject {
    var name: String?
}

internal interface SemanticValueRef {
    fun getPropertyName(groupParentName: String?): String
    fun getPropertyClass(): KClass<*>
    fun getPropertyInitializer(): String
    fun getValueForDataClassInitializer(groupParentName: String?): String
}

internal interface JsonLeaf {
    fun getPropertyName(groupParentName: String?): String
    fun getPropertyClass(): KClass<*>
    fun getPropertyInitializer(): String
}