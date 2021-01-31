package com.pantsu.scrollwidget.view.view;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewTreeObserver;
import android.widget.LinearLayout;
import androidx.annotation.NonNull;
import androidx.core.view.NestedScrollingChild;
import androidx.core.view.NestedScrollingParent;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.pantsu.scrollwidget.R;

public class NestedScrollRefreshLayout extends LinearLayout implements NestedScrollingParent {

  private static final long ANIMATE_TO_LOAD_DURATION = 300L;
  private static final long ANIMATE_TO_START_DURATION = 300L;

  private enum ScrollState {
    NONE,
    SHOW_TOP,
    SHOW_BOTTOM
  }

  private enum RefreshDirection {
    REFRESH_TOP,
    REFRESH_BOTTOM
  }

  public interface OnRefreshLayout {
    void onRefresh(RefreshDirection direction);
  }

  public interface OnRefreshStatusLayout {

  }

  private RecyclerView mRecyclerView;
  private View mTopView, mBottomView;

  private NestedScrollingParentHelper mNestedScrollHelper;
  private int mTopHeight, mBottomHeight;
  private int mMinScrollY, mOriginScrollY, mMaxScrollY;
  private int mTouchSlop;

  /** 滑动状态 */
  private ScrollState mScrollState = ScrollState.NONE;

  /** 过渡到Loading状态的动画 */
  private Animator mRefreshPreAnimator;

  private long mAnimateToLoadDuration = ANIMATE_TO_LOAD_DURATION;
  private long mAnimateToStartDuration = ANIMATE_TO_START_DURATION;

  public NestedScrollRefreshLayout(Context context) {
    super(context);
  }

  public NestedScrollRefreshLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

    mNestedScrollHelper = new NestedScrollingParentHelper(this);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    mRecyclerView = findViewById(R.id.recycler_view);
    mRecyclerView.addOnScrollListener(mRecyclerScrollListener);
//    mRecyclerView.setOnFlingListener(mRecyclerFlingListener);

    mTopView = getChildAt(0);
    mBottomView = getChildAt(getChildCount() - 1);

    getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
        initParam();
      }
    });
  }

  private void initParam() {
    mTopHeight = mTopView.getMeasuredHeight();
    mBottomHeight = mBottomView.getMeasuredHeight();
    mMinScrollY = 0;
    mOriginScrollY = mTopHeight;
    mMaxScrollY = mTopHeight + mBottomHeight;

    mRecyclerView.getLayoutParams().height = getHeight();
    mRecyclerView.requestLayout();
    scrollTo(mTopHeight);
  }

  private int mLastScrollState;
  private int mLastFlingVelocity;

  private RecyclerView.OnScrollListener mRecyclerScrollListener = new RecyclerView.OnScrollListener() {
    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
      super.onScrollStateChanged(recyclerView, newState);
//      Log.i("foobar", "scroll_state:" + newState);
      if (mLastScrollState == RecyclerView.SCROLL_STATE_SETTLING
          && newState == RecyclerView.SCROLL_STATE_IDLE
          && mLastFlingVelocity > 100) {
        Log.i("foobar", "mLastFlingV:" + mLastFlingVelocity);

      }
      if (newState == RecyclerView.SCROLL_STATE_IDLE) {
        mLastFlingVelocity = 0;
      }
      mLastScrollState = newState;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
      super.onScrolled(recyclerView, dx, dy);
    }
  };

  private RecyclerView.OnFlingListener mOnFlingListener = new RecyclerView.OnFlingListener() {
    @Override
    public boolean onFling(int velocityX, int velocityY) {
      return false;
    }
  };

  /** 通过target参数判断ChildView以及滚动方向，决定是否进行嵌套滚动 */
  @Override
  public boolean onStartNestedScroll(View child, View target, int nestedScrollAxes) {
    return target instanceof NestedScrollingChild;
  }

  @Override
  public void onNestedScrollAccepted(View child, View target, int nestedScrollAxes) {
    mNestedScrollHelper.onNestedScrollAccepted(child, target, nestedScrollAxes);
  }

  @Override
  public void onStopNestedScroll(View target) {
    mNestedScrollHelper.onStopNestedScroll(target);
  }

  @Override
  public int getNestedScrollAxes() {
    return mNestedScrollHelper.getNestedScrollAxes();
  }

  @Override
  public void onNestedPreScroll(View target, int deltaX, int deltaY, int[] consumed) {
    if (deltaY == 0) {
      return;
    }
    Log.i("foobar", "dy:" + deltaY + " getScrollY():" + getScrollY());
    final int scrollY = getScrollY();

    if (mScrollState == ScrollState.NONE) {
      if (scrollY < mOriginScrollY || scrollY == mOriginScrollY && deltaY < 0) {
        mScrollState = ScrollState.SHOW_TOP;
      } else if (scrollY > mOriginScrollY || scrollY == mOriginScrollY && deltaY > 0) {
        mScrollState = ScrollState.SHOW_BOTTOM;
      }
    }

    switch (mScrollState) {
      case NONE:
        break;
      case SHOW_TOP:
        handleNestedScrollTop(deltaY, consumed);
        break;
      case SHOW_BOTTOM:
        handleNestedScrollBottom(deltaY, consumed);
        break;
    }
  }

  private void handleNestedScrollTop(int deltaY, int[] consumed) {
    if (getScrollY() <= mOriginScrollY && !mRecyclerView.canScrollVertically(-1)) {//展开Top
      int targetScrollY = getScrollY() + deltaY;
      if (targetScrollY < mMinScrollY) {
        targetScrollY = mMinScrollY;
      }
      if (targetScrollY > mOriginScrollY) {
        targetScrollY = mOriginScrollY;
      }
      scrollTo(targetScrollY);
      consumed[1] = deltaY;
    }
  }

  private void handleNestedScrollBottom(int deltaY, int[] consumed) {
    if (getScrollY() >= mOriginScrollY && !mRecyclerView.canScrollVertically(1)) {
      int targetScrollY = getScrollY() + deltaY;
      if (targetScrollY > mMaxScrollY) {
        targetScrollY = mMaxScrollY;
      }
      if (targetScrollY < mOriginScrollY) {
        targetScrollY = mOriginScrollY;
      }
      scrollTo(targetScrollY);
      consumed[1] = deltaY;
    }
  }

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL: {
        final int diff = Math.abs(getScrollY() - mOriginScrollY);
        if (diff < mTouchSlop) {
          resetScrollState();
        } else {
          final RefreshDirection direction = (getScrollY() < mOriginScrollY)
              ? RefreshDirection.REFRESH_TOP : RefreshDirection.REFRESH_BOTTOM;
          animateToRefreshState(direction);
        }
        break;
      }
    }
    return super.dispatchTouchEvent(event);
  }

  private void resetScrollState() {
    mScrollState = ScrollState.NONE;
    scrollTo(mOriginScrollY);
  }

  private void animateToRefreshState(RefreshDirection direction) {
    if (mRefreshPreAnimator != null) {
      mRefreshPreAnimator.cancel();
    }
    ValueAnimator animator = ValueAnimator.ofInt(getScrollY(), getRefreshScrollY(direction));
    animator.setDuration(getAnimationDuration(direction));
    animator.addUpdateListener(animation -> {
      setScrollY((int) animation.getAnimatedValue());
    });
    mRefreshPreAnimator = animator;
    mRefreshPreAnimator.start();
  }

  private long getAnimationDuration(RefreshDirection direction) {
    final long animateDuration = (direction == RefreshDirection.REFRESH_TOP)
        ? mAnimateToStartDuration
        : mAnimateToLoadDuration;
    final long viewHeight = (direction == RefreshDirection.REFRESH_TOP)
        ? mTopHeight
        : mBottomHeight;
    return (long) (Math.abs(getScaleY() - getRefreshScrollY(direction)) / viewHeight * animateDuration);
  }

  private int getRefreshScrollY(RefreshDirection direction) {
    return (direction == RefreshDirection.REFRESH_TOP) ? mMinScrollY : mMaxScrollY;
  }

  private void scrollTo(int y) {
    scrollTo(getScrollX(), y);
  }

  private void scrollBy(int y) {
    scrollBy(getScrollX(), y);
  }


  public void setLoading(boolean loading, boolean animate) {

  }
}
