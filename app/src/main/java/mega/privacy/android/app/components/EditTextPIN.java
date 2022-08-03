package mega.privacy.android.app.components;

import android.content.Context;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputConnectionWrapper;

import androidx.annotation.NonNull;
import androidx.appcompat.widget.AppCompatEditText;

import timber.log.Timber;

public class EditTextPIN extends AppCompatEditText {

    AppCompatEditText et = null;

    public EditTextPIN(Context context, AttributeSet attrs,
                       int defStyle) {
        super(context, attrs, defStyle);
    }

    public EditTextPIN(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public EditTextPIN(Context context) {
        super(context);
    }

    public void setEt(AppCompatEditText et) {
        this.et = et;
    }

    @Override
    public InputConnection onCreateInputConnection(@NonNull EditorInfo outAttrs) {
        Timber.tag("EditTextPIN").d("onCreateInputConnection()");
        return new PinInputConnection(super.onCreateInputConnection(outAttrs), true);
    }

    private class PinInputConnection extends InputConnectionWrapper {

        public PinInputConnection(InputConnection target, boolean mutable) {
            super(target, mutable);
        }

        @Override
        public boolean sendKeyEvent(KeyEvent event) {
            if (event.getAction() == KeyEvent.ACTION_DOWN && event.getKeyCode() == KeyEvent.KEYCODE_DEL) {
                Timber.tag("EditTextPIN").d("KEYCODE_DEL");
                if (getText() != null) {
                    if (getText().toString().compareTo("") != 0) {
                        setText("");
                    } else {
                        if (et != null) {
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
}