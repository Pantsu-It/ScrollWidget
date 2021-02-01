package com.pantsu.scrollwidget.view.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class ChatLoadStatus extends FrameLayout implements NestedScrollLoadingLayout.LoadStatus {

  private View mProgressBar;

  public ChatLoadStatus(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    mProgressBar = getChildAt(0);
    mProgressBar.setVisibility(INVISIBLE);
  }

  @Override
  public void loading() {
    mProgressBar.setVisibility(VISIBLE);
  }

  @Override
  public void reset() {
    mProgressBar.setVisibility(INVISIBLE);
  }
}
