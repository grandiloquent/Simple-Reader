package psycho.euphoria.translator;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class SearchAdapter extends BaseAdapter {
    private List<Integer> mIntegers = new ArrayList<>();

    public SearchAdapter() {
    }

    public void update(List<Integer> integers) {
        mIntegers.clear();
        mIntegers.addAll(integers);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mIntegers.size();
    }

    @Override
    public Integer getItem(int position) {
        return mIntegers.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder viewHolder;
        if (convertView == null) {
            viewHolder = new ViewHolder();
            TextView textView = new TextView(parent.getContext());
            textView.setTextSize(16);
            textView.setPadding(24, 12, 24, 12);
            convertView = textView;
            viewHolder.textView = textView;
            convertView.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) convertView.getTag();
        }
        viewHolder.textView.setText(Integer.toString(mIntegers.get(position)));
        return convertView;
    }

    public class ViewHolder {
        TextView textView;
    }
}