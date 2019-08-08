package mega.privacy.android.app.jobservices;

public class SyncRecord {

    public static final int STATUS_SUCCESS = 1;
    public static final int STATUS_PENDING = 0;
    public static final int STATUS_TO_COMPRESS = 3;
    public static final int STATUS_FAILED = 2;

    public static final int TYPE_PHOTO = 1;
    public static final int TYPE_VIDEO = 2;
    public static final int TYPE_OTHER = 3;
    public static final int TYPE_ANY = -1;

    private int id;

    private String localPath;

    private String newPath;

    private String originFingerprint;

    private String newFingerprint;

    private String fileName;

    private Long timestamp;

    private Float latitude;

    private Float longitude;

    private Long nodeHandle;

    private Boolean secondary = false;

    private Boolean copyOnly = false;

    private int status = STATUS_PENDING;

    private int type;

    public SyncRecord() {

    }

    public SyncRecord(String localPath,String fileName,Long timestamp,Boolean secondary,int type) {
        this.localPath = localPath;
        this.fileName = fileName;
        this.timestamp = timestamp;
        this.secondary = secondary;
        this.type = type;
    }

    public SyncRecord(Long handle,String name,boolean copyOnly,String filePath,Long timestamp,Boolean secondary,int type) {
        this.copyOnly = copyOnly;
        this.nodeHandle = handle;
        this.fileName = name;
        this.localPath = filePath;
        this.timestamp = timestamp;
        this.secondary = secondary;
        this.type = type;
    }


    public Boolean isCopyOnly() {
        return copyOnly;
    }

    public void setCopyOnly(Boolean copyOnly) {
        this.copyOnly = copyOnly;
    }

    public Boolean isSecondary() {
        return secondary;
    }

    public void setSecondary(Boolean secondary) {
        this.secondary = secondary;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getLocalPath() {
        return localPath;
    }

    public void setLocalPath(String localPath) {
        this.localPath = localPath;
    }

    public Long getNodeHandle() {
        return nodeHandle;
    }

    public void setNodeHandle(Long nodeHandle) {
        this.nodeHandle = nodeHandle;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    public String getOriginFingerprint() {
        return originFingerprint;
    }

    public void setOriginFingerprint(String originFingerprint) {
        this.originFingerprint = originFingerprint;
    }

    public String getNewPath() {
        return newPath;
    }

    public void setNewPath(String newPath) {
        this.newPath = newPath;
    }

    public String getNewFingerprint() {
        return newFingerprint;
    }

    public void setNewFingerprint(String newFingerprint) {
        this.newFingerprint = newFingerprint;
    }

    public Float getLatitude() {
        return latitude;
    }

    public void setLatitude(Float latitude) {
        this.latitude = latitude;
    }

    public Float getLongitude() {
        return longitude;
    }

    public void setLongitude(Float longitude) {
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "SyncRecord{" +
                "id=" + id +
                ", localPath='" + localPath + '\'' +
                ", newPath='" + newPath + '\'' +
                ", originFingerprint='" + originFingerprint + '\'' +
                ", newFingerprint='" + newFingerprint + '\'' +
                ", fileName='" + fileName + '\'' +
                ", timestamp=" + timestamp +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", nodeHandle=" + nodeHandle +
                ", secondary=" + secondary +
                ", copyOnly=" + copyOnly +
                ", status=" + status +
                ", type=" + type +
                '}';
    }
}
