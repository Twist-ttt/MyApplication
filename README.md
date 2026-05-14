# 随手记保质期

一款 Android 商品保质期管理应用，帮助用户追踪食品、药品、化妆品等商品的保质期，避免使用过期物品。

## 主要功能

- **商品管理**：添加、编辑、删除商品信息（名称、类别、购买日期、过期日期、备注等）
- **过期提醒**：自动计算剩余天数，通过通知及时提醒即将过期的商品
- **分类筛选**：支持按类别（食品、饮品、日用品、药品、化妆品、其他）筛选商品
- **搜索功能**：支持按商品名称搜索，可结合类别筛选
- **状态标记**：自动标记正常/即将过期/已过期状态，支持手动标记已使用
- **后台检查**：通过 WorkManager 每 6 小时自动检查商品过期状态

## 使用技术

| 技术 | 说明 |
|------|------|
| **Room** | 本地 SQLite 数据库 ORM，管理商品数据持久化 |
| **WorkManager** | 后台周期性任务，定时检查过期商品并发送通知 |
| **LiveData** | 响应式数据观察，自动更新 UI |
| **Material Design** | Material Components 组件库，统一视觉风格 |
| **RecyclerView** | 高效列表展示，配合 DiffUtil 优化性能 |
| **MaterialDatePicker** | 日期选择器，支持购买/过期日期选择 |
| **ChipGroup** | 类别筛选标签组 |
| **NotificationChannel** | Android 8.0+ 通知渠道管理 |

## 项目结构

```
app/src/main/java/com/example/myapplication/
├── ExpiryTrackerApplication.java         # Application 类，初始化通知和 Worker
├── data/
│   ├── AppDatabase.java                  # Room 数据库单例
│   ├── ItemRepository.java               # 数据仓库，封装 DAO 操作
│   ├── entity/
│   │   └── Item.java                     # 商品实体类
│   └── dao/
│       └── ItemDao.java                  # 数据访问对象
├── ui/
│   ├── MainActivity.java                 # 主界面：列表 + 搜索 + 筛选
│   ├── AddItemActivity.java              # 添加/编辑商品
│   ├── DetailActivity.java               # 商品详情
│   └── adapter/
│       └── ItemListAdapter.java          # 列表适配器
├── util/
│   ├── Constants.java                    # 全局常量
│   ├── DateUtils.java                    # 日期工具类
│   └── NotificationHelper.java           # 通知工具类
└── worker/
    └── ExpiryCheckWorker.java            # 后台过期检查 Worker
```

## 运行方式

### 环境要求

- Android Studio (Iguana | 2024.2.1 或更高版本)
- JDK 11
- Android SDK：compileSdk 36, minSdk 24, targetSdk 36
- Gradle 9.3.1 + AGP 9.1.0

### 构建步骤

1. 克隆仓库：
   ```bash
   git clone https://github.com/Twist-ttt/MyApplication.git
   cd MyApplication
   ```

2. 使用 Android Studio 打开项目

3. 等待 Gradle 同步完成（首次可能需要下载依赖）

4. 连接 Android 设备或启动模拟器

5. 点击 **Run** 按钮或执行：
   ```bash
   ./gradlew assembleDebug
   ```

## 开发过程

本项目采用分阶段开发策略，共 7 次提交：

| 阶段 | 内容 | 涉及模块 |
|------|------|----------|
| 1 | 构建配置 | Gradle 版本目录、依赖管理 |
| 2 | 数据层 | Room 实体、DAO、数据库、仓库 |
| 3 | 工具类 | 日期计算、全局常量 |
| 4 | 通知与后台任务 | NotificationHelper、WorkManager Worker、Application |
| 5 | UI 资源 | 布局 XML、字符串、颜色、主题 |
| 6-7 | 核心功能实现 | Activity、适配器、Manifest |
| 8 | 验证与修复 | 编译问题修复 |

## 作者

- GitHub: [Twist-ttt](https://github.com/Twist-ttt)

## 许可证

本项目仅供学习使用。
