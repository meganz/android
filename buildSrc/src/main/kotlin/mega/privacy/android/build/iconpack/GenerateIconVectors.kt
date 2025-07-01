package mega.privacy.android.build.iconpack

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

/**
 * Generates the IconPack object with ImageVector properties from icon definitions.
 * This generator creates an object that implements IconPackInterface and provides
 * ImageVector properties for each defined icon.
 */
class GenerateIconVectors(
    private val outputDir: File,
    private val mainObjectPackage: String,
    private val mainObjectName: String,
    private val iconDefinitions: List<IconDefinition>,
    private val xmlConverter: XmlToImageVectorConverter,
    private val svgConverter: SvgToImageVectorConverter,
) {
    private val vectorsPackage = "$mainObjectPackage.vectors"

    /**
     * Generates the IconPack object with all defined icons.
     */
    fun generate() {
        println("Generating IconPack with ${iconDefinitions.size} icon definitions...")

        // First, generate individual files for each icon
        val individualGenerator = IndividualFileGenerator(outputDir, vectorsPackage)
        individualGenerator.generateIndividualFiles(iconDefinitions, xmlConverter, svgConverter)

        // Then, generate the main IconPack object that references the individual functions
        generateIconPackObject()
    }

    /**
     * Generates the main IconPack object that references the individual ImageVector functions.
     */
    private fun generateIconPackObject() {
        val root = TypeSpec.objectBuilder(mainObjectName)
            .addSuperinterface(ClassName(mainObjectPackage, "IconPackInterface"))

        // Group icons by size, weight, and style
        val groupedIcons = iconDefinitions.groupBy { it.size }
            .mapValues { (_, bySize) ->
                bySize.groupBy { it.weight }
                    .mapValues { (_, byWeight) ->
                        byWeight.groupBy { it.style }
                    }
            }

        val vectorImports = mutableListOf<String>()

        // Generate the object structure
        for ((size, bySize) in groupedIcons.toSortedMap()) {
            val sizeObject = TypeSpec.objectBuilder(size)
                .addSuperinterface(ClassName(mainObjectPackage, "IconPackInterface.$size"))

            for ((weight, byStyle) in bySize.toSortedMap()) {
                val weightObject = TypeSpec.objectBuilder(weight)
                    .addSuperinterface(
                        ClassName(
                            mainObjectPackage,
                            "IconPackInterface.$size.$weight"
                        )
                    )

                for ((style, iconsInGroup) in byStyle.toSortedMap()) {
                    val styleObject = TypeSpec.objectBuilder(style)
                        .addSuperinterface(
                            ClassName(
                                mainObjectPackage,
                                "IconPackInterface.$size.$weight.$style"
                            )
                        )

                    for (icon in iconsInGroup.sortedBy { it.name }) {
                        val uniqueFunctionName =
                            "create${icon.size}${icon.weight}${icon.style}${icon.name}ImageVector"
                        vectorImports.add(uniqueFunctionName)
                        val property = PropertySpec.builder(
                            icon.getPropertyName(),
                            ClassName("androidx.compose.ui.graphics.vector", "ImageVector")
                        )
                            .addModifiers(com.squareup.kotlinpoet.KModifier.OVERRIDE)
                            .delegate("lazy { $uniqueFunctionName() }")
                            .build()
                        styleObject.addProperty(property)
                    }
                    weightObject.addType(styleObject.build())
                }
                sizeObject.addType(weightObject.build())
            }
            root.addType(sizeObject.build())
        }

        // Generate the main IconPack file
        val fileBuilder = FileSpec.builder(mainObjectPackage, mainObjectName)
            .addImport("androidx.compose.ui.graphics.vector", "ImageVector")
            .indent("    ")
            .addFileComment(
                """
                |
                |Generated automatically by ${this.javaClass.simpleName}.
                |Do not modify this file manually.
                |
                |This file contains the main IconPack object that implements IconPackInterface.
                |Each icon property references a function in a separate file.
                |""".trimMargin()
            )
            .addType(root.build())
        vectorImports.forEach { fileBuilder.addImport(vectorsPackage, it) }

        fileBuilder.build().writeTo(outputDir)
        println("IconPack object generated successfully!")
    }
} 