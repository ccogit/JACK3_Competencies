# Install the commands needed below
apt-get -qq update && apt-get -qq install wget zip > /dev/null
# Check if Wildfly was cached before
echo "${WILDFLY_SHA256_SUM} wildfly-${WILDFLY_VERSION}.zip" | sha256sum -c &>/dev/null
if [ $? -eq 0 ]; 
	then 
		echo "Using cached wildfly-${WILDFLY_VERSION}.zip"
	else
		echo "Downloading wildfly-${WILDFLY_VERSION}.zip"
		wget -q https://github.com/wildfly/wildfly/releases/download/${WILDFLY_VERSION}/wildfly-${WILDFLY_VERSION}.zip
fi

# Unzip it to the correct folder
unzip -q wildfly-${WILDFLY_VERSION}.zip -d ./jack3-product/arquillian-wildfly/
mv ./jack3-product/arquillian-wildfly/wildfly-${WILDFLY_VERSION} ./jack3-product/arquillian-wildfly/wildfly-current

# Setup logging and database for Arquillian
chmod +x ./jack3-product/arquillian-wildfly/wildfly-current/bin/jboss-cli.sh
./jack3-product/arquillian-wildfly/wildfly-current/bin/jboss-cli.sh --file=./test-setup/logging.cli --timeout=30000
./jack3-product/arquillian-wildfly/wildfly-current/bin/jboss-cli.sh --file=./test-setup/enable-reactive-messaging.cli --timeout=30000
