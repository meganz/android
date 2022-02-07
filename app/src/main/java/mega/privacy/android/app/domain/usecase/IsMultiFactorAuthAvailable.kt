package mega.privacy.android.app.domain.usecase

interface IsMultiFactorAuthAvailable {
    operator fun invoke(): Boolean
}