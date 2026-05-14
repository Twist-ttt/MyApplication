package com.example.myapplication.ui;

import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.myapplication.R;
import com.example.myapplication.data.ItemRepository;
import com.example.myapplication.data.entity.Item;
import com.example.myapplication.util.Constants;
import com.example.myapplication.util.DateUtils;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.datepicker.MaterialDatePicker;
import com.google.android.material.textfield.TextInputEditText;

public class AddItemActivity extends AppCompatActivity {

    public static final String EXTRA_IS_EDIT = "extra_is_edit";
    public static final String EXTRA_ITEM_ID = "extra_item_id";

    private MaterialToolbar toolbar;
    private TextInputEditText editName;
    private AutoCompleteTextView autoCompleteCategory;
    private TextInputEditText editBuyDate;
    private TextInputEditText editExpireDate;
    private TextInputEditText editRemindDays;
    private TextInputEditText editNote;
    private MaterialButton btnSave;

    private ItemRepository repository;
    private boolean isEditMode = false;
    private int editItemId = -1;

    private String buyDateUtc = null;
    private String expireDateUtc = null;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_item);

        repository = ItemRepository.getInstance((android.app.Application) getApplicationContext());

        initViews();
        setupToolbar();
        setupCategoryDropdown();
        setupDatePickers();
        setupSaveButton();

        // 判断是否编辑模式
        isEditMode = getIntent().getBooleanExtra(EXTRA_IS_EDIT, false);
        if (isEditMode) {
            editItemId = getIntent().getIntExtra(EXTRA_ITEM_ID, -1);
            toolbar.setTitle(R.string.title_edit_item);
            loadItemData();
        } else {
            toolbar.setTitle(R.string.title_add_item);
            // 默认购买日期为今天
            buyDateUtc = DateUtils.getCurrentDateStr();
            editBuyDate.setText(buyDateUtc);
        }
    }

    private void initViews() {
        toolbar = findViewById(R.id.toolbar_add);
        editName = findViewById(R.id.edit_item_name);
        autoCompleteCategory = findViewById(R.id.auto_complete_category);
        editBuyDate = findViewById(R.id.edit_buy_date);
        editExpireDate = findViewById(R.id.edit_expire_date);
        editRemindDays = findViewById(R.id.edit_remind_days);
        editNote = findViewById(R.id.edit_note);
        btnSave = findViewById(R.id.btn_save);
    }

    private void setupToolbar() {
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setHomeAsUpIndicator(android.R.drawable.ic_menu_close_clear_cancel);
        }
    }

    private void setupCategoryDropdown() {
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_dropdown_item_1line, Constants.CATEGORIES);
        autoCompleteCategory.setAdapter(adapter);
        autoCompleteCategory.setText(Constants.CATEGORIES[0], false);
    }

    private void setupDatePickers() {
        editBuyDate.setOnClickListener(v -> showDatePicker(true));
        editExpireDate.setOnClickListener(v -> showDatePicker(false));
    }

    private void showDatePicker(boolean isBuyDate) {
        MaterialDatePicker<Long> picker = MaterialDatePicker.Builder.datePicker()
                .setTitleText(isBuyDate ? "选择购买日期" : "选择过期日期")
                .setSelection(MaterialDatePicker.todayInUtcMilliseconds())
                .build();

        picker.addOnPositiveButtonClickListener(selection -> {
            String dateStr = DateUtils.utcMillisToDateString(selection);
            if (isBuyDate) {
                buyDateUtc = dateStr;
                editBuyDate.setText(dateStr);
            } else {
                expireDateUtc = dateStr;
                editExpireDate.setText(dateStr);
            }
        });

        picker.show(getSupportFragmentManager(), isBuyDate ? "BUY_DATE" : "EXPIRE_DATE");
    }

    private void setupSaveButton() {
        btnSave.setOnClickListener(v -> saveItem());
    }

    private void loadItemData() {
        if (editItemId == -1) return;
        repository.getItemById(editItemId).observe(this, item -> {
            if (item != null) {
                editName.setText(item.getName());
                autoCompleteCategory.setText(item.getCategory(), false);
                buyDateUtc = item.getBuyDate();
                expireDateUtc = item.getExpireDate();
                editBuyDate.setText(buyDateUtc);
                editExpireDate.setText(expireDateUtc);
                editRemindDays.setText(String.valueOf(item.getRemindDays()));
                editNote.setText(item.getNote());
            }
        });
    }

    private void saveItem() {
        String name = editName.getText() != null ? editName.getText().toString().trim() : "";
        String category = autoCompleteCategory.getText().toString().trim();
        String remindStr = editRemindDays.getText() != null ? editRemindDays.getText().toString().trim() : "7";
        String note = editNote.getText() != null ? editNote.getText().toString().trim() : "";

        // 验证
        if (name.isEmpty()) {
            editName.setError(getString(R.string.error_name_empty));
            return;
        }
        if (buyDateUtc == null || buyDateUtc.isEmpty()) {
            Toast.makeText(this, R.string.error_date_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (expireDateUtc == null || expireDateUtc.isEmpty()) {
            Toast.makeText(this, R.string.error_date_empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (!DateUtils.isValidDateRange(buyDateUtc, expireDateUtc)) {
            Toast.makeText(this, R.string.error_date_range, Toast.LENGTH_SHORT).show();
            return;
        }

        int remindDays;
        try {
            remindDays = Integer.parseInt(remindStr);
            if (remindDays < 0) remindDays = Constants.DEFAULT_REMIND_DAYS;
        } catch (NumberFormatException e) {
            remindDays = Constants.DEFAULT_REMIND_DAYS;
        }

        String status = DateUtils.getStatus(expireDateUtc);

        if (isEditMode && editItemId != -1) {
            Item item = new Item(name, category, buyDateUtc, expireDateUtc, remindDays, note, status);
            item.setId(editItemId);
            repository.update(item);
        } else {
            Item item = new Item(name, category, buyDateUtc, expireDateUtc, remindDays, note, status);
            repository.insert(item);
        }

        Toast.makeText(this, R.string.msg_saved, Toast.LENGTH_SHORT).show();
        finish();
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
