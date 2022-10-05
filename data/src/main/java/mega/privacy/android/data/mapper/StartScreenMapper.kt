package mega.privacy.android.data.mapper

import mega.privacy.android.domain.entity.preference.StartScreen


/**
 * Converts preference int value to start screen preference
 */
typealias StartScreenMapper = (@JvmSuppressWildcards Int?) -> @JvmSuppressWildcards StartScreen?