package mega.privacy.android.core.ui.preview

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

/**
 * Helper class to allow to use debug string resources in compose previews without breaking release builds.
 */
sealed interface PreviewTextValue {
    /**
     * Returns the text to be shown
     */
    @Composable
    fun getText(): String

    /**
     * Hardcoded text
     */
    data class Literal(private val stringLiteral: String) : PreviewTextValue {
        @Composable
        override fun getText() = stringLiteral
    }

    /**
     * Text from string resources
     */
    data class Resource(@StringRes private val id: Int) : PreviewTextValue {
        @Composable
        override fun getText() = stringResource(id = id)
    }
}

/**
 * Helper function to simplify construction of PreviewTextValue.Literal
 */
fun PreviewTextValue(literal: String) = PreviewTextValue.Literal(literal)

/**
 * Helper function to simplify construction of PreviewTextValue.Resource
 */
fun PreviewTextValue(@StringRes id: Int) = PreviewTextValue.Resource(id)