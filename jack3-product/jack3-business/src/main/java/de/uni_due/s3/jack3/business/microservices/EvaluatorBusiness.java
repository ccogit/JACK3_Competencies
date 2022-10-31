package de.uni_due.s3.jack3.business.microservices;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import de.uni_due.s3.evaluator_api.calculator.ICalculatorService;
import de.uni_due.s3.evaluator_api.calculator.request.CalculatorRequest;
import de.uni_due.s3.evaluator_api.calculator.response.CalculatorResponse;
import de.uni_due.s3.evaluator_api.converter.IConverterService;
import de.uni_due.s3.evaluator_api.converter.request.ConverterRequest;
import de.uni_due.s3.evaluator_api.converter.response.ConverterResponse;
import de.uni_due.s3.jack3.business.AbstractBusiness;
import feign.Feign;
import feign.jaxb.JAXBContextFactory;
import feign.jaxb.JAXBDecoder;
import feign.jaxb.JAXBEncoder;
import feign.ribbon.RibbonClient;
import feign.slf4j.Slf4jLogger;

@ApplicationScoped
public class EvaluatorBusiness extends AbstractBusiness {

	private static JAXBContextFactory jaxbFactory = new JAXBContextFactory.Builder().build();

	private ICalculatorService calculatorService;

	private IConverterService converterService;

	@Inject
	private EurekaBusiness eurekaBusiness;

	@PostConstruct
	public void initialize() {
		eurekaBusiness.touch();
		calculatorService = initializeService(ICalculatorService.class);
		converterService = initializeService(IConverterService.class);
	}

	static <T> T initializeService(Class<T> serviceClass) {
		return Feign.builder().client(RibbonClient.create())
			.encoder(new JAXBEncoder(jaxbFactory))
			.decoder(new JAXBDecoder(jaxbFactory))
			.logger(new Slf4jLogger(CalculatorBusiness.class))
			.logLevel(feign.Logger.Level.BASIC)
			.target(serviceClass, "http://evaluator-service");
	}

	public CalculatorResponse calculate(CalculatorRequest request) {
		return calculatorService.calculate(request);
	}

	public ConverterResponse convert(ConverterRequest request) {
		return converterService.convert(request);
	}
}
