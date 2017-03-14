package mega.privacy.android.app;

import android.os.Parcel;
import android.os.Parcelable;

public class Product implements Parcelable{

	long handle;
	int level;
	int months;
	long storage;
	int amount;
	long transfer;

	public Product (long _handle, int _level, int _months, long _storage, int _amount, long _transfer){
		this.handle=_handle;
		this.level = _level;
		this.months = _months;
		this.storage = _storage;
		this.amount = _amount;
		this.transfer = _transfer;
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

	public long getStorage() {
		return storage;
	}

	public void setStorage(long storage) {
		this.storage = storage;
	}

	public int getAmount() {
		return amount;
	}

	public void setAmount(int amount) {
		this.amount = amount;
	}

	public long getTransfer() {
		return transfer;
	}

	public void setTransfer(long transfer) {
		this.transfer = transfer;
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
		out.writeLong(storage);
		out.writeInt(amount);
		out.writeLong(transfer);
	}



}
