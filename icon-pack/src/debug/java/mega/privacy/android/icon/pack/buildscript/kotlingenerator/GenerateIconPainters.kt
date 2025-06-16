package mega.privacy.android.icon.pack.buildscript.kotlingenerator

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.FunSpec
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import java.io.File

/**
 * Script to generate Icons from icon-pack resources.
 * It searches for all the drawable resources that follows this [DRAWABLE_REGEX_PATTERN] pattern in default drawable folder in icon-pack module.
 */
object GenerateIconPainters {

    /**
     * Invoke
     */
    operator fun invoke(outputDir: File) {
        getDrawableFiles().mapNotNull { getIconInfo(it) }.also { icons ->
            println("${icons.size} icons found")
            generateIconCode(icons, outputDir)
        }
    }

    private fun getDrawableFiles(): List<File> =
        File(DRAWABLES_PATH)
            .takeIf { dir ->
                dir.exists().also { if (!it) println("Dir ${dir.absolutePath} doesn't exist") }
            }
            ?.listFiles()?.toList()
            ?: emptyList()

    private fun getIconInfo(iconFile: File): IconInfo? {
        return Regex(DRAWABLE_REGEX_PATTERN).matchEntire(iconFile.name)?.let { matchResult ->
            val (name, size, weight, style) = matchResult.destructured
            val iconName =
                name.split('_').joinToString("") { it.replaceFirstChar(Char::uppercaseChar) }
            IconInfo(
                name = iconName,
                size = size.replaceFirstChar(Char::uppercaseChar),
                weight = weight.replaceFirstChar(Char::uppercaseChar),
                style = style.replaceFirstChar(Char::uppercaseChar),
                resourceId = "R.drawable.${iconFile.nameWithoutExtension}"
            )
        }
    }

    private fun generateIconCode(icons: List<IconInfo>, outputDir: File) {
        val root = TypeSpec.objectBuilder(MAIN_OBJECT_NAME)

        val groupedBySizeWeightStyle = icons.groupBy { it.size }
            .mapValues { (_, bySize) ->
                bySize.groupBy { it.weight }
                    .mapValues { (_, byWeight) ->
                        byWeight.groupBy { it.style }
                    }
            }

        for ((size, bySize) in groupedBySizeWeightStyle) {
            val sizeObject = TypeSpec.objectBuilder(size)

            for ((weight, byStyle) in bySize) {
                val weightObject = TypeSpec.objectBuilder(weight)

                for ((style, iconsInGroup) in byStyle) {
                    val styleObject = TypeSpec.objectBuilder(style)

                    for (icon in iconsInGroup) {
                        val property = PropertySpec.builder(
                            icon.name,
                            ClassName("androidx.compose.ui.graphics.painter", "Painter")
                        ).getter(
                            FunSpec.getterBuilder()
                                .addAnnotation(ClassName("androidx.compose.runtime", "Composable"))
                                .addCode("return painterResource(${icon.resourceId})") //hardcoded statement because
                                .build()
                        ).build()
                        styleObject.addProperty(property)
                    }
                    weightObject.addType(styleObject.build())
                }
                sizeObject.addType(weightObject.build())
            }

            root.addType(sizeObject.build())
        }

        val file = FileSpec.builder(APP_ICONS_OBJECT_PACKAGE, MAIN_OBJECT_NAME)
            .addImport("androidx.compose.ui.res", "painterResource")
            .indent("    ")
            .addFileComment(
                """
            |
            |Generated automatically by ${this.javaClass.simpleName}.
            |Do not modify this file manually.
            |""".trimMargin()
            )
            .addType(root.build())
            .build()

        file.writeTo(outputDir)
    }
}