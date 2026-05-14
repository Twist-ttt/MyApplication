package com.example.myapplication.ui;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.myapplication.R;
import com.example.myapplication.data.entity.Item;
import com.example.myapplication.data.ItemRepository;
import com.example.myapplication.ui.adapter.ItemListAdapter;
import com.example.myapplication.util.Constants;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_NOTIFICATION_PERMISSION = 1001;
    private static final int REQUEST_ADD_ITEM = 2001;

    private TextInputEditText searchEditText;
    private ChipGroup chipGroupCategory;
    private RecyclerView recyclerView;
    private TextView textEmpty;
    private FloatingActionButton fabAdd;

    private ItemListAdapter adapter;
    private ItemRepository repository;

    private String currentCategory = null;
    private String currentQuery = "";
    private final List<Item> allItems = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        repository = ItemRepository.getInstance((android.app.Application) getApplicationContext());

        initViews();
        setupToolbar();
        setupCategoryChips();
        setupSearch();
        setupRecyclerView();
        setupFab();
        checkNotificationPermission();
        observeData();
    }

    private void initViews() {
        searchEditText = findViewById(R.id.search_edit_text);
        chipGroupCategory = findViewById(R.id.chip_group_category);
        recyclerView = findViewById(R.id.recycler_view_items);
        textEmpty = findViewById(R.id.text_empty);
        fabAdd = findViewById(R.id.fab_add);
    }

    private void setupToolbar() {
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
    }

    private void setupCategoryChips() {
        // "全部" chip
        Chip chipAll = new Chip(this);
        chipAll.setText(Constants.CATEGORIES.length > 0 ? "全部" : "全部");
        chipAll.setCheckable(true);
        chipAll.setChecked(true);
        chipAll.setOnClickListener(v -> {
            currentCategory = null;
            applyFilter();
        });
        chipGroupCategory.addView(chipAll);

        // 各类别 chip
        for (String category : Constants.CATEGORIES) {
            Chip chip = new Chip(this);
            chip.setText(category);
            chip.setCheckable(true);
            chip.setOnClickListener(v -> {
                currentCategory = category;
                applyFilter();
            });
            chipGroupCategory.addView(chip);
        }
    }

    private void setupSearch() {
        searchEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                currentQuery = s.toString().trim();
                applyFilter();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void setupRecyclerView() {
        adapter = new ItemListAdapter();
        adapter.setOnItemClickListener(item -> {
            Intent intent = new Intent(this, DetailActivity.class);
            intent.putExtra(Constants.EXTRA_ITEM_ID, item.getId());
            startActivity(intent);
        });

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);
    }

    private void setupFab() {
        fabAdd.setOnClickListener(v -> {
            Intent intent = new Intent(this, AddItemActivity.class);
            startActivityForResult(intent, REQUEST_ADD_ITEM);
        });
    }

    private void checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        REQUEST_NOTIFICATION_PERMISSION);
            }
        }
    }

    private void observeData() {
        repository.getAllItems().observe(this, items -> {
            if (items != null) {
                allItems.clear();
                allItems.addAll(items);
                applyFilter();
            }
        });
    }

    /**
     * 组合过滤：搜索 + 类别。
     */
    private void applyFilter() {
        List<Item> filtered = new ArrayList<>();
        for (Item item : allItems) {
            boolean matchCategory = (currentCategory == null)
                    || currentCategory.equals(item.getCategory());
            boolean matchQuery = currentQuery.isEmpty()
                    || (item.getName() != null && item.getName().toLowerCase()
                        .contains(currentQuery.toLowerCase()));
            if (matchCategory && matchQuery) {
                filtered.add(item);
            }
        }

        adapter.submitList(filtered);
        textEmpty.setVisibility(filtered.isEmpty() ? View.VISIBLE : View.GONE);
        recyclerView.setVisibility(filtered.isEmpty() ? View.GONE : View.VISIBLE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        // 从添加/编辑页面返回后，LiveData 会自动刷新列表
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        // 通知权限结果，无论是否授予都不影响主功能
    }
}
