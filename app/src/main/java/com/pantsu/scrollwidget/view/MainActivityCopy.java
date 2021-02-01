package com.pantsu.scrollwidget.view;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pantsu.scrollwidget.R;
import com.pantsu.scrollwidget.view.data.MyListAdapter;
import com.pantsu.scrollwidget.view.view.NestedScrollRefreshLayout;

public class MainActivityCopy extends AppCompatActivity {


  private PageList mPageList;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main_copy);

    MyListAdapter adapter = new MyListAdapter();
    RecyclerView recyclerView = findViewById(R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    recyclerView.setAdapter(adapter);

    PageList pageList = new PageList((NestedScrollRefreshLayout) recyclerView.getParent(), recyclerView, adapter);
    mPageList = pageList;

    NestedScrollRefreshLayout nestedScrollRefreshLayout = findViewById(R.id.refresh_layout);
    nestedScrollRefreshLayout.addOnRefreshListener(direction -> {
      if (direction == NestedScrollRefreshLayout.Direction.TOP) {
        mPageList.loadMoreOld();
      } else if (direction == NestedScrollRefreshLayout.Direction.BOTTOM) {
        mPageList.loadMoreNew();
      }
    });
  }
}
