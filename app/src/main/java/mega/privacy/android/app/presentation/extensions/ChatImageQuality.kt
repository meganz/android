package mega.privacy.android.app.presentation.extensions

import mega.privacy.android.app.R
import mega.privacy.android.app.domain.entity.ChatImageQuality


internal val ChatImageQuality.title: Int
    get() = when (this) {
        ChatImageQuality.Automatic -> R.string.automatic_image_quality
        ChatImageQuality.Original -> R.string.high_image_quality
        ChatImageQuality.Optimised -> R.string.optimised_image_quality
    }

internal val ChatImageQuality.description: Int
    get() = when (this) {
        ChatImageQuality.Automatic -> R.string.automatic_image_quality_text
        ChatImageQuality.Original -> R.string.high_image_quality_text
        ChatImageQuality.Optimised -> R.string.optimised_image_quality_text
    }

