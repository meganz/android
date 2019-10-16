package mega.privacy.android.app;

import android.os.Parcel;
import android.os.Parcelable;

public class Product implements Parcelable{

	long handle;
	int level;
	int months;
	int storage;
	int amount;
	int transfer;
	boolean isBusiness;

	public Product (long _handle, int _level, int _months, int _storage, int _amount, int _transfer, boolean _isBusiness){
		this.handle=_handle;
		this.level = _level;
		this.months = _months;
		this.storage = _storage;
		this.amount = _amount;
		this.transfer = _transfer;
		this.isBusiness = _isBusiness;
	}

	public long getHandle() {
		return handle;
	}

	public void setHandle(long handle) {
		this.handle = handle;
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getMonths() {
		return months;
	}

	public void setMonths(int months) {
		this.months = months;
	}

	public int getStorage() {
		return storage;
	}

	public void setStorage(int storage) {
		this.storage = storage;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public int getTransfer() {
		return transfer;
	}

	public void setTransfer(int transfer) {
		this.transfer = transfer;
	}

	public boolean isBusiness() {
		return isBusiness;
	}

	public void setBusiness(boolean isBusiness) {
		this.isBusiness = isBusiness;
	}

	@Override
	public int describeContents() {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void writeToParcel(Parcel out, int flags) {
		// TODO Auto-generated method stub
		out.writeLong(handle);
		out.writeInt(level);
		out.writeInt(months);
		out.writeInt(storage);
		out.writeInt(amount);
		out.writeInt(transfer);
		out.writeInt(isBusiness ? 1 : 0);
	}



}
