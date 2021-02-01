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

public class NestedScrollRefreshLayout extends LinearLayout implements NestedScrollingParent2 {

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

  public interface OnRefreshListener {
    void onRefresh(Direction direction);
  }

  private RecyclerView mTargetView;
  private View mTopRefreshView, mBottomRefreshView;

  private NestedScrollingParentHelper mNestedScrollHelper;
  private int mTopViewHeight, mBottomViewHeight;
  private int mStartPosition;
  private int mTopPosition, mBottomPosition;

  private int mTouchSlop;

  /** 滑动状态 */
  private ScrollState mScrollState = ScrollState.NONE;

  /** 过渡到Loading状态的动画 */
  private Animator mRefreshAnimator;

  private long mAnimateToRefreshDuration = ANIMATE_TO_START_DURATION;

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
    mTargetView = findViewById(R.id.recycler_view);
    mTargetView.addOnScrollListener(mOnScrollListener);

    mTopRefreshView = getChildAt(0);
    mBottomRefreshView = getChildAt(getChildCount() - 1);

    getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
        initParam();
      }
    });
  }

  private void initParam() {
    mTopViewHeight = mTopRefreshView.getMeasuredHeight();
    mBottomViewHeight = mBottomRefreshView.getMeasuredHeight();
    mTopPosition = 0;
    mStartPosition = mTopViewHeight;
    mBottomPosition = mTopViewHeight + mBottomViewHeight;

    mTargetView.getLayoutParams().height = getHeight();
    mTargetView.requestLayout();
    scrollTo(mTopViewHeight);
  }

  private boolean mIsRefreshing;
  private Direction mRefreshingDirection;

  public boolean isRefreshing() {
    return mIsRefreshing;
  }

  private boolean mShowTopRefreshView = true;
  private boolean mShowBottomRefreshView = true;

  public void setShowTopRefreshView(boolean showTopRefreshView) {
    mShowTopRefreshView = showTopRefreshView;
  }

  public void setShowBottomRefreshView(boolean showBottomRefreshView) {
    mShowBottomRefreshView = showBottomRefreshView;
  }

  public int getTargetViewOffset() {
    return Math.abs(getPosition() - mStartPosition);
  }

  public void startRefreshing(Direction direction) {
    if (mIsRefreshing && mRefreshingDirection == direction) {
      return;
    }
    if (mRefreshAnimator != null && mRefreshAnimator.isStarted()) {
      mRefreshAnimator.cancel();
    }
    mIsRefreshing = true;
    mRefreshingDirection = direction;
    animateToRefreshingPosition(direction);
  }

  public void stopRefreshing(boolean animation) {
    if (!mIsRefreshing && mRefreshAnimator == null) {
      return;
    }
    if (mRefreshAnimator != null && mRefreshAnimator.isStarted()) {
      mRefreshAnimator.cancel();
      mRefreshAnimator = null;
    }
    mIsRefreshing = false;
    mRefreshingDirection = null;
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
    if (!mShowTopRefreshView) {
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
    if (!mShowBottomRefreshView) {
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
          final Direction direction = getTargetRefreshDirection();
          mIsRefreshing = true;
          mRefreshingDirection = direction;
          animateToRefreshingPosition(direction);
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
    if (mIsRefreshing) {
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

  private void animateToRefreshingPosition(Direction direction) {
    if (mRefreshAnimator != null) {
      mRefreshAnimator.cancel();
    }
    ValueAnimator animator = ValueAnimator.ofInt(getPosition(), getRefreshPosition(direction));
    animator.setDuration(getAnimationDuration(direction, getRefreshPosition(direction)));
    animator.addUpdateListener(animation -> setPosition((int) animation.getAnimatedValue()));
    animator.addListener(new AnimatorListenerAdapter() {
      @Override
      public void onAnimationEnd(Animator animation) {
        mRefreshAnimator = null;
        notifyRefreshEvent();
      }
    });
    mRefreshAnimator = animator;
    mRefreshAnimator.start();
  }

  private List<OnRefreshListener> mOnRefreshListeners = new ArrayList<>();

  public void addOnRefreshListener(OnRefreshListener onRefreshListener) {
    mOnRefreshListeners.add(onRefreshListener);
  }

  public void removeOnRefreshListener(OnRefreshListener onRefreshListener) {
    mOnRefreshListeners.remove(onRefreshListener);
  }

  /** RefreshView完全弹出时，通知业务方加载数据 */
  private void notifyRefreshEvent() {
    for (OnRefreshListener onRefreshListener : mOnRefreshListeners) {
      onRefreshListener.onRefresh(mRefreshingDirection);
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
          mRefreshAnimator = null;
        }
      });
      mRefreshAnimator = animator;
      mRefreshAnimator.start();
    }
  }

  private long getAnimationDuration(@Nullable Direction direction, int targetPosition) {
    if (direction == null) {
      direction = getTargetRefreshDirection();
    }
    final long viewHeight = (direction == Direction.TOP) ? mTopViewHeight : mBottomViewHeight;
    return mAnimateToRefreshDuration * Math.abs(getPosition() - targetPosition) / viewHeight;
  }

  private int getRefreshPosition(Direction direction) {
    return (direction == Direction.TOP) ? mTopPosition : mBottomPosition;
  }

  private Direction getTargetRefreshDirection() {
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
