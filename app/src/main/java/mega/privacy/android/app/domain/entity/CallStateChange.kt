package mega.privacy.android.app.domain.entity

sealed class CallStateChange{
    abstract val callId: Long
}

data class CallStatusChange(override val callId: Long, val callStatus: CallStatus): CallStateChange()
data class LocalAvflagsChange(override val callId: Long): CallStateChange()
data class RingingStatusChange(override val callId: Long): CallStateChange()
data class CallCompositionChange(override val callId: Long): CallStateChange()
data class CallOnHoldChange(override val callId: Long): CallStateChange()
data class CallSpeakChange(override val callId: Long): CallStateChange()
data class AudioLevelChange(override val callId: Long): CallStateChange()
data class NetworkQualityChange(override val callId: Long): CallStateChange()
