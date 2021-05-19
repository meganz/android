package mega.privacy.android.app.meeting

import android.util.Log
import mega.privacy.android.app.MegaApplication
import mega.privacy.android.app.listeners.BaseListener
import mega.privacy.android.app.listeners.ChatBaseListener
import nz.mega.sdk.*
import java.text.SimpleDateFormat
import java.util.*

object GuestTool {

    private val application = MegaApplication.getInstance()
    private val megaApi = application.megaApi
    private val chatApi = application.megaChatApi

    fun start(link: String) {
        chatApi.openChatPreview(link, object : ChatBaseListener(application) {

            override fun onRequestFinish(
                api: MegaChatApiJava,
                request: MegaChatRequest,
                e: MegaChatError
            ) {
                log("[${request.requestString}] -> Param type: ${request.paramType}, Chat id: ${request.chatHandle}, Flag: ${request.flag}, Error code: ${e.errorCode} [${e.errorString}]")
                log("[${request.requestString}] -> Call id: ${request.megaHandleList?.get(0)}")

                // Remove global listener.
                chatApi.removeChatRequestListener(application)

                chatApi.logout(object : ChatBaseListener(application) {

                    override fun onRequestFinish(
                        api: MegaChatApiJava,
                        request: MegaChatRequest,
                        e: MegaChatError
                    ) {
                        log("[${request.requestString}] -> Error code: ${e.errorCode} [${e.errorString}]")

                        // INIT_WAITING_NEW_SESSION    = 1,    /// No \c sid provided at init() --> force a login+fetchnodes
                        val initResult = chatApi.init(null)
                        log("Chat init result: $initResult")

                        megaApi.createEphemeralAccountPlusPlus("The Anonymous: ", "Ash Wu", object : BaseListener(application) {

                            override fun onRequestFinish(
                                api: MegaApiJava,
                                request: MegaRequest,
                                e: MegaError
                            ) {
                                log("[${request.requestString}] -> Error code: ${e.errorCode} [${e.errorString}]")

                                megaApi.fetchNodes(object : BaseListener(application) {

                                    override fun onRequestFinish(
                                        api: MegaApiJava,
                                        request: MegaRequest,
                                        e: MegaError
                                    ) {
                                        log("[${request.requestString}] -> Error code: ${e.errorCode} [${e.errorString}]")

                                        chatApi.connect(object : ChatBaseListener(application) {

                                            override fun onRequestFinish(
                                                api: MegaChatApiJava,
                                                request: MegaChatRequest,
                                                e: MegaChatError
                                            ) {
                                                log("[${request.requestString}] -> Error code: ${e.errorCode} [${e.errorString}]")

                                                chatApi.openChatPreview(link, object : ChatBaseListener(application) {

                                                    override fun onRequestFinish(
                                                        api: MegaChatApiJava,
                                                        request: MegaChatRequest,
                                                        e: MegaChatError
                                                    ) {
                                                        log("[${request.requestString}] -> Param type: ${request.paramType}, Chat id: ${request.chatHandle}, Flag: ${request.flag}, Error code: ${e.errorCode} [${e.errorString}]")
                                                        log("[${request.requestString}] -> Call id: ${request.megaHandleList?.get(0)}")

                                                        chatApi.autojoinPublicChat(request.chatHandle, object : ChatBaseListener(application) {

                                                            override fun onRequestFinish(
                                                                api: MegaChatApiJava,
                                                                request: MegaChatRequest,
                                                                e: MegaChatError
                                                            ) {
                                                                log("[${request.requestString}] -> Error code: ${e.errorCode} [${e.errorString}]")
                                                            }
                                                        })
                                                    }
                                                })
                                            }
                                        })
                                    }
                                })
                            }
                        })
                    }
                })
            }
        })
    }
    
    private val DF = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

    fun log(any: Any?){
        var msg = any?.toString() ?: "NULL"
        val dateStr = DF.format (Date())
        msg = "[" + dateStr + "] " + "---> " + composeStackInfo() + "\n" + msg
        Log.d("@#@", msg)
    }

    private fun composeStackInfo(): String? {
        val stackTrace = Thread.currentThread().stackTrace[4]
        val fileName = stackTrace.fileName
        val methodName = stackTrace.methodName
        val line = stackTrace.lineNumber
        return "$fileName -> $methodName:$line"
    }
}