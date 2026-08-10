package mega.privacy.android.core.formatter

/**
 * Strips MEGA string annotation tags ([A]/[/A], [B]/[/B], [C]/[/C]) from a string resource,
 * returning plain text suitable for display without clickable or styled spans.
 *
 * Example: "[A]10 GB[/A] used" → "10 GB used", "[A]Expires on[/A] [B]Jul 22, 2026[/B]" -> Expires on Jul 22, 2026
 */
fun String.stripLinkAnnotations(): String =
    replace("[A]", "").replace("[/A]", "")
        .replace("[B]", "").replace("[/B]", "")
        .replace("[C]", "").replace("[/C]", "")
