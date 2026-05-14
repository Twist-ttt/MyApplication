package com.example.myapplication.data.entity;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

/**
 * 商品保质期追踪实体类，映射到 SQLite 的 items 表。
 */
@Entity(tableName = "items")
public class Item {

    @PrimaryKey(autoGenerate = true)
    private int id;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "category", defaultValue = "其他")
    private String category;

    @ColumnInfo(name = "buy_date")
    private String buyDate;

    @ColumnInfo(name = "expire_date")
    private String expireDate;

    @ColumnInfo(name = "remind_days", defaultValue = "7")
    private int remindDays;

    @ColumnInfo(name = "note", defaultValue = "")
    private String note;

    @ColumnInfo(name = "status", defaultValue = "正常")
    private String status;

    /** Room 需要的空构造函数 */
    public Item() {
    }

    public Item(String name, String category, String buyDate,
                String expireDate, int remindDays, String note, String status) {
        this.name = name;
        this.category = category;
        this.buyDate = buyDate;
        this.expireDate = expireDate;
        this.remindDays = remindDays;
        this.note = note;
        this.status = status;
    }

    // --- getters & setters ---

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getBuyDate() { return buyDate; }
    public void setBuyDate(String buyDate) { this.buyDate = buyDate; }

    public String getExpireDate() { return expireDate; }
    public void setExpireDate(String expireDate) { this.expireDate = expireDate; }

    public int getRemindDays() { return remindDays; }
    public void setRemindDays(int remindDays) { this.remindDays = remindDays; }

    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    @Override
    public String toString() {
        return "Item{id=" + id
                + ", name='" + name + '\''
                + ", category='" + category + '\''
                + ", expireDate='" + expireDate + '\''
                + ", status='" + status + '\''
                + '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Item item = (Item) o;
        return id == item.id
                && remindDays == item.remindDays
                && (name != null ? name.equals(item.name) : item.name == null)
                && (category != null ? category.equals(item.category) : item.category == null)
                && (buyDate != null ? buyDate.equals(item.buyDate) : item.buyDate == null)
                && (expireDate != null ? expireDate.equals(item.expireDate) : item.expireDate == null)
                && (note != null ? note.equals(item.note) : item.note == null)
                && (status != null ? status.equals(item.status) : item.status == null);
    }

    @Override
    public int hashCode() {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (category != null ? category.hashCode() : 0);
        result = 31 * result + (buyDate != null ? buyDate.hashCode() : 0);
        result = 31 * result + (expireDate != null ? expireDate.hashCode() : 0);
        result = 31 * result + remindDays;
        result = 31 * result + (note != null ? note.hashCode() : 0);
        result = 31 * result + (status != null ? status.hashCode() : 0);
        return result;
    }
}
