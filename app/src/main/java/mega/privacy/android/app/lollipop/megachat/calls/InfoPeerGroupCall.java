package mega.privacy.android.app.lollipop.megachat.calls;


import android.view.SurfaceView;

import mega.privacy.android.app.lollipop.listeners.GroupCallListener;

public class InfoPeerGroupCall {
    Long handle;
    String name;
    boolean videoOn;
    boolean audioOn;
    GroupCallListener listener;
    SurfaceView surfaceview;

    public InfoPeerGroupCall(Long handle, String name, boolean videoOn, boolean audioOn, GroupCallListener listener, SurfaceView surfaceview) {
        this.handle = handle;
        this.name = name;
        this.videoOn = videoOn;
        this.audioOn = audioOn;
        this.listener = listener;
        this.surfaceview = surfaceview;
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

    public SurfaceView getSurfaceview() {
        return surfaceview;
    }

    public void setSurfaceview(SurfaceView surfaceview) {
        this.surfaceview = surfaceview;
    }

}
