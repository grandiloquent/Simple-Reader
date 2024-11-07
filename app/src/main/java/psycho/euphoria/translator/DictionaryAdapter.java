package psycho.euphoria.translator;

import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class DictionaryAdapter extends BaseAdapter {
    private List<String> mStrings = new ArrayList<>();

    public DictionaryAdapter() {
    }

    public void update(List<String> strings) {
        mStrings.clear();
        mStrings.addAll(strings);
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return mStrings.size();
    }

    @Override
    public String getItem(int position) {
        return mStrings.get(position);
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
        viewHolder.textView.setText(mStrings.get(position));
        return convertView;
    }

    public class ViewHolder {
        TextView textView;
    }
}