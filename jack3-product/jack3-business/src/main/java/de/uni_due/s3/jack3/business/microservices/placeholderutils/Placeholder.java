package de.uni_due.s3.jack3.business.microservices.placeholderutils;

import java.util.Objects;

import de.uni_due.s3.evaluator_api.converter.request.ConverterTaskType;
import de.uni_due.s3.evaluator_api.converter.response.ConverterResult;
import de.uni_due.s3.evaluator_api.converter.response.ConverterResultType;
import de.uni_due.s3.jack3.business.microservices.converterutils.ConverterFormatPropertiesProducer;
import de.uni_due.s3.jack3.business.microservices.converterutils.ConverterTaskTypeProducer;
import de.uni_due.s3.jack3.business.microservices.evaluatorutils.EvaluatorVariableTypeProducer;

public class Placeholder {

	private final String whole;
	public final String name;
	public final String typeIdentifier;
	public final boolean latexFlag;
	public final String decimals;
	public final String siPrefix;

	public Placeholder(String whole, String type, String name, boolean latexFlag, String decimals, String siPrefix) {
		this.whole = whole;
		this.typeIdentifier = type;
		this.name = name;
		this.latexFlag = latexFlag;
		this.decimals = decimals;
		this.siPrefix = siPrefix;
	}

	public boolean matchesConverterResultProperties(ConverterResult result) {
		return name.equals(result.properties.name)
				&& EvaluatorVariableTypeProducer.byPlaceholder(this).equals(result.properties.variableType)
				&& ConverterFormatPropertiesProducer.byPlaceholder(this).equals(result.format)
				&& belongsTaskTypeToResultType(result.type);
	}

	private boolean belongsTaskTypeToResultType(ConverterResultType resultType) {
		if (ConverterResultType.LATEX.equals(resultType))
			return ConverterTaskType.LATEX.equals(ConverterTaskTypeProducer.byPlaceholder(this));
		else if (ConverterResultType.STRING.equals(resultType))
			return ConverterTaskType.STRING.equals(ConverterTaskTypeProducer.byPlaceholder(this));
		else
			return false;
	}

	@Override
	public int hashCode() {
		return Objects.hash(decimals, latexFlag, name, siPrefix, typeIdentifier);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Placeholder other = (Placeholder) obj;
		return Objects.equals(decimals, other.decimals) && latexFlag == other.latexFlag
				&& Objects.equals(name, other.name) && Objects.equals(siPrefix, other.siPrefix)
				&& Objects.equals(typeIdentifier, other.typeIdentifier);
	}

	public String getWholeRegex() {
		return whole;
	}

}
