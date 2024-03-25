package mega.privacy.android.domain.exception.extension

import mega.privacy.android.domain.exception.NotEnoughQuotaMegaException
import mega.privacy.android.domain.exception.QuotaExceededMegaException
import mega.privacy.android.domain.exception.node.ForeignNodeException

internal fun Throwable.shouldEmitErrorForNodeMovement(): Boolean =
    this is QuotaExceededMegaException || this is NotEnoughQuotaMegaException || this is ForeignNodeException
