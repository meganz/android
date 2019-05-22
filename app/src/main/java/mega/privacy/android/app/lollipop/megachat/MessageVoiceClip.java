package mega.privacy.android.app.lollipop.megachat;

import android.media.MediaPlayer;

import mega.privacy.android.app.utils.Constants;

public class MessageVoiceClip {

    long idMessage;
    long userHandle;
    long messageHandle;
    int progress = 0;
    boolean isPaused = false;
    MediaPlayer mediaPlayer = null;
    int isAvailable = 0;

    public MessageVoiceClip(long idMessage, long userHandle, long messageHandle) {
        this.idMessage = idMessage;
        this.userHandle = userHandle;
        this.messageHandle = messageHandle;
    }

    public long getMessageHandle() {
        return messageHandle;
    }

    public void setMessageHandle(long messageHandle) {
        this.messageHandle = messageHandle;
    }

    public long getIdMessage() {
        return idMessage;
    }

    public void setIdMessage(long idMessage) {
        this.idMessage = idMessage;
    }

    public long getUserHandle() {
        return userHandle;
    }

    public void setUserHandle(long userHandle) {
        this.userHandle = userHandle;
    }

    public MediaPlayer getMediaPlayer() {
        if(this.mediaPlayer == null){
            this.mediaPlayer =  new MediaPlayer();
            this.progress = 0;
        }
        return mediaPlayer;
    }
    public void setMediaPlayer(MediaPlayer mediaPlayer) {
        this.mediaPlayer = mediaPlayer;
    }

    public int getProgress() {
        return progress;
    }

    public void setProgress(int progress) {
        this.progress = progress;
    }

    public boolean isPaused() {
        return isPaused;
    }

    public void setPaused(boolean paused) {
        isPaused = paused;
    }
    public int getIsAvailable() {
        return isAvailable;
    }

    public void setIsAvailable(int isAvailable) {
        this.isAvailable = isAvailable;
    }


}
