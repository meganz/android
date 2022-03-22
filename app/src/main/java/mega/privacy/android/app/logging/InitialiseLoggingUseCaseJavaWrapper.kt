package mega.privacy.android.app.logging

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import mega.privacy.android.app.domain.usecase.InitialiseLogging

class InitialiseLoggingUseCaseJavaWrapper(private val useCase: InitialiseLogging) {
    @OptIn(DelicateCoroutinesApi::class)
    fun invokeUseCase(isDebug: Boolean){
        GlobalScope.launch{ useCase(isDebug) }
    }
}