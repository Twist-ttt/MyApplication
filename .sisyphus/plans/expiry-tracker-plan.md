# 📋 开发路线图：随手记保质期 (Easy Expiry Tracker)

## Epic 总览
- **商业价值**：帮助用户追踪食品、药品、护肤品等保质期，通过过期提醒减少浪费和安全隐患
- **成功指标**：添加/查看/过滤/删除物品；过期前7天、1天和当天收到通知
- **预估时间**：5-7天（约45-55小时）
- **优先级**：高

---

## 技术架构

### 组件
| 组件 | 职责 | 技术 |
|---|---|---|
| **MainActivity** | 包含 RecyclerView 的主页商品列表 | AppCompatActivity + RecyclerView |
| **AddItemActivity** | 用于添加新商品的表单 | AppCompatActivity + DatePickerDialog |
| **DetailActivity** | 商品详情视图及操作 | AppCompatActivity |
| **Item Entity** | 映射到 SQLite 的 Room 实体 | Room @Entity |
| **ItemDao** | CRUD 数据库操作 | Room @Dao |
| **AppDatabase** | 单例数据库持有者 | RoomDatabase |
| **ItemRepository** | 数据访问的抽象层 | 自定义 Repository 模式 |
| **ExpiryCheckWorker** | 周期性过期检查 | WorkManager Worker |
| **NotificationHelper** | NotificationChannel 设置 | NotificationCompat |
| **ItemListAdapter** | RecyclerView 适配器 | RecyclerView.Adapter |
| **DateUtils** | 日期计算工具 | Java LocalDate/ChronoUnit |

### 数据流
```
用户输入 → Activity → Repository → Room DAO → SQLite
                                    ↓
                            LiveData → Adapter → RecyclerView

WorkManager (周期性) → Repository → 检查过期商品 → NotificationHelper → 系统通知
```

### 技术栈
- **Android SDK**: minSdk 24, targetSdk 36, compileSdk 36
- **语言**: 仅限 Java 11
- **UI**: XML 布局 + Material Design 组件
- **数据库**: Room (最新稳定版 2.6.x)
- **后台工作**: WorkManager 2.9.x
- **构建**: AGP 9.1.0 / Gradle 9.3.1 / 版本目录

---

## 库与框架研究

### 必需依赖

| 库 | 用途 | 版本 | 许可证 | 备注 |
|---|---|---|---|---|
| `room-runtime` | 本地 SQLite ORM | 2.6.1 | Apache 2.0 | Google 维护，稳定的 Jetpack 组件 |
| `room-compiler` | Room 的注解处理器 | 2.6.1 | Apache 2.0 | 必须与 room-runtime 版本匹配 |
| `work-runtime` | 周期性后台任务 | 2.9.1 | Apache 2.0 | 支持 minSdk 14+，推荐用于后台工作 |
| `recyclerview` | 商品列表显示 | 1.3.2 | Apache 2.0 | 核心 AndroidX 组件 |
| `cardview` | 商品卡片样式 | 1.0.0 | Apache 2.0 | Material CardView 包含在 Material 库中 |
| `lifecycle-livedata` | 响应式数据观察 | 2.7.0 | Apache 2.0 | 与 Room LiveData 返回配合工作 |
| `lifecycle-viewmodel` | ViewModel 支持 | 2.7.0 | Apache 2.0 | 配置更改期间保留数据 |
| `lifecycle-process` | 进程级别的生命周期所有者 | 2.7.0 | Apache 2.0 | 如果需要，用于 WorkManager 初始化 |
| `core-runtime` | 用于 Room 的 LiveData 转换 | 1.12.0 | Apache 2.0 | 必需传递依赖 |

### 现有依赖（已配置）
- `appcompat` 1.7.1 — 兼容性层
- `material` 1.13.0 — Material Design 组件

### 无需自定义实现
- **日期选择器**：使用 `MaterialDatePicker` (来自 Material 库)
- **日期计算**：使用 `java.time.LocalDate` 和 `java.time.temporal.ChronoUnit` (API 24+ 可用，通过脱糖)
- **通知**：使用 `NotificationManagerCompat` (来自 core 库)
- **数据库迁移**：v1 首次发布无需迁移

---

## 详细任务分解

### 阶段 1：构建配置 (3 个任务 — 1.5 小时)

---

#### 任务 1.1：将 Room、WorkManager 和其他依赖添加到版本目录
- **文件**：`gradle/libs.versions.toml`
- **操作**：添加版本条目、库条目和所需的任何插件条目
- **预估**：30 分钟
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 版本目录包含以下条目：`room-runtime`、`room-compiler`、`work-runtime`、`recyclerview`、`lifecycle-livedata`、`lifecycle-viewmodel`、`cardview`、`core`
  - 所有版本都引用一个 `room` 版本变量、一个 `work` 版本变量、一个 `lifecycle` 版本变量
  - `room-compiler` 被定义为一个 `ksp` 或 `annotationProcessor` 库条目（由于这是 Java 项目，请使用 `annotationProcessor` 配置名称）
- **依赖**：无
- **具体更改**：
  ```toml
  # 在 [versions] 中添加：
  room = "2.6.1"
  work = "2.9.1"
  recyclerview = "1.3.2"
  lifecycle = "2.7.0"
  cardview = "1.0.0"
  core = "1.12.0"

  # 在 [libraries] 中添加：
  room-runtime = { group = "androidx.room", name = "room-runtime", version.ref = "room" }
  room-compiler = { group = "androidx.room", name = "room-compiler", version.ref = "room" }
  work-runtime = { group = "androidx.work", name = "work-runtime", version.ref = "work" }
  recyclerview = { group = "androidx.recyclerview", name = "recyclerview", version.ref = "recyclerview" }
  lifecycle-livedata = { group = "androidx.lifecycle", name = "lifecycle-livedata", version.ref = "lifecycle" }
  lifecycle-viewmodel = { group = "androidx.lifecycle", name = "lifecycle-viewmodel", version.ref = "lifecycle" }
  cardview = { group = "androidx.cardview", name = "cardview", version.ref = "cardview" }
  core = { group = "androidx.core", name = "core", version.ref = "core" }
  ```

---

#### 任务 1.2：将依赖和注解处理器配置添加到 app build.gradle.kts
- **文件**：`app/build.gradle.kts`
- **操作**：将所有依赖添加到依赖块中，添加 `annotationProcessor` 用于 Room 编译器
- **预估**：30 分钟
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - `dependencies` 块包含所有新的 `libs.xxx` 引用
  - `annotationProcessor(libs.room.compiler)` 包含在 `dependencies` 中
  - 构建成功运行 `./gradlew assembleDebug`
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
      implementation(libs.cardview)
      implementation(libs.core)
      testImplementation(libs.junit)
      androidTestImplementation(libs.ext.junit)
      androidTestImplementation(libs.espresso.core)
  }
  ```

---

#### 任务 1.3：验证构建成功并下载依赖
- **文件**：无（验证步骤）
- **操作**：运行 `./gradlew assembleDebug` 以确认所有依赖都正确解析
- **预估**：30 分钟
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - `./gradlew assembleDebug` 以 `BUILD SUCCESSFUL` 完成
  - 无依赖解析错误
- **依赖**：任务 1.2

---

### 阶段 2：数据层 (5 个任务 — 4 小时)

---

#### 任务 2.1：创建 Item 实体类
- **文件**：`app/src/main/java/com/example/myapplication/data/entity/Item.java` (新文件)
- **操作**：使用 `@Entity` 注解、主键、所有列和适当的类型转换器创建 Room Entity 类
- **预估**：1 小时
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 类使用 `@Entity(tableName = "items")` 进行注解
  - `id` 字段是 `@PrimaryKey(autoGenerate = true)`
  - 所有字段与数据模型匹配：`name` (String)、`category` (String)、`buyDate` (String, ISO 格式 "yyyy-MM-dd")、`expireDate` (String, ISO 格式)、`remindDays` (int, 默认 7)、`note` (String)、`status` (String)
  - 默认构造函数和带有所有字段的构造函数
  - `@Ignore` 注解应用于任何非列辅助方法
  - 使用 `@TypeConverters` 或将日期存储为 ISO 字符串（简单项目推荐：ISO 字符串）
- **依赖**：任务 1.2

---

#### 任务 2.2：创建用于类型转换的 Converters 类
- **文件**：`app/src/main/java/com/example/myapplication/data/Converters.java` (新文件)
- **操作**：创建一个 Room TypeConverters 类，以防未来需要 `Date` ↔ `Long` 转换，尽管 v1 版本将日期存储为字符串。包含一个用于 `Date` ↔ `String` 转换的极简转换器。
- **预估**：30 分钟
- **负责人**：Android 开发人员
- **优先级**：中
- **验收标准**：
  - 类使用 `@TypeConverters` 进行注解
  - 包含 `@TypeConverter` 方法用于 `Date` ↔ `Long`（未来兼容性）
  - 目前是极简的——v1 使用 String 日期
- **依赖**：任务 2.1

---

#### 任务 2.3：创建 ItemDao 接口
- **文件**：`app/src/main/java/com/example/myapplication/data/dao/ItemDao.java` (新文件)
- **操作**：使用所有 CRUD 操作和查询方法创建 Room DAO 接口
- **预估**：1 小时
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 接口使用 `@Dao` 进行注解
  - 包含以下方法：
    - `@Insert long insert(Item item)` — 插入单个商品，返回行 ID
    - `@Update void update(Item item)` — 更新现有商品
    - `@Delete void delete(Item item)` — 删除商品
    - `@Query("SELECT * FROM items ORDER BY expireDate ASC") LiveData<List<Item>> getAllItems()` — 所有商品按过期日期排序
    - `@Query("SELECT * FROM items WHERE category = :category ORDER BY expireDate ASC") LiveData<List<Item>> getItemsByCategory(String category)` — 按类别过滤
    - `@Query("SELECT * FROM items WHERE name LIKE '%' || :query || '%' ORDER BY expireDate ASC") LiveData<List<Item>> searchItems(String query)` — 按名称搜索
    - `@Query("SELECT * FROM items WHERE status != '已使用' AND expireDate <= :dateStr") List<Item> getExpiringItems(String dateStr)` — 用于 WorkManager 的同步查询
    - `@Query("SELECT * FROM items WHERE id = :id") LiveData<Item> getItemById(int id)` — 单个商品
    - `@Query("DELETE FROM items WHERE id = :id") void deleteById(int id)` — 按 ID 删除
    - `@Query("UPDATE items SET status = :status WHERE id = :id") void updateStatus(int id, String status)` — 更新状态
- **依赖**：任务 2.1

---

#### 任务 2.4：创建 AppDatabase 类
- **文件**：`app/src/main/java/com/example/myapplication/data/AppDatabase.java` (新文件)
- **操作**：创建单例 RoomDatabase 类
- **预估**：1.5 小时
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 类继承 `RoomDatabase` 并使用 `@Database(entities = {Item.class}, version = 1, exportSchema = false)` 进行注解
  - 抽象方法 `ItemDao itemDao()`
  - 带有 `synchronized` 块的线程安全单例模式 `getInstance(Context)` 方法
  - 使用 `Room.databaseBuilder()` 及其应用上下文构建
  - 数据库名称：`"expiry_tracker_db"`
  - 包含 `.fallbackToDestructiveMigration()`（简单 v1）
- **依赖**：任务 2.2, 任务 2.3

---

#### 任务 2.5：创建 ItemRepository 类
- **文件**：`app/src/main/java/com/example/myapplication/data/ItemRepository.java` (新文件)
- **操作**：创建一个仓库类，将 DAO 方法封装为干净的 API
- **预估**：1 小时
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 将 `Application` 上下文或 `AppDatabase` 作为构造函数参数的单例
  - 将所有 DAO 方法封装为仓库方法
  - 数据库写入使用 `ExecutorService`（Room 不允许主线程写入）
  - 使用固定的 4 线程池进行单个 `ExecutorService`
  - 用于以下功能的方法：
    - `getAllItems()` → 返回 `LiveData<List<Item>>`
    - `getItemsByCategory(String)` → 返回 `LiveData<List<Item>>`
    - `searchItems(String)` → 返回 `LiveData<List<Item>>`
    - `insert(Item)` → void，异步
    - `update(Item)` → void，异步
    - `delete(Item)` → void，异步
    - `deleteById(int)` → void，异步
    - `updateStatus(int, String)` → void，异步
    - `getExpiringItems(String)` → 同步（用于 WorkManager，在自己的线程上运行）
- **依赖**：任务 2.4

---

### 阶段 3：工具类 (2 个任务 — 1.5 小时)

---

#### 任务 3.1：创建 DateUtils 工具类
- **文件**：`app/src/main/java/com/example/myapplication/util/DateUtils.java` (新文件)
- **操作**：创建日期计算和格式化的静态工具方法
- **预估**：1 小时
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 所有方法都是 `static` 的
  - `getDaysRemaining(String expireDateStr)` → 从今天到过期日期的天数（使用 `java.time.LocalDate` 和 `ChronoUnit.DAYS`）
  - `getStatus(String expireDateStr, int remindDays)` → 根据过期日期返回 "正常"/"即将过期"/"已过期"
    - 如果剩余天数 < 0："已过期"
    - 如果剩余天数 <= remindDays："即将过期"
    - 否则："正常"
  - `formatDate(String isoDate)` → 将 "2026-05-20" 格式化为 "2026年05月20日" 以供显示
  - `getCurrentDateStr()` → 将今天的日期作为 ISO 字符串返回
  - `isValidDateRange(String buyDate, String expireDate)` → 如果 expireDate 不在 buyDate 之前则返回 true
  - `calculateNotificationMessage(String itemName, int daysRemaining)` → 根据剩余天数返回相应的通知消息字符串
- **依赖**：无（纯 Java，不依赖 Android/Room）

---

#### 任务 3.2：创建常量类
- **文件**：`app/src/main/java/com/example/myapplication/util/Constants.java` (新文件)
- **操作**：定义整个应用中使用的应用范围常量
- **预估**：15 分钟
- **负责人**：Android 开发人员
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
- **文件**：`app/src/main/java/com/example/myapplication/util/NotificationHelper.java` (新文件)
- **操作**：创建一个辅助类，用于设置 NotificationChannel 并构建通知
- **预估**：1.5 小时
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - `static void createNotificationChannel(Context)` — 如果 Android O+ (API 26+)，则创建具有高重要性的 `NotificationChannel`。我们的 `minSdk` 是 24，因此需要此检查。
  - `static void showExpiryNotification(Context, Item, int daysRemaining)` — 构建并显示通知：
    - 使用 `NotificationCompat.Builder`
    - 设置小图标、标题、正文文本（使用 `DateUtils.calculateNotificationMessage`）
    - 设置优先级为 `PRIORITY_HIGH`
    - 设置自动取消为 true
    - 使用 `System.currentTimeMillis()` 的唯一通知 ID（或商品 ID + 偏移量）
  - `static void showExpiredNotification(Context, Item)` — 显示"已过期"类型通知
  - 使用 `NotificationManagerCompat.notify()` 显示通知
- **依赖**：任务 3.1, 任务 3.2

---

#### 任务 4.2：创建 ExpiryCheckWorker 类
- **文件**：`app/src/main/java/com/example/myapplication/worker/ExpiryCheckWorker.java` (新文件)
- **操作**：创建一个 WorkManager Worker，定期检查过期商品并发送通知
- **预估**：1.5 小时
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 类继承 `Worker`（非 CoroutineWorker，因为我们是纯 Java 项目）
  - 构造函数接受 `Context` 和 `WorkerParameters`
  - `doWork()` 返回 `Result.success()` 或 `Result.failure()`
  - 在 `doWork()` 中：
    1. 获取 `AppDatabase` 和 `ItemDao` 的实例
    2. 同步查询状态不是 "已使用" 的所有商品
    3. 对于每个商品，计算 `daysRemaining`
    4. 如果 `daysRemaining <= remindDays` 或 `daysRemaining < 0`：
       - 在数据库中更新商品状态
       - 调用 `NotificationHelper.showExpiryNotification()`
    5. 返回 `Result.success()`
  - 正确处理异常并返回 `Result.failure()`
- **依赖**：任务 2.5, 任务 4.1

---

#### 任务 4.3：创建 ExpiryTrackerApplication 类
- **文件**：`app/src/main/java/com/example/myapplication/ExpiryTrackerApplication.java` (新文件)
- **操作**：创建 Application 子类，用于在启动时初始化 NotificationChannel 和 WorkManager 周期性任务
- **预估**：30 分钟
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 类继承 `android.app.Application`
  - `onCreate()` 调用：
    1. `NotificationHelper.createNotificationChannel(this)` — 提前创建通道
    2. `schedulePeriodicWork()` — 私有方法用于调度周期性工作
  - `schedulePeriodicWork()` 使用：
    - `PeriodicWorkRequest.Builder`，`ExpiryCheckWorker.class`，每 24 小时重复一次
    - `Constraints`：`RequiresDeviceIdle=false`，`RequiresCharging=false`
    - `WorkManager.getInstance(this).enqueueUniquePeriodicWork()`，使用 `ExistingPeriodicWorkPolicy.KEEP`
    - 唯一工作名称：`"expiry_check_work"`
  - 使用 `SharedPreferences` 标志来避免重复调度
- **依赖**：任务 4.2

---

### 阶段 5：资源文件 (7 个任务 — 3 小时)

---

#### 任务 5.1：更新字符串资源
- **文件**：`app/src/main/res/values/strings.xml`
- **操作**：添加所有中文字符串资源
- **预估**：45 分钟
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 应用名称更改为 "随手记保质期"
  - 添加所有页面标题、按钮标签、提示文本、类别名称、状态标签、通知文本
  - 关键字符串包括：
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
    - `error_name_empty` = "请输入物品名称"
    - `error_date_empty` = "请选择过期日期"
    - `content_description_add_item` = "添加新物品"
    - `empty_list_message` = "暂无物品，点击 + 添加"
- **依赖**：无

---

#### 任务 5.2：更新颜色资源
- **文件**：`app/src/main/res/values/colors.xml`
- **操作**：添加状态指示器的语义颜色
- **预估**：15 分钟
- **负责人**：Android 开发人员
- **优先级**：中
- **验收标准**：
  - 添加状态颜色：
    - `color_status_normal` = 绿色 (#4CAF50)
    - `color_status_expiring` = 橙色/黄色 (#FF9800)
    - `color_status_expired` = 红色 (#F44336)
    - `color_status_used` = 灰色 (#9E9E9E)
  - 添加主题颜色：
    - `color_primary` = 适合食品/过期应用的绿色色调 (#4CAF50 或 #2E7D32)
    - `color_primary_dark` = 更深色调
    - `color_accent` = 强调色
- **依赖**：无

---

#### 任务 5.3：更新主题以适应应用品牌
- **文件**：`app/src/main/res/values/themes.xml`
- **操作**：更新主题颜色以匹配绿色应用品牌
- **预估**：15 分钟
- **负责人**：Android 开发人员
- **优先级**：中
- **验收标准**：
  - `colorPrimary` 使用 `@color/color_primary`（绿色）
  - `colorPrimaryVariant` 使用 `@color/color_primary_dark`
  - `colorOnPrimary` 使用 `@color/white`
  - 主题仍使用 `Theme.MaterialComponents.DayNight.DarkActionBar` 父主题
  - 同时更新 `values-night/themes.xml` 以保持一致
- **依赖**：任务 5.2

---

#### 任务 5.4：创建主页布局
- **文件**：`app/src/main/res/layout/activity_main.xml` (新文件)
- **操作**：为 MainActivity 创建布局，包含搜索栏、类别过滤器、商品列表和 FAB
- **预估**：1 小时
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 根布局：`androidx.coordinatorlayout.widget.CoordinatorLayout`
  - 顶部 `AppBarLayout` + `MaterialToolbar`，标题为 "随手记保质期"
  - 主体内容：
    - `EditText` 或 `TextInputLayout` + `TextInputEditText` 用于搜索（带有搜索图标）
    - 带有类别芯片的 `HorizontalScrollView` 或 `com.google.android.material.chip.ChipGroup`："全部", "食品", "药品", "护肤品", "日用品", "其他"
    - `androidx.recyclerview.widget.RecyclerView`，`layout_height="match_parent"`，`clipToPadding="false"`
  - `com.google.android.material.floatingactionbutton.FloatingActionButton`，图标为 "+"，锚定到右下角
  - 空状态 `TextView`（最初为 `gone`），用于显示"暂无物品，点击 + 添加"
- **依赖**：任务 5.1

---

#### 任务 5.5：创建商品卡片布局
- **文件**：`app/src/main/res/layout/item_card.xml` (新文件)
- **操作**：创建 RecyclerView 的单个卡片布局
- **预估**：45 分钟
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 根布局：`com.google.android.material.card.MaterialCardView`，带有 `cardCornerRadius`、`cardElevation`、垂直 `LinearLayout` 内的适当边距
  - 卡片内容 (垂直 `LinearLayout`)：
    - 第 1 行：水平 `LinearLayout`
      - 商品名称 `TextView` (粗体, 16sp)
      - 状态标签 `TextView`（颜色编码：绿色/橙色/红色/灰色，靠右对齐）
    - 第 2 行：类别 `TextView`，文本为 "分类：{category}" (14sp, 灰色)
    - 第 3 行：过期日期 `TextView`，文本为 "过期日期：{date}" (14sp)
    - 第 4 行：剩余天数 `TextView`，文本为 "剩余 X 天" 或 "已过期 X 天" (14sp, 醒目颜色)
  - 卡片上的 `android:foreground="?attr/selectableItemBackground"` 用于涟漪效果
  - 适当的内边距：16dp
  - 卡片外边距：8dp 水平，4dp 垂直
- **依赖**：任务 5.1, 任务 5.2

---

#### 任务 5.6：创建添加物品布局
- **文件**：`app/src/main/res/layout/activity_add_item.xml` (新文件)
- **操作**：创建添加/编辑物品表单的布局
- **预估**：1 小时
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 根布局：`LinearLayout` (垂直) 在 `ScrollView` 中
  - 带有返回箭头和标题 "添加物品" 的 `MaterialToolbar`
  - 表单字段（均使用 `TextInputLayout` 包装）：
    1. `TextInputEditText` `et_item_name` — 物品名称，提示 "请输入物品名称"
    2. `Spinner` 或 `MaterialAutoCompleteTextView` `spinner_category` — 类别选择器，带有 5 个选项
    3. `TextInputEditText` `et_buy_date` — 购买日期，可聚焦于 `false`，点击时显示日期选择器，提示 "购买日期"
    4. `TextInputEditText` `et_expire_date` — 过期日期，可聚焦于 `false`，点击时显示日期选择器，提示 "过期日期"
    5. `TextInputEditText` `et_remind_days` — 提醒天数，输入类型为 `number`，默认 "7"
    6. `TextInputEditText` `et_note` — 备注，多行，最多 200 个字符
  - 底部固定 `MaterialButton` `btn_save` — "保存"，全宽，填充样式
  - 所有字段都有 16dp 的内边距
  - 带有 16dp 垂直边距的垂直间距
- **依赖**：任务 5.1

---

#### 任务 5.7：创建详情页布局
- **文件**：`app/src/main/res/layout/activity_detail.xml` (新文件)
- **操作**：创建物品详情视图布局
- **预估**：45 分钟
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 根布局：`LinearLayout` (垂直)
  - 带有返回箭头和标题 "物品详情" 的 `MaterialToolbar`
  - 滚动区域内的 `MaterialCardView`：
    - 物品名称 `TextView` (24sp, 粗体)
    - 分隔线
    - 类别行：标签 + 值 `TextView`
    - 购买日期行：标签 + 值 `TextView`
    - 过期日期行：标签 + 值 `TextView`
    - 剩余天数行：标签 + 值 `TextView`（颜色编码）
    - 状态行：标签 + 状态标签 `TextView`（颜色编码）
    - 备注行：标签 + 值 `TextView`（如果备注为空则隐藏）
  - 底部固定 `LinearLayout` (水平)，带有两个按钮：
    - `MaterialButton` `btn_mark_used` — "标记已使用"，轮廓样式
    - `MaterialButton` `btn_delete` — "删除"，红底文本危险样式
  - 详情卡片的内边距：16dp
  - 标签值对之间使用 8dp 的垂直间距
- **依赖**：任务 5.1, 任务 5.2

---

### 阶段 6：RecyclerView 适配器 (1 个任务 — 1.5 小时)

---

#### 任务 6.1：创建 ItemListAdapter
- **文件**：`app/src/main/java/com/example/myapplication/ui/adapter/ItemListAdapter.java` (新文件)
- **操作**：创建带有内部 ViewHolder 的 RecyclerView 适配器
- **预估**：1.5 小时
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 类继承 `RecyclerView.Adapter<ItemListAdapter.ItemViewHolder>`
  - 内部类 `ItemViewHolder` 持有视图引用：
    - `tvName` (商品名称)
    - `tvCategory` (类别)
    - `tvExpireDate` (过期日期)
    - `tvDaysRemaining` (剩余天数)
    - `tvStatus` (状态标签)
  - 构造函数接受 `List<Item>` 数据和 `OnItemClickListener` 接口
  - `OnItemClickListener` 接口带有 `void onItemClick(int itemId)` 方法
  - `onCreateViewHolder()` 从 `item_card.xml` 膨胀
  - `onBindViewHolder()`：
    - 将商品数据绑定到视图
    - 使用 `DateUtils` 计算剩余天数
    - 根据当前状态为状态 `TextView` 设置文本和颜色
    - 根据剩余天数为 `tvDaysRemaining` 设置适当的文本（"剩余 X 天" 或 "已过期 X 天"）
    - `cardView` 上的点击监听器调用 `onItemClick(item.getId())`
  - `setItems(List<Item>)` 方法，使用 `DiffUtil` 或 `notifyDataSetChanged()` 更新列表
  - `getItemCount()` 返回列表大小
- **依赖**：任务 2.1, 任务 3.1, 任务 5.5

---

### 阶段 7：Activity 类 (3 个任务 — 8 小时)

---

#### 任务 7.1：创建 MainActivity
- **文件**：`app/src/main/java/com/example/myapplication/ui/MainActivity.java` (新文件)
- **操作**：创建包含商品列表、搜索、类别过滤和 FAB 导航的主 Activity
- **预估**：3.5 小时
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 类继承 `AppCompatActivity`
  - `onCreate()`：
    1. 设置内容视图 `activity_main`
    2. 设置 `MaterialToolbar` 的工具栏
    3. 初始化 `RecyclerView`，使用 `LinearLayoutManager` 和 `ItemListAdapter`
    4. 通过 `ItemRepository` 观察 `getAllItems()` `LiveData` — 观察者更新适配器并切换空状态可见性
    5. 设置 `FAB` 点击监听器 → 启动 `AddItemActivity`
    6. 设置搜索 `EditText`，使用 `TextWatcher` → 使用搜索查询调用 `repository.searchItems()`
    7. 设置类别 `ChipGroup`，使用 `OnCheckedChangeListener` → 调用 `repository.getItemsByCategory()` 或 `getAllItems()`
    8. 适配器项点击监听器 → 启动 `DetailActivity`，在 `intent` 中传递商品 `ID`
  - 搜索逻辑：
    - 搜索 `EditText` 带有 `TextWatcher`，在输入 500 毫秒后进行去抖（使用 `Handler` 或 `Runnable`）
    - 空搜索显示所有商品
  - 类别过滤器逻辑：
    - "全部" 芯片 = 无过滤器，显示所有商品
    - 特定类别 = 调用 `getItemsByCategory()`
    - 搜索和类别过滤器组合在一起
  - 从 `AddItemActivity` / `DetailActivity` 返回时刷新
  - `onResume()` 刷新列表（如果返回时数据可能发生变化）
- **依赖**：任务 2.5, 任务 5.4, 任务 6.1

---

#### 任务 7.2：创建 AddItemActivity
- **文件**：`app/src/main/java/com/example/myapplication/ui/AddItemActivity.java` (新文件)
- **操作**：创建带有表单验证、日期选择器和数据库插入的添加物品表单 Activity
- **预估**：2.5 小时
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 类继承 `AppCompatActivity`
  - `onCreate()`：
    1. 设置内容视图 `activity_add_item`
    2. 带有返回箭头的工具栏，启用 `Navigate Up`
    3. 使用 `ArrayAdapter` 和 `CATEGORIES` 数组设置 `category Spinner`
    4. 设置默认提醒天数 = 7
    5. 日期 `EditText` 字段上的日期选择器点击监听器：
       - 使用 `MaterialDatePicker.Builder.datePicker()`
       - 选择时，格式化并填充 `EditText`
    6. 保存按钮点击监听器：
       a. 验证物品名称不为空 → 显示 `Toast` 错误
       b. 验证过期日期不为空 → 显示 `Toast` 错误
       c. 如果购买日期为空，则默认为今天
       d. 创建新的 `Item` 对象，包含表单值
       e. 使用 `DateUtils.getStatus()` 计算初始状态
       f. 调用 `repository.insert(item)`
       g. 显示 `Toast` "保存成功"
       h. `finish()` 返回到 `MainActivity`
  - `onSupportNavigateUp()` 返回 `true` 并调用 `finish()`
  - 所有日期选择器都使用 `MaterialDatePicker` 以保持一致性
- **依赖**：任务 2.5, 任务 3.1, 任务 5.6

---

#### 任务 7.3：创建 DetailActivity
- **文件**：`app/src/main/java/com/example/myapplication/ui/DetailActivity.java` (新文件)
- **操作**：创建显示完整物品信息和操作的详情 Activity
- **预估**：2 小时
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 类继承 `AppCompatActivity`
  - `onCreate()`：
    1. 设置内容视图 `activity_detail`
    2. 带有返回箭头的工具栏，启用 `Navigate Up`
    3. 从 `Intent extras` 获取商品 `ID` (`int`)
    4. 使用 `itemDao.getItemById(id)` 观察 `LiveData<Item>` — 观察者更新所有 `TextView`
    5. 在观察者中：使用物品数据填充所有字段，使用 `DateUtils` 计算并显示剩余天数，使用状态颜色更新状态标签
  - 删除按钮点击：
    1. 显示 `AlertDialog` 确认："确定要删除吗？"
    2. 确认后：调用 `repository.deleteById(itemId)`
    3. 显示 `Toast` "已删除"
    4. `finish()` 返回
  - 标记已使用按钮点击：
    1. 调用 `repository.updateStatus(itemId, "已使用")`
    2. 显示 `Toast` "已标记为已使用"
    3. `finish()` 返回
  - `onSupportNavigateUp()` 返回 `true` 并调用 `onBackPressed()`
  - 优雅处理商品为 `null` 的情况（例如，物品在查看时被删除）
- **依赖**：任务 2.5, 任务 3.1, 任务 5.7

---

### 阶段 8：清单和应用程序配置 (2 个任务 — 1 小时)

---

#### 任务 8.1：更新 AndroidManifest.xml
- **文件**：`app/src/main/AndroidManifest.xml`
- **操作**：添加所有 Activity、Application 类、权限
- **预估**：30 分钟
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 添加权限：`android.permission.POST_NOTIFICATIONS`（Android 13+ / API 33 所需）
  - 在清单根部 `<manifest>` 标签内添加：
    ```xml
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    ```
  - 更新 `<application>` 标签：
    - `android:name=".ExpiryTrackerApplication"`（添加 Application 类引用）
  - 添加 `<activity>` 声明：
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
  - `MainActivity` 具有带有 `MAIN`/`LAUNCHER` 的 `intent-filter`
  - `AddItemActivity` 和 `DetailActivity` 具有用于向上导航的 `parentActivityName`
- **依赖**：任务 4.3, 任务 7.1, 任务 7.2, 任务 7.3

---

#### 任务 8.2：添加 Android 13+ 运行时通知权限请求
- **文件**：`app/src/main/java/com/example/myapplication/ui/MainActivity.java` (修改)
- **操作**：在 `MainActivity` 中添加运行时权限请求，用于 `POST_NOTIFICATIONS`（Android 13 / API 33 需要）
- **预估**：30 分钟
- **负责人**：Android 开发人员
- **优先级**：高
- **验收标准**：
  - 在 `onCreate()` 中，检查 `Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU`
  - 如果是，检查 `ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)`
  - 如果未授予权限，则调用 `ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_NOTIFICATIONS)`
  - `onRequestPermissionsResult()` 回调处理结果：
    - 如果已授予权限，则继续
    - 如果拒绝，则显示 `Toast` 解释通知将不起作用
  - 定义 `private static final int REQUEST_CODE_NOTIFICATIONS = 100`
- **依赖**：任务 7.1, 任务 8.1

---

### 阶段 9：完善和验证 (3 个任务 — 2.5 小时)

---

#### 任务 9.1：添加搜索和类别过滤组合逻辑
- **文件**：`app/src/main/java/com/example/myapplication/ui/MainActivity.java` (修改)
- **操作**：确保搜索文本和类别过滤器可以无缝协同工作
- **预估**：1 小时
- **负责人**：Android 开发人员
- **优先级**：中
- **验收标准**：
  - 当搜索文本和类别过滤器都激活时，列表显示匹配两者的商品
  - 实现方法：在应用过滤器之前，在内存中的 `LiveData` 观察器回调中过滤 `List<Item>`；或者如果 `DAO` 支持组合查询，则创建一个组合 `DAO` 查询
  - 推荐方法：观察所有商品 `LiveData`，然后在传递给适配器之前应用内存过滤器（更简单，适合商品数量较少的情况）
  - 空搜索 + "全部" 类别 = 显示所有商品
  - 搜索文本 + 特定类别 = 都过滤
  - 搜索文本 + "全部" = 仅搜索过滤
- **依赖**：任务 7.1

---

#### 任务 9.2：添加空状态处理
- **文件**：`app/src/main/java/com/example/myapplication/ui/MainActivity.java` (修改)
- **操作**：当列表为空时显示空状态消息
- **预估**：30 分钟
- **负责人**：Android 开发人员
- **优先级**：中
- **验收标准**：
  - 当适配器列表为空时，显示 `empty_list_message` 的 `TextView` 可见
  - 当适配器列表有商品时，空状态 `TextView` 为 `gone`
  - 过滤后无结果也显示不同的消息："没有找到匹配的物品"
- **依赖**：任务 7.1

---

#### 任务 9.3：构建、测试和验证完整应用流程
- **文件**：无（验证步骤）
- **操作**：运行完整构建、lint 和手动测试清单
- **预估**：1 小时
- **负责人**：Android 开发人员 / QA
- **优先级**：高
- **验收标准**：
  - `./gradlew assembleDebug` — `BUILD SUCCESSFUL`
  - `./gradlew lint` — 无严重错误
  - `./gradlew test` — 所有单元测试通过
  - 手动测试清单：
    - [ ] 应用启动，显示空状态
    - [ ] 点击 FAB → 打开添加物品页面
    - [ ] 填写表单并保存 → 返回主页，显示新物品
    - [ ] 物品卡片显示正确的名称、类别、日期、状态
    - [ ] 点击卡片 → 打开详情页面
    - [ ] 详情页面显示所有信息
    - [ ] 删除按钮 → 确认对话框 → 物品已移除
    - [ ] 标记已使用 → 状态更改为 "已使用"
    - [ ] 搜索栏按名称过滤商品
    - [ ] 类别芯片按类别过滤
    - [ ] 搜索和类别过滤器组合使用
    - [ ] WorkManager 在过期物品上发送通知（可通过更改设备日期或为测试设置 15 分钟间隔进行测试）
    - [ ] 返回导航正常工作
    - [ ] 屏幕旋转不会崩溃（Room `LiveData` 自动处理）
- **依赖**：所有先前任务

---

## 风险评估矩阵

| 风险 | 可能性 | 影响 | 缓解措施 | 负责人 |
|---|---|---|---|---|
| AGP 9.1.0 兼容性问题（非常新的版本，文档/示例较少） | 高 | 高 | 仔细使用新的 `compileSdk` 块语法；在添加功能之前测试基本构建；保留 `AGENTS.md` 作为参考 | Android 开发人员 |
| Room 注解处理器无法与 Java 11 + AGP 9.1.0 正确配合工作 | 中 | 高 | 在添加所有代码之前尽早验证构建（任务 1.3）；检查 Room 版本兼容性；如果失败，则回退到 Room 2.5.x | Android 开发人员 |
| 在 API 33+ 上未授予权限时通知不显示 | 中 | 中 | 尽早实现运行时权限请求（任务 8.2）；在未授予权限时显示解释；优雅降级 | Android 开发人员 |
| WorkManager 周期性工作不触发或过于频繁 | 中 | 中 | 使用 `UniquePeriodicWork` 并使用 `KEEP` 策略；通过 `WorkManager` 诊断日志验证；测试时使用 `TestWorkerBuilder` | Android 开发人员 |
| `java.time` 在所有 minSdk 24 设备上不可用（需要脱糖） | 低 | 高 | 要么在 `coreLibraryDesugaring` 中启用，要么改用 `java.util.Calendar`。推荐：添加 `coreLibraryDesugaring` 依赖 | Android 开发人员 |
| Material DatePicker 在某些设备配置上崩溃 | 低 | 中 | 带有 `try-catch` 保护；使用 `DatePickerDialog` 作为后备 | Android 开发人员 |
| 大型物品列表导致卡顿（目前不太可能） | 低 | 低 | 使用 `DiffUtil` 更新；`RecyclerView` 仅在有意义时才考虑分页 | Android 开发人员 |

---

## 资源需求
- **开发时间**：总计约 45-55 小时
  - 构建配置：1.5 小时
  - 数据层：4 小时
  - 工具类：1.5 小时
  - 通知和工作：3 小时
  - 资源文件：3 小时
  - 适配器：1.5 小时
  - Activity：8 小时
  - 清单和权限：1 小时
  - 完善和验证：2.5 小时
  - 缓冲（20%）：约 10 小时
- **所需技能**：Android Java 开发、Room 数据库、WorkManager、Material Design 组件
- **外部依赖**：Android SDK，Gradle 9.3.1
- **测试要求**：手动测试 Android 8+ 设备上的完整用户流程

---

## 关键路径分析

```
任务 1.1 → 任务 1.2 → 任务 1.3 (构建验证)
    ↓
任务 2.1 (Entity) → 任务 2.3 (DAO) → 任务 2.4 (Database) → 任务 2.5 (Repository)
    ↓                                                           ↓
任务 3.1 (DateUtils)                                   任务 4.2 (Worker)
任务 3.2 (Constants)                                          ↓
任务 4.1 (NotificationHelper) → 任务 4.3 (Application)
    ↓
任务 5.1-5.7 (所有布局 — 可并行处理)
    ↓
任务 6.1 (适配器)
    ↓
任务 7.1 (MainActivity) → 任务 8.2 (权限)
任务 7.2 (AddItemActivity)
任务 7.3 (DetailActivity)
    ↓
任务 8.1 (清单)
    ↓
任务 9.1-9.3 (完善和验证)
```

**最长路径**：任务 1.1 → 1.2 → 1.3 → 2.1 → 2.3 → 2.4 → 2.5 → 7.1 → 8.2 → 9.3

**可并行化**：
- 任务 3.1 + 3.2（工具类）可以在数据层工作期间并行运行
- 任务 5.1-5.7（布局）可以由不同开发人员或使用 AI 工具并行创建
- 任务 7.1、7.2、7.3（Activity）可以在布局完成后并行开发

---

## 文件树摘要

### 新建文件（按创建顺序）
```
app/src/main/java/com/example/myapplication/
├── ExpiryTrackerApplication.java         (Phase 4, Task 4.3)
├── data/
│   ├── AppDatabase.java                  (Phase 2, Task 2.4)
│   ├── Converters.java                   (Phase 2, Task 2.2)
│   ├── ItemRepository.java               (Phase 2, Task 2.5)
│   ├── entity/
│   │   └── Item.java                     (Phase 2, Task 2.1)
│   └── dao/
│       └── ItemDao.java                  (Phase 2, Task 2.3)
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
