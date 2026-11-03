@echo off
setlocal
cd /d "%~dp0"

echo ======================================================================
echo                  KHOI DONG SO THU CHI QUAN BUN (BUNLEDGER)
echo ======================================================================
echo.

where java >nul 2>nul
if %ERRORLEVEL% neq 0 (
    echo [LOI] Khong tim thay Java tren he thong. Vui long cai dat JDK 17 hoac 21.
    pause
    exit /b 1
)

if not exist "target\bunledger-0.0.1-SNAPSHOT.jar" (
    echo [THONG BAO] Dang dong goi ung dung...
    call mvnw.cmd clean package -DskipTests
    if %ERRORLEVEL% neq 0 (
        echo [LOI] Dong goi that bai.
        pause
        exit /b 1
    )
)

echo Dang khoi dong ung dung BunLedger...
start "BunLedger Server" java -jar target\bunledger-0.0.1-SNAPSHOT.jar

echo Dang cho ung dung san sang...
timeout /t 5 /nobreak >nul

echo Dang mo trinh duyet tai http://localhost:8080 ...
start http://localhost:8080

echo.
echo ======================================================================
echo   BunLedger dang chay tai: http://localhost:8080
echo   De tat ung dung, dong cua so "BunLedger Server".
echo ======================================================================

