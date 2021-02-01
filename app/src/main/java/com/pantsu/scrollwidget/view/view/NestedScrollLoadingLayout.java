package com.pantsu.scrollwidget.view.view;

import java.util.ArrayList;
import java.util.List;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
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
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild2;
import androidx.core.view.NestedScrollingParent2;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pantsu.scrollwidget.R;

public class NestedScrollLoadingLayout extends LinearLayout implements NestedScrollingParent2 {

  private static final long ANIMATE_TO_START_DURATION = 300L;

  private enum ScrollState {
    NONE,
    SHOW_TOP,
    SHOW_BOTTOM
  }

  public enum Direction {
    TOP,
    BOTTOM
  }

  public interface OnLoadListener {
    void onLoad(Direction direction);
  }

  private RecyclerView mTargetView;
  private View mTopLoadView, mBottomLoadView;

  private NestedScrollingParentHelper mNestedScrollHelper;
  private int mTopViewHeight, mBottomViewHeight;
  private int mStartPosition;
  private int mTopPosition, mBottomPosition;

  private int mTouchSlop;

  /** 滑动状态 */
  private ScrollState mScrollState = ScrollState.NONE;

  /** 过渡到Loading状态的动画 */
  private Animator mLoadAnimator;

  private long mAnimateToLoadDuration = ANIMATE_TO_START_DURATION;

  public NestedScrollLoadingLayout(Context context) {
    super(context);
  }

  public NestedScrollLoadingLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    mTouchSlop = ViewConfiguration.get(context).getScaledTouchSlop();

    mNestedScrollHelper = new NestedScrollingParentHelper(this);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    mTargetView = findViewById(R.id.recycler_view);
    mTargetView.addOnScrollListener(mOnScrollListener);

    mTopLoadView = getChildAt(0);
    mBottomLoadView = getChildAt(getChildCount() - 1);

    getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
        initParam();
      }
    });
  }

  private void initParam() {
    mTopViewHeight = mTopLoadView.getMeasuredHeight();
    mBottomViewHeight = mBottomLoadView.getMeasuredHeight();
    mTopPosition = 0;
    mStartPosition = mTopViewHeight;
    mBottomPosition = mTopViewHeight + mBottomViewHeight;

    mTargetView.getLayoutParams().height = getHeight();
    mTargetView.requestLayout();
    scrollTo(mTopViewHeight);
  }

  private boolean mIsLoading;
  private Direction mLoadingDirection;

  public boolean isLoading() {
    return mIsLoading;
  }

  private boolean mShowTopLoadView = true;
  private boolean mShowBottomLoadView = true;

  public void setShowTopLoadingView(boolean showTopLoadView) {
    mShowTopLoadView = showTopLoadView;
  }

  public void setShowBottomLoadingView(boolean showBottomLoadView) {
    mShowBottomLoadView = showBottomLoadView;
  }

  public int getTargetViewOffset() {
    return Math.abs(getPosition() - mStartPosition);
  }

  public void startLoading(Direction direction) {
    if (mIsLoading && mLoadingDirection == direction) {
      return;
    }
    if (mLoadAnimator != null && mLoadAnimator.isStarted()) {
      mLoadAnimator.cancel();
    }
    mIsLoading = true;
    mLoadingDirection = direction;
    animateToLoadingPosition(direction);
  }

  public void stopLoading(boolean animation) {
    if (!mIsLoading && mLoadAnimator == null) {
      return;
    }
    if (mLoadAnimator != null && mLoadAnimator.isStarted()) {
      mLoadAnimator.cancel();
      mLoadAnimator = null;
    }
    mIsLoading = false;
    mLoadingDirection = null;
    resetToStartPosition(animation);
  }

  /** 通过target参数判断ChildView以及滚动方向，决定是否进行嵌套滚动 */
  @Override
  public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
    if (!isEnabled() || !(target instanceof NestedScrollingChild2)) {
      return false;
    }
    /** FIX：滑动速度较大时，FLING状态一直被 NestedScrollParent 假消费，导致直到滑动停止的耗时长达数秒 */
    if (type == ViewCompat.TYPE_NON_TOUCH
        && (getPosition() < mStartPosition && !canScrollVertically(-1)
        || getPosition() > mStartPosition && !canScrollVertically(1))) {
      return false;
    }
    return true;
  }

  @Override
  public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes, int type) {
    mNestedScrollHelper.onNestedScrollAccepted(child, target, axes);
  }

  @Override
  public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed, int dxUnconsumed, int dyUnconsumed, int type) {
    onNestedScroll(target, dxConsumed, dyConsumed, dxUnconsumed, dyUnconsumed);
  }

  @Override
  public void onStopNestedScroll(@NonNull View target, int type) {
    mNestedScrollHelper.onStopNestedScroll(target);
  }

  @Override
  public int getNestedScrollAxes() {
    return mNestedScrollHelper.getNestedScrollAxes();
  }

  @Override
  public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed, int type) {
    if (dy == 0) {
      return;
    }
    Log.i("touch-me", "onNestedPreScroll");
    switch (mScrollState) {
      case NONE:
        break;
      case SHOW_TOP:
        handleNestedScrollTop(dy, consumed);
        break;
      case SHOW_BOTTOM:
        handleNestedScrollBottom(dy, consumed);
        break;
    }
  }

  private void handleNestedScrollTop(int deltaY, int[] consumed) {
    if (!mShowTopLoadView) {
      return;
    }
    if (getPosition() <= mStartPosition && !mTargetView.canScrollVertically(-1)) {//展开Top
      int targetPosition = getPosition() + deltaY;
      if (targetPosition < mTopPosition) {
        targetPosition = mTopPosition;
      }
      if (targetPosition > mStartPosition) {
        targetPosition = mStartPosition;
      }
      scrollTo(targetPosition);
      consumed[1] = deltaY;
    }
  }

  private void handleNestedScrollBottom(int deltaY, int[] consumed) {
    if (!mShowBottomLoadView) {
      return;
    }
    if (getPosition() >= mStartPosition && !mTargetView.canScrollVertically(1)) {
      int targetPosition = getPosition() + deltaY;
      if (targetPosition > mBottomPosition) {
        targetPosition = mBottomPosition;
      }
      if (targetPosition < mStartPosition) {
        targetPosition = mStartPosition;
      }
      scrollTo(targetPosition);
      consumed[1] = deltaY;
    }
  }

  private RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
      if (newState == RecyclerView.SCROLL_STATE_IDLE) {
        final int diff = Math.abs(getPosition() - mStartPosition);
        if (diff < mTouchSlop) {
          resetToStartPosition(false);
        } else {
          final Direction direction = getTargetLoadDirection();
          mIsLoading = true;
          mLoadingDirection = direction;
          animateToLoadingPosition(direction);
        }
      }
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
      super.onScrolled(recyclerView, dx, dy);
    }
  };

  private float mLastTouchY;

  @Override
  public boolean dispatchTouchEvent(MotionEvent event) {
    if (mIsLoading) {
      return true;
    }
    switch (event.getActionMasked()) {
      case MotionEvent.ACTION_DOWN:
        mLastTouchY = event.getY();
        break;
      case MotionEvent.ACTION_MOVE:
        final float deltaY = event.getY() - mLastTouchY;
        if (mScrollState == ScrollState.NONE) {
          if (deltaY > 0 && !mTargetView.canScrollVertically(-1)) {
            mScrollState = ScrollState.SHOW_TOP;
          } else if (deltaY < 0 && !mTargetView.canScrollVertically(1)) {
            mScrollState = ScrollState.SHOW_BOTTOM;
          }
        }
        mLastTouchY = event.getY();
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL: {
//        mScrollState = ScrollState.NONE;// FIXME: 2021/2/1
        break;
      }
    }
    return super.dispatchTouchEvent(event);
  }

  private void animateToLoadingPosition(Direction direction) {
    if (mLoadAnimator != null) {
      mLoadAnimator.cancel();
    }
    ValueAnimator animator = ValueAnimator.ofInt(getPosition(), getLoadPosition(direction));
    animator.setDuration(getAnimationDuration(direction, getLoadPosition(direction)));
    animator.addUpdateListener(animation -> setPosition((int) animation.getAnimatedValue()));
    animator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        mLoadAnimator = null;
        notifyLoadEvent();
      }
    });
    mLoadAnimator = animator;
    mLoadAnimator.start();
  }

  private List<OnLoadListener> mOnLoadListeners = new ArrayList<>();

  public void addOnLoadListener(OnLoadListener onLoadListener) {
    mOnLoadListeners.add(onLoadListener);
  }

  public void removeOnLoadListener(OnLoadListener onLoadListener) {
    mOnLoadListeners.remove(onLoadListener);
  }

  /** LoadView完全弹出时，通知业务方加载数据 */
  private void notifyLoadEvent() {
    for (OnLoadListener onLoadListener : mOnLoadListeners) {
      onLoadListener.onLoad(mLoadingDirection);
    }
  }

  private void resetToStartPosition(boolean animation) {
    if (!animation) {
      scrollTo(mStartPosition);
    } else {
      ValueAnimator animator = ValueAnimator.ofInt(getPosition(), mStartPosition);
      animator.setDuration(getAnimationDuration(null, mStartPosition));
      animator.addUpdateListener(anim -> setPosition((int) anim.getAnimatedValue()));
      animator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          mLoadAnimator = null;
        }
      });
      mLoadAnimator = animator;
      mLoadAnimator.start();
    }
  }

  private long getAnimationDuration(@Nullable Direction direction, int targetPosition) {
    if (direction == null) {
      direction = getTargetLoadDirection();
    }
    final long viewHeight = (direction == Direction.TOP) ? mTopViewHeight : mBottomViewHeight;
    return mAnimateToLoadDuration * Math.abs(getPosition() - targetPosition) / viewHeight;
  }

  private int getLoadPosition(Direction direction) {
    return (direction == Direction.TOP) ? mTopPosition : mBottomPosition;
  }

  private Direction getTargetLoadDirection() {
    return (getPosition() < mStartPosition) ? Direction.TOP : Direction.BOTTOM;
  }

  private void scrollTo(int position) {
    scrollTo(getScrollX(), position);
  }

  private void scrollBy(int position) {
    scrollBy(getScrollX(), position);
  }

  private int getPosition() {
    return getScrollY();
  }

  private void setPosition(int position) {
    setScrollY(position);
  }

  /** 实现此方法，用于正确计算 {@link #canScrollVertically(int)} */
  @Override
  protected int computeVerticalScrollRange() {
    return getHeight() + mTopViewHeight + mBottomViewHeight;
  }
}
