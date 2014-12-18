package nz.mega.android;

import java.util.List;

import nz.mega.android.FileStorageActivity.FileDocument;
import nz.mega.android.FileStorageActivity.Mode;
import nz.mega.android.utils.Util;

import android.content.Context;
import android.text.format.DateUtils;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;


/*
 * Adapter for FilestorageActivity list
 */
public class FileStorageAdapter extends BaseAdapter implements OnClickListener {

	// Listener for item check
	public interface OnItemCheckClickListener {
		public void onItemCheckClick(int position);
	}
		
	private Context mContext;
	private List<FileDocument> currentFiles;
	private Mode mode;
	private OnItemCheckClickListener checkClickListener;
	
	public FileStorageAdapter(Context context, Mode mode) {
		setContext(context);
		this.mode = mode;
	}
	
	public void setContext(Context context) {
		mContext = context;
	}
	
	public void setOnItemCheckClickListener(OnItemCheckClickListener listener) {
		this.checkClickListener = listener;
	}
	
	// Set new files on folder change
	public void setFiles(List<FileDocument> newFiles) {
		currentFiles = newFiles;
		notifyDataSetChanged();
	}
	
	public FileDocument getDocumentAt(int position) {
		return currentFiles.get(position);
	}
	
	@Override
	public int getCount() {
		if (currentFiles == null) {
			return 0;
		}
		int size = currentFiles.size();
		return size == 0 ? 1 : size;
	}

	@Override
	public Object getItem(int arg0) {
		return null;
	}

	@Override
	public long getItemId(int arg0) {
		return 0;
	}
	
	@Override
	public int getItemViewType(int arg0) {
		return 0;
	}
	
	@Override
	public boolean isEnabled(int position) {
		if (currentFiles.size() == 0) {
			return false;
		}
		FileDocument document = currentFiles.get(position);
		if (mode == Mode.PICK_FOLDER && !document.isFolder()) {
			return false;
		}
		if (document.getFile().canRead() == false) {
			return false;
		}

		return true;
	}

	@Override
	public View getView(final int position, View rowView, ViewGroup parentView) {
		boolean isCheckable = mode == Mode.PICK_FILE;
		LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		if (position == 0 && currentFiles.size() == 0) {
			TextView textView = (TextView) inflater.inflate(R.layout.file_list_empty, parentView, false);
			int resId = R.string.file_browser_empty_folder;
			textView.setText(mContext.getString(resId));
			return textView;
		}
		FileDocument document = currentFiles.get(position);
		
		int layoutId;
		
		if (document.isFolder()) {
			if (isCheckable) {
				layoutId = R.layout.file_list_item_checkable;
			} else {
				layoutId = R.layout.file_list_item;
			}
		} else {
			if (isCheckable) {
				layoutId = R.layout.file_list_item_file_checkable;
			} else {
				layoutId = R.layout.file_list_item_file;
			}
		}
		
		rowView = inflater.inflate(layoutId, parentView, false);
		TextView textView = (TextView) rowView.findViewById(R.id.file_text);
		ImageView imageView = (ImageView) rowView.findViewById(R.id.file_image);
		
		if (isCheckable) {
			View checkArea = rowView.findViewById(R.id.checkbox);
			checkArea.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					checkClickListener.onItemCheckClick(position);
				}
			});
		}
		
		int imageResId = 0;
		String text = document.getName();
		if (!document.isFolder()) {
			imageResId = document.getMimeType().getIconResourceId();
			String descriptionText = Formatter.formatFileSize(mContext,document.getSize());
			TextView fileDetails = (TextView) rowView.findViewById(R.id.file_description);
			fileDetails.setText(descriptionText);
			TextView fileDate = (TextView) rowView.findViewById(R.id.file_date);
			try {fileDate.setText(DateUtils.getRelativeTimeSpanString(document.getTimestampInMillis()));} 
			catch(Exception ex)	{fileDate.setText("");}
			if (mode == Mode.PICK_FOLDER || document.getFile().canRead() == false) {
				Util.setViewAlpha(imageView, .4f);
				textView.setTextColor(mContext.getResources().getColor(R.color.text_secondary));
			}
		}
		else if (document.isFolder()) {
			if (document.getFile().canRead() == false) {
				Util.setViewAlpha(imageView, .4f);
				textView.setTextColor(mContext.getResources().getColor(R.color.text_secondary));
			}
			imageResId = R.drawable.ic_folder_list;
		}
		
		imageView.setImageDrawable(mContext.getResources().getDrawable(imageResId));
		textView.setText(text);
		
		return rowView;
	}
	
	@Override
	public void onClick(View v) {
		log("click!");
	}	

	private static void log(String message) {
		Util.log("FileStorageAdapter", message);
	}
}
