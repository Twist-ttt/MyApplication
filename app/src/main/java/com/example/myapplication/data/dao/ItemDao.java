package com.example.myapplication.data.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.example.myapplication.data.entity.Item;

import java.util.List;

@Dao
public interface ItemDao {

    @Insert
    long insert(Item item);

    @Update
    void update(Item item);

    @Delete
    void delete(Item item);

    @Query("SELECT * FROM items ORDER BY expire_date ASC")
    LiveData<List<Item>> getAllItems();

    @Query("SELECT * FROM items WHERE category = :category ORDER BY expire_date ASC")
    LiveData<List<Item>> getItemsByCategory(String category);

    @Query("SELECT * FROM items WHERE name LIKE '%' || :query || '%' ORDER BY expire_date ASC")
    LiveData<List<Item>> searchItems(String query);

    @Query("SELECT * FROM items WHERE name LIKE '%' || :query || '%' AND category = :category ORDER BY expire_date ASC")
    LiveData<List<Item>> searchItemsByCategory(String query, String category);

    @Query("SELECT * FROM items WHERE status != '已使用' AND expire_date <= :dateStr")
    List<Item> getExpiringItems(String dateStr);

    @Query("SELECT * FROM items WHERE id = :id")
    LiveData<Item> getItemById(int id);

    @Query("DELETE FROM items WHERE id = :id")
    void deleteById(int id);

    @Query("UPDATE items SET status = :status WHERE id = :id")
    void updateStatus(int id, String status);
}
