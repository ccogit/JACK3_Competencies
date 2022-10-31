package de.uni_due.s3.jack3.multitenancy;

public class TenantPerDeploymentDatasourceIdentifier {

	public static final String getDataSourceJndiName()
	{
		return getDataSourceJndiName(TenantIdentifier.get());
	}

	public static String getDataSourceJndiName(final String tenantIdentifier) {
		return "java:jboss/datasources/jack-" + tenantIdentifier + "-datasource";
	}
}
