package com.example.myapplication.data;

import android.app.Application;

import androidx.lifecycle.LiveData;

import com.example.myapplication.data.dao.ItemDao;
import com.example.myapplication.data.entity.Item;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ItemRepository {

    private final ItemDao itemDao;
    private final ExecutorService executor;

    private static volatile ItemRepository INSTANCE;

    public ItemRepository(Application application) {
        AppDatabase db = AppDatabase.getInstance(application);
        itemDao = db.itemDao();
        executor = Executors.newSingleThreadExecutor();
    }

    public static ItemRepository getInstance(Application application) {
        if (INSTANCE == null) {
            synchronized (ItemRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ItemRepository(application);
                }
            }
        }
        return INSTANCE;
    }

    // --- LiveData 查询（直接返回，由 Room 在后台线程执行） ---

    public LiveData<List<Item>> getAllItems() {
        return itemDao.getAllItems();
    }

    public LiveData<List<Item>> getItemsByCategory(String category) {
        return itemDao.getItemsByCategory(category);
    }

    public LiveData<List<Item>> searchItems(String query) {
        return itemDao.searchItems(query);
    }

    public LiveData<List<Item>> searchItemsByCategory(String query, String category) {
        return itemDao.searchItemsByCategory(query, category);
    }

    public LiveData<Item> getItemById(int id) {
        return itemDao.getItemById(id);
    }

    // --- 写操作（通过 ExecutorService 在后台线程执行） ---

    public void insert(Item item) {
        executor.execute(() -> itemDao.insert(item));
    }

    public void update(Item item) {
        executor.execute(() -> itemDao.update(item));
    }

    public void delete(Item item) {
        executor.execute(() -> itemDao.delete(item));
    }

    public void deleteById(int id) {
        executor.execute(() -> itemDao.deleteById(id));
    }

    public void updateStatus(int id, String status) {
        executor.execute(() -> itemDao.updateStatus(id, status));
    }

    // --- 同步查询（供 WorkManager 在自己的线程调用） ---

    public List<Item> getExpiringItems(String dateStr) {
        return itemDao.getExpiringItems(dateStr);
    }
}
