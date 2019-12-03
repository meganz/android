package mega.privacy.android.app;

import android.os.Parcel;
import android.os.Parcelable;

import static mega.privacy.android.app.utils.LogUtil.*;

public class MegaOffline implements Parcelable {

	public static final String FOLDER = "1";
	public static final String FILE = "0";

	public static final int INCOMING = 1;
	public static final int INBOX = 2;
	public static final int OTHER = 0;

	private int id = -1;
	private String handle = "";
	private String path = "";
	private String name = "";
	private int parentId = -1;
	private String type = "";
	private int origin = OTHER;
	private String handleIncoming = "";

	public MegaOffline(String handle, String path, String name, int parentId, String type, int origin, String handleIncoming) {
		this.handle = handle;
		this.path = path;
		this.name = name;
		this.parentId = parentId;
		this.type = type;
		this.origin = origin;
		this.handleIncoming = handleIncoming;
	}

	public MegaOffline(int id, String handle, String path, String name, int parentId, String type, int origin, String handleIncoming) {
		this.id=id;
		this.handle = handle;
		this.path = path;
		this.name = name;
		this.parentId = parentId;
		this.type = type;
		this.origin = origin;
		this.handleIncoming = handleIncoming;
	}

	protected MegaOffline(Parcel in) {
		id = in.readInt();
		handle = in.readString();
		path = in.readString();
		name = in.readString();
		parentId = in.readInt();
		type = in.readString();
		origin = in.readInt();
		handleIncoming = in.readString();
	}

	public String getHandle() {
		return handle;
	}

	public void setHandle(String handle) {
		this.handle = handle;
	}

	public String getPath() {
		return path;
	}

	public void setPath(String path) {
		this.path = path;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public int getParentId() {
		return parentId;
	}

	public void setParentId(int parentId) {
		this.parentId = parentId;
	}

	public String getHandleIncoming() {
		return handleIncoming;
	}

	public void setHandleIncoming(String handleIncoming) {
		this.handleIncoming = handleIncoming;
	}

	public boolean isFolder(){
		if (type != null){
			if(type.equals(FOLDER)){
				return true;
			}
		}
		else{
			logDebug("isFolder type is NULL");
		}
		return false;
	}

	public int getOrigin() {
		return origin;
	}

	public void setOrigin(int origin) {
		this.origin = origin;
	}

	public static final Creator<MegaOffline> CREATOR = new Creator<MegaOffline>() {
		@Override
		public MegaOffline createFromParcel(Parcel in) {
			return new MegaOffline(in);
		}

		@Override
		public MegaOffline[] newArray(int size) {
			return new MegaOffline[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(id);
		dest.writeString(handle);
		dest.writeString(path);
		dest.writeString(name);
		dest.writeInt(parentId);
		dest.writeString(type);
		dest.writeInt(origin);
		dest.writeString(handleIncoming);
	}
}