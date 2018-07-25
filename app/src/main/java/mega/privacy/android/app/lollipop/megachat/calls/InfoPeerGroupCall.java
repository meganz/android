package mega.privacy.android.app.lollipop.megachat.calls;


import mega.privacy.android.app.lollipop.listeners.GroupCallListener;

public class InfoPeerGroupCall {
    Long handle;
    String name;
    boolean videoOn;
    boolean audioOn;
    GroupCallListener listener;

    public InfoPeerGroupCall(Long handle, String name, boolean videoOn, boolean audioOn, GroupCallListener listener) {
        this.handle = handle;
        this.name = name;
        this.videoOn = videoOn;
        this.audioOn = audioOn;
        this.listener = listener;
    }

    public Long getHandle() {
        return handle;
    }

    public void setHandle(Long handle) {
        this.handle = handle;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isVideoOn() {
        return videoOn;
    }

    public void setVideoOn(boolean videoOn) {
        this.videoOn = videoOn;
    }

    public boolean isAudioOn() {
        return audioOn;
    }

    public void setAudioOn(boolean audioOn) {
        this.audioOn = audioOn;
    }

    public GroupCallListener getListener() {
        return listener;
    }

    public void setListener(GroupCallListener listener) {
        this.listener = listener;
    }


}
