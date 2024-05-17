package mega.privacy.android.shared.original.core.ui.controls.chat.messages.format

/**
 * Data class defining a formatted sentence.
 *
 * @property formatStart Start index of the formatted sentence.
 * @property formatEnd End index of the formatted sentence.
 * @property type List of [FormatType]. As different formats can be applied to the same sentence.
 * @property sentenceStart Start index of the final formatted sentence.
 * @property sentenceEnd End index of the final formatted sentence.
 */
data class Format(
    val formatStart: Int,
    val formatEnd: Int,
    val type: List<FormatType>,
    val sentenceStart: Int,
    val sentenceEnd: Int,
)