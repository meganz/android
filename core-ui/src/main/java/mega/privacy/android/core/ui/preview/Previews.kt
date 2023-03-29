package mega.privacy.android.core.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview


/**
 * Annotation to generate previews with multiple font scales
 */
@Preview(
    showBackground = true,
    name = "small font",
    group = "font scales",
    fontScale = 0.7f
)
@Preview(
    showBackground = true,
    name = "default font",
    group = "font scales",
    fontScale = 1f
)
@Preview(
    showBackground = true,
    name = "large font",
    group = "font scales",
    fontScale = 1.5f
)
annotation class FontScalePreviews

/**
 * Annotation to generate a preview with french locale
 */
@Preview(
    locale = "fr",
    showBackground = true,
)
annotation class FrenchLocale


/**
 * Annotation to generate previews with night and day themes
 */
@Preview(
    showBackground = true,
    backgroundColor = 0xFF121212,
    name = "dark theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    showBackground = true,
    name = "light theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
annotation class CombinedThemePreviews

/**
 * Annotation to generate previews for views with texts (font scales and locales)
 */
@FrenchLocale
@FontScalePreviews
annotation class CombinedTextPreviews

/**
 * Annotation to generate previews for views with texts (font scales and locales) and night and day themes
 */
@CombinedTextPreviews
@CombinedThemePreviews
annotation class CombinedTextAndThemePreviews
