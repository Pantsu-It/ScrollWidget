package com.pantsu.scrollwidget.view;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pantsu.scrollwidget.view.data.MyListAdapter;
import com.pantsu.scrollwidget.view.view.NestedScrollLoadingLayout;

import io.reactivex.Single;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.schedulers.Schedulers;

public class PageList {

  private NestedScrollLoadingLayout mLoadingLayout;
  private RecyclerView mRecyclerView;
  private MyListAdapter mAdapter;

  private int mFromId, mToId;
  private int mMinId, mMaxId;

  public PageList(NestedScrollLoadingLayout refreshLayout, RecyclerView recyclerView, MyListAdapter adapter) {
    mLoadingLayout = refreshLayout;
    mRecyclerView = recyclerView;
    mAdapter = adapter;

    initData();
  }

  private void initData() {
    mFromId = 100;
    mToId = 139;
    mMinId = Integer.MIN_VALUE;
    mMaxId = Integer.MAX_VALUE;
    mAdapter.setData(loadData(mFromId, mToId));
  }

  private boolean hasMoreOld() {
    return mFromId > mMinId;
  }

  private boolean hasMoreNew() {
    return mToId < mMaxId;
  }

  private List<String> loadData(int from, int to) {
    mFromId = Math.max(from, mMinId);
    mToId = Math.min(to, mMaxId);

    List<String> list = new ArrayList<>(to - from);
    for (int i = mFromId; i <= mToId; ++i) {
      list.add(" 消息id=" + i + " 消息id=" + i + " 消息id=" + i);
    }
    return list;
  }

  public void loadMoreOld() {
    Single
        .timer(300, TimeUnit.MILLISECONDS)
        .map(ignore -> loadData(mFromId - 20, mToId))
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(list -> {
          onFinishLoading(list);

          mAdapter.setData(list);
          mAdapter.notifyDataSetChanged();
        });
  }

  public void loadMoreNew() {
    Single
        .timer(5000, TimeUnit.MILLISECONDS)
        .map(ignore -> loadData(mFromId, mToId + 20))
        .subscribeOn(Schedulers.computation())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(list -> {
          onFinishLoading(list);

          mAdapter.setData(list);
          mAdapter.notifyDataSetChanged();
        });
  }

  private void onFinishLoading(List<String> newData) {
    // 滚动列表
    final LinearLayoutManager mLayoutManager = (LinearLayoutManager) mRecyclerView.getLayoutManager();
    final boolean keepPosition = mAdapter.getData().size() > 0
        && (mLayoutManager.findFirstCompletelyVisibleItemPosition() == 0);
    if (keepPosition) {
      String firstItem = mAdapter.getData().get(0);
      int index = newData.indexOf(firstItem);
      int scrollOffset = mLoadingLayout.getTargetViewOffset();
      mRecyclerView.post(() -> mLayoutManager.scrollToPositionWithOffset(Math.max(index, 0), scrollOffset));
    }

    // 停止刷新
    boolean animation = mAdapter.getData().size() == newData.size();
    mLoadingLayout.stopLoading(animation);

    mLoadingLayout.setShowTopLoadingView(hasMoreOld());
    mLoadingLayout.setShowBottomLoadingView(hasMoreNew());
  }
}
