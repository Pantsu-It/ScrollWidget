package com.pantsu.scrollwidget.view.data;

import java.util.ArrayList;
import java.util.List;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.pantsu.scrollwidget.R;

public class MyListAdapter extends RecyclerView.Adapter<MyListAdapter.TextViewHolder> {

  private List<String> mData = new ArrayList<>();

  public void setData(List<String> data) {
    mData = data;
  }

  public List<String> getData() {
    return mData;
  }

  @NonNull
  @Override
  public TextViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
    View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_text_main, parent, false);
    return new TextViewHolder(view);
  }

  @Override
  public void onBindViewHolder(@NonNull TextViewHolder holder, int position) {
    TextView textView = holder.itemView.findViewById(R.id.text);
    textView.setText(mData.get(position));

    holder.itemView.getLayoutParams().width = ViewGroup.LayoutParams.MATCH_PARENT;
    holder.itemView.requestLayout();
  }

  @Override
  public int getItemCount() {
    return mData.size();
  }

  static class TextViewHolder extends RecyclerView.ViewHolder {
    public TextViewHolder(@NonNull View itemView) {
      super(itemView);
    }
  }
}

