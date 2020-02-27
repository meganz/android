package mega.privacy.android.app.lollipop.megachat.calls;
import mega.privacy.android.app.lollipop.listeners.GroupCallListener;

public class InfoPeerGroupCall {

    private long peerId;
    private long clientId;
    private String name;
    private boolean videoOn;
    private boolean audioOn;
    private boolean greenLayer;
    private GroupCallListener listener;
    private boolean goodQuality;

    public InfoPeerGroupCall(long peerId, long clientId, String name){
        this.peerId = peerId;
        this.clientId = clientId;
        this.name = name;
        this.videoOn = false;
        this.audioOn = false;
        this.greenLayer = false;
        this.goodQuality = true;
        this.listener = null;

    }

    public InfoPeerGroupCall(long peerId, long clientId, String name, boolean videoOn, boolean audioOn, boolean greenLayer, boolean goodQuality, GroupCallListener listener) {

        this.peerId = peerId;
        this.clientId = clientId;
        this.name = name;
        this.videoOn = videoOn;
        this.audioOn = audioOn;
        this.greenLayer = greenLayer;
        this.goodQuality = goodQuality;
        this.listener = listener;
    }

    public long getPeerId() {
        return peerId;
    }

    public void setPeerId(Long peerId) {
        this.peerId = peerId;
    }

    public long getClientId() {
        return clientId;
    }

    public void setClientId(Long clientId) {
        this.clientId = clientId;
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

    public boolean isGoodQuality() {
        return goodQuality;
    }

    public void setGoodQuality(boolean goodQuality) {
        this.goodQuality = goodQuality;
    }

    public boolean hasGreenLayer() {
        return greenLayer;
    }

    public void setGreenLayer(boolean greenLayer) {
        this.greenLayer = greenLayer;
    }

    public GroupCallListener getListener() {
        return listener;
    }

    public void setListener(GroupCallListener listener) {
        this.listener = listener;
    }

}
