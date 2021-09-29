package mega.privacy.android.app.meeting.fragments

import nz.mega.sdk.MegaChatApiAndroid
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CreateMeetingRepository @Inject constructor(private val megaChatApi: MegaChatApiAndroid) {
    fun getMyFullName(): String? = megaChatApi.myFullname
}