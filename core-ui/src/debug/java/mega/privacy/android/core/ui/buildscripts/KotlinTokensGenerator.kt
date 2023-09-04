package mega.privacy.android.core.ui.buildscripts

import androidx.compose.ui.graphics.Color
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import mega.privacy.android.core.ui.buildscripts.deserializers.jsonNameToKotlinName
import mega.privacy.android.core.ui.buildscripts.model.json.JsonColor
import mega.privacy.android.core.ui.buildscripts.model.json.JsonColorRef
import mega.privacy.android.core.ui.buildscripts.model.json.JsonCoreUiObject
import mega.privacy.android.core.ui.buildscripts.model.json.JsonGroup
import java.io.File
import java.security.InvalidParameterException
import kotlin.reflect.KClass

/**
 * Utility class to generate a kotlin file with all the tokens of the given type
 * @param type the type of [JsonCoreUiObject] we want to generate
 * @param coreObject the object we want to generate
 * @param fileName the name of the kotlin file that will be generated
 * @param packageName the package where we want to put this file
 * @param destinationPath the root path where this file will be added (without packages)
 */
internal class KotlinTokensGenerator<T : JsonCoreUiObject>(
    private val type: KClass<T>,
    private val coreObject: JsonCoreUiObject,
    private val fileName: String,
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
        addRootObject(coreUiFile, coreObject)
        val destination = File(destinationPath)
        coreUiFile.build().writeTo(destination)
    }

    private fun addRootObject(file: FileSpec.Builder, rootObject: JsonCoreUiObject) {
        if (rootObject is JsonGroup) {
            if (rootObject.name == null) {
                //unnamed root group is discarded, children objects (usually only one) will be created
                rootObject.children.forEach { child ->
                    if (child is JsonGroup) {
                        addRootGroupChild(file, child)
                    } else {
                        throw InvalidParameterException("Root object children should be a group: $child")
                    }
                }
            } else {
                //named root group is add as it is
                addRootGroupChild(file, rootObject)
            }
        } else {
            throw InvalidParameterException("Root object should be a group: $rootObject")
        }
    }

    private fun addObject(groupObject: TypeSpec.Builder, coreObject: JsonCoreUiObject) {
        when {
            coreObject is JsonGroup -> addGroup(groupObject, coreObject)
            (coreObject::class == type) -> {
                when (coreObject) {
                    is JsonColor -> addColor(groupObject, coreObject)
                    is JsonColorRef -> addColorRef(groupObject, coreObject)
                    else -> println("not done yet")
                }
            }
        }
    }

    private fun addRootGroupChild(file: FileSpec.Builder, group: JsonGroup) {
        createGroupObject(group)?.let {
            it.modifiers.add(KModifier.INTERNAL)
            file.addType(it.build())
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
                .objectBuilder(group.name?.jsonNameToKotlinName() ?: "Unknown")
            group.children.forEach {
                addObject(groupObject, it)
            }
            groupObject
        } else {
            null
        }

    private fun addColor(groupObject: TypeSpec.Builder, color: JsonColor) {
        if (color.name == null || color.value == null) {
            println("Color not well defined $color")
        } else {
            groupObject.addProperty(
                PropertySpec.builder(color.name?.jsonNameToKotlinName() ?: "unknown", Color::class)
                    .initializer(
                        "Color(%L, %L, %L, %L)",
                        color.value.red.toBase255(),
                        color.value.green.toBase255(),
                        color.value.blue.toBase255(),
                        color.value.alpha.toBase255(),
                    )
                    .build()
            )
        }
    }

    private fun addColorRef(groupObject: TypeSpec.Builder, color: JsonColorRef) {
        if (color.name == null || color.tokenName == null) {
            println("Color not well defined $color")
        } else {
            groupObject.addProperty(
                PropertySpec.builder(color.name?.jsonNameToKotlinName() ?: "unknown", Color::class)
                    .initializer(color.tokenName.value)
                    .build()
            )
        }
    }

    companion object {

        private fun Float.toBase255() = (this * 255f).toInt()

        private const val THEME_TOKENS_PACKAGE = "mega.privacy.android.core.ui.theme.tokens"
        private const val DESTINATION_PATH = "core-ui/src/main/java"
    }
}