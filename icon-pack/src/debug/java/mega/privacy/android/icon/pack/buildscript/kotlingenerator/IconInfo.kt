package mega.privacy.android.icon.pack.buildscript.kotlingenerator

internal data class IconInfo(
    val name: String,
    val size: Size,
    val weight: Weight,
    val style: Style,
    val resourceId: String,
)

internal typealias Size = String
internal typealias Weight = String
internal typealias Style = String