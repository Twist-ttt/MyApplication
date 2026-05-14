# ============================================
# Git 初始化 + GitHub 推送 一键脚本
# ============================================

Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  Git Init + Push to GitHub" -ForegroundColor Cyan
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""

# 进入项目目录
Set-Location "D:\APP0\final"
Write-Host "[1/6] 工作目录: $(Get-Location)" -ForegroundColor Green

# 检查是否已经是 git 仓库
if (Test-Path ".git") {
    Write-Host "[1/6] Git 仓库已存在，跳过初始化" -ForegroundColor Yellow
} else {
    Write-Host "[2/6] 初始化 Git 仓库..." -ForegroundColor Green
    git init
    Write-Host "      完成!" -ForegroundColor Green
}

# 设置默认分支为 main
Write-Host "[3/6] 设置默认分支为 main..." -ForegroundColor Green
git checkout -b main 2>$null

# 添加所有文件
Write-Host "[4/6] 添加文件到暂存区..." -ForegroundColor Green
git add .

# 查看将要提交的文件
Write-Host ""
Write-Host "      将要提交的文件:" -ForegroundColor DarkGray
git status --short
Write-Host ""

# 提交
Write-Host "[5/6] 创建初始提交..." -ForegroundColor Green
git commit -m "Initial commit: Android project"

# 创建 GitHub 仓库并推送
Write-Host "[6/6] 创建 GitHub 仓库并推送..." -ForegroundColor Green
gh repo create MyApplication --public --source=. --push --description "Android application project"

Write-Host ""
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host "  完成! 项目已推送到 GitHub" -ForegroundColor Green
Write-Host "=====================================" -ForegroundColor Cyan
Write-Host ""
Write-Host "按任意键退出..." -ForegroundColor DarkGray
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
