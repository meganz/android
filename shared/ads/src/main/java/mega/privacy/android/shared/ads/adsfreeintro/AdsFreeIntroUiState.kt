package mega.privacy.android.shared.ads.adsfreeintro

import mega.privacy.android.core.formatter.model.FormattedSize
import mega.privacy.android.domain.entity.ThemeMode

/**
 * Ads Free Intro UI state
 *
 * @property formattedPrice the formatted price string of the cheapest subscription
 * @property storageSize the formatted storage size of the cheapest subscription
 */
data class AdsFreeIntroUiState(
    val formattedPrice: String? = null,
    val storageSize: FormattedSize? = null,
    val themeMode: ThemeMode = ThemeMode.System,
)
