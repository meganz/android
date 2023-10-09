package mega.privacy.android.core.ui.buildscripts

import androidx.compose.ui.graphics.Color
import com.google.gson.GsonBuilder
import mega.privacy.android.core.ui.buildscripts.deserializers.ColorDeserializer
import mega.privacy.android.core.ui.buildscripts.deserializers.JsonCoreUiObjectDeserializer
import mega.privacy.android.core.ui.buildscripts.deserializers.JsonTokenNameDeserializer
import mega.privacy.android.core.ui.buildscripts.model.json.JsonColor
import mega.privacy.android.core.ui.buildscripts.model.json.JsonColorRef
import mega.privacy.android.core.ui.buildscripts.model.json.JsonCoreUiObject
import mega.privacy.android.core.ui.buildscripts.model.json.JsonTokenName
import java.io.File
import kotlin.reflect.KClass


/**
 * Given the exported JSON Figma color tokens, it generates Kotlin files for:
 *   - Core colors tokens
 *   - Semantic tokens for dark theme
 *   - Semantic tokens for light theme
 */
class GenerateTokens {
    //this can be set by arguments using Clikt in the future
    private val jsonCoreFileName = "core"
    private val jsonSemanticFileNamePrefix = "SemanticTokens"

    private val gson = GsonBuilder()
        .registerTypeAdapter(Color::class.java, ColorDeserializer())
        .registerTypeAdapter(JsonCoreUiObject::class.java, JsonCoreUiObjectDeserializer())
        .registerTypeAdapter(JsonTokenName::class.java, JsonTokenNameDeserializer())
        .create()

    internal fun run() {
        //generate color core tokens
        generateTokensKotlinFile(
            type = JsonColor::class,
            jsonFileName = jsonCoreFileName,
            kotlinOutputName = "CoreColors",
            generationType = TokenGenerationType.NestedObjects
        )


        //generate color semantic tokens interface
        generateTokensKotlinFile(
            type = JsonColorRef::class,
            jsonFileName = "Semantic tokens.Light.tokens",
            kotlinOutputName = jsonSemanticFileNamePrefix,
            generationType = TokenGenerationType.InterfaceDefinition,
            rootGroupName = jsonSemanticFileNamePrefix,
        )

        //generate color semantic tokens for each theme mode
        listOf("Light", "Dark").forEach { mode ->
            generateTokensKotlinFile(
                type = JsonColorRef::class,
                jsonFileName = "Semantic tokens.$mode.tokens",
                kotlinOutputName = "$jsonSemanticFileNamePrefix$mode",
                generationType = TokenGenerationType.InterfaceImplementation(
                    jsonSemanticFileNamePrefix
                ),
                rootGroupName = "$jsonSemanticFileNamePrefix$mode",
            )
        }
    }

    private fun <T : JsonCoreUiObject> generateTokensKotlinFile(
        type: KClass<T>,
        jsonFileName: String,
        kotlinOutputName: String,
        generationType: TokenGenerationType,
        rootGroupName: String? = null,
    ) {
        val json =
            File("designSystemAssets/$jsonFileName.json").bufferedReader().readText()
        val coreObject = gson.fromJson(json, JsonCoreUiObject::class.java)
        if (rootGroupName != null) {
            coreObject.name = rootGroupName
        }
        KotlinTokensGenerator(
            type = type,
            coreObject = coreObject,
            fileName = kotlinOutputName,
            generationType = generationType,
        ).generateFile()
    }
}

/**
 * Runs the Script to generate Kotlin files with the tokens from json files
 */
fun main(args: Array<String>) = GenerateTokens().run()

