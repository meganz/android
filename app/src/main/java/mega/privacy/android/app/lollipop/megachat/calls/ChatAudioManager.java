package mega.privacy.android.app.lollipop.megachat.calls;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Vibrator;

import java.io.IOException;

import mega.privacy.android.app.R;
import nz.mega.sdk.MegaChatCall;
import nz.mega.sdk.MegaHandleList;

import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;

public class ChatAudioManager {

    Context myContext;
    private AudioManager audioManager = null;
    private MediaPlayer mediaPlayer = null;
    private Vibrator vibrator = null;

    private ChatAudioManager(Context context) {
        myContext = context;
        initializeAudioManager();
    }

    public static ChatAudioManager create(Context context) {
        return new ChatAudioManager(context);
    }

    public void initializeAudioManager() {
        if (audioManager != null) return;
        logDebug("Initializing audio manager...");
        audioManager = (AudioManager) myContext.getSystemService(Context.AUDIO_SERVICE);
    }

    public void setAudioManagerValues(MegaChatCall call, MegaHandleList listCallsRequest, MegaHandleList listCallsRing) {

        int callStatus = call.getStatus();
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
        if (mediaPlayer != null && mediaPlayer.isPlaying()) return;
        initializeAudioManager();
        if (audioManager == null) return;
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

    }

    private void incomingCallSound() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) return;
        initializeAudioManager();
        if (audioManager == null) return;
        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if (ringtoneUri == null) return;
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
    }

    private void checkVibration() {
        logDebug("Ringer mode: " + audioManager.getRingerMode() + ", Stream volume: " + audioManager.getStreamVolume(AudioManager.STREAM_RING));

        if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_SILENT) {
            if (vibrator == null || !vibrator.hasVibrator()) return;
            stopVibration();
            return;
        }

        if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_VIBRATE) {
            startVibration();
            return;
        }

        if (audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0) {
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

    public void stopAudioSignals() {
        stopSound();
        stopVibration();
    }

    private void stopSound() {
        try {
            if (mediaPlayer != null) {
                logDebug("Stopping sound...");
                mediaPlayer.stop();
                mediaPlayer.reset();
                mediaPlayer.release();
                mediaPlayer = null;
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
