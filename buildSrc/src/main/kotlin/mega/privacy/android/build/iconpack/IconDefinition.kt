package mega.privacy.android.build.iconpack

/**
 * Defines an icon that should be generated in the IconPack.
 *
 * @param name The name of the icon (e.g., "Check", "ArrowUp")
 * @param size The size category (e.g., "Medium", "Small")
 * @param weight The weight category (e.g., "Regular", "Thin")
 * @param style The style category (e.g., "Outline", "Solid")
 * @param sourceName The name of the source file (without extension)
 */
data class IconDefinition(
    val name: String,
    val size: Size,
    val weight: Weight,
    val style: Style,
    val sourceName: String,
) {
    /**
     * Generates the property name for this icon in the generated code.
     */
    fun getPropertyName(): String = name

    /**
     * Generates the file name pattern for this icon in XML resources.
     */
    fun getFileNamePattern(): String =
        "ic_${sourceName}_${size.lowercase()}_${weight.lowercase()}_${style.lowercase()}"

    /**
     * Generates the file name pattern for this icon.
     */
    fun getFileNamePatternForSvg(): String =
        "icon_${
            sourceName.replace("_", "-")
        }_${size.lowercase()}_${weight.lowercase()}_${style.lowercase()}"
}

internal typealias Size = String
internal typealias Weight = String
internal typealias Style = String