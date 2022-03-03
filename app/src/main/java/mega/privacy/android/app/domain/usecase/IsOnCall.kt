package mega.privacy.android.app.domain.usecase

interface IsOnCall {
    suspend operator fun invoke(): Boolean
}
