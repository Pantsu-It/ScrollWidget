package com.pantsu.scrollwidget.view;

import java.util.ArrayList;
import java.util.List;

import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.pantsu.scrollwidget.R;

public class MainActivityCopy extends AppCompatActivity {


  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main_copy);

    RecyclerView recyclerView = findViewById(R.id.recycler_view);
    recyclerView.setLayoutManager(new LinearLayoutManager(this));
    recyclerView.setAdapter(new MyListAdapter());
  }

  private static class MyListAdapter extends RecyclerView.Adapter<TextViewHolder> {

    private List<String> mData = new ArrayList<>();

    {
      for (int i = 0; i < 40; ++i) {
        mData.add("哈哈哈哈哈哈哈哈哈哈哈哈嘿嘿  ->" + i);
      }
    }

    @NonNull
    @Override
    public TextViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      View view = View.inflate(parent.getContext(), R.layout.item_text_main, null);
      return new TextViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TextViewHolder holder, int position) {
      TextView textView = holder.itemView.findViewById(R.id.text);
      textView.setText(mData.get(position));
    }

    @Override
    public int getItemCount() {
      return mData.size();
    }
  }

  private static class TextViewHolder extends RecyclerView.ViewHolder {

    public TextViewHolder(@NonNull View itemView) {
      super(itemView);
    }
  }
}
