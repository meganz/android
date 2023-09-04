package mega.privacy.android.core.ui.buildscripts.deserializers

/**
 * Returns a String with Kotlin idiomatic format:
 *  - remove spaces
 *  - remove "--" prefix
 *  - snake_case to camelCase
 *  - add an "n" prefix in case it does not starts with a letter
 */
internal fun String.jsonNameToKotlinName(): String {
    val pattern = "([_\\-])[a-z,0-9]".toRegex()
    val camelCase = this
        .removePrefix("--") //remove "--" prefixes
        .replace(" ", "_") //convert spaces to underscore
        .replace(pattern) { it.value.last().uppercase() } //snake_case to camelCase
        .trim()
    return if (camelCase.matches("^[^a-zA-Z].*".toRegex())) {
        "n$camelCase" //if is not starting with a letter (usually a number) add "n" prefix
    } else {
        camelCase
    }
}