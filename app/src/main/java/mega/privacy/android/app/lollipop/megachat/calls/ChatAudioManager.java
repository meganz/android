package mega.privacy.android.app.lollipop.megachat.calls;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioFocusRequest;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;
import java.io.IOException;

import mega.privacy.android.app.R;
import mega.privacy.android.app.lollipop.listeners.AudioFocusListener;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaHandleList;

import static android.media.AudioManager.STREAM_RING;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class ChatAudioManager {

    Context myContext;
    private AudioManager audioManager = null;
    private MediaPlayer mediaPlayer = null;
    private Vibrator vibrator = null;
    private AudioFocusRequest request;
    private AudioFocusListener audioFocusListener;
    private boolean isIncomingSound = false;
    private int previousVolume;

    private ChatAudioManager(Context context) {
        myContext = context;
        initializeAudioManager();
    }

    public static ChatAudioManager create(Context context) {
        return new ChatAudioManager(context);
    }

    public void initializeAudioManager() {
        logDebug("Initializing audio manager in mode normal...");

        if (audioManager != null) {
            return;
        }

        audioManager = (AudioManager) myContext.getSystemService(Context.AUDIO_SERVICE);
    }

    public void setAudioManagerValues(int callStatus, MegaHandleList listCallsRequest, MegaHandleList listCallsRing) {
        logDebug("Call status: " + callStatusToString(callStatus));

        if (callStatus == MegaChatCall.CALL_STATUS_REQUEST_SENT) {
            if (listCallsRing != null && listCallsRing.size() > 0) {
                logDebug("There was also an incoming call (stop incoming call sound)");
                stopAudioSignals();
            }
            outgoingCallSound();

        } else if (callStatus == MegaChatCall.CALL_STATUS_RING_IN) {
            if (listCallsRequest == null || listCallsRequest.size() < 1) {
                logDebug("I'm not calling");
                if (listCallsRing != null && listCallsRing.size() > 1) {
                    logDebug("There is another incoming call (stop the sound of the previous incoming call)");
                    stopAudioSignals();
                }

                incomingCallSound();
            }

            checkVibration();
        }
    }

    private void outgoingCallSound() {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            return;

        initializeAudioManager();
        if (audioManager == null)
            return;

        audioFocusListener = new AudioFocusListener(myContext);
        int focusType = AudioManager.AUDIOFOCUS_GAIN;
        int streamType = AudioManager.STREAM_VOICE_CALL;
        request = getRequest(audioFocusListener, focusType);

        if (getAudioFocus(audioManager, audioFocusListener, request, focusType, streamType)) {
            audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL), 0);
            Resources res = myContext.getResources();
            AssetFileDescriptor afd = res.openRawResourceFd(R.raw.outgoing_voice_video_call);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioStreamType(AudioManager.STREAM_VOICE_CALL);
            mediaPlayer.setLooping(true);

            try {
                mediaPlayer.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                logDebug("Preparing mediaPlayer");
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
                logError("Error preparing mediaPlayer", e);
                return;
            }

            logDebug("Start outgoing call sound");
            mediaPlayer.start();
            isIncomingSound = false;
        }
    }

    private void incomingCallSound() {
        if (mediaPlayer != null && mediaPlayer.isPlaying())
            return;

        initializeAudioManager();
        if (audioManager == null)
            return;

        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if (ringtoneUri == null)
            return;

        audioFocusListener = new AudioFocusListener(myContext);
        request = getRequest(audioFocusListener, AUDIOFOCUS_DEFAULT);

        if (getAudioFocus(audioManager, audioFocusListener, request, AUDIOFOCUS_DEFAULT, STREAM_MUSIC_DEFAULT)) {
            muteOrUnmuteIncomingCall(false);
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build());
            mediaPlayer.setLooping(true);

            try {
                mediaPlayer.setDataSource(myContext, ringtoneUri);
                logDebug("Preparing mediaPlayer");
                mediaPlayer.prepare();
            } catch (IOException e) {
                e.printStackTrace();
                logError("Error preparing mediaPlayer", e);
                return;
            }

            logDebug("Start incoming call sound");
            mediaPlayer.start();
            previousVolume = audioManager.getStreamVolume(STREAM_RING);
            isIncomingSound = true;
        }
    }

    private void checkVibration() {
        logDebug("Ringer mode: " + audioManager.getRingerMode() + ", Stream volume: " + audioManager.getStreamVolume(STREAM_RING));

        if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
            if (vibrator == null || !vibrator.hasVibrator()) return;
            stopVibration();
            return;
        }

        if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
            startVibration();
            return;
        }

        if (audioManager.getStreamVolume(STREAM_RING) == 0) {
            return;
        }
        startVibration();
    }

    private void startVibration() {
        if (vibrator != null) return;
        vibrator = (Vibrator) myContext.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        logDebug("Vibration begins");
        long[] pattern = {0, 1000, 500, 500, 1000};
        vibrator.vibrate(pattern, 0);
    }

    /**
     * Method of checking whether the volume has been raised or lowered using the keys on the device.
     *
     * @param newVolume The new volume detected.
     */
    public void checkVolume(int newVolume) {
        if (newVolume < previousVolume) {
            muteOrUnmuteIncomingCall(true);
        } else if (newVolume > previousVolume && isPlayingIncomingCall()) {
            muteOrUnmuteIncomingCall(false);
        }
        previousVolume = newVolume;
    }

    /**
     * Method to know if an incoming call is ringing
     *
     * @return True, if it's ringing. False, if not.
     */
    private boolean isPlayingIncomingCall() {
        return mediaPlayer != null && mediaPlayer.isPlaying() && isIncomingSound && audioManager != null;
    }

    /**
     * Method to mute or unmute an incoming call.
     */
    public void muteOrUnmuteIncomingCall(boolean isNeccesaryMute) {
        if (isNeccesaryMute && !isPlayingIncomingCall()){
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (isNeccesaryMute && !audioManager.isStreamMute(STREAM_RING)) {
                audioManager.adjustStreamVolume(STREAM_RING, AudioManager.ADJUST_MUTE, 0);
            } else if (!isNeccesaryMute && audioManager.isStreamMute(STREAM_RING)) {
                audioManager.adjustStreamVolume(STREAM_RING, AudioManager.ADJUST_UNMUTE, 0);
            }
        } else {
            audioManager.setStreamMute(STREAM_RING, isNeccesaryMute);
        }
    }

    public void stopAudioSignals() {
        logDebug("Stop sound and vibration");
        stopSound();
        stopVibration();
        if (audioManager != null) {
            abandonAudioFocus(audioFocusListener, audioManager, request);
        }
    }

    private void stopSound() {
        try {
            if (mediaPlayer != null) {
                logDebug("Stopping sound...");
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
                isIncomingSound = false;
                muteOrUnmuteIncomingCall(false);
            }
        } catch (Exception e) {
            logWarning("Exception stopping player", e);
        }
    }

    private void stopVibration() {
        try {
            if (vibrator != null && vibrator.hasVibrator()) {
                logDebug("Canceling vibration...");
                vibrator.cancel();
                vibrator = null;
            }
        } catch (Exception e) {
            logWarning("Exception canceling vibrator", e);
        }
    }
}
