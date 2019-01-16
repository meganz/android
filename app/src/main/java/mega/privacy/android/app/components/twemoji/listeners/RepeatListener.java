package mega.privacy.android.app.components.twemoji.listeners;

import android.annotation.SuppressLint;
import android.os.Handler;
import android.os.SystemClock;
import android.view.MotionEvent;
import android.view.View;

public final class RepeatListener implements View.OnTouchListener {
  final long normalInterval;
  final View.OnClickListener clickListener;

  final Handler handler = new Handler();
  private final long initialInterval;
  View downView;

  private final Runnable handlerRunnable = new Runnable() {
    @Override public void run() {
      if (downView != null) {
        handler.removeCallbacksAndMessages(downView);
        handler.postAtTime(this, downView, SystemClock.uptimeMillis() + normalInterval);
        clickListener.onClick(downView);
      }
    }
  };

  public RepeatListener(final long initialInterval, final long normalInterval,
      final View.OnClickListener clickListener) {
    if (clickListener == null) {
      throw new IllegalArgumentException("null runnable");
    }

    if (initialInterval < 0 || normalInterval < 0) {
      throw new IllegalArgumentException("negative interval");
    }

    this.initialInterval = initialInterval;
    this.normalInterval = normalInterval;
    this.clickListener = clickListener;
  }

  @Override @SuppressLint("ClickableViewAccessibility") public boolean onTouch(final View view, final MotionEvent motionEvent) {
    switch (motionEvent.getAction()) {
      case MotionEvent.ACTION_DOWN:
        handler.removeCallbacks(handlerRunnable);
        handler.postAtTime(handlerRunnable, downView, SystemClock.uptimeMillis() + initialInterval);
        downView = view;
        downView.setPressed(true);
        clickListener.onClick(view);
        return true;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL:
      case MotionEvent.ACTION_OUTSIDE:
        handler.removeCallbacksAndMessages(downView);
        downView.setPressed(false);
        downView = null;
        return true;
      default:
        break;
    }

    return false;
  }
}
