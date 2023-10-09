package mega.privacy.android.core.ui.buildscripts

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import mega.privacy.android.core.ui.buildscripts.model.json.JsonCoreUiObject
import mega.privacy.android.core.ui.buildscripts.model.json.JsonGroup
import mega.privacy.android.core.ui.buildscripts.model.json.JsonLeaf
import mega.privacy.android.core.ui.buildscripts.model.json.SemanticValueRef
import java.io.File
import java.security.InvalidParameterException
import kotlin.reflect.KClass

/**
 * Utility class to generate a kotlin file with all the tokens of the given type
 * @param type the type of [JsonCoreUiObject] we want to generate
 * @param coreObject the object we want to generate
 * @param fileName the name of the kotlin file that will be generated
 * @param generationType defines what it will be generated
 * @param packageName the package where we want to put this file
 * @param destinationPath the root path where this file will be added (without packages)
 */

internal class KotlinTokensGenerator<T : JsonCoreUiObject>(
    private val type: KClass<T>,
    private val coreObject: JsonCoreUiObject,
    private val fileName: String,
    private val generationType: TokenGenerationType,
    private val packageName: String = THEME_TOKENS_PACKAGE,
    private val destinationPath: String = DESTINATION_PATH,
) {
    fun generateFile() {
        val coreUiFile = FileSpec
            .builder(packageName, fileName)
            .indent("    ")
        coreUiFile.addFileComment(
            """
            |
            |Generated automatically by ${this.javaClass.simpleName}.
            |Do not modify this file manually.
            |""".trimMargin()
        )
        if (generationType == TokenGenerationType.InterfaceDefinition) {
            addDataClasses(coreUiFile, coreObject)
        }
        addRootObject(coreUiFile, coreObject)
        val destination = File(destinationPath)
        coreUiFile.build().writeTo(destination)
    }

    private fun addDataClasses(file: FileSpec.Builder, coreUiObject: JsonCoreUiObject) {
        if (coreUiObject is JsonGroup) {
            coreUiObject.children
                .filterIsInstance(type.java)
                .filterIsInstance<SemanticValueRef>()
                .map {
                    Triple(
                        it.getPropertyName(coreUiObject.name),
                        it.getPropertyClass(),
                        it.getPropertyInitializer()
                    )
                }.takeIf { it.isNotEmpty() }?.let {
                    file.addType(
                        createDataClass(coreUiObject.name.jsonNameToKotlinName(), it)
                    )
                }
            coreUiObject.children.forEach {
                addDataClasses(file, it)
            }
        }
    }

    private fun addRootObject(file: FileSpec.Builder, rootObject: JsonCoreUiObject) {
        if (rootObject is JsonGroup) {
            if (rootObject.name == null) {
                //unnamed root group is discarded, children objects (usually only one) will be created
                rootObject.children.forEach { child ->
                    if (child is JsonGroup) {
                        addObject(file, child)
                    } else {
                        throw InvalidParameterException("Root object children should be a group: $child")
                    }
                }
            } else {
                //named root group is add as it is
                addObject(file, rootObject)
            }
        } else {
            throw InvalidParameterException("Root object should be a group: $rootObject")
        }
    }

    private fun addObject(file: FileSpec.Builder, group: JsonGroup) {
        if (!group.hasChildOfType(type)) return
        val mainType = when (generationType) {
            is TokenGenerationType.InterfaceImplementation -> {
                TypeSpec.objectBuilder(group.name.jsonNameToKotlinName())
                    .addSuperinterface(
                        ClassName(
                            THEME_TOKENS_PACKAGE,
                            generationType.interfaceName
                        )
                    )
            }

            is TokenGenerationType.InterfaceDefinition -> {
                TypeSpec.interfaceBuilder(group.name.jsonNameToKotlinName())
            }

            is TokenGenerationType.NestedObjects -> {
                TypeSpec.objectBuilder(group.name.jsonNameToKotlinName())
            }
        }
        mainType.addModifiers(KModifier.INTERNAL)
        if (generationType is TokenGenerationType.NestedObjects) {
            group.children.forEach {
                addChildObjectRecursively(mainType, it, group.name)
            }
        } else {
            group.children
                .filterIsInstance<JsonGroup>()
                .filter { (it as? JsonGroup)?.hasChildOfType(type) == true }
                .mapNotNull { createProperty(it) }
                .takeIf { it.isNotEmpty() }?.let { properties ->
                    properties.forEach {
                        mainType.addProperty(it)
                    }
                }
        }
        file.addType(mainType.build())
    }

    private fun createProperty(child: JsonGroup): PropertySpec? {
        child.children
            .filter { it::class == type }
            .filterIsInstance<SemanticValueRef>()
            .takeIf { it.isNotEmpty() }?.let { properties ->
                val className = child.name.jsonNameToKotlinName()
                val propertyName = child.name.jsonNameToKotlinName().lowercaseFirstChar()
                val propSpecBuilder = PropertySpec.builder(
                    propertyName,
                    ClassName(THEME_TOKENS_PACKAGE, className)
                )
                if (generationType is TokenGenerationType.InterfaceImplementation) {
                    propSpecBuilder
                        .addModifiers(KModifier.OVERRIDE)
                        .initializer(
                            properties.joinToString(
                                separator = ",\n",
                                prefix = "$className(\n",
                                postfix = ",\n)"
                            ) { it.getValueForDataClassInitializer(child.name) })
                }
                return propSpecBuilder.build()
            }
        return null
    }

    private fun addChildObjectRecursively(
        groupObject: TypeSpec.Builder,
        coreObject: JsonCoreUiObject,
        groupParentName: String?,
    ) {
        when {
            coreObject is JsonGroup -> addGroup(groupObject, coreObject)
            (coreObject::class == type && coreObject is JsonLeaf) -> {
                groupObject.addProperty(
                    PropertySpec
                        .builder(
                            coreObject.getPropertyName(groupParentName ?: ""),
                            coreObject.getPropertyClass()
                        )
                        .initializer(coreObject.getPropertyInitializer()).build()
                )
            }
        }
    }

    private fun addGroup(parentGroupObject: TypeSpec.Builder, group: JsonGroup) {
        createGroupObject(group)?.let {
            parentGroupObject.addType(it.build())
        }
    }

    private fun createGroupObject(group: JsonGroup) =
        if (group.hasChildOfType(type)) {
            val groupObject = TypeSpec
                .objectBuilder(group.name.jsonNameToKotlinName())
            group.children.forEach {
                addChildObjectRecursively(groupObject, it, group.name)
            }
            groupObject
        } else {
            null
        }

    companion object {

        private const val THEME_TOKENS_PACKAGE = "mega.privacy.android.core.ui.theme.tokens"
        private const val DESTINATION_PATH = "core-ui/src/main/java"
    }
}

/**
 * Defines what it will be generated
 */
internal sealed class TokenGenerationType {
    /**
     * A nested objects with constant values will be generated
     */
    data object NestedObjects : TokenGenerationType()

    /**
     * An Interface and all related data classes will be generated
     */
    data object InterfaceDefinition : TokenGenerationType()

    /**
     * An object implementing the corresponding interface will be generated
     */
    data class InterfaceImplementation(val interfaceName: String) : TokenGenerationType()
}