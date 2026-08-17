package mega.privacy.android.feature.sync.ui.preview

import android.content.res.Configuration
import androidx.compose.ui.tooling.preview.Preview

/**
 * Light and dark previews on a landscape phone.
 *
 * core-ui offers portrait and tablet variants but no phone landscape one, and the sync setup flow
 * lays itself out differently in landscape, so it is defined here rather than importing the
 * original-core-ui annotation this module is migrating away from.
 */
@Preview(
    showBackground = true,
    locale = "en",
    backgroundColor = 0xFF151616,
    name = "1-Dark theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_YES,
    device = "spec:width=800dp,height=360dp",
)
@Preview(
    showBackground = true,
    locale = "en",
    name = "2-Light theme",
    group = "themes",
    uiMode = Configuration.UI_MODE_NIGHT_NO,
    device = "spec:width=800dp,height=360dp",
)
annotation class CombinedThemePhoneLandscapePreviews
