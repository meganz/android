package mega.privacy.android.core.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview


/**
 * Annotation to generate previews with multiple font scales
 */
@Preview(
    showBackground = true,
    name = "4-Small font",
    group = "font scales",
    fontScale = 0.7f
)
@Preview(
    showBackground = true,
    name = "5-Large font",
    group = "font scales",
    fontScale = 1.5f
)
private annotation class FontScalePreviews

/**
 * Annotation to generate a preview with french locale
 */
@Preview(
    locale = "fr",
    name = "3-French locale",
    group = "locales",
    showBackground = true,
)
private annotation class FrenchLocale

/**
 * Annotation to generate a preview with Arabic locale
 */
@Preview(
    locale = "ar",
    name = "4-Arabic locale",
    group = "locales",
    showBackground = true,
)
private annotation class ArabicLocale


/**
 * Annotation to generate previews with night and day themes
 */
@Preview(
    showBackground = true,
    locale = "en",
    backgroundColor = 0xFF121212,
    name = "1-Dark theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Preview(
    showBackground = true,
    locale = "en",
    name = "2-Light theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_NO
)
annotation class CombinedThemePreviews

/**
 * Annotation to generate previews with night and day themes and RTL layout
 */
@ArabicLocale
@CombinedThemePreviews
annotation class CombinedThemeRtlPreviews

/**
 * Annotation to generate previews for views with texts (font scales and locales) and night and day themes
 */
@FrenchLocale
@FontScalePreviews
@CombinedThemePreviews
annotation class CombinedTextAndThemePreviews
