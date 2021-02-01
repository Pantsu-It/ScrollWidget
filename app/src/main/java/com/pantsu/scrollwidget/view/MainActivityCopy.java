package com.pantsu.scrollwidget.view;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pantsu.scrollwidget.R;
import com.pantsu.scrollwidget.view.data.MyListAdapter;
import com.pantsu.scrollwidget.view.view.NestedScrollLoadingLayout;

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

    PageList pageList = new PageList((NestedScrollLoadingLayout) recyclerView.getParent(), recyclerView, adapter);
    mPageList = pageList;

    NestedScrollLoadingLayout nestedScrollLoadingLayout = findViewById(R.id.refresh_layout);
    nestedScrollLoadingLayout.addOnLoadListener(direction -> {
      if (direction == NestedScrollLoadingLayout.Direction.TOP) {
        mPageList.loadMoreOld();
      } else if (direction == NestedScrollLoadingLayout.Direction.BOTTOM) {
        mPageList.loadMoreNew();
      }
    });
  }
}
