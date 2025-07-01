package mega.privacy.android.build.iconpack

import com.squareup.kotlinpoet.AnnotationSpec
import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import java.io.File

/**
 * Generates individual Kotlin files for each ImageVector.
 * Each icon gets its own file with a function that returns the ImageVector.
 */
class IndividualFileGenerator(
    private val outputDir: File,
    private val vectorsPackage: String,
) {

    /**
     * Generates individual files for each icon definition.
     *
     * @param iconDefinitions List of icons to generate files for
     * @param xmlConverter Converter for XML drawables
     * @param svgConverter Converter for SVG files
     */
    fun generateIndividualFiles(
        iconDefinitions: List<IconDefinition>,
        xmlConverter: XmlToImageVectorConverter,
        svgConverter: SvgToImageVectorConverter,
    ) {
        println("Generating ${iconDefinitions.size} individual ImageVector files...")

        for (icon in iconDefinitions) {
            generateIconFile(icon, xmlConverter, svgConverter)
        }
    }

    /**
     * Generates a single file for an icon.
     */
    private fun generateIconFile(
        icon: IconDefinition,
        xmlConverter: XmlToImageVectorConverter,
        svgConverter: SvgToImageVectorConverter,
    ) {
        val imageVectorCode =
            svgConverter.convertSvgToImageVector(icon.getFileNamePattern())
                ?: xmlConverter.convertXmlToImageVector(icon.getFileNamePattern())

        if (imageVectorCode != null) {
            // Generate unique names based on the full hierarchy
            val uniqueFileName = "${icon.size}${icon.weight}${icon.style}${icon.name}ImageVector"
            val uniqueFunctionName =
                "create${icon.size}${icon.weight}${icon.style}${icon.name}ImageVector"
            val previewFunctionName =
                "${icon.size}${icon.weight}${icon.style}${icon.name}ImageVectorPreview"

            val function = FunSpec.builder(uniqueFunctionName)
                .returns(ClassName("androidx.compose.ui.graphics.vector", "ImageVector"))
                .addCode("return $imageVectorCode")
                .build()

            // Create preview function
            val previewFunction = FunSpec.builder(previewFunctionName)
                .addAnnotation(
                    AnnotationSpec.builder(
                        ClassName(
                            "androidx.compose.ui.tooling.preview",
                            "Preview"
                        )
                    ).build()
                )
                .addAnnotation(
                    AnnotationSpec.builder(
                        ClassName(
                            "androidx.compose.runtime",
                            "Composable"
                        )
                    ).build()
                )
                .addModifiers(com.squareup.kotlinpoet.KModifier.PRIVATE)
                .addCode(
                    """
                    Icon(
                        ${uniqueFunctionName}(),
                        contentDescription = "${icon.name}"
                    )
                    """.trimIndent()
                )
                .build()

            val file = FileSpec.builder(vectorsPackage, uniqueFileName)
                .addImport("androidx.compose.ui.graphics.vector", "ImageVector")
                .addImport("androidx.compose.ui.graphics.vector", "path")
                .addImport("androidx.compose.ui.graphics", "SolidColor")
                .addImport("androidx.compose.ui.graphics", "Color")
                .addImport("androidx.compose.ui.graphics", "StrokeCap")
                .addImport("androidx.compose.ui.graphics", "StrokeJoin")
                .addImport("androidx.compose.ui.graphics", "PathFillType")
                .addImport("androidx.compose.ui.unit", "dp")
                .addImport("androidx.compose.material", "Icon")
                .addImport("androidx.compose.runtime", "Composable")
                .addImport("androidx.compose.ui.tooling.preview", "Preview")
                .indent("    ")
                .addFileComment(
                    """
                    |
                    |Generated automatically by ${this.javaClass.simpleName}.
                    |Do not modify this file manually.
                    |
                    |Icon: ${icon.sourceName}
                    |""".trimMargin()
                )
                .addFunction(function)
                .addFunction(previewFunction)
                .build()

            file.writeTo(outputDir)
        } else {
            println("Warning: Could not generate ImageVector for icon ${icon.name}")
        }
    }
} 