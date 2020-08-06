$LOGS = "C:\programs\servers\payara5\glassfish\domains\domain1\logs\server.log"
Get-content $LOGS -Tail 0 -Wait
Read-Host -Prompt "Press Enter to continue"