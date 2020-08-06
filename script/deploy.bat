set ASADMIN="C:\programs\servers\payara5\bin"
set WAR="C:\programs\eclipse-workspace-kalambury\KalamburyPro\target\KalamburyPro-1.war"
cd %ASADMIN%
asadmin deploy %WAR%
pause