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
import androidx.core.view.NestedScrollingChild2;
import androidx.core.view.NestedScrollingParent2;
import androidx.core.view.NestedScrollingParentHelper;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.pantsu.scrollwidget.R;

public class NestedScrollLoadingLayout extends LinearLayout implements NestedScrollingParent2 {

  private static final long ANIMATE_TO_START_DURATION = 300L;

  private long mAnimateToLoadDuration = ANIMATE_TO_START_DURATION;

  private RecyclerView mTargetView;
  private View mTopLoadingView;
  private View mBottomLoadingView;
  private boolean mShowTopLoadingView = true;
  private boolean mShowBottomLoadingView = true;

  private int mTopViewHeight;
  private int mBottomViewHeight;
  private int mStartPosition;
  private int mTopPosition;
  private int mBottomPosition;

  private NestedScrollingParentHelper mNestedScrollHelper;
  private int mTouchSlop;
  private float mLastTouchY;
  private ScrollState mScrollState = ScrollState.NONE;

  private boolean mIsLoading;
  private Direction mLoadingDirection;
  private Animator mAnimator;

  private int mLastScrollState;
  private int mLastScrollDeltaY;

  private RecyclerView.OnScrollListener mOnScrollListener = new RecyclerView.OnScrollListener() {
    @Override
    public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
      if (newState == RecyclerView.SCROLL_STATE_IDLE) {
        // 场景1：通过 FLING 滑动到列表边界，需要弹出 LoadingView
        if (mLastScrollState == RecyclerView.SCROLL_STATE_SETTLING) {
          if (mLastScrollDeltaY < 0 && !recyclerView.canScrollVertically(-1)) {
            startLoading(Direction.TOP, true);
          } else if (mLastScrollDeltaY > 0 && !recyclerView.canScrollVertically(1)) {
            startLoading(Direction.BOTTOM, true);
          }
        }
        // 场景2：通过 DRAG 滑动到列表边界，并且已经显示 LoadingView，需要弹出 LoadingView
        int diff = Math.abs(getPosition() - mStartPosition);
        if (diff < mTouchSlop) {
          resetToStartPosition(false);
        } else {
          Direction direction = getTargetLoadDirection();
          startLoading(direction, true);
        }
      }

      mLastScrollState = newState;
    }

    @Override
    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
      mLastScrollDeltaY = dy;
    }
  };


  public NestedScrollLoadingLayout(Context context) {
    this(context, null);
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

    mTopLoadingView = getChildAt(0);
    mBottomLoadingView = getChildAt(getChildCount() - 1);

    getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
      @Override
      public void onGlobalLayout() {
        getViewTreeObserver().removeOnGlobalLayoutListener(this);
        initParam();
      }
    });
  }

  private void initParam() {
    mTopViewHeight = mTopLoadingView.getMeasuredHeight();
    mBottomViewHeight = mBottomLoadingView.getMeasuredHeight();
    mTopPosition = 0;
    mStartPosition = mTopViewHeight;
    mBottomPosition = mTopViewHeight + mBottomViewHeight;

    mTargetView.getLayoutParams().height = getHeight();
    mTargetView.requestLayout();
    scrollTo(mStartPosition);
  }

  public boolean isLoading() {
    return mIsLoading;
  }

  public void setShowTopLoadingView(boolean showTopLoadView) {
    mShowTopLoadingView = showTopLoadView;
  }

  public void setShowBottomLoadingView(boolean showBottomLoadView) {
    mShowBottomLoadingView = showBottomLoadView;
  }

  public int getTargetViewOffset() {
    return Math.abs(getPosition() - mStartPosition);
  }

  public void startLoading(@NonNull Direction direction) {
    if (mIsLoading && mLoadingDirection == direction) {
      return;
    }
    if (mAnimator != null && mAnimator.isStarted()) {
      mAnimator.cancel();
    }
    startLoading(direction, true);
  }

  private void startLoading(@NonNull Direction direction, boolean animation) {
    mIsLoading = true;
    mLoadingDirection = direction;

    setLoadingStatus(direction);
    moveToLoadingPosition(direction, animation);
  }

  public void stopLoading(boolean animation) {
    if (!mIsLoading && mAnimator == null) {
      return;
    }
    if (mAnimator != null && mAnimator.isStarted()) {
      mAnimator.cancel();
      mAnimator = null;
    }
    mIsLoading = false;
    mLoadingDirection = null;
    resetLoadStatus();
    resetToStartPosition(animation);
  }

  /** 通过target参数判断ChildView以及滚动方向，决定是否进行嵌套滚动 */
  @Override
  public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes, int type) {
    if (!isEnabled() || !(target instanceof NestedScrollingChild2)) {
      return false;
    }
    /** FIX：滑动速度较大时，FLING状态一直被 NestedScrollParent 假消费，导致直到滑动停止的耗时长达数秒 */
    if (type == ViewCompat.TYPE_NON_TOUCH) {
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

  private enum ScrollState {
    NONE,
    LOADING_TOP,
    LOADING_BOTTOM
  }

  /**
   * 监听触摸事件并设置滚动状态 {@link #mScrollState }
   * <p>
   * 配合 {@link #onNestedPreScroll(View, int, int, int[], int)} 方法，
   * 实现『 滚动出现LoadingView后，在抬起触摸手指之前，只触发当前LoadingView滑动，不触发列表滑动 』的交互效果。
   */
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
        final float dy = event.getY() - mLastTouchY;
        if (mScrollState == ScrollState.NONE) {
          if (dy > 0 && !mTargetView.canScrollVertically(-1)) {
            mScrollState = ScrollState.LOADING_TOP;
          } else if (dy < 0 && !mTargetView.canScrollVertically(1)) {
            mScrollState = ScrollState.LOADING_BOTTOM;
          }
        }
        mLastTouchY = event.getY();
        break;
      case MotionEvent.ACTION_UP:
      case MotionEvent.ACTION_CANCEL: {
        mScrollState = ScrollState.NONE;
        break;
      }
    }
    return super.dispatchTouchEvent(event);
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
      case LOADING_TOP:
        handleNestedScrollTop(dy, consumed);
        break;
      case LOADING_BOTTOM:
        handleNestedScrollBottom(dy, consumed);
        break;
    }
  }

  private void handleNestedScrollTop(int dy, int[] consumed) {
    if (!mShowTopLoadingView) {
      return;
    }
    if (getPosition() <= mStartPosition && !mTargetView.canScrollVertically(-1)) {//展开Top
      int targetPosition = getPosition() + dy;
      if (targetPosition < mTopPosition) {
        targetPosition = mTopPosition;
      }
      if (targetPosition > mStartPosition) {
        targetPosition = mStartPosition;
      }
      scrollTo(targetPosition);
      consumed[1] = dy;
    }
  }

  private void handleNestedScrollBottom(int dy, int[] consumed) {
    if (!mShowBottomLoadingView) {
      return;
    }
    if (getPosition() >= mStartPosition && !mTargetView.canScrollVertically(1)) {
      int targetPosition = getPosition() + dy;
      if (targetPosition > mBottomPosition) {
        targetPosition = mBottomPosition;
      }
      if (targetPosition < mStartPosition) {
        targetPosition = mStartPosition;
      }
      scrollTo(targetPosition);
      consumed[1] = dy;
    }
  }

  private void moveToLoadingPosition(@NonNull Direction direction, boolean animation) {
    if (!animation) {
      setPosition(mStartPosition);
    } else {
      if (mAnimator != null) {
        mAnimator.cancel();
      }
      ValueAnimator animator = ValueAnimator.ofInt(getPosition(), getLoadPosition(direction));
      animator.setDuration(getAnimationDuration(direction, getLoadPosition(direction)));
      animator.addUpdateListener(anim -> setPosition((int) anim.getAnimatedValue()));
      animator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          mAnimator = null;
          notifyLoadEvent();
        }
      });
      mAnimator = animator;
      mAnimator.start();
    }
  }

  private void resetToStartPosition(boolean animation) {
    if (!animation) {
      setPosition(mStartPosition);
    } else {
      ValueAnimator animator = ValueAnimator.ofInt(getPosition(), mStartPosition);
      animator.setDuration(getAnimationDuration(getTargetLoadDirection(), mStartPosition));
      animator.addUpdateListener(anim -> setPosition((int) anim.getAnimatedValue()));
      animator.addListener(new AnimatorListenerAdapter() {
        @Override
        public void onAnimationEnd(Animator animation) {
          mAnimator = null;
        }
      });
      mAnimator = animator;
      mAnimator.start();
    }
  }

  private long getAnimationDuration(@NonNull Direction direction, int targetPosition) {
    final long viewHeight = (direction == Direction.TOP) ? mTopViewHeight : mBottomViewHeight;
    return mAnimateToLoadDuration * Math.abs(getPosition() - targetPosition) / viewHeight;
  }

  public interface LoadStatus {
    void loading();

    void reset();
  }

  private void setLoadingStatus(@NonNull Direction direction) {
    final View loadView = (direction == Direction.TOP)
        ? mTopLoadingView : mBottomLoadingView;
    if (loadView instanceof LoadStatus) {
      ((LoadStatus) loadView).loading();
    }
  }

  private void resetLoadStatus() {
    final View loadView = (getTargetLoadDirection() == Direction.TOP)
        ? mTopLoadingView : mBottomLoadingView;
    if (loadView instanceof LoadStatus) {
      ((LoadStatus) loadView).reset();
    }
  }

  public interface OnLoadListener {
    void onLoad(@NonNull Direction direction);
  }

  public enum Direction {TOP, BOTTOM}

  private List<OnLoadListener> mOnLoadListeners = new ArrayList<>();

  public void addOnLoadListener(@NonNull OnLoadListener onLoadListener) {
    mOnLoadListeners.add(onLoadListener);
  }

  public void removeOnLoadListener(@NonNull OnLoadListener onLoadListener) {
    mOnLoadListeners.remove(onLoadListener);
  }

  /** LoadView完全弹出时，通知业务方加载数据 */
  private void notifyLoadEvent() {
    for (OnLoadListener onLoadListener : mOnLoadListeners) {
      onLoadListener.onLoad(mLoadingDirection);
    }
  }

  private int getLoadPosition(@NonNull Direction direction) {
    return (direction == Direction.TOP) ? mTopPosition : mBottomPosition;
  }

  @NonNull
  private Direction getTargetLoadDirection() {
    return (getPosition() <= mStartPosition) ? Direction.TOP : Direction.BOTTOM;
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

  /** 重写此方法，用于正确计算 {@link #canScrollVertically(int)} */
  @Override
  protected int computeVerticalScrollRange() {
    return getHeight() + mTopViewHeight + mBottomViewHeight;
  }
}
