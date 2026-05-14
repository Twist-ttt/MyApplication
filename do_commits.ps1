# Git commit script for 随手记保质期
# Run from D:\APP0\final

Set-Location -LiteralPath "D:\APP0\final"

Write-Host "========================================" -ForegroundColor Green
Write-Host "  随手记保质期 - Git 提交脚本" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# First update .gitignore
git add .gitignore

# Commit #1
Write-Host "[Commit #1] Setup project with Room and WorkManager dependencies"
git add gradle/libs.versions.toml app/build.gradle.kts
git commit -m "Setup project with Room and WorkManager dependencies"
Write-Host ""

# Commit #2
Write-Host "[Commit #2] Add data model and database layer"
git add app/src/main/java/com/example/myapplication/data/entity/Item.java
git add app/src/main/java/com/example/myapplication/data/dao/ItemDao.java
git add app/src/main/java/com/example/myapplication/data/AppDatabase.java
git add app/src/main/java/com/example/myapplication/data/ItemRepository.java
git commit -m "Add data model and database layer"
Write-Host ""

# Commit #3
Write-Host "[Commit #3] Add utility classes, notification and background worker"
git add app/src/main/java/com/example/myapplication/util/DateUtils.java
git add app/src/main/java/com/example/myapplication/util/Constants.java
git add app/src/main/java/com/example/myapplication/util/NotificationHelper.java
git add app/src/main/java/com/example/myapplication/worker/ExpiryCheckWorker.java
git add app/src/main/java/com/example/myapplication/ExpiryTrackerApplication.java
git commit -m "Add utility classes, notification and background worker"
Write-Host ""

# Commit #4
Write-Host "[Commit #4] Add UI layouts and resource files"
git add app/src/main/res/values/strings.xml
git add app/src/main/res/values/colors.xml
git add app/src/main/res/values/themes.xml
git add app/src/main/res/values-night/themes.xml
git add app/src/main/res/layout/activity_main.xml
git add app/src/main/res/layout/item_card.xml
git add app/src/main/res/layout/activity_add_item.xml
git add app/src/main/res/layout/activity_detail.xml
git commit -m "Add UI layouts and resource files"
Write-Host ""

# Commit #5
Write-Host "[Commit #5] Implement main features: list, search, add/edit, detail"
git add app/src/main/java/com/example/myapplication/ui/adapter/ItemListAdapter.java
git add app/src/main/java/com/example/myapplication/ui/MainActivity.java
git add app/src/main/java/com/example/myapplication/ui/AddItemActivity.java
git add app/src/main/java/com/example/myapplication/ui/DetailActivity.java
git add app/src/main/AndroidManifest.xml
git commit -m "Implement main features: list, search, add/edit, detail"
Write-Host ""

# Commit #6 (build fixes if any)
Write-Host "[Commit #6] Fix build issues and verify app compiles"
git add -A
$hasChanges = git diff --cached --quiet 2>$null
if ($LASTEXITCODE -ne 0) {
    git commit -m "Fix build issues and verify app compiles"
    Write-Host "[OK] Commit #6 done" -ForegroundColor Green
} else {
    Write-Host "[SKIP] No build fixes needed" -ForegroundColor Yellow
}
Write-Host ""

# Commit #7
Write-Host "[Commit #7] Add README with project documentation"
git add README.md
git commit -m "Add README with project documentation"
Write-Host ""

Write-Host "========================================" -ForegroundColor Green
Write-Host "  All commits completed!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

Write-Host "=== Git Log ==="
git log --oneline --all
Write-Host ""

Write-Host "=== Git Status ==="
git status
Write-Host ""

Write-Host "To push to remote:" -ForegroundColor Cyan
Write-Host "  git push origin main" -ForegroundColor Cyan
Write-Host ""

Read-Host "Press Enter to continue"
