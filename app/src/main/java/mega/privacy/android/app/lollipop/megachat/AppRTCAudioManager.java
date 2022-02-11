/*
 *  Copyright 2014 The WebRTC Project Authors. All rights reserved.
 *
 *  Use of this source code is governed by a BSD-style license
 *  that can be found in the LICENSE file in the root of the source
 *  tree. An additional intellectual property rights grant can be found
 *  in the file PATENTS.  All contributing project authors may
 *  be found in the AUTHORS file in the root of the source tree.
 */

package mega.privacy.android.app.lollipop.megachat;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.Resources;
import android.media.AudioAttributes;
import android.media.AudioDeviceInfo;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Vibrator;

import com.jeremyliao.liveeventbus.LiveEventBus;

import org.webrtc.ThreadUtils;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import mega.privacy.android.app.MegaApplication;
import mega.privacy.android.app.R;
import mega.privacy.android.app.interfaces.OnProximitySensorListener;
import mega.privacy.android.app.utils.VideoCaptureUtils;

import static android.media.AudioManager.RINGER_MODE_NORMAL;
import static android.media.AudioManager.RINGER_MODE_SILENT;
import static android.media.AudioManager.RINGER_MODE_VIBRATE;
import static mega.privacy.android.app.constants.EventConstants.EVENT_AUDIO_OUTPUT_CHANGE;
import static mega.privacy.android.app.utils.CallUtil.*;
import static mega.privacy.android.app.utils.Constants.*;
import static mega.privacy.android.app.utils.ChatUtil.*;
import static mega.privacy.android.app.utils.LogUtil.*;

import androidx.annotation.RequiresApi;

/**
 * AppRTCAudioManager manages all audio related parts of the AppRTC demo.
 */
public class AppRTCAudioManager {
    private static final String TAG = "AppRTCAudioManager";
    private final Context apprtcContext;
    // Handles all tasks related to Bluetooth headset devices.
    private AppRTCBluetoothManager bluetoothManager;
    private final AudioManager audioManager;
    private AudioManagerState amState;
    private MediaPlayer mediaPlayer;
    private Vibrator vibrator;
    private boolean savedIsSpeakerPhoneOn = false;
    private boolean savedIsMicrophoneMute = false;
    private boolean hasWiredHeadset = false;
    private OnProximitySensorListener proximitySensorListener;
    private int typeAudioManager;
    private boolean isTemporary;
    private boolean isIncomingSound = false;
    private int previousVolume;

    // Default audio device; speaker phone for video calls or earpiece for audio
    // only calls.
    private AudioDevice defaultAudioDevice;
    // Contains the currently selected audio device.
    // This device is changed automatically using a certain scheme where e.g.
    // a wired headset "wins" over speaker phone. It is also possible for a
    // user to explicitly select a device (and overrid any predefined scheme).
    // See |userSelectedAudioDevice| for details.
    private AudioDevice selectedAudioDevice;
    // Contains the user-selected audio device which overrides the predefined
    // selection scheme.
    // Add support for explicit selection based on choice by userSelectedAudioDevice.
    private AudioDevice userSelectedAudioDevice = AudioDevice.NONE;
    // Proximity sensor object. It measures the proximity of an object in cm
    // relative to the view screen of a device and can therefore be used to
    // assist device switching (close to ear <=> use headset earpiece if
    // available, far from ear <=> use speaker phone).
    private AppRTCProximitySensor proximitySensor = null;
    // Contains a list of available audio devices. A Set collection is used to
    // avoid duplicate elements.
    private Set<AudioDevice> audioDevices = new HashSet<>();
    // Broadcast receiver for wired headset intent broadcasts.
    private final BroadcastReceiver wiredHeadsetReceiver;
    // Callback method for changes in audio focus.
    private AudioManager.OnAudioFocusChangeListener audioFocusChangeListener;

    private AppRTCAudioManager(Context context, boolean statusSpeaker, int type) {
        ThreadUtils.checkIsOnMainThread();
        apprtcContext = context;
        startBluetooth();
        audioManager = ((AudioManager) context.getSystemService(Context.AUDIO_SERVICE));
        wiredHeadsetReceiver = new WiredHeadsetReceiver();
        amState = AudioManagerState.UNINITIALIZED;
        this.typeAudioManager = type;
        isTemporary = false;
        start(statusSpeaker);
        if (apprtcContext instanceof ChatActivityLollipop) {
            registerProximitySensor();
        }

        logDebug("Default audio device is " + defaultAudioDevice);
        AppRTCUtils.logDeviceInfo(TAG);
    }

    public void startBluetooth() {
        if (bluetoothManager == null){
            logDebug("Starting bluetooth");
            bluetoothManager = AppRTCBluetoothManager.create(apprtcContext, this);
        }
    }
    public void stopBluetooth() {
        if (bluetoothManager == null)
            return;

        logDebug("Stopping bluetooth");
        bluetoothManager.stop();
        bluetoothManager = null;
    }

    public void setOnProximitySensorListener(OnProximitySensorListener proximitySensorListener) {
        this.proximitySensorListener = proximitySensorListener;
    }

    public boolean registerProximitySensor() {
        // Create and initialize the proximity sensor.
        // Note that, the sensor will not be active until start() has been called.
        //This method will be called each time a state change is detected.
        if (proximitySensor != null) return false;

        logDebug("Registering proximity sensor");
        proximitySensor = AppRTCProximitySensor.create(apprtcContext, this::onProximitySensorChangedState);
        return true;
    }

    public boolean startProximitySensor() {
        if (registerProximitySensor()) {
            logDebug("Starting proximity sensor");
            proximitySensor.start();
            return true;
        }

        return false;
    }

    /**
     * This method is called when the proximity sensor reports a state change,
     * e.g. from "NEAR to FAR" or from "FAR to NEAR".
     */
    private void onProximitySensorChangedState() {
        boolean isNear = proximitySensor.sensorReportsNearState();
        if (isNear) {
            if (VideoCaptureUtils.isFrontCameraInUse()) {
                // Sensor reports that a "handset is being held up to a person's ear", or "something is covering the light sensor".
                proximitySensor.turnOffScreen();
                logDebug("Screen off");

                if ((apprtcContext instanceof MegaApplication && MegaApplication.isSpeakerOn &&
                        (bluetoothManager == null || bluetoothManager.getState() != AppRTCBluetoothManager.State.SCO_CONNECTED)) ||
                        apprtcContext instanceof ChatActivityLollipop) {
                    logDebug("Disabling the speakerphone:");
                    selectAudioDevice(AudioDevice.EARPIECE, true);
                }
            }
        } else {
            // Sensor reports that a "handset is removed from a person's ear", or "the light sensor is no longer covered".
            proximitySensor.turnOnScreen();
            logDebug("Screen on");

            if ((apprtcContext instanceof MegaApplication && MegaApplication.isSpeakerOn &&
                    (bluetoothManager == null || bluetoothManager.getState() != AppRTCBluetoothManager.State.SCO_CONNECTED)) ||
                    apprtcContext instanceof ChatActivityLollipop) {
                logDebug("Enabling the speakerphone: ");
                selectAudioDevice(AudioDevice.SPEAKER_PHONE, true);
            }
        }

        if (proximitySensorListener != null) proximitySensorListener.needToUpdate(isNear);
    }

    public void setTypeAudioManager(int type) {
        this.typeAudioManager = type;

        if (typeAudioManager == AUDIO_MANAGER_CALL_IN_PROGRESS) {
            stopAudioSignals();
        }

        setValues();
    }

    public int getTypeAudioManager() {
        return typeAudioManager;
    }

    private void setValues() {
        if ((typeAudioManager != AUDIO_MANAGER_CALL_RINGING && typeAudioManager != AUDIO_MANAGER_CALL_OUTGOING) ||
                bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_AVAILABLE ||
                bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTING) {
            return;
        }
        setAudioManagerValues();
    }

    private void setAudioManagerValues() {
        logDebug("Updating values of Chat Audio Manager...");
        if (typeAudioManager == AUDIO_MANAGER_CALL_OUTGOING) {
            logDebug("If there was also an incoming call (stop incoming call sound)");
            stopAudioSignals();
            outgoingCallSound();
        } else if (typeAudioManager == AUDIO_MANAGER_CALL_RINGING) {
            logDebug("If there is another incoming call (stop the sound of the previous incoming call)");
            stopAudioSignals();
            incomingCallSound();
            checkVibration();
        }
    }

    private void outgoingCallSound() {
        if (audioManager == null || (mediaPlayer != null && mediaPlayer.isPlaying()))
            return;

        audioManager.setStreamVolume(AudioManager.STREAM_VOICE_CALL, audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL), 0);
        Resources res = MegaApplication.getInstance().getBaseContext().getResources();
        AssetFileDescriptor afd = res.openRawResourceFd(R.raw.outgoing_voice_video_call);

        if (mediaPlayer != null) {
            stopSound();
        }

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

    private void incomingCallSound() {
        if (audioManager == null || (mediaPlayer != null && mediaPlayer.isPlaying()))
            return;

        Uri ringtoneUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
        if (ringtoneUri == null)
            return;

        if (mediaPlayer != null) {
            stopSound();
        }

        mediaPlayer = new MediaPlayer();
        if(audioManager.getRingerMode() != RINGER_MODE_SILENT) {
            audioManager.setStreamVolume(AudioManager.STREAM_RING, audioManager.getStreamVolume(AudioManager.STREAM_RING), 0);
        }
        mediaPlayer.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_NOTIFICATION_RINGTONE).build());
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_RING);
        mediaPlayer.setLooping(true);

        try {
            mediaPlayer.setDataSource(MegaApplication.getInstance().getBaseContext(), ringtoneUri);
            logDebug("Preparing mediaPlayer");
            mediaPlayer.prepare();
        } catch (IOException e) {
            e.printStackTrace();
            logError("Error preparing mediaPlayer", e);
            return;
        }
        logDebug("Start incoming call sound");
        mediaPlayer.start();
        previousVolume = audioManager.getStreamVolume(AudioManager.STREAM_RING);
        isIncomingSound = true;
    }

    private void checkVibration() {
        if (audioManager == null)
            return;

        logDebug("Ringer mode: " + audioManager.getRingerMode() + ", Stream volume: " + audioManager.getStreamVolume(AudioManager.STREAM_RING) + ", Voice call volume: " + audioManager.getStreamVolume(AudioManager.STREAM_VOICE_CALL));
        if (participatingInACall()) {
            if (vibrator == null || !vibrator.hasVibrator())
                return;

            stopVibration();
            return;
        }

        switch (audioManager.getRingerMode()) {
            case RINGER_MODE_SILENT:
                stopVibration();
                break;

            case RINGER_MODE_VIBRATE:
                startVibration();
                break;

            case RINGER_MODE_NORMAL:
                if (audioManager.getStreamVolume(AudioManager.STREAM_RING) == 0 && audioManager.isStreamMute(AudioManager.STREAM_RING)) {
                    stopVibration();
                } else {
                    startVibration();
                }
                break;
        }
    }

    private void startVibration() {
        if (vibrator == null) {
            vibrator = (Vibrator) apprtcContext.getSystemService(Context.VIBRATOR_SERVICE);
        }

        if (vibrator == null || !vibrator.hasVibrator())
            return;

        long[] pattern = {0, 1000, 500, 500, 1000};

        AudioAttributes audioAttributes = new AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_ALARM)
                .build();

        logDebug("Vibration begins");
        vibrator.vibrate(pattern, 0, audioAttributes);
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
        if (audioManager == null || audioManager.getRingerMode() == RINGER_MODE_SILENT ||
                (isNeccesaryMute && !isPlayingIncomingCall())) {
            return;
        }

        if (audioManager.getRingerMode() == RINGER_MODE_VIBRATE) {
            if (isNeccesaryMute) {
                stopVibration();
            } else {
                startVibration();
            }
            return;
        }

        if (isNeccesaryMute && !audioManager.isStreamMute(AudioManager.STREAM_RING)) {
            audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_MUTE, 0);
            stopVibration();

            if (participatingInACall()) {
                MegaApplication.getInstance().removeRTCAudioManagerRingIn();
            }
        } else if (!isNeccesaryMute && audioManager.isStreamMute(AudioManager.STREAM_RING)) {
            adjustStreamVolume();
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    private void adjustStreamVolume() {
        audioManager.adjustStreamVolume(AudioManager.STREAM_RING, AudioManager.ADJUST_UNMUTE, 0);
        checkVibration();
    }

    /**
     * Method for stopping sound and vibration.
     */
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

    public void unregisterProximitySensor() {
        if (proximitySensor == null) return;

        logDebug("Stopping proximity sensor");
        proximitySensor.stop();
        proximitySensor = null;
    }

    private void speakerElements(boolean isOn) {
        setDefaultAudioDevice(isOn ? AudioDevice.SPEAKER_PHONE : AudioDevice.EARPIECE);
        MegaApplication.isSpeakerOn = isOn;
    }

    /**
     * Construction.
     */
   public static AppRTCAudioManager create(Context context, boolean isSpeakerOn, int type) {
       return new AppRTCAudioManager(context, isSpeakerOn, type);
    }

    public void updateSpeakerStatus(boolean speakerStatus, int type) {
        typeAudioManager = type;

        logDebug("Speaker status is " + speakerStatus);
        selectAudioDevice(speakerStatus ? AudioDevice.SPEAKER_PHONE : AudioDevice.EARPIECE, false);
    }

    private void start(boolean statusSpeaker) {
        ThreadUtils.checkIsOnMainThread();
        if (amState == AudioManagerState.RUNNING) {
            logError("AudioManager is already active");
            return;
        }

        logDebug("AudioManager starts... ");
        amState = AudioManagerState.RUNNING;

        // Store current audio state so we can restore it when stop() is called.
        savedIsSpeakerPhoneOn = audioManager.isSpeakerphoneOn();
        savedIsMicrophoneMute = audioManager.isMicrophoneMute();
        hasWiredHeadset = hasWiredHeadset();

        // Create an AudioManager.OnAudioFocusChangeListener instance.
        // Called on the listener to notify if the audio focus for this listener has been changed.
// The |focusChange| value indicates whether the focus was gained, whether the focus was lost,
// and whether that loss is transient, or whether the new focus holder will hold it for an
// unknown amount of time.
// logging for now.
        audioFocusChangeListener = focusChange -> {
            String typeOfChange = "AUDIOFOCUS_NOT_DEFINED";
            switch (focusChange) {
                case AudioManager.AUDIOFOCUS_GAIN:
                    typeOfChange = "AUDIOFOCUS_GAIN";
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                    typeOfChange = "AUDIOFOCUS_GAIN_TRANSIENT";
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE:
                    typeOfChange = "AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE";
                case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                    typeOfChange = "AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK";
                    startBluetooth();
                    break;

                case AudioManager.AUDIOFOCUS_LOSS:
                    typeOfChange = "AUDIOFOCUS_LOSS";
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                    typeOfChange = "AUDIOFOCUS_LOSS_TRANSIENT";
                case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                    typeOfChange = "AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK";
                    stopBluetooth();
                    break;

                default:
                    typeOfChange = "AUDIOFOCUS_INVALID";
                    break;
            }
            logDebug("Audio focus change " + typeOfChange);
        };

        int typeStream;
        int typeFocus;
        switch (typeAudioManager) {
            case AUDIO_MANAGER_PLAY_VOICE_CLIP:
                typeStream = STREAM_MUSIC_DEFAULT;
                typeFocus = AUDIOFOCUS_DEFAULT;
                break;

            case AUDIO_MANAGER_CALL_RINGING:
                typeStream = AudioManager.STREAM_RING;
                typeFocus = AUDIOFOCUS_DEFAULT;
                break;

            default:
                typeStream = AudioManager.STREAM_VOICE_CALL;
                typeFocus = AudioManager.AUDIOFOCUS_GAIN;
                break;
        }

        int result = audioManager.requestAudioFocus(audioFocusChangeListener, typeStream, typeFocus);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            logDebug("Audio focus request granted for VOICE_CALL streams");
        } else {
            logError("Audio focus request failed");
        }

        if (typeAudioManager != AUDIO_MANAGER_PLAY_VOICE_CLIP &&
                typeAudioManager != AUDIO_MANAGER_CALL_RINGING &&
                (Build.VERSION.SDK_INT < Build.VERSION_CODES.N ||
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)) {
            logDebug("Mode communication");
            audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
        } else {
            logDebug("Mode normal");
            audioManager.setMode(AudioManager.MODE_NORMAL);
        }

        // Always disable microphone mute during a WebRTC call.
        setMicrophoneMute(false);

        // Set initial device states.
        if (typeAudioManager == AUDIO_MANAGER_PLAY_VOICE_CLIP || typeAudioManager == AUDIO_MANAGER_CREATING_JOINING_MEETING) {
            userSelectedAudioDevice = AudioDevice.SPEAKER_PHONE;
        } else {
            userSelectedAudioDevice = AudioDevice.NONE;
        }

        selectedAudioDevice = AudioDevice.NONE;
        audioDevices.clear();

        // Do initial selection of audio device. This setting can later be changed
        // either by adding/removing a BT or wired headset or by covering/uncovering
        // the proximity sensor.
        if (!participatingInACall()) {
            speakerElements(statusSpeaker);
        }

        // Register receiver for broadcast intents related to adding/removing a
        // wired headset.
        registerReceiver(wiredHeadsetReceiver, new IntentFilter(Intent.ACTION_HEADSET_PLUG));
        logDebug("AudioManager started");
    }

    public void stop() {
        logDebug("Stopping audio manager");
        ThreadUtils.checkIsOnMainThread();
        if (amState != AudioManagerState.RUNNING) {
            logError("Trying to stop AudioManager in incorrect state: " + amState);
            return;
        }

        typeAudioManager = INVALID_CALL_STATUS;
        amState = AudioManagerState.UNINITIALIZED;
        unregisterReceiver(wiredHeadsetReceiver);

        stopAudioSignals();
        stopBluetooth();

        // Restore previously stored audio states.
        setSpeakerphoneOn(savedIsSpeakerPhoneOn);
        setMicrophoneMute(savedIsMicrophoneMute);
        audioManager.setMode(AudioManager.MODE_NORMAL);

        // Abandon audio focus. Gives the previous focus owner, if any, focus.
        if(audioFocusChangeListener != null) audioManager.abandonAudioFocus(audioFocusChangeListener);
        audioFocusChangeListener = null;
        logDebug("Abandoned audio focus for VOICE_CALL streams");
        unregisterProximitySensor();
        logDebug("AudioManager stopped");
    }

    /**
     * Changes selection of the currently active audio device.
     */
    private void setAudioDeviceInternal(AudioDevice device) {
        logDebug("Selected audio device internal is " + device);
        device = getDeviceAvailable(device);
        if (device == null)
            return;

        logDebug("Audio device internal finally selected is " + device);
        AppRTCUtils.assertIsTrue(audioDevices.contains(device));
        switch (device) {
            case SPEAKER_PHONE:
                if(typeAudioManager == AUDIO_MANAGER_PLAY_VOICE_CLIP){
                    audioManager.setMode(AudioManager.MODE_NORMAL);
                }

                MegaApplication.isSpeakerOn = true;
                setSpeakerphoneOn(true);
                break;

            case EARPIECE:
                if(!isTemporary) {
                    MegaApplication.isSpeakerOn = false;
                }

                if(typeAudioManager == AUDIO_MANAGER_PLAY_VOICE_CLIP){
                    audioManager.setMode(AudioManager.MODE_IN_COMMUNICATION);
                }

                setSpeakerphoneOn(false);
                break;

            case WIRED_HEADSET:
            case BLUETOOTH:
                MegaApplication.isSpeakerOn = false;
                setSpeakerphoneOn(false);
                break;

            default:
                logError("Invalid audio device selection: " + device);
                return;
        }

        if (selectedAudioDevice != device) {
            selectedAudioDevice = device;
            logDebug("New audio device selected is " + selectedAudioDevice);
            LiveEventBus.get(EVENT_AUDIO_OUTPUT_CHANGE, AudioDevice.class).post(selectedAudioDevice);
            setValues();
        }
    }

    /**
     * Changes default audio device.
     * TODO(henrika): add usage of this method in the AppRTCMobile client.
     */
    private void setDefaultAudioDevice(AudioDevice defaultDevice) {
        ThreadUtils.checkIsOnMainThread();
        switch (defaultDevice) {
            case SPEAKER_PHONE:
                defaultAudioDevice = AudioDevice.SPEAKER_PHONE;
                break;

            case EARPIECE:
                if (hasEarpiece()) {
                    defaultAudioDevice = defaultDevice;
                } else {
                    defaultAudioDevice = AudioDevice.SPEAKER_PHONE;
                }
                break;

            default:
                logError("Invalid default audio device selection: " + defaultDevice);
                break;
        }

        logDebug("Set default audio device is " + defaultAudioDevice);
        updateAudioDeviceState();
    }

    /**
     * Method to know if an AudioDevice is available on the device
     *
     * @param device AudioDevice
     * @return True, if the array of available audioDevices contains that device. False, otherwise.
     */
    private boolean isDeviceAvailable(AudioDevice device) {
        return audioDevices != null && audioDevices.contains(device);
    }

    private AudioDevice getDeviceAvailable(AudioDevice device) {
        if (isDeviceAvailable(device))
            return device;

        device = AudioDevice.WIRED_HEADSET;
        if (isDeviceAvailable(device))
            return device;

        device = AudioDevice.BLUETOOTH;
        if (isDeviceAvailable(device))
            return device;

        device = AudioDevice.SPEAKER_PHONE;
        if (isDeviceAvailable(device))
            return device;

        logError("Can not select " + device + ", from available " + audioDevices);
        return null;
    }

    /**
     * Changes selection of the currently active audio device.
     */
    public void selectAudioDevice(AudioDevice device, boolean temporary) {
        ThreadUtils.checkIsOnMainThread();
        logDebug("Selected audio device is " + device);
        device = getDeviceAvailable(device);
        if (device == null)
            return;

        logDebug("Audio device finally selected is " + device);

        isTemporary = temporary;
        userSelectedAudioDevice = device;
        updateAudioDeviceState();
    }

    /**
     * Returns current set of available/selectable audio devices.
     */
    public Set<AudioDevice> getAudioDevices() {
        ThreadUtils.checkIsOnMainThread();
        return Collections.unmodifiableSet(new HashSet<>(audioDevices));
    }

    /**
     * Returns the currently selected audio device.
     */
    public AudioDevice getSelectedAudioDevice() {
        ThreadUtils.checkIsOnMainThread();
        return selectedAudioDevice;
    }

    /**
     * Helper method for receiver registration.
     */
    private void registerReceiver(BroadcastReceiver receiver, IntentFilter filter) {
        apprtcContext.registerReceiver(receiver, filter);
    }

    /**
     * Helper method for unregistration of an existing receiver.
     */
    private void unregisterReceiver(BroadcastReceiver receiver) {
        apprtcContext.unregisterReceiver(receiver);
    }

    /**
     * Sets the speaker phone mode.
     */
    private void setSpeakerphoneOn(boolean on) {
        if(audioManager == null)
            return;

        boolean wasOn = audioManager.isSpeakerphoneOn();
        if (wasOn == on) {
            return;
        }

        audioManager.setSpeakerphoneOn(on);
    }

    /**
     * Sets the microphone mute state.
     */
    private void setMicrophoneMute(boolean on) {
        if(audioManager == null)
            return;

        boolean wasMuted = audioManager.isMicrophoneMute();
        if (wasMuted == on) {
            return;
        }
        audioManager.setMicrophoneMute(on);
    }

    /**
     * Gets the current earpiece state.
     */
    private boolean hasEarpiece() {
        return apprtcContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY);
    }

    /**
     * Checks whether a wired headset is connected or not.
     * This is not a valid indication that audio playback is actually over
     * the wired headset as audio routing depends on other conditions. We
     * only use it as an early indicator (during initialization) of an attached
     * wired headset.
     */
    @Deprecated
    private boolean hasWiredHeadset() {
        final AudioDeviceInfo[] devices = audioManager.getDevices(AudioManager.GET_DEVICES_ALL);
        for (AudioDeviceInfo device : devices) {
            final int type = device.getType();
            if (type == AudioDeviceInfo.TYPE_WIRED_HEADSET) {
                logDebug("Found wired headset");
                return true;
            }

            if (type == AudioDeviceInfo.TYPE_USB_DEVICE) {
                logDebug("Found USB audio device");
                return true;
            }
        }

        return false;
    }

    public void changeUserSelectedAudioDeviceForHeadphone(AudioDevice device){
        userSelectedAudioDevice = device;
    }

    /**
     * Updates list of possible audio devices and make new device selection.
     * TODO(henrika): add unit test to verify all state transitions.
     */
    public void updateAudioDeviceState() {
        startBluetooth();
        ThreadUtils.checkIsOnMainThread();
        logDebug("Update audio device state. Wired headset " + hasWiredHeadset + ", Bluetooth " + bluetoothManager.getState());
        logDebug("Device status:. available " + audioDevices + ", selected " + selectedAudioDevice + ", user selected " + userSelectedAudioDevice);
        // Check if any Bluetooth headset is connected. The internal BT state will
        // change accordingly.
        if (bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_AVAILABLE
                || bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_UNAVAILABLE
                || bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_DISCONNECTING) {
            bluetoothManager.updateDevice();
        }

        // Update the set of available audio devices.
        Set<AudioDevice> newAudioDevices = new HashSet<>();

        if (bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTED
                || bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTING
                || bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_AVAILABLE) {
            newAudioDevices.add(AudioDevice.BLUETOOTH);
        }

        if (hasWiredHeadset) {
            // If a wired headset is connected, then it is the only possible option.
            newAudioDevices.add(AudioDevice.WIRED_HEADSET);
        }

        // No wired headset, hence the audio-device list can contain speaker
        // phone (on a tablet), or speaker phone and earpiece (on mobile phone).
        newAudioDevices.add(AudioDevice.SPEAKER_PHONE);

        if (hasEarpiece()) {
            newAudioDevices.add(AudioDevice.EARPIECE);
        }

        boolean audioDeviceSetUpdated;
        // Store state which is set to true if the device list has changed.
        if(audioDevices.equals(newAudioDevices)){
            //Equals
            audioDeviceSetUpdated = false;
        }else{
            audioDevices.clear();
            // Update the existing audio device set.
            audioDevices = newAudioDevices;
            audioDeviceSetUpdated = true;
        }

        if (bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_UNAVAILABLE && userSelectedAudioDevice == AudioDevice.BLUETOOTH) {
            logWarning("Bluetooth is not available");
            userSelectedAudioDevice = AudioDevice.EARPIECE;
        }

        if (userSelectedAudioDevice == AudioDevice.NONE) {
            if (MegaApplication.isSpeakerOn) {
                userSelectedAudioDevice = AudioDevice.SPEAKER_PHONE;
            } else if (hasWiredHeadset) {
                userSelectedAudioDevice = AudioDevice.WIRED_HEADSET;
            } else if (bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_AVAILABLE) {
                userSelectedAudioDevice = AudioDevice.BLUETOOTH;
            } else {
                userSelectedAudioDevice = AudioDevice.EARPIECE;
            }
        } else if (hasWiredHeadset && userSelectedAudioDevice != AudioDevice.SPEAKER_PHONE) {
            userSelectedAudioDevice = AudioDevice.WIRED_HEADSET;
        } else if (bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_AVAILABLE && userSelectedAudioDevice != AudioDevice.SPEAKER_PHONE) {
            userSelectedAudioDevice = AudioDevice.BLUETOOTH;
        } else if (userSelectedAudioDevice != AudioDevice.SPEAKER_PHONE) {
            userSelectedAudioDevice = AudioDevice.EARPIECE;
        }

        // Need to start Bluetooth if it is available and user either selected it explicitly or
        // user did not select any output device.
        boolean needBluetoothAudioStart =
                bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_AVAILABLE
                        && (userSelectedAudioDevice == AudioDevice.EARPIECE
                        || userSelectedAudioDevice == AudioDevice.BLUETOOTH);

        // Need to stop Bluetooth audio if user selected different device and
        // Bluetooth SCO connection is established or in the process.
        boolean needBluetoothAudioStop =
                (bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTED
                        || bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTING)
                        && (userSelectedAudioDevice != AudioDevice.EARPIECE
                        && userSelectedAudioDevice != AudioDevice.BLUETOOTH);

        if (bluetoothManager.getState() == AppRTCBluetoothManager.State.HEADSET_AVAILABLE
                || bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTING
                || bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTED) {
            logDebug("Need Bluetooth audio. Start " + needBluetoothAudioStart + ". Stop " + needBluetoothAudioStop + ". Bluetooth state " + bluetoothManager.getState());
        }

        // Start or stop Bluetooth SCO connection given states set earlier.
        if (needBluetoothAudioStop) {
            bluetoothManager.stopScoAudio();
            bluetoothManager.updateDevice();
        }

        if (needBluetoothAudioStart && !needBluetoothAudioStop) {
            // Attempt to start Bluetooth SCO audio (takes a few second to start).
            if (!bluetoothManager.startScoAudio()) {
                // Remove BLUETOOTH from list of available devices since SCO failed.
                audioDevices.remove(AudioDevice.BLUETOOTH);
                audioDeviceSetUpdated = true;
            }
        }

        updateAudioDevice(audioDeviceSetUpdated);
        logDebug("Updated audio device state");
    }

    private void updateAudioDevice(boolean audioDeviceSetUpdated){
        // Update selected audio device.
        AudioDevice newAudioDevice;

        if (bluetoothManager.getState() == AppRTCBluetoothManager.State.SCO_CONNECTED) {
            // If a Bluetooth is connected, then it should be used as output audio
            // device. Note that it is not sufficient that a headset is available;
            // an active SCO channel must also be up and running.
            if (userSelectedAudioDevice == AudioDevice.SPEAKER_PHONE) {
                newAudioDevice = AudioDevice.SPEAKER_PHONE;
            } else {
                newAudioDevice = AudioDevice.BLUETOOTH;
            }
        } else if (hasWiredHeadset) {
            // If a wired headset is connected, but Bluetooth is not, then wired headset is used as
            // audio device.
            if (userSelectedAudioDevice == AudioDevice.SPEAKER_PHONE) {
                newAudioDevice = AudioDevice.SPEAKER_PHONE;
            } else {
                newAudioDevice = AudioDevice.WIRED_HEADSET;
            }
        } else if (userSelectedAudioDevice == AudioDevice.NONE) {
            if (typeAudioManager == AUDIO_MANAGER_CALL_RINGING) {
                newAudioDevice = AudioDevice.SPEAKER_PHONE;
            } else {
                newAudioDevice = defaultAudioDevice;
            }
        } else {
            newAudioDevice = userSelectedAudioDevice;
        }
        defaultAudioDevice = newAudioDevice;

        // Switch to new device but only if there has been any changes.
        if (newAudioDevice != selectedAudioDevice || audioDeviceSetUpdated) {
            // Do the required device switch.
            setAudioDeviceInternal(newAudioDevice);
            logDebug("New device status: available " + audioDevices + ", selected " + newAudioDevice);
        }

        logDebug("Updated audio device state");
    }

    /**
     * AudioDevice is the names of possible audio devices that we currently
     * support.
     */
    public enum AudioDevice {
        SPEAKER_PHONE, WIRED_HEADSET, EARPIECE, BLUETOOTH, NONE
    }

    /**
     * AudioManager state.
     */
    public enum AudioManagerState {
        UNINITIALIZED,
        PREINITIALIZED,
        RUNNING,
    }

    /* Receiver which handles changes in wired headset availability. */
    private class WiredHeadsetReceiver extends BroadcastReceiver {
        private static final int STATE_UNPLUGGED = 0;
        private static final int STATE_PLUGGED = 1;
        private static final int HAS_NO_MIC = 0;
        private static final int HAS_MIC = 1;

        @Override
        public void onReceive(Context context, Intent intent) {
            int state = intent.getIntExtra("state", STATE_UNPLUGGED);
            int microphone = intent.getIntExtra("microphone", HAS_NO_MIC);
            String name = intent.getStringExtra("name");
            logDebug("WiredHeadsetReceiver.onReceive" + AppRTCUtils.getThreadInfo() + ": "
                    + "a=" + intent.getAction() + ", s="
                    + (state == STATE_UNPLUGGED ? "unplugged" : "plugged") + ", m="
                    + (microphone == HAS_MIC ? "mic" : "no mic") + ", n=" + name + ", sb="
                    + isInitialStickyBroadcast());
            hasWiredHeadset = (state == STATE_PLUGGED);
            if (state == STATE_PLUGGED) {
                changeUserSelectedAudioDeviceForHeadphone(AudioDevice.WIRED_HEADSET);
            }

            updateAudioDeviceState();
        }
    }
}
