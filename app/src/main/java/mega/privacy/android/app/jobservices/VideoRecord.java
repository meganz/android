package mega.privacy.android.app.jobservices;

public class VideoRecord {

    public static final int VIDEO_UPLOAD_SUCCESS = 1;
    public static final int VIDEO_UPLOAD_PENDING = 0;
    public static final int VIDEO_UPLOAD_FAILED = 2;

    private int id;

    private String originLocalPath;

    private String conversionLocalPath;

    private String originFingerprint;

    private String conversionFingerprint;

    private String fileName;

    private Long timestamp;

    private Long nodeHandle;

    private Boolean isSecondary = false;

    private Boolean copyOnly = false;

    private int status = VIDEO_UPLOAD_PENDING;

    public VideoRecord() {

    }

    public VideoRecord(String originLocalPath,String fileName,Long timestamp, Boolean isSecondary) {
        this.originLocalPath = originLocalPath;
        this.fileName = fileName;
        this.timestamp = timestamp;
        this.isSecondary = isSecondary;
    }

    public VideoRecord(Long handle,String name,boolean copyOnly,String originLocalPath,Long timestamp, Boolean isSecondary) {
        this.copyOnly = copyOnly;
        this.nodeHandle = handle;
        this.fileName = name;
        this.originLocalPath = originLocalPath;
        this.timestamp = timestamp;
        this.isSecondary = isSecondary;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getOriginLocalPath() {
        return originLocalPath;
    }

    public void setOriginLocalPath(String originLocalPath) {
        this.originLocalPath = originLocalPath;
    }

    public String getConversionLocalPath() {
        return conversionLocalPath;
    }

    public void setConversionLocalPath(String conversionLocalPath) {
        this.conversionLocalPath = conversionLocalPath;
    }

    public String getOriginFingerprint() {
        return originFingerprint;
    }

    public void setOriginFingerprint(String originFingerprint) {
        this.originFingerprint = originFingerprint;
    }

    public String getConversionFingerprint() {
        return conversionFingerprint;
    }

    public void setConversionFingerprint(String conversionFingerprint) {
        this.conversionFingerprint = conversionFingerprint;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public Long getNodeHandle() {
        return nodeHandle;
    }

    public void setNodeHandle(Long nodeHandle) {
        this.nodeHandle = nodeHandle;
    }

    public Boolean getSecondary() {
        return isSecondary;
    }

    public void setSecondary(Boolean secondary) {
        isSecondary = secondary;
    }

    public Boolean getCopyOnly() {
        return copyOnly;
    }

    public void setCopyOnly(Boolean copyOnly) {
        this.copyOnly = copyOnly;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "VideoRecord{" +
                "id=" + id +
                ", originLocalPath='" + originLocalPath + '\'' +
                ", conversionLocalPath='" + conversionLocalPath + '\'' +
                ", originFingerprint='" + originFingerprint + '\'' +
                ", conversionFingerprint='" + conversionFingerprint + '\'' +
                ", fileName='" + fileName + '\'' +
                ", timestamp=" + timestamp +
                ", nodeHandle=" + nodeHandle +
                ", isSecondary=" + isSecondary +
                ", copyOnly=" + copyOnly +
                ", status=" + status +
                '}';
    }
}
