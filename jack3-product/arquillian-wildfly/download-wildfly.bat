del "wildfly-current" /S /Q
REM To install you can use: "choco install wget". See: https://community.chocolatey.org/
wget "https://github.com/wildfly/wildfly/releases/download/25.0.0.Final/wildfly-25.0.0.Final.zip"
REM To install you can use: "choco install 7zip". See: https://community.chocolatey.org/
7z x "wildfly-25.0.0.Final.zip"
move "wildfly-25.0.0.Final" "wildfly-current"
del "wildfly-25.0.0.Final.zip"
set /P name="Press enter..."