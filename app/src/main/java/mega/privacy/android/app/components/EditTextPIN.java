package mega.privacy.android.app.components;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;
import android.widget.EditText;

public class EditTextPIN extends EditText {

	EditText et = null;

    public EditTextPIN(Context context, AttributeSet attrs,
					   int defStyle) {
        super(context, attrs, defStyle);

    }

    public EditTextPIN(Context context, AttributeSet attrs) {
        super(context, attrs);

    }

	public void setEt(EditText et){
		this.et = et;
	}

    public EditTextPIN(Context context) {
        super(context);
    }

	private class PinInputConnection extends InputConnectionWrapper{

		public PinInputConnection(InputConnection target, boolean mutable) {
			super(target, mutable);
		}

		@Override
		public boolean sendKeyEvent(KeyEvent event) {
			if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
				Log.d("EditTextPIN", "KEYCODE_DEL");
				if (getText() != null){
					if (getText().toString().compareTo("") != 0){
						setText("");
					}
					else{
						if (et != null){
							et.requestFocus();
							et.setCursorVisible(true);
							et.setText("");
						}
					}
				}
				return true;
			}

			return super.sendKeyEvent(event);
		}
	}

	@Override
	public InputConnection onCreateInputConnection(EditorInfo outAttrs) {
		Log.d("EditTextPIN", "onCreateInputConnection()");
		return new PinInputConnection(super.onCreateInputConnection(outAttrs), true);
	}


}