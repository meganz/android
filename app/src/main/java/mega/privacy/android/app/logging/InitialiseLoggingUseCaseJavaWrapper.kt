package mega.privacy.android.app.logging

import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.InitialiseLogging

class InitialiseLoggingUseCaseJavaWrapper(private val useCase: InitialiseLogging) {
    fun invokeUseCase(isDebug: Boolean){
        GlobalScope.launch{ useCase(isDebug) }
    }
}