package mega.privacy.android.app.presentation.settings.model

import androidx.annotation.XmlRes

/**
 * Preference resource - A data class to hold a preference resource id for dependency injection
 *
 * @property resource The preference resource id
 */
data class PreferenceResource(@XmlRes val resource: Int)