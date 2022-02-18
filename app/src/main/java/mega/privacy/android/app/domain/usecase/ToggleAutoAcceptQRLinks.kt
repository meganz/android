package mega.privacy.android.app.domain.usecase

interface ToggleAutoAcceptQRLinks {
    suspend operator fun invoke(): Boolean
}
