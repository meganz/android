package mega.privacy.android.feature.texteditor.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.DisableSelection
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import mega.privacy.android.feature.texteditor.components.MarkdownBlockSpacing.listOrQuoteRow
import mega.privacy.android.feature.texteditor.components.MarkdownBlockSpacing.taskRow

/**
 * Shared chrome for Markdown block constructs — the single source of truth for how list rows,
 * quote bars, and code boxes look, consumed by both the read-only preview ([MarkdownBlock])
 * and the rich editor ([mega.privacy.android.feature.texteditor.components.markdown.rich.RichDocumentEditor]).
 * The content slot holds the construct's text: a styled [Text] in the preview, an editable
 * field in the rich editor.
 */

internal val MarkdownListIndentStep = 24.dp

/** Base inset of every list row from the body-text margin, as markdown renderers convention. */
internal val MarkdownListStartPadding = 16.dp

private val QuoteBarWidth = 3.dp

/** Vertical rhythm shared by the preview's block spacing and the rich editor's row padding. */
internal object MarkdownBlockSpacing {
    fun heading(level: Int): PaddingValues =
        PaddingValues(top = if (level <= 2) 20.dp else 14.dp, bottom = 6.dp)

    val listOrQuoteRow = PaddingValues(vertical = 2.dp)
    val block = PaddingValues(bottom = 12.dp)
    val thematicBreak = PaddingValues(vertical = 12.dp)
    val nested = PaddingValues(bottom = 4.dp)

    /** One edge of [listOrQuoteRow], for flattened rows that pad their edges independently. */
    val listOrQuoteGap = 2.dp

    /** Task rows breathe a little more than plain list rows so the boxes read as separate. */
    val taskRow = PaddingValues(vertical = 4.dp)

    /** One edge of [taskRow], the flattened-row counterpart. */
    val taskRowGap = 4.dp

    /**
     * Bottom of the LAST row of a flattened list/quote run: the row gap plus the block gap the
     * preview puts after the whole list/quote block, so run boundaries match across surfaces.
     */
    val runEndGap = 14.dp
}

/** The leading decoration of a list item row. */
internal sealed interface MarkdownListMarker {
    data object Bullet : MarkdownListMarker
    data class Number(val number: Int) : MarkdownListMarker

    /** [enabled] is false in the read-only preview: viewing must never edit the file. */
    data class TaskCheckbox(
        val checked: Boolean,
        val enabled: Boolean = true,
        val onCheckedChange: (Boolean) -> Unit = {},
    ) : MarkdownListMarker
}

/** A list item row: indent, marker (kept out of copied text), then the item content. */
@Composable
internal fun MarkdownListItemFrame(
    marker: MarkdownListMarker,
    colors: MarkdownColors,
    modifier: Modifier = Modifier,
    indent: Int = 0,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                if (marker is MarkdownListMarker.TaskCheckbox) {
                    MarkdownBlockSpacing.taskRow
                } else {
                    MarkdownBlockSpacing.listOrQuoteRow
                },
            ),
        verticalAlignment = Alignment.Top,
    ) {
        MarkdownListItemChrome(marker = marker, colors = colors, indent = indent)
        content()
    }
}

/**
 * The leading chrome of a list item row — indent plus marker — for callers that own the row
 * (the rich editor keeps its text field a stable sibling so retyping a block does not recreate
 * the field and drop focus/keyboard).
 */
@Composable
internal fun MarkdownListItemChrome(
    marker: MarkdownListMarker,
    colors: MarkdownColors,
    indent: Int = 0,
) {
    Spacer(
        modifier = Modifier.width(MarkdownListStartPadding + MarkdownListIndentStep * indent),
    )
    DisableSelection {
        when (marker) {
            is MarkdownListMarker.TaskCheckbox -> {
                // The checkbox glyph is centered on the text's FIRST line: the marker box is
                // sized from the body line height (sp-based, so it tracks font scale) instead
                // of the component's tap container, whose extra height rides below the line.
                // The row stays top-aligned so wrapped lines flow under the marker.
                val firstLineHeight = with(LocalDensity.current) {
                    MaterialTheme.typography.bodyMedium.lineHeight.toDp()
                }
                Box(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .height(firstLineHeight)
                        .toggleable(
                            value = marker.checked,
                            enabled = marker.enabled,
                            role = Role.Checkbox,
                            onValueChange = marker.onCheckedChange,
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    Checkbox(
                        checked = marker.checked,
                        // Display-only: interactivity and semantics live on the wrapping
                        // toggleable. Always rendered with enabled colors — the disabled
                        // tint is illegible in dark mode, and the read-only preview should
                        // show the task state clearly, just not accept taps.
                        onCheckedChange = null,
                        enabled = true,
                    )
                }
            }

            is MarkdownListMarker.Number -> Text(
                text = "${marker.number}. ",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text,
            )

            MarkdownListMarker.Bullet -> Text(
                text = "•  ",
                style = MaterialTheme.typography.bodyMedium,
                color = colors.text,
            )
        }
    }
}

/** A quote row: [depth] full-height bars, then the quoted content. */
@Composable
internal fun MarkdownQuoteFrame(
    depth: Int,
    colors: MarkdownColors,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .padding(MarkdownBlockSpacing.listOrQuoteRow),
        verticalAlignment = Alignment.Top,
    ) {
        MarkdownQuoteChrome(depth = depth, colors = colors)
        content()
    }
}

/**
 * The leading chrome of a quote row — [depth] full-height bars. The parent row must constrain
 * its height (e.g. `IntrinsicSize.Min`) for the bars to fill it.
 */
@Composable
internal fun MarkdownQuoteChrome(
    depth: Int,
    colors: MarkdownColors,
) {
    repeat(depth.coerceAtLeast(1)) {
        Box(
            modifier = Modifier
                .padding(end = 8.dp)
                .width(QuoteBarWidth)
                .fillMaxHeight()
                .background(colors.quoteBar),
        )
    }
}

/** A code box: rounded surface with inner padding; [scrollState] scrolls wide code sideways. */
@Composable
internal fun MarkdownCodeFrame(
    colors: MarkdownColors,
    modifier: Modifier = Modifier,
    scrollState: ScrollState? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.codeBackground, RoundedCornerShape(8.dp))
            .then(scrollState?.let { Modifier.horizontalScroll(it) } ?: Modifier)
            .padding(12.dp),
        content = content,
    )
}
