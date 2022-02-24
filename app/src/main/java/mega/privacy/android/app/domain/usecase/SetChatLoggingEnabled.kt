package mega.privacy.android.app.domain.usecase

interface SetChatLoggingEnabled {
    operator fun invoke(enabled: Boolean)
}
