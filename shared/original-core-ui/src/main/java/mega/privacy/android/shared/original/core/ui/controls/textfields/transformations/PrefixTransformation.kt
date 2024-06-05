package mega.privacy.android.shared.original.core.ui.controls.textfields.transformations

import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation


/**
 * Prefix Transformation
 * Add a non editable prefix to a text field
 * offset position is modified considering the prefix length
 *
 * @param prefix    Prefix string
 */
class PrefixTransformation(private val prefix: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val result = AnnotatedString(prefix) + text
        val textWithPrefixMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int = offset + prefix.length

            override fun transformedToOriginal(offset: Int): Int =
                if (offset <= prefix.length) 0 else offset - prefix.length
        }
        return TransformedText(result, textWithPrefixMapping)
    }
}