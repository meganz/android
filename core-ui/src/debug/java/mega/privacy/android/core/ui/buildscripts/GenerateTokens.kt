package mega.privacy.android.core.ui.buildscripts

import androidx.compose.ui.graphics.Color
import com.google.gson.GsonBuilder
import mega.privacy.android.core.ui.buildscripts.deserializers.ColorDeserializer
import mega.privacy.android.core.ui.buildscripts.deserializers.JsonCoreUiObjectDeserializer
import mega.privacy.android.core.ui.buildscripts.deserializers.JsonTokenNameDeserializer
import mega.privacy.android.core.ui.buildscripts.kotlingenerator.ExposeGroupsAsEnums
import mega.privacy.android.core.ui.buildscripts.kotlingenerator.KotlinTokensGenerator
import mega.privacy.android.core.ui.buildscripts.kotlingenerator.TokenGenerationType
import mega.privacy.android.core.ui.buildscripts.model.json.JsonColor
import mega.privacy.android.core.ui.buildscripts.model.json.JsonColorRef
import mega.privacy.android.core.ui.buildscripts.model.json.JsonCoreUiObject
import mega.privacy.android.core.ui.buildscripts.model.json.JsonTokenName
import java.io.File
import kotlin.reflect.KClass


/**
 * Given the exported JSON Figma color tokens, it generates Kotlin files for:
 *   - Core colors tokens
 *   - Semantic tokens interfaces and data classes
 *   - Semantic tokens for dark theme
 *   - Semantic tokens for light theme
 */
class GenerateTokens {

    private val gson = GsonBuilder()
        .registerTypeAdapter(Color::class.java, ColorDeserializer())
        .registerTypeAdapter(JsonCoreUiObject::class.java, JsonCoreUiObjectDeserializer())
        .registerTypeAdapter(JsonTokenName::class.java, JsonTokenNameDeserializer())
        .create()

    /**
     * Generate Core and semantic tokens kotlin files from figma JSON files
     * @param appPrefix a prefix to be added to generated classes to easily distinguish them from other implementations.
     * @param packageName string representing the package where generated classes will be saved
     * @param destinationPath string representing the base path of the code destination, it usually ends with ".../src/main/java", package folders will be added to place the file in the right place
     * @param assetsFolder string representing the path where the json files with tokens are saved
     */
    fun generate(
        appPrefix: String,
        packageName: String,
        destinationPath: String,
        assetsFolder: String,
    ) = generate(appPrefix, packageName, destinationPath, false, assetsFolder)

    internal fun generate(
        appPrefix: String,
        packageName: String,
        destinationPath: String,
        generateInterfaces: Boolean,
        assetsFolder: String = DEFAULT_ASSETS_FOLDER,
    ) {
        //for now we only have color tokens
        generateColorsTokens(
            appPrefix = appPrefix,
            packageName = packageName,
            destinationPath = destinationPath,
            generateInterfaces = generateInterfaces,
            assetsFolder = assetsFolder,
        )
    }

    private fun generateColorsTokens(
        appPrefix: String,
        packageName: String,
        destinationPath: String,
        generateInterfaces: Boolean,
        assetsFolder: String,
    ) = generateTokens(
        appPrefix = appPrefix,
        packageName = packageName,
        destinationPath = destinationPath,
        generateInterfaces = generateInterfaces,
        assetsFolder = assetsFolder,
        coreType = JsonColor::class,
        semanticType = JsonColorRef::class,
        exposeGroupsAsEnums = ExposeGroupsAsEnums(listOf("Text"), enumSuffix = "Color"),
    )

    private fun <T : JsonCoreUiObject, E : JsonCoreUiObject> generateTokens(
        appPrefix: String,
        packageName: String,
        destinationPath: String,
        generateInterfaces: Boolean,
        assetsFolder: String,
        coreType: KClass<T>,
        semanticType: KClass<E>,
        exposeGroupsAsEnums: ExposeGroupsAsEnums?,
    ) {
        //generate color core tokens
        generateTokensKotlinFile(
            type = coreType,
            assetsFolder = assetsFolder,
            jsonFileName = DEFAULT_JSON_CORE_FILE_NAME,
            kotlinOutputName = "CoreColors",
            generationType = TokenGenerationType.NestedObjects,
            packageName = packageName,
            destinationPath = destinationPath,
            appPrefix = "",
        )


        //generate color semantic tokens interface
        if (generateInterfaces) {
            generateTokensKotlinFile(
                type = semanticType,
                assetsFolder = assetsFolder,
                jsonFileName = "Semantic tokens.Light.tokens",
                kotlinOutputName = SEMANTIC_TOKENS_PREFIX,
                generationType = TokenGenerationType.InterfaceDefinition(exposeGroupsAsEnums),
                rootGroupName = SEMANTIC_TOKENS_PREFIX,
                packageName = packageName,
                destinationPath = destinationPath,
                appPrefix = ""
            )
        }

        //generate color semantic tokens for each theme mode
        listOf("Light", "Dark").forEach { mode ->
            generateTokensKotlinFile(
                type = semanticType,
                assetsFolder = assetsFolder,
                jsonFileName = "Semantic tokens.$mode.tokens",
                appPrefix = appPrefix,
                kotlinOutputName = "$SEMANTIC_TOKENS_PREFIX$mode",
                generationType = TokenGenerationType.InterfaceImplementation(
                    SEMANTIC_TOKENS_PREFIX,
                    if (generateInterfaces) null else DEFAULT_PACKAGE
                ),
                packageName = packageName,
                rootGroupName = "$SEMANTIC_TOKENS_PREFIX$mode",
                destinationPath = destinationPath,
            )
        }
    }

    private fun <T : JsonCoreUiObject> generateTokensKotlinFile(
        type: KClass<T>,
        assetsFolder: String,
        jsonFileName: String,
        appPrefix: String,
        kotlinOutputName: String,
        generationType: TokenGenerationType,
        packageName: String,
        destinationPath: String,
        rootGroupName: String? = null,
    ) {
        val json =
            File("$assetsFolder/$jsonFileName.json").bufferedReader().readText()
        val coreObject = gson.fromJson(json, JsonCoreUiObject::class.java)
        if (rootGroupName != null) {
            coreObject.name = rootGroupName
        }
        KotlinTokensGenerator(
            generationType = generationType,
            coreObject = coreObject,
            type = type,
            appPrefix = appPrefix,
            fileName = kotlinOutputName,
            destinationPackageName = packageName,
            destinationPath = destinationPath,
        ).generateFile()
    }

    companion object {
        /**
         * package of default tokens and interfaces
         */
        const val DEFAULT_PACKAGE = "mega.privacy.android.core.ui.theme.tokens"
        const val DEFAULT_ASSETS_FOLDER = "designSystemAssets"
        private const val DEFAULT_JSON_CORE_FILE_NAME = "core"
        private const val SEMANTIC_TOKENS_PREFIX = "SemanticTokens"
    }
}

