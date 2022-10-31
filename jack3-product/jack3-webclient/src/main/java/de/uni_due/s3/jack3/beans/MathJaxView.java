package de.uni_due.s3.jack3.beans;

import javax.enterprise.context.RequestScoped;
import javax.inject.Named;

/**
 * This bean provides the MathJax configuration to the JSF files.
 */
@Named
@RequestScoped
public class MathJaxView extends ConfigurableUrlView {

	private static final long serialVersionUID = -4977425479424797446L;

	public static final String MATH_JAX_URL_KEY = "MathJaxURL";

	public MathJaxView() {
		super(MATH_JAX_URL_KEY);
	}

	@Override
	public String getConfigurationHint() {
		return formatLocalizedMessage("missingConfiguration.mathJax.detail", new Object[] { MATH_JAX_URL_KEY });
	}
}
