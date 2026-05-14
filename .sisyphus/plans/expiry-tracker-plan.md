# 📋 开发路线图：随手记保质期 (Easy Expiry Tracker)

## Epic 总览
- **商业价值**：帮助用户追踪食品、药品、护肤品等保质期，通过过期提醒减少浪费和安全隐患
- **成功指标**：添加/查看/过滤/删除/标记已使用物品；过期前N天、1天和当天收到通知；支持搜索和分类过滤组合
- **预估时间**：5-7天（约45-55小时）
- **优先级**：高

---

## 技术架构

### 组件
| 组件 | 职责 | 技术 |
|---|---|---|
| **MainActivity** | 包含 RecyclerView 的主页商品列表 | AppCompatActivity + RecyclerView |
| **AddItemActivity** | 用于添加新商品的表单（支持新增/编辑两种模式） | AppCompatActivity + MaterialDatePicker |
| **DetailActivity** | 商品详情视图及操作 | AppCompatActivity |
| **Item Entity** | 映射到 SQLite 的 Room 实体 | Room @Entity |
| **ItemDao** | CRUD 数据库操作 | Room @Dao |
| **AppDatabase** | 单例数据库持有者 | RoomDatabase |
| **ItemRepository** | 数据访问的抽象层 | 自定义 Repository 模式 |
| **ExpiryCheckWorker** | 周期性过期检查 | WorkManager Worker |
| **NotificationHelper** | NotificationChannel 设置 | NotificationCompat |
| **ItemListAdapter** | RecyclerView 适配器（含 DiffUtil） | RecyclerView.Adapter + ListAdapter |
| **DateUtils** | 日期计算工具 | java.util.Calendar（避免脱糖问题） |

### 数据流
```
用户输入 → Activity → Repository → Room DAO → SQLite
                                    ↓
                            LiveData → Adapter → RecyclerView

WorkManager (周期性) → Repository → 检查过期商品 → NotificationHelper → 系统通知
```

### 技术栈
- **Android SDK**: minSdk 24, targetSdk 36, compileSdk 36
- **语言**: 仅限 Java 11（无 Kotlin 插件）
- **UI**: XML 布局 + Material Design 组件
- **数据库**: Room 2.6.1
- **后台工作**: WorkManager 2.9.1
- **日期处理**: `java.util.Calendar` + `java.text.SimpleDateFormat`（minSdk 24 原生支持，无需脱糖）
- **构建**: AGP 9.1.0 / Gradle 9.3.1 / 版本目录

### 关键架构决策

#### 为什么用 `java.util.Calendar` 而不是 `java.time`？
- `java.time`（LocalDate / ChronoUnit）需要 API 26+ 或 coreLibraryDesugaring
- minSdk 24 无法保证 `java.time` 可用
- `coreLibraryDesugaring` 需要额外的 Gradle 配置和依赖，增加 AGP 9.1.0 兼容性风险
- `java.util.Calendar` 在所有 API 24+ 设备上原生可用，零额外配置
- 对于本项目（简单的日期差计算），Calendar 完全够用

#### 为什么不用 cardview 依赖？
- `material` 1.13.0 已包含 `MaterialCardView`，额外引入 `cardview:1.0.0` 是冗余的旧版依赖

---

## 库与框架研究

### 必需依赖

| 库 | 用途 | 版本 | 许可证 | 备注 |
|---|---|---|---|---|
| `room-runtime` | 本地 SQLite ORM | 2.6.1 | Apache 2.0 | Google 维护，稳定的 Jetpack 组件 |
| `room-compiler` | Room 的注解处理器 | 2.6.1 | Apache 2.0 | 必须与 room-runtime 版本匹配 |
| `work-runtime` | 周期性后台任务 | 2.9.1 | Apache 2.0 | 支持 minSdk 14+，推荐用于后台工作 |
| `recyclerview` | 商品列表显示 | 1.3.2 | Apache 2.0 | 核心 AndroidX 组件 |
| `lifecycle-livedata` | 响应式数据观察 | 2.7.0 | Apache 2.0 | 与 Room LiveData 返回配合工作 |
| `lifecycle-viewmodel` | ViewModel 支持 | 2.7.0 | Apache 2.0 | 配置更改期间保留数据 |

### 现有依赖（已配置）
- `appcompat` 1.7.1 — 兼容性层
- `material` 1.13.0 — Material Design 组件（已包含 MaterialCardView，无需额外 cardview 依赖）

### 无需自定义实现
- **日期选择器**：使用 `MaterialDatePicker` (来自 Material 库)
- **日期计算**：使用 `java.util.Calendar` 和 `java.text.SimpleDateFormat`
- **通知**：使用 `NotificationManagerCompat` (来自 core 库，appcompat 已传递包含)
- **数据库迁移**：v1 首次发布无需迁移
- **卡片组件**：`MaterialCardView` 来自 material 库，无需额外依赖

---

## 详细任务分解

### 阶段 1：构建配置 (3 个任务 — 1.5 小时)

---

#### 任务 1.1：将 Room、WorkManager 和其他依赖添加到版本目录
- **文件**：`gradle/libs.versions.toml`
- **操作**：添加版本条目和库条目
- **预估**：20 分钟
- **优先级**：高
- **验收标准**：
  - 版本目录包含以下条目：`room-runtime`、`room-compiler`、`work-runtime`、`recyclerview`、`lifecycle-livedata`、`lifecycle-viewmodel`
  - 所有版本都引用版本变量：`room`、`work`、`lifecycle`
  - `room-compiler` 被定义为库条目（将使用 `annotationProcessor` 配置）
  - 不包含 `cardview`（Material 库已包含）
  - 不包含 `lifecycle-process`（v1 不需要）
- **依赖**：无
- **具体更改**：
  ```toml
  # 在 [versions] 中添加：
  room = "2.6.1"
  work = "2.9.1"
  recyclerview = "1.3.2"
  lifecycle = "2.7.0"

  # 在 [libraries] 中添加：
  room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
  room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
  work-runtime = { group = "androidx.work", name = "work-runtime", version.ref = "work" }
  recyclerview = { group = "androidx.recyclerview", name = "recyclerview", version.ref = "recyclerview" }
  lifecycle-livedata = { group = "androidx.lifecycle", name = "lifecycle-livedata", version.ref = "lifecycle" }
  lifecycle-viewmodel = { group = "androidx.lifecycle", name = "lifecycle-viewmodel", version.ref = "lifecycle" }
  ```

---

#### 任务 1.2：将依赖和注解处理器配置添加到 app build.gradle.kts
- **文件**：`app/build.gradle.kts`
- **操作**：将所有依赖添加到 dependencies 块中，添加 annotationProcessor 用于 Room 编译器
- **预估**：20 分钟
- **优先级**：高
- **验收标准**：
  - `dependencies` 块包含所有新的 `libs.xxx` 引用
  - `annotationProcessor(libs.room.compiler)` 包含在 dependencies 中
  - 不包含 `cardview` 依赖
  - 不包含 `lifecycle-process` 依赖
- **依赖**：任务 1.1
- **具体更改**：
  ```kotlin
  dependencies {
      implementation(libs.appcompat)
      implementation(libs.material)
      implementation(libs.room.runtime)
      annotationProcessor(libs.room.compiler)
      implementation(libs.work.runtime)
      implementation(libs.recyclerview)
      implementation(libs.lifecycle.livedata)
      implementation(libs.lifecycle.viewmodel)
      testImplementation(libs.junit)
      androidTestImplementation(libs.ext.junit)
      androidTestImplementation(libs.espresso.core)
  }
  ```

---

#### 任务 1.3：验证构建成功（含 Room 注解处理器验证）
- **文件**：`app/src/main/java/com/example/myapplication/data/entity/Item.java`（临时测试文件）
- **操作**：
  1. 创建一个最小的 `@Entity` 注解类来验证 Room annotation processor 真正工作
  2. 运行 `./gradlew assembleDebug` 确认编译通过
  3. 确认 `Item.java` 被正确处理（Room 生成了 `Item_DatabaseName` 等辅助类）
- **预估**：30 分钟
- **优先级**：高
- **验收标准**：
  - `./gradlew assembleDebug` 以 `BUILD SUCCESSFUL` 完成
  - 无依赖解析错误
  - Room annotation processor 成功处理了 `@Entity` 注解（检查 build 产物中是否有生成的类）
  - **如果此步失败**：记录具体错误，尝试降低 Room 版本到 2.5.2 后重试
- **依赖**：任务 1.2
- **最小验证实体**：
  ```java
  package com.example.myapplication.data.entity;

  import androidx.room.Entity;
  import androidx.room.PrimaryKey;

  @Entity(tableName = "items")
  public class Item {
      @PrimaryKey(autoGenerate = true)
      public int id;
  }
  ```
  > 注意：此文件在后续任务 2.1 中会被完整替换为最终版本。此步骤仅验证构建链。

---

### 阶段 2：数据层 (4 个任务 — 3.5 小时)

---

#### 任务 2.1：完善 Item 实体类
- **文件**：`app/src/main/java/com/example/myapplication/data/entity/Item.java`（替换任务 1.3 的临时版本）
- **操作**：将最小的 Entity 扩展为完整的 Room Entity，包含所有业务字段
- **预估**：45 分钟
- **优先级**：高
- **验收标准**：
  - 类使用 `@Entity(tableName = "items")` 注解
  - `id` 字段是 `@PrimaryKey(autoGenerate = true)`
  - 所有字段：`name` (String)、`category` (String, 默认 "其他")、`buyDate` (String, ISO "yyyy-MM-dd")、`expireDate` (String, ISO "yyyy-MM-dd")、`remindDays` (int, 默认 7)、`note` (String, 默认 "")、`status` (String, 默认 "正常")
  - 使用 `@ColumnInfo(defaultValue = ...)` 为有默认值的字段设置默认值
  - 日期字段使用 String 类型存储 ISO 格式（`yyyy-MM-dd`），无需 TypeConverter
  - 提供完整构造函数和 getter/setter
  - `toString()` 用于调试日志
- **依赖**：任务 1.3
- **字段定义**：
  ```java
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
  ```

---

#### 任务 2.2：创建 ItemDao 接口
- **文件**：`app/src/main/java/com/example/myapplication/data/dao/ItemDao.java`（新文件）
- **操作**：创建 Room DAO 接口，包含所有 CRUD 操作和查询方法
- **预估**：45 分钟
- **优先级**：高
- **验收标准**：
  - 接口使用 `@Dao` 注解
  - 包含以下方法：
    - `@Insert long insert(Item item)` — 插入单个商品，返回行 ID
    - `@Update void update(Item item)` — 更新现有商品
    - `@Delete void delete(Item item)` — 删除商品
    - `@Query("SELECT * FROM items ORDER BY expireDate ASC") LiveData<List<Item>> getAllItems()` — 所有商品按过期日期排序
    - `@Query("SELECT * FROM items WHERE category = :category ORDER BY expireDate ASC") LiveData<List<Item>> getItemsByCategory(String category)` — 按类别过滤
    - `@Query("SELECT * FROM items WHERE name LIKE '%' || :query || '%' ORDER BY expireDate ASC") LiveData<List<Item>> searchItems(String query)` — 按名称搜索
    - `@Query("SELECT * FROM items WHERE name LIKE '%' || :query || '%' AND category = :category ORDER BY expireDate ASC") LiveData<List<Item>> searchItemsByCategory(String query, String category)` — **搜索+类别组合查询**
    - `@Query("SELECT * FROM items WHERE status != '已使用' AND expireDate <= :dateStr") List<Item> getExpiringItems(String dateStr)` — 用于 WorkManager 的同步查询
    - `@Query("SELECT * FROM items WHERE id = :id") LiveData<Item> getItemById(int id)` — 单个商品
    - `@Query("DELETE FROM items WHERE id = :id") void deleteById(int id)` — 按 ID 删除
    - `@Query("UPDATE items SET status = :status WHERE id = :id") void updateStatus(int id, String status)` — 更新状态
  - 所有 SQL 关键字大写，表名/列名与 Entity 定义匹配
- **依赖**：任务 2.1

---

#### 任务 2.3：创建 AppDatabase 类
- **文件**：`app/src/main/java/com/example/myapplication/data/AppDatabase.java`（新文件）
- **操作**：创建单例 RoomDatabase 类
- **预估**：1 小时
- **优先级**：高
- **验收标准**：
  - 类继承 `RoomDatabase` 并使用 `@Database(entities = {Item.class}, version = 1, exportSchema = false)` 注解
  - 抽象方法 `ItemDao itemDao()`
  - 带有 `synchronized` 块的线程安全单例模式 `getInstance(Context)` 方法
  - 使用 `Room.databaseBuilder()` 及应用上下文构建
  - 数据库名称：`"expiry_tracker_db"`
  - 包含 `.fallbackToDestructiveMigration()`（简单 v1）
  - 不需要 `@TypeConverters`（日期存储为 String）
- **依赖**：任务 2.2

---

#### 任务 2.4：创建 ItemRepository 类
- **文件**：`app/src/main/java/com/example/myapplication/data/ItemRepository.java`（新文件）
- **操作**：创建仓库类，封装 DAO 方法并提供干净的 API
- **预估**：1 小时
- **优先级**：高
- **验收标准**：
  - 接受 `Application` 上下文的单例（通过 `AppDatabase.getInstance(context)` 获取 DAO）
  - 数据库写入使用 `ExecutorService`（Room 不允许主线程写入）
  - 使用 `Executors.newSingleThreadExecutor()`（简单项目，单线程足够）
  - 提供以下方法：
    - `LiveData<List<Item>> getAllItems()` — 直接返回 DAO 的 LiveData
    - `LiveData<List<Item>> getItemsByCategory(String category)` — 直接返回
    - `LiveData<List<Item>> searchItems(String query)` — 直接返回
    - `LiveData<List<Item>> searchItemsByCategory(String query, String category)` — 组合查询，直接返回
    - `void insert(Item item)` — 通过 executor 异步执行
    - `void update(Item item)` — 通过 executor 异步执行
    - `void delete(Item item)` — 通过 executor 异步执行
    - `void deleteById(int id)` — 通过 executor 异步执行
    - `void updateStatus(int id, String status)` — 通过 executor 异步执行
    - `List<Item> getExpiringItems(String dateStr)` — 同步调用（WorkManager 在自己的线程调用）
- **依赖**：任务 2.3

---

### 阶段 3：工具类 (2 个任务 — 1 小时)

---

#### 任务 3.1：创建 DateUtils 工具类
- **文件**：`app/src/main/java/com/example/myapplication/util/DateUtils.java`（新文件）
- **操作**：创建日期计算和格式化的静态工具方法
- **预估**：45 分钟
- **优先级**：高
- **验收标准**：
  - 所有方法都是 `static` 的
  - 使用 `java.util.Calendar` 和 `java.text.SimpleDateFormat`（**不使用** `java.time`，避免 API 24 兼容性问题）
  - `getDaysRemaining(String expireDateStr)` → 从今天到过期日期的天数
    - 使用 `Calendar` 实例比较，去掉时分秒只比较日期
    - 返回正数表示剩余天数，负数表示已过期天数
  - `getStatus(String expireDateStr, int remindDays)` → 返回状态字符串
    - 如果剩余天数 < 0：返回 `"已过期"`
    - 如果剩余天数 <= remindDays：返回 `"即将过期"`
    - 否则：返回 `"正常"`
  - `formatDate(String isoDate)` → 将 `"2026-05-20"` 格式化为 `"2026年05月20日"` 供显示
  - `getCurrentDateStr()` → 返回今天日期的 ISO 字符串 (`"yyyy-MM-dd"`)
  - `isValidDateRange(String buyDate, String expireDate)` → 如果 expireDate 不在 buyDate 之前返回 true
  - `calculateNotificationMessage(String itemName, int daysRemaining)` → 根据剩余天数返回通知消息
    - daysRemaining < 0：`"{itemName}已过期{abs}天，请检查是否需要丢弃"`
    - daysRemaining == 0：`"{itemName}今天过期，请尽快处理"`
    - daysRemaining == 1：`"{itemName}明天过期，请尽快使用"`
    - daysRemaining <= 7：`"{itemName}还有{daysRemaining}天过期"`
    - 其他：`"{itemName}即将过期，请注意"`
  - `utcMillisToDateString(long millis)` → 将 `MaterialDatePicker` 返回的 UTC 毫秒转换为本地日期的 ISO 字符串
    - 使用 `Calendar` + `TimeZone.getDefault()` 进行转换
- **依赖**：无（纯 Java，不依赖 Android/Room）

---

#### 任务 3.2：创建常量类
- **文件**：`app/src/main/java/com/example/myapplication/util/Constants.java`（新文件）
- **操作**：定义整个应用中使用的应用范围常量
- **预估**：15 分钟
- **优先级**：中
- **验收标准**：
  - `String[] CATEGORIES = {"食品", "药品", "护肤品", "日用品", "其他"}`
  - `String CHANNEL_ID = "expiry_reminder_channel"`
  - `String CHANNEL_NAME = "过期提醒"`
  - `String CHANNEL_DESCRIPTION = "商品过期提醒通知"`
  - `int NOTIFICATION_ID_BASE = 1000`
  - `String STATUS_NORMAL = "正常"`
  - `String STATUS_EXPIRING = "即将过期"`
  - `String STATUS_EXPIRED = "已过期"`
  - `String STATUS_USED = "已使用"`
  - `String PREFS_NAME = "expiry_tracker_prefs"`
  - `String KEY_WORK_INITIALIZED = "work_initialized"`
- **依赖**：无

---

### 阶段 4：通知和后台工作 (3 个任务 — 3 小时)

---

#### 任务 4.1：创建 NotificationHelper 工具类
- **文件**：`app/src/main/java/com/example/myapplication/util/NotificationHelper.java`（新文件）
- **操作**：创建辅助类，用于设置 NotificationChannel 并构建通知
- **预估**：1 小时
- **优先级**：高
- **验收标准**：
  - `static void createNotificationChannel(Context)` — 如果 Android O+ (API 26+) 创建高重要性 NotificationChannel。minSdk 24 需要此检查
  - `static void showExpiryNotification(Context context, Item item, int daysRemaining)` — 构建并显示通知：
    - 使用 `NotificationCompat.Builder`
    - 设置小图标 `android.R.drawable.ic_dialog_alert`
    - 标题：`"随手记保质期"`
    - 正文：使用 `DateUtils.calculateNotificationMessage(item.getName(), daysRemaining)`
    - 优先级 `PRIORITY_HIGH` / `PRIORITY_DEFAULT`
    - 自动取消 `setAutoCancel(true)`
    - 唯一通知 ID：`Constants.NOTIFICATION_ID_BASE + item.getId()`
  - 使用 `NotificationManagerCompat.from(context).notify()` 显示通知
  - **注意**：不在这里检查 `POST_NOTIFICATIONS` 权限，权限检查在 MainActivity 中完成
- **依赖**：任务 2.1, 任务 3.1, 任务 3.2

---

#### 任务 4.2：创建 ExpiryCheckWorker 类
- **文件**：`app/src/main/java/com/example/myapplication/worker/ExpiryCheckWorker.java`（新文件）
- **操作**：创建 WorkManager Worker，定期检查过期商品并发送通知
- **预估**：1 小时
- **优先级**：高
- **验收标准**：
  - 类继承 `Worker`（非 CoroutineWorker，纯 Java 项目）
  - 构造函数接受 `Context` 和 `WorkerParameters`
  - `doWork()` 返回 `Result.success()` 或 `Result.failure()`
  - 在 `doWork()` 中：
    1. 获取 `AppDatabase` 和 `ItemDao` 实例
    2. 调用 `DateUtils.getCurrentDateStr()` 获取当前日期
    3. 同步查询所有状态不是"已使用"且过期日期 <= 当前日期 + remindDays 最大值的商品
       - 实际实现：查询所有 `status != '已使用'` 的商品，然后在 Java 代码中过滤
    4. 对每个商品：
       - 计算 `daysRemaining = DateUtils.getDaysRemaining(item.getExpireDate())`
       - 如果 `daysRemaining < 0`（已过期）：
         - 更新状态为 `"已过期"`
         - 调用 `NotificationHelper.showExpiryNotification()`
       - 如果 `0 <= daysRemaining && daysRemaining <= item.getRemindDays()`（即将过期）：
         - 更新状态为 `"即将过期"`
         - 调用 `NotificationHelper.showExpiryNotification()`
    5. 返回 `Result.success()`
  - 用 try-catch 包裹所有逻辑，异常时返回 `Result.failure()`
  - **不**在 Worker 中修改状态为"正常"（状态只在用户交互时重置为正常不合理）
- **依赖**：任务 2.4, 任务 4.1

---

#### 任务 4.3：创建 ExpiryTrackerApplication 类
- **文件**：`app/src/main/java/com/example/myapplication/ExpiryTrackerApplication.java`（新文件）
- **操作**：创建 Application 子类，在启动时初始化 NotificationChannel 和 WorkManager 周期性任务
- **预估**：30 分钟
- **优先级**：高
- **验收标准**：
  - 类继承 `android.app.Application`
  - `onCreate()` 调用：
    1. `NotificationHelper.createNotificationChannel(this)` — 提前创建通道
    2. `schedulePeriodicWork()` — 私有方法调度周期性工作
  - `schedulePeriodicWork()` 实现：
    - 使用 `SharedPreferences` 检查 `KEY_WORK_INITIALIZED` 标志，避免重复调度
    - `PeriodicWorkRequest.Builder`，`ExpiryCheckWorker.class`，每 24 小时重复一次
    - `Constraints`：`RequiresDeviceIdle=false`，`RequiresCharging=false`
    - `WorkManager.getInstance(this).enqueueUniquePeriodicWork()`，使用 `ExistingPeriodicWorkPolicy.KEEP`
    - 唯一工作名称：`"expiry_check_work"`
    - 调度成功后设置 `KEY_WORK_INITIALIZED = true`
- **依赖**：任务 4.2

---

### 阶段 5：资源文件 (7 个任务 — 3 小时)

---

#### 任务 5.1：更新字符串资源
- **文件**：`app/src/main/res/values/strings.xml`
- **操作**：添加所有中文字符串资源（替换当前只有 `app_name` 的内容）
- **预估**：30 分钟
- **优先级**：高
- **验收标准**：
  - 所有文本使用中文
  - 关键字符串（完整列表）：
    - `app_name` = "随手记保质期"
    - `home_title` = "随手记保质期"
    - `add_item_title` = "添加物品"
    - `edit_item_title` = "编辑物品"
    - `detail_title` = "物品详情"
    - `hint_item_name` = "请输入物品名称"
    - `label_category` = "分类"
    - `label_buy_date` = "购买日期"
    - `label_expire_date` = "过期日期"
    - `label_remind_days` = "提前提醒天数"
    - `label_note` = "备注"
    - `btn_save` = "保存"
    - `btn_delete` = "删除"
    - `btn_mark_used` = "标记已使用"
    - `status_normal` = "正常"
    - `status_expiring` = "即将过期"
    - `status_expired` = "已过期"
    - `status_used` = "已使用"
    - `hint_search` = "搜索物品..."
    - `label_all` = "全部"
    - `remaining_days` = "剩余 %d 天"
    - `expired_days` = "已过期 %d 天"
    - `notification_expiring` = "%s快过期了"
    - `notification_tomorrow` = "%s明天过期，请尽快使用"
    - `notification_expired` = "%s已过期，请检查是否需要丢弃"
    - `toast_saved` = "保存成功"
    - `toast_deleted` = "已删除"
    - `toast_marked_used` = "已标记为已使用"
    - `toast_updated` = "更新成功"
    - `error_name_empty` = "请输入物品名称"
    - `error_date_empty` = "请选择过期日期"
    - `content_description_add_item` = "添加新物品"
    - `empty_list_message` = "暂无物品，点击 + 添加"
    - `empty_filter_message` = "没有找到匹配的物品"
    - `dialog_delete_title` = "确认删除"
    - `dialog_delete_message` = "确定要删除这个物品吗？"
    - `dialog_confirm` = "确定"
    - `dialog_cancel` = "取消"
    - `notification_permission_rationale` = "需要通知权限才能发送过期提醒"
- **依赖**：无

---

#### 任务 5.2：更新颜色资源
- **文件**：`app/src/main/res/values/colors.xml`
- **操作**：添加状态指示器的语义颜色和主题颜色
- **预估**：15 分钟
- **优先级**：中
- **验收标准**：
  - 保留现有颜色（`black`, `white` 等）
  - 替换主题颜色：
    - `color_primary` = #4CAF50（绿色，符合"保鲜/保质"主题）
    - `color_primary_dark` = #388E3C
    - `color_primary_light` = #C8E6C9
    - `color_accent` = #FF9800（橙色强调）
  - 添加状态颜色：
    - `color_status_normal` = #4CAF50（绿色）
    - `color_status_expiring` = #FF9800（橙色）
    - `color_status_expired` = #F44336（红色）
    - `color_status_used` = #9E9E9E（灰色）
  - 添加通用颜色：
    - `color_text_primary` = #212121
    - `color_text_secondary` = #757575
    - `color_background` = #F5F5F5
- **依赖**：无

---

#### 任务 5.3：更新主题
- **文件**：`app/src/main/res/values/themes.xml`
- **操作**：更新主题颜色以匹配绿色应用品牌
- **预估**：15 分钟
- **优先级**：中
- **验收标准**：
  - `colorPrimary` 使用 `@color/color_primary`（绿色）
  - `colorPrimaryVariant` 使用 `@color/color_primary_dark`
  - `colorOnPrimary` 使用 `@color/white`
  - `colorSecondary` 使用 `@color/color_accent`（橙色）
  - 父主题仍为 `Theme.MaterialComponents.DayNight.DarkActionBar`
  - 如果存在 `values-night/themes.xml`，同步更新夜间模式颜色
- **依赖**：任务 5.2

---

#### 任务 5.4：创建主页布局
- **文件**：`app/src/main/res/layout/activity_main.xml`（新文件）
- **操作**：创建 MainActivity 布局，包含搜索栏、类别过滤器、商品列表和 FAB
- **预估**：1 小时
- **优先级**：高
- **验收标准**：
  - 根布局：`androidx.coordinatorlayout.widget.CoordinatorLayout`
  - 顶部 `AppBarLayout` + `MaterialToolbar`，标题引用 `@string/home_title`
  - 主体内容区域（`LinearLayout` 垂直）：
    - `TextInputLayout`（outlined 模式）+ `TextInputEditText` `et_search` — 搜索框，提示引用 `@string/hint_search`，带有搜索图标
    - `HorizontalScrollView` 包裹 `com.google.android.material.chip.ChipGroup` `chip_group_categories`：
      - 芯片：全部（默认选中）、食品、药品、护肤品、日用品、其他
    - `FrameLayout` 或 `ConstraintLayout` 包含：
      - `RecyclerView` `rv_items`，`layout_height="match_parent"`，`clipToPadding="false"`
      - 空状态 `TextView` `tv_empty_state`，最初 `visibility="gone"`，文本引用 `@string/empty_list_message`
  - `FloatingActionButton` `fab_add`，图标为 `@android:drawable/ic_input_add`，锚定到右下角
  - 所有 ID 使用 `snake_case` 命名
- **依赖**：任务 5.1

---

#### 任务 5.5：创建商品卡片布局
- **文件**：`app/src/main/res/layout/item_card.xml`（新文件）
- **操作**：创建 RecyclerView 的单个卡片布局
- **预估**：30 分钟
- **优先级**：高
- **验收标准**：
  - 根布局：`com.google.android.material.card.MaterialCardView`
    - `cardCornerRadius="8dp"`
    - `cardElevation="2dp"`
    - `cardUseCompatPadding="true"`
    - 水平 margin 8dp，垂直 margin 4dp
  - 卡片内容（垂直 `LinearLayout`，padding 16dp）：
    - 第 1 行：水平 `LinearLayout`
      - 商品名称 `TextView` `tv_item_name`（粗体, 16sp, `layout_weight="1"`）
      - 状态标签 `TextView` `tv_item_status`（12sp, 靠右对齐, 将在代码中设置背景色）
    - 第 2 行：类别 `TextView` `tv_item_category`（14sp, `@color/color_text_secondary`）
    - 第 3 行：过期日期 `TextView` `tv_expire_date`（14sp）
    - 第 4 行：剩余天数 `TextView` `tv_days_remaining`（14sp, 粗体, 醒目颜色将在代码中设置）
  - `MaterialCardView` 上 `android:foreground="?attr/selectableItemBackground"` 用于涟漪效果
- **依赖**：任务 5.1, 任务 5.2

---

#### 任务 5.6：创建添加/编辑物品布局
- **文件**：`app/src/main/res/layout/activity_add_item.xml`（新文件）
- **操作**：创建添加/编辑物品表单的布局（AddItemActivity 同时用于新增和编辑）
- **预估**：45 分钟
- **优先级**：高
- **验收标准**：
  - 根布局：`LinearLayout`（垂直）在 `ScrollView` 中
  - 顶部 `MaterialToolbar`，带有返回导航图标
  - 表单字段（均使用 `TextInputLayout` outlined 模式包装）：
    1. `TextInputEditText` `et_item_name` — 物品名称，提示 `@string/hint_item_name`
    2. `MaterialAutoCompleteTextView` `actv_category` — 类别选择器，放在 `TextInputLayout` 内，通过 `ArrayAdapter` 填充
    3. `TextInputEditText` `et_buy_date` — 购买日期，`focusable="false"`，`cursorVisible="false"`，点击显示日期选择器
    4. `TextInputEditText` `et_expire_date` — 过期日期，`focusable="false"`，`cursorVisible="false"`，点击显示日期选择器
    5. `TextInputEditText` `et_remind_days` — 提醒天数，`inputType="number"`，默认 "7"
    6. `TextInputEditText` `et_note` — 备注，`inputType="textMultiLine"`，`maxLength="200"`，`minLines="2"`
  - 底部 `MaterialButton` `btn_save` — 引用 `@string/btn_save`，全宽，填充样式，margin 16dp
  - 所有 `TextInputLayout` 之间 8dp 垂直间距
  - 整体 padding 16dp
- **依赖**：任务 5.1

---

#### 任务 5.7：创建详情页布局
- **文件**：`app/src/main/res/layout/activity_detail.xml`（新文件）
- **操作**：创建物品详情视图布局
- **预估**：30 分钟
- **优先级**：高
- **验收标准**：
  - 根布局：`LinearLayout`（垂直）
  - 顶部 `MaterialToolbar`，带返回导航图标
  - 滚动区域内的 `MaterialCardView`（padding 16dp）：
    - 物品名称 `TextView` `tv_detail_name`（24sp, 粗体）
    - `View` 分隔线（1dp 高，灰色，marginVertical 8dp）
    - 标签-值对（每对是一个水平 `LinearLayout`，8dp 垂直间距）：
      - 类别：`tv_label_category` + `tv_detail_category`
      - 购买日期：`tv_label_buy_date` + `tv_detail_buy_date`
      - 过期日期：`tv_label_expire_date` + `tv_detail_expire_date`
      - 剩余天数：`tv_label_remaining` + `tv_detail_days_remaining`（颜色编码）
      - 状态：`tv_label_status` + `tv_detail_status`（颜色编码）
      - 备注：`tv_label_note` + `tv_detail_note`（备注为空时 `visibility="gone"`）
  - 底部固定水平 `LinearLayout`（在 ScrollView 外部，或在底部对齐）：
    - `MaterialButton` `btn_mark_used` — 引用 `@string/btn_mark_used`，outline 样式，`layout_weight="1"`
    - `MaterialButton` `btn_delete` — 引用 `@string/btn_delete`，红色文字危险样式，`layout_weight="1"`
- **依赖**：任务 5.1, 任务 5.2

---

### 阶段 6：RecyclerView 适配器 (1 个任务 — 1.5 小时)

---

#### 任务 6.1：创建 ItemListAdapter
- **文件**：`app/src/main/java/com/example/myapplication/ui/adapter/ItemListAdapter.java`（新文件）
- **操作**：创建带有 DiffUtil 的 RecyclerView 适配器
- **预估**：1.5 小时
- **优先级**：高
- **验收标准**：
  - 类继承 `ListAdapter<Item, ItemListAdapter.ItemViewHolder>`（使用 `DiffUtil.ItemCallback`）
  - `DiffUtil.ItemCallback` 实现：
    - `areItemsTheSame()` — 比较 `item.getId()`
    - `areContentsTheSame()` — 比较 `item.equals()`（确保 Entity 正确实现 equals）
  - 内部类 `ItemViewHolder` 持有视图引用：
    - `MaterialCardView cardView`
    - `TextView tvName`, `tvCategory`, `tvExpireDate`, `tvDaysRemaining`, `tvStatus`
  - 构造函数接受 `OnItemClickListener` 接口
  - `OnItemClickListener` 接口：`void onItemClick(int itemId)`
  - `onCreateViewHolder()` 从 `item_card.xml` 膨胀
  - `onBindViewHolder()`：
    - 绑定商品数据到视图
    - 使用 `DateUtils.getDaysRemaining()` 计算剩余天数
    - 根据 `item.getStatus()` 设置状态文本和颜色
    - 根据 `daysRemaining` 设置 `tvDaysRemaining` 文本（"剩余 X 天" 或 "已过期 X 天"）和颜色
    - `cardView.setOnClickListener` → `onItemClick(item.getId())`
  - **不**使用 `notifyDataSetChanged()`，依靠 `ListAdapter` 的 `submitList()` 自动 Diff
- **依赖**：任务 2.1, 任务 3.1, 任务 5.5

---

### 阶段 7：Activity 类 (3 个任务 — 8 小时)

---

#### 任务 7.1：创建 MainActivity
- **文件**：`app/src/main/java/com/example/myapplication/ui/MainActivity.java`（新文件）
- **操作**：创建主 Activity，包含商品列表、搜索、类别过滤（**含组合过滤**）和 FAB 导航
- **预估**：3.5 小时
- **优先级**：高
- **验收标准**：
  - 类继承 `AppCompatActivity`
  - 成员变量：
    - `ItemRepository repository`
    - `ItemListAdapter adapter`
    - `ChipGroup chipGroupCategories`
    - `EditText etSearch`
    - `TextView tvEmptyState`
    - `String currentCategory = "全部"`
    - `String currentSearchQuery = ""`
    - `Handler searchHandler`
    - `Runnable searchRunnable`
  - `onCreate()`：
    1. 设置内容视图 `activity_main`
    2. 设置 `MaterialToolbar` 为 setSupportActionBar
    3. 初始化 `ItemRepository`
    4. 初始化 `RecyclerView`，使用 `LinearLayoutManager` 和 `ItemListAdapter`
    5. 设置 FAB 点击监听器 → 启动 `AddItemActivity`
    6. 设置搜索 EditText 的 `TextWatcher`：
       - 使用 Handler + Runnable 实现 500ms 防抖
       - 更新 `currentSearchQuery`
       - 调用 `applyFilter()` 方法
    7. 设置 ChipGroup 监听器：
       - 选中 "全部" 芯片 → `currentCategory = "全部"`
       - 选中特定类别 → `currentCategory = 该类别名`
       - 调用 `applyFilter()` 方法
    8. 适配器项点击监听器 → 启动 `DetailActivity`，Intent 传递 `item_id`
    9. **Android 13+ 通知权限请求**：
       - 检查 `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU`
       - 未授权则 `ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_NOTIFICATIONS)`
    10. 初始数据观察 → 调用 `applyFilter()`
  - **核心方法 `applyFilter()`**（搜索+类别组合过滤）：
    ```
    如果 currentCategory == "全部" 且 currentSearchQuery 为空：
        观察 repository.getAllItems()
    如果 currentCategory == "全部" 且 currentSearchQuery 不为空：
        观察 repository.searchItems(currentSearchQuery)
    如果 currentCategory != "全部" 且 currentSearchQuery 为空：
        观察 repository.getItemsByCategory(currentCategory)
    如果 currentCategory != "全部" 且 currentSearchQuery 不为空：
        观察 repository.searchItemsByCategory(currentSearchQuery, currentCategory)
    ```
    - 每次调用先移除前一个 LiveData 观察者（避免重复观察）
    - 观察者回调中：
      - 调用 `adapter.submitList(items)`
      - 切换空状态可见性（列表为空显示 `tvEmptyState`）
  - `onRequestPermissionsResult()`：
    - 处理通知权限结果
    - 拒绝时显示 Toast "需要通知权限才能发送过期提醒"
  - `onResume()`：无需额外刷新（LiveData 自动更新）
  - 常量：`private static final int REQUEST_CODE_NOTIFICATIONS = 100`
  - 常量：`public static final String EXTRA_ITEM_ID = "item_id"`
- **依赖**：任务 2.4, 任务 3.1, 任务 5.4, 任务 6.1

---

#### 任务 7.2：创建 AddItemActivity（支持新增/编辑模式）
- **文件**：`app/src/main/java/com/example/myapplication/ui/AddItemActivity.java`（新文件）
- **操作**：创建表单 Activity，通过 Intent extra 区分新增和编辑模式
- **预估**：2.5 小时
- **优先级**：高
- **验收标准**：
  - 类继承 `AppCompatActivity`
  - 支持两种模式：
    - **新增模式**：无 `EXTRA_ITEM_ID`，标题 "添加物品"，保存时 `repository.insert()`
    - **编辑模式**：有 `EXTRA_ITEM_ID`，标题 "编辑物品"，加载现有数据，保存时 `repository.update()`
  - 成员变量：
    - `ItemRepository repository`
    - `boolean isEditMode = false`
    - `int editItemId = -1`
    - `TextInputEditText etItemName, etBuyDate, etExpireDate, etRemindDays, etNote`
    - `MaterialAutoCompleteTextView actvCategory`
    - `MaterialDatePicker<Long> datePicker`（复用同一个变量）
  - `onCreate()`：
    1. 设置内容视图 `activity_add_item`
    2. 设置 Toolbar，带返回箭头，启用 navigateUp
    3. 检查 Intent extras：
       - 如果有 `EXTRA_ITEM_ID` → 设置 `isEditMode = true`，更新标题为 "编辑物品"
    4. 使用 `ArrayAdapter` 和 `Constants.CATEGORIES` 设置类别 AutoCompleteTextView
    5. 设置默认提醒天数 = "7"
    6. 日期字段点击监听器：
       - 使用 `MaterialDatePicker.Builder.datePicker().build()`
       - 选择后，使用 `DateUtils.utcMillisToDateString(selection)` 转换并填充
       - 需要用 `FragmentManager` 显示
    7. 保存按钮点击：
       a. 验证物品名称不为空 → Toast 错误
       b. 验证过期日期不为空 → Toast 错误
       c. 购买日期为空则默认今天
       d. 构建 `Item` 对象
       e. 使用 `DateUtils.getStatus()` 计算初始状态
       f. 如果 `isEditMode`：`repository.update(item)`，Toast "更新成功"
       g. 如果新增：`repository.insert(item)`，Toast "保存成功"
       h. `finish()` 返回
    8. 编辑模式额外逻辑：
       - 通过 `repository.getItemById(editItemId)` 观察 LiveData
       - 观察者填充所有表单字段
       - **注意**：只在首次加载时填充，避免覆盖用户编辑
  - `onSupportNavigateUp()` → `true` + `finish()`
  - 常量：`public static final String EXTRA_ITEM_ID = "item_id"`
- **依赖**：任务 2.4, 任务 3.1, 任务 5.6

---

#### 任务 7.3：创建 DetailActivity
- **文件**：`app/src/main/java/com/example/myapplication/ui/DetailActivity.java`（新文件）
- **操作**：创建详情 Activity，显示完整物品信息和操作
- **预估**：2 小时
- **优先级**：高
- **验收标准**：
  - 类继承 `AppCompatActivity`
  - `onCreate()`：
    1. 设置内容视图 `activity_detail`
    2. 设置 Toolbar，带返回箭头
    3. 从 Intent extras 获取 `item_id`（int）
    4. 通过 `repository.getItemById(itemId)` 观察 `LiveData<Item>`：
       - 观察者中更新所有 TextView
       - 使用 `DateUtils.formatDate()` 格式化日期
       - 使用 `DateUtils.getDaysRemaining()` 计算剩余天数
       - 根据状态设置颜色编码
       - 处理 item 为 null 的情况（物品在查看时被删除）→ Toast "物品不存在" + finish()
  - 删除按钮：
    1. `AlertDialog.Builder` 确认："确定要删除这个物品吗？"
    2. 确认后：`repository.deleteById(itemId)`
    3. Toast "已删除"
    4. `finish()`
  - 标记已使用按钮：
    1. `repository.updateStatus(itemId, Constants.STATUS_USED)`
    2. Toast "已标记为已使用"
    3. `finish()`
  - `onSupportNavigateUp()` → `true` + `onBackPressed()`
- **依赖**：任务 2.4, 任务 3.1, 任务 5.7

---

### 阶段 8：清单和应用程序配置 (1 个任务 — 30 分钟)

---

#### 任务 8.1：更新 AndroidManifest.xml
- **文件**：`app/src/main/AndroidManifest.xml`
- **操作**：添加所有 Activity、Application 类、权限
- **预估**：30 分钟
- **优先级**：高
- **验收标准**：
  - 添加权限：`android.permission.POST_NOTIFICATIONS`（Android 13+ / API 33 所需）
  - 更新 `<application>` 标签：
    - `android:name=".ExpiryTrackerApplication"`
  - 添加 Activity 声明：
    ```xml
    <activity
        android:name=".ui.MainActivity"
        android:exported="true">
        <intent-filter>
            <action android:name="android.intent.action.MAIN" />
            <category android:name="android.intent.category.LAUNCHER" />
        </intent-filter>
    </activity>
    <activity
        android:name=".ui.AddItemActivity"
        android:parentActivityName=".ui.MainActivity"
        android:exported="false" />
    <activity
        android:name=".ui.DetailActivity"
        android:parentActivityName=".ui.MainActivity"
        android:exported="false" />
    ```
  - MainActivity 有 MAIN/LAUNCHER intent-filter
  - AddItemActivity 和 DetailActivity 有 parentActivityName 用于向上导航
  - 保留现有的 allowBackup、dataExtractionRules、fullBackupContent、icon、label 等属性
- **依赖**：任务 4.3, 任务 7.1, 任务 7.2, 任务 7.3

---

### 阶段 9：验证 (1 个任务 — 1.5 小时)

---

#### 任务 9.1：构建、测试和验证完整应用流程
- **文件**：无（验证步骤）
- **操作**：运行完整构建、lint 和手动测试清单
- **预估**：1.5 小时
- **优先级**：高
- **验收标准**：
  - `./gradlew assembleDebug` — `BUILD SUCCESSFUL`
  - `./gradlew lint` — 无严重错误
  - `./gradlew test` — 所有单元测试通过
  - 手动测试清单：
    - [ ] 应用启动，显示空状态 "暂无物品，点击 + 添加"
    - [ ] 点击 FAB → 打开添加物品页面
    - [ ] 填写表单并保存 → 返回主页，显示新物品
    - [ ] 物品卡片显示正确的名称、类别、日期、状态
    - [ ] 点击卡片 → 打开详情页面
    - [ ] 详情页面显示所有信息
    - [ ] 删除按钮 → 确认对话框 → 物品已移除
    - [ ] 标记已使用 → 状态更改为 "已使用"
    - [ ] 搜索栏按名称过滤商品
    - [ ] 类别芯片按类别过滤
    - [ ] 搜索 + 类别过滤器组合工作正常
    - [ ] 从详情页点击返回 → 列表自动刷新（LiveData）
    - [ ] Android 13+ 设备上通知权限弹出
    - [ ] WorkManager 在过期物品上发送通知（可通过设置短间隔测试）
    - [ ] 屏幕旋转不崩溃（LiveData 自动处理）
    - [ ] 编辑模式：从详情页能跳转到编辑（如果有编辑入口）
  - **如果构建失败**：
    - 记录具体错误信息
    - 区分是编译错误、依赖问题还是资源问题
    - 修复后重新验证
- **依赖**：所有先前任务

---

## 风险评估矩阵

| 风险 | 可能性 | 影响 | 缓解措施 |
|---|---|---|---|
| AGP 9.1.0 + Room annotation processor 兼容性问题 | 高 | 高 | 任务 1.3 专门验证此组合；如果失败，降级 Room 到 2.5.2 |
| `MaterialDatePicker` 返回 UTC 毫秒导致日期偏移 | 中 | 高 | `DateUtils.utcMillisToDateString()` 方法专门处理时区转换 |
| WorkManager 周期性工作不触发 | 中 | 中 | 使用 `UniquePeriodicWork` + `KEEP` 策略；测试时用短间隔 |
| API 33+ 上未授权通知权限导致通知不显示 | 中 | 中 | MainActivity 中运行时请求权限；拒绝时 Toast 提示 |
| Room 注解处理器在 Java 11 环境下的行为差异 | 低 | 高 | `compileOptions` 已设置 Java 11；任务 1.3 验证 |

---

## 关键路径分析

```
任务 1.1 → 任务 1.2 → 任务 1.3 (构建+注解处理器验证)
    ↓
任务 2.1 (Entity) → 任务 2.2 (DAO) → 任务 2.3 (Database) → 任务 2.4 (Repository)
    ↓                                                           ↓
任务 3.1 (DateUtils) ─────────────────────────────────→ 任务 4.2 (Worker)
任务 3.2 (Constants) ────────────────────────→ 任务 4.1 (NotificationHelper)
                                                         ↓
                                                  任务 4.3 (Application)
                                                         ↓
任务 5.1-5.7 (所有布局 — 完全并行)                          ↓
    ↓                                                      ↓
任务 6.1 (适配器) ←─────────────────────────────────────┘
    ↓
任务 7.1 (MainActivity，含组合过滤+权限请求) ← 并行
任务 7.2 (AddItemActivity，含编辑模式)       ← 并行
任务 7.3 (DetailActivity)                    ← 并行
    ↓
任务 8.1 (Manifest)
    ↓
任务 9.1 (验证)
```

**最长路径**：任务 1.1 → 1.2 → 1.3 → 2.1 → 2.2 → 2.3 → 2.4 → 7.1 → 8.1 → 9.1

**可并行化**：
- **阶段 3 + 阶段 5** 可与数据层并行：工具类和布局文件不依赖 Room
- **任务 5.1-5.7**（所有布局）可完全并行创建
- **任务 7.1、7.2、7.3**（Activity）可在布局完成后并行开发

---

## 文件树摘要

### 新建文件（按创建顺序）
```
app/src/main/java/com/example/myapplication/
├── ExpiryTrackerApplication.java         (Phase 4, Task 4.3)
├── data/
│   ├── AppDatabase.java                  (Phase 2, Task 2.3)
│   ├── ItemRepository.java               (Phase 2, Task 2.4)
│   ├── entity/
│   │   └── Item.java                     (Phase 1 → Phase 2, Task 1.3 → 2.1)
│   └── dao/
│       └── ItemDao.java                  (Phase 2, Task 2.2)
├── ui/
│   ├── MainActivity.java                 (Phase 7, Task 7.1)
│   ├── AddItemActivity.java              (Phase 7, Task 7.2)
│   ├── DetailActivity.java               (Phase 7, Task 7.3)
│   └── adapter/
│       └── ItemListAdapter.java          (Phase 6, Task 6.1)
├── util/
│   ├── Constants.java                    (Phase 3, Task 3.2)
│   ├── DateUtils.java                    (Phase 3, Task 3.1)
│   └── NotificationHelper.java           (Phase 4, Task 4.1)
└── worker/
    └── ExpiryCheckWorker.java            (Phase 4, Task 4.2)

app/src/main/res/
├── layout/
│   ├── activity_main.xml                 (Phase 5, Task 5.4)
│   ├── item_card.xml                     (Phase 5, Task 5.5)
│   ├── activity_add_item.xml             (Phase 5, Task 5.6)
│   └── activity_detail.xml              (Phase 5, Task 5.7)
```

### 修改的文件
```
gradle/libs.versions.toml                 (Phase 1, Task 1.1)
app/build.gradle.kts                      (Phase 1, Task 1.2)
app/src/main/AndroidManifest.xml          (Phase 8, Task 8.1)
app/src/main/res/values/strings.xml       (Phase 5, Task 5.1)
app/src/main/res/values/colors.xml        (Phase 5, Task 5.2)
app/src/main/res/values/themes.xml        (Phase 5, Task 5.3)
```

### 删除的文件（对比原计划）
```
(无 Converters.java — v1 不需要 TypeConverter)
```

---

## 任务计数变化（原计划 vs 修复后）

| 变化 | 原计划 | 修复后 | 原因 |
|---|---|---|---|
| 总任务数 | 24 | 22 | 移除 Converters 任务、合并权限请求到 MainActivity、合并搜索+过滤到 MainActivity、合并最终验证 |
| Phase 2 任务 | 5 | 4 | 移除 Converters.java（v1 用 String 存日期，不需要） |
| Phase 7 | 搜索+过滤在 Phase 9 | 搜索+过滤在 Phase 7 | 核心功能不应延迟到最后 |
| Phase 8 | 2 个任务 | 1 个任务 | 权限请求合并到 MainActivity |
| Phase 9 | 3 个任务 | 1 个任务 | 空状态和组合过滤已在 Phase 7 完成 |
