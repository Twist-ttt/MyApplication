package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.myapplication.R;
import com.example.myapplication.data.ItemRepository;
import com.example.myapplication.data.entity.Item;
import com.example.myapplication.util.Constants;
import com.example.myapplication.util.DateUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;

public class DetailActivity extends AppCompatActivity {

    private MaterialToolbar toolbar;
    private TextView textName;
    private TextView textStatus;
    private TextView textRemaining;
    private TextView textCategory;
    private TextView textBuyDate;
    private TextView textExpireDate;
    private TextView textRemindDays;
    private TextView textNote;
    private MaterialButton btnMarkUsed;
    private MaterialButton btnDelete;

    private ItemRepository repository;
    private Item currentItem;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);

        repository = ItemRepository.getInstance((android.app.Application) getApplicationContext());

        initViews();
        setupToolbar();
        setupButtons();

        int itemId = getIntent().getIntExtra(Constants.EXTRA_ITEM_ID, -1);
        if (itemId != -1) {
            loadItem(itemId);
        } else {
            finish();
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_detail);
        textName = findViewById(R.id.detail_name);
        textStatus = findViewById(R.id.detail_status);
        textRemaining = findViewById(R.id.detail_remaining);
        textCategory = findViewById(R.id.detail_category);
        textBuyDate = findViewById(R.id.detail_buy_date);
        textExpireDate = findViewById(R.id.detail_expire_date);
        textRemindDays = findViewById(R.id.detail_remind_days);
        textNote = findViewById(R.id.detail_note);
        btnMarkUsed = findViewById(R.id.btn_mark_used);
        btnDelete = findViewById(R.id.btn_delete);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_menu_revert);
        }
    }

    private void setupButtons() {
        btnMarkUsed.setOnClickListener(v -> {
            if (currentItem != null) {
                repository.updateStatus(currentItem.getId(), Constants.STATUS_USED);
                Toast.makeText(this, R.string.msg_marked_used, Toast.LENGTH_SHORT).show();
                finish();
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (currentItem != null) {
                new AlertDialog.Builder(this)
                        .setTitle(R.string.dialog_delete_title)
                        .setMessage(String.format(getString(R.string.dialog_delete_message),
                                currentItem.getName()))
                        .setPositiveButton(R.string.dialog_confirm, (dialog, which) -> {
                            repository.deleteById(currentItem.getId());
                            Toast.makeText(this, R.string.msg_deleted, Toast.LENGTH_SHORT).show();
                            finish();
                        })
                        .setNegativeButton(R.string.dialog_cancel, null)
                        .show();
            }
        });
    }

    private void loadItem(int itemId) {
        repository.getItemById(itemId).observe(this, item -> {
            if (item != null) {
                currentItem = item;
                displayItem(item);
            } else {
                finish();
            }
        });
    }

    private void displayItem(@NonNull Item item) {
        textName.setText(item.getName());
        textCategory.setText(item.getCategory());
        textBuyDate.setText(item.getBuyDate());
        textExpireDate.setText(item.getExpireDate());
        textRemindDays.setText(item.getRemindDays() + " 天");
        textNote.setText(item.getNote() != null && !item.getNote().isEmpty()
                ? item.getNote() : "无");

        // 已使用
        if (Constants.STATUS_USED.equals(item.getStatus())) {
            textStatus.setText(Constants.STATUS_USED);
            textStatus.setBackgroundColor(
                    ContextCompat.getColor(this, R.color.status_used));
            textRemaining.setText("--");
            textRemaining.setTextColor(
                    ContextCompat.getColor(this, R.color.text_secondary));
            btnMarkUsed.setEnabled(false);
            return;
        }

        int days = DateUtils.getDaysRemaining(item.getExpireDate());
        String status = DateUtils.getStatus(item.getExpireDate());

        // 剩余天数
        if (days < 0) {
            textRemaining.setText(String.format(getString(R.string.label_expired),
                    Math.abs(days)));
        } else if (days == 0) {
            textRemaining.setText(R.string.label_today);
        } else {
            textRemaining.setText(String.format(getString(R.string.label_remaining), days));
        }

        // 颜色编码
        int color;
        switch (status) {
            case Constants.STATUS_EXPIRED:
                color = ContextCompat.getColor(this, R.color.status_expired);
                break;
            case Constants.STATUS_EXPIRING:
                color = ContextCompat.getColor(this, R.color.status_expiring);
                break;
            default:
                color = ContextCompat.getColor(this, R.color.status_normal);
                break;
        }

        textRemaining.setTextColor(color);
        textStatus.setText(status);
        textStatus.setBackgroundColor(color);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
