package mega.privacy.android.domain.usecase

import mega.privacy.android.domain.entity.preference.StartScreen

/**
 * Set start screen preference
 */
fun interface SetStartScreenPreference {
    suspend operator fun invoke(preference: StartScreen)
}