package mega.privacy.android.core.ui.buildscripts.kotlingenerator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import mega.privacy.android.core.ui.buildscripts.model.json.JsonCoreUiObject
import mega.privacy.android.core.ui.buildscripts.model.json.JsonGroup
import mega.privacy.android.core.ui.buildscripts.model.json.JsonLeaf
import mega.privacy.android.core.ui.buildscripts.model.json.SemanticValueRef
import mega.privacy.android.core.ui.controls.text.MegaText
import java.io.File
import java.security.InvalidParameterException
import kotlin.reflect.KClass

/**
 * Utility class to generate a kotlin file with all the tokens of the given type
 * @param generationType defines what it will be generated
 * @param coreObject the object we want to generate
 * @param type the type of [JsonCoreUiObject] we want to generate
 * @param appPrefix a prefix to be added to generated classes to easily distinguish them from other implementations.
 * @param fileName the name of the kotlin file that will be generated (with [appPrefix])
 * @param destinationPackageName the package where we want to put this file
 * @param destinationPath the root path where this file will be added (without packages)
 */

internal class KotlinTokensGenerator<T : JsonCoreUiObject>(
    private val generationType: TokenGenerationType,
    private val coreObject: JsonCoreUiObject,
    private val type: KClass<T>,
    private val appPrefix: String,
    private val fileName: String,
    private val destinationPackageName: String,
    private val destinationPath: String,
) {
    fun generateFile() {
        val coreUiFile = FileSpec
            .builder(destinationPackageName, "$appPrefix$fileName")
            .indent("    ")
        coreUiFile.addFileComment(
            """
            |
            |Generated automatically by ${this.javaClass.simpleName}.
            |Do not modify this file manually.
            |""".trimMargin()
        )
        if (generationType is TokenGenerationType.InterfaceDefinition) {
            addDataClasses(coreUiFile, coreObject, generationType)
        }
        addRootObject(coreUiFile, coreObject)
        val destination = File(destinationPath)
        coreUiFile.build().writeTo(destination)
    }

    private fun addDataClasses(
        file: FileSpec.Builder,
        coreUiObject: JsonCoreUiObject,
        interfaceDefinition: TokenGenerationType.InterfaceDefinition,
    ) {
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
                }.takeIf { it.isNotEmpty() }?.let { properties ->
                    val enumName = interfaceDefinition.exposeGroupsAsEnums
                        ?.takeIf { coreUiObject.name.jsonNameToKotlinName() in it.groupsToExpose }
                        ?.let { exposeAsEnum ->
                            coreUiObject.name.jsonNameToKotlinName() + exposeAsEnum.enumSuffix
                        }
                    file.addType(
                        createDataClass(
                            coreUiObject.name.jsonNameToKotlinName(),
                            properties,
                            destinationPackageName,
                            enumName,
                        )
                    )
                    enumName?.let {
                        file.addType(
                            createEnumClass(enumName, properties)
                        )
                    }
                }
            coreUiObject.children.forEach {
                addDataClasses(file, it, interfaceDefinition)
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
                TypeSpec.objectBuilder("$appPrefix${group.name.jsonNameToKotlinName()}")
                    .addSuperinterface(
                        ClassName(
                            generationType.interfacePackage ?: destinationPackageName,
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
        if (generationType !is TokenGenerationType.InterfaceDefinition) {
            mainType.addModifiers(KModifier.INTERNAL)
        }
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
                    ClassName(
                        packageName = (generationType as? TokenGenerationType.InterfaceImplementation)?.interfacePackage
                            ?: destinationPackageName,
                        className,
                    )
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
}

/**
 *
 * @param groupsToExpose a list of groups that will be exposed as public enums, to be used to assign tokens from outside of core-ui module, see an example in [MegaText]
 */
internal data class ExposeGroupsAsEnums(
    val groupsToExpose: List<String>,
    val enumSuffix: String,
)

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
    data class InterfaceDefinition(
        val exposeGroupsAsEnums: ExposeGroupsAsEnums?,
    ) : TokenGenerationType()

    /**
     * An object implementing the corresponding interface will be generated
     */
    data class InterfaceImplementation(
        val interfaceName: String,
        val interfacePackage: String?,
    ) : TokenGenerationType()
}