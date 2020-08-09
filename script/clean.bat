set DOMAIN="C:\programs\servers\payara5\glassfish\domains\domain1"
cd %DOMAIN%

if exist "applications" rmdir /S /Q "applications"
if not exist "applications" mkdir applications

if exist "autodeploy" rmdir /S /Q "autodeploy"
if not exist "autodeploy" mkdir autodeploy

if exist "generated" rmdir /S /Q "generated"
if not exist "generated" mkdir generated

if exist "logs" rmdir /S /Q "logs"
if not exist "logs" mkdir logs

if exist "osgi-cache" rmdir /S /Q "osgi-cache"
if not exist "osgi-cache" mkdir osgi-cache

pause;