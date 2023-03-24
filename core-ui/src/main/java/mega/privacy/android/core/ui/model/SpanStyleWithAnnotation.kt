package mega.privacy.android.core.ui.model

import androidx.compose.ui.text.SpanStyle

/**
 * SpanStyle and an associated annotation string to be used in clickable texts
 *
 * @param spanStyle [SpanStyle]
 * @param annotation [String] it can be null if we don't need to make it clickable, usually an url link
 */
data class SpanStyleWithAnnotation(val spanStyle: SpanStyle, val annotation: String?)
