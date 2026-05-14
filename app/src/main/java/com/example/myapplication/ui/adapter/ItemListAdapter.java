package com.example.myapplication.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.entity.Item;
import com.example.myapplication.util.Constants;
import com.example.myapplication.util.DateUtils;

/**
 * RecyclerView 适配器：展示商品列表。
 */
public class ItemListAdapter extends ListAdapter<Item, ItemListAdapter.ItemViewHolder> {

    private OnItemClickListener onItemClickListener;

    public interface OnItemClickListener {
        void onItemClick(Item item);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.onItemClickListener = listener;
    }

    public ItemListAdapter() {
        super(DIFF_CALLBACK);
    }

    private static final DiffUtil.ItemCallback<Item> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<Item>() {
                @Override
                public boolean areItemsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
                    return oldItem.getId() == newItem.getId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull Item oldItem, @NonNull Item newItem) {
                    return oldItem.equals(newItem);
                }
            };

    @NonNull
    @Override
    public ItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_card, parent, false);
        return new ItemViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemViewHolder holder, int position) {
        Item item = getItem(position);
        holder.bind(item);
    }

    class ItemViewHolder extends RecyclerView.ViewHolder {

        private final TextView textName;
        private final TextView textCategory;
        private final TextView textExpireDate;
        private final TextView textDaysRemaining;
        private final TextView textStatus;

        ItemViewHolder(@NonNull View itemView) {
            super(itemView);
            textName = itemView.findViewById(R.id.text_item_name);
            textCategory = itemView.findViewById(R.id.text_item_category);
            textExpireDate = itemView.findViewById(R.id.text_item_expire_date);
            textDaysRemaining = itemView.findViewById(R.id.text_days_remaining);
            textStatus = itemView.findViewById(R.id.text_item_status);

            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && onItemClickListener != null) {
                    onItemClickListener.onItemClick(getItem(pos));
                }
            });
        }

        void bind(Item item) {
            textName.setText(item.getName());
            textCategory.setText(item.getCategory());
            textExpireDate.setText("过期日期：" + item.getExpireDate());

            // 已使用
            if (Constants.STATUS_USED.equals(item.getStatus())) {
                textStatus.setText(Constants.STATUS_USED);
                textStatus.setBackgroundColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.status_used));
                textDaysRemaining.setText("--");
                textDaysRemaining.setTextColor(
                        ContextCompat.getColor(itemView.getContext(), R.color.text_secondary));
                return;
            }

            int days = DateUtils.getDaysRemaining(item.getExpireDate());
            String status = DateUtils.getStatus(item.getExpireDate());

            // 剩余天数
            if (days < 0) {
                textDaysRemaining.setText("过期" + Math.abs(days) + "天");
            } else if (days == 0) {
                textDaysRemaining.setText("今天");
            } else {
                textDaysRemaining.setText(days + "天");
            }

            // 颜色编码
            int color;
            switch (status) {
                case Constants.STATUS_EXPIRED:
                    color = ContextCompat.getColor(itemView.getContext(), R.color.status_expired);
                    break;
                case Constants.STATUS_EXPIRING:
                    color = ContextCompat.getColor(itemView.getContext(), R.color.status_expiring);
                    break;
                default:
                    color = ContextCompat.getColor(itemView.getContext(), R.color.status_normal);
                    break;
            }

            textDaysRemaining.setTextColor(color);
            textStatus.setText(status);
            textStatus.setBackgroundColor(color);
        }
    }
}
