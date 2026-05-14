@echo off
cd /d "D:\APP0\final"

echo =====================================
echo   Git Init + Push to GitHub
echo =====================================
echo.

echo [1/6] Initializing Git repository...
git init

echo [2/6] Setting default branch to main...
git checkout -b main

echo [3/6] Adding all files...
git add .

echo [4/6] Creating initial commit...
git commit -m "Initial commit: Android project"

echo [5/6] Creating GitHub repo and pushing...
gh repo create MyApplication --public --source=. --push --description "Android application project"

echo.
echo =====================================
echo   Done! Check your GitHub account.
echo =====================================
echo.
pause
