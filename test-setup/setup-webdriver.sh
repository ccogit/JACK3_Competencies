# Check if geckodriver was cached before
echo "e612450084cdfd72da3a1eae0b3ffd8494d57b69a0e46bb61739ceb98a1e04ce  geckodriver" | sha256sum -c &>/dev/null
if [ $? -eq 0 ]; 
	then 
		echo "Using cached geckodriver"
	else
		echo "Downloading and unzip geckodriver"
		wget -q wget https://github.com/mozilla/geckodriver/releases/download/v0.28.0/geckodriver-v0.28.0-linux64.tar.gz
		tar -xvzf geckodriver-v0.28.0-linux64.tar.gz
fi

chmod +x ./jack3-product/arquillian-wildfly/wildfly-current/bin/jboss-cli.sh
./jack3-product/arquillian-wildfly/wildfly-current/bin/jboss-cli.sh --file=./test-setup/database-tenant.cli --timeout=30000
