package de.uni_due.s3.jack3.business;

import java.io.File;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.TemporalAmount;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.function.BooleanSupplier;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;

import de.uni_due.s3.jack3.business.microservices.CalculatorBusiness;
import de.uni_due.s3.jack3.business.microservices.ConverterBusiness;
import de.uni_due.s3.jack3.business.microservices.EvaluatorMaps;
import de.uni_due.s3.jack3.business.microservices.calculatorutils.InternalErrorEvaluatorException;
import de.uni_due.s3.jack3.business.microservices.converterutils.ConverterException;
import de.uni_due.s3.jack3.business.microservices.converterutils.InternalErrorConverterException;
import de.uni_due.s3.jack3.entities.tenant.EvaluatorExpression;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;
import de.uni_due.s3.jack3.multitenancy.LoggerProvider;
import de.uni_due.s3.jack3.services.DatabaseTestService;
import de.uni_due.s3.jack3.services.EmailService;
import de.uni_due.s3.jack3.utils.ByteCount;

@ApplicationScoped
public class SystemHealthBusiness extends AbstractBusiness {

	public static class TestResult {

		private final String id;

		private final int tests;

		private final int errors;

		private final long latency;

		private final Instant timestamp;

		private TestResult(final String id,final int tests,final int errors,final long durationInNs) {
			this.timestamp = Instant.now();
			this.id = id;
			this.tests = tests;
			this.errors = errors;
			this.latency = TimeUnit.NANOSECONDS.toMillis(durationInNs / Math.max(tests,1));
		}

		public String getId() {
			return id;
		}

		public int getTests() {
			return tests;
		}

		public int getErrors() {
			return errors;
		}

		public long getLatency() {
			return latency;
		}

		public boolean isPassed() {
			return tests > 0 && errors == 0;
		}

		public LocalDateTime getTimeStamp() {
			return LocalDateTime.ofInstant(timestamp,ZoneId.systemDefault());
		}

		boolean isExpired(final TemporalAmount expiry) {
			return timestamp.plus(expiry).isBefore(Instant.now());
		}
	}

	private static class Test {

		private final Supplier<TestResult> supplier;

		private TestResult result;

		private Duration validity;

		Test(final Supplier<TestResult> supplier,final Duration validity) {
			this.supplier = Objects.requireNonNull(supplier);
			this.validity = validity;
		}

		TestResult getResult() {
			if (result == null || result.isExpired(validity)) {
				final long start = System.nanoTime();
				try {
					result = supplier.get();
				} catch (final RuntimeException e) {
					LoggerProvider.get(getClass()).warn("Failed to update system health result object.",e);
					result = new TestResult("failure", 1, 1,System.nanoTime() - start);
				}
			}
			return result;
		}
	}

	private static final long MIN_DISK_SPACE = 5L * 1024 * 1024 * 1024;

	@Inject
	EmailService emailService;

	@Inject
	private CalculatorBusiness evaluatorBusiness;

	@Inject
	private ConverterBusiness converterBusiness;

	@Inject
	private DatabaseTestService databaseTestService;

	private final List<Test> tests;

	public SystemHealthBusiness() {
		this.tests = List.of(
			new Test(this::createEvaluatorTestResult, Duration.ofSeconds(30)),
			new Test(this::createSageTestResult,      Duration.ofSeconds(30)),
			new Test(this::createRTestResult,         Duration.ofSeconds(30)),
			new Test(this::createDbConnectivityResult,Duration.ofMinutes(1)),
			new Test(this::createDbWriteResult,       Duration.ofMinutes(1)),
			new Test(this::createEmailTestResults,    Duration.ofMinutes(5)),
			new Test(this::createSystemLoadTestResult,Duration.ofSeconds(10)),
			new Test(this::createFreeDiskSpaceResult, Duration.ofMinutes(5))
		);
	}

	public List<TestResult> getResults() {
		synchronized (tests) {
			return tests.stream().map(Test::getResult).collect(Collectors.toList());
		}
	}

	private TestResult createEvaluatorTestResult() {
		return createEvaluatorTestResults("evaluator",createEvaluatorTestExpressions());
	}

	private TestResult createSageTestResult() {
		return createEvaluatorTestResults("sage",createCasTestExpressions("Sage"));
	}

	private TestResult createRTestResult() {
		return createEvaluatorTestResults("r",createCasTestExpressions("R"));
	}

	private TestResult createEvaluatorTestResults(final String name,final Map<String,String> expressions) {
		int errors = 0;
		final long start = System.nanoTime();
		for (Entry<String,String> item : expressions.entrySet()) {
			if (!testExpression(item.getKey(),item.getValue())) {
				++errors;
			}
		}
		final long duration = System.nanoTime() - start;
		return new TestResult(name,10, errors,duration);
	}

	private Map<String,String> createEvaluatorTestExpressions() {
		Random random = ThreadLocalRandom.current();
		final Map<String,String> expression = new HashMap<>();
		for (int i = 0; i < 10; i++) {
			final String a = Integer.toString(random.nextInt());
			final String b = Integer.toString(random.nextInt());
			expression.put("concat('" + a + "','" + b + "')",a + b);
		}
		return expression;
	}

	private Map<String,String> createCasTestExpressions(final String cas) {
		Random random = ThreadLocalRandom.current();
		final Map<String,String> expression = new HashMap<>();
		for (int i = 0; i < 10; i++) {
			final int a = random.nextInt(Short.MAX_VALUE);
			final int b = random.nextInt(Short.MAX_VALUE);
			final int result = Math.addExact(a, b);
			expression.put(String.format("evaluateIn%s('%d+%d')",cas,a,b),Integer.toString(result));
		}
		return expression;
	}

	private boolean testExpression(final String expression,final String expected) {
		try {
			final String actual = evaluate(expression);
			if (expected.equals(actual)) {
				return true;
			} else {
				getLogger().warnf(
					"Expected to receive \"%s\" while evaluating \"%s\" but received \"%s\".",
					expected, expression, actual);
				return false;
			}
		} catch (InternalErrorEvaluatorException | InternalErrorConverterException | ConverterException e) {
			getLogger().warn("Failed to test expression " + expression,e);
			return false;
		}
	}

	private String evaluate(final String expression)
			throws InternalErrorEvaluatorException, InternalErrorConverterException, ConverterException {

		EvaluatorExpression ee = new EvaluatorExpression(expression);
		VariableValue result = evaluatorBusiness.calculateToVariableValue(ee,new EvaluatorMaps());
		return converterBusiness.convertToString(result);
	}

	private TestResult createDbConnectivityResult() {
		return createBinaryResult("databaseConnectivity",databaseTestService::isConnected);
	}

	private TestResult createDbWriteResult() {
		return createBinaryResult("databaseWriteAccess",databaseTestService::canWrite);
	}

	private TestResult createEmailTestResults() {
		return createBinaryResult("emailService",emailService::isReady);
	}

	private TestResult createBinaryResult(final String name,final BooleanSupplier test) {
		final long start = System.nanoTime();
		final int errors = test.getAsBoolean() ? 0 : 1;
		final long duration = System.nanoTime() - start;
		return new TestResult(name,1,errors,duration);
	}

	private TestResult createFreeDiskSpaceResult() {
		int errors = 0;
		final long start = System.nanoTime();
		final File[] roots = File.listRoots();
		for (final File root : roots) {
			final long freeSpace = root.getFreeSpace();
			if (freeSpace < MIN_DISK_SPACE) {
				getLogger().warnf("%s has less than %s of free space (%s).",
					root,ByteCount.toIECString(MIN_DISK_SPACE),ByteCount.toIECString(freeSpace));
				++errors;
			}
		}
		final long duration = System.nanoTime() - start;
		return new TestResult("freeDiskSpace",roots.length,errors,duration);
	}

	private TestResult createSystemLoadTestResult() {
		final long start = System.nanoTime();
		OperatingSystemMXBean osMxBean = ManagementFactory.getOperatingSystemMXBean();
		final double load = osMxBean.getSystemLoadAverage();

		// A negative load means the feature is not supported on the current platform.
		// We skip the test in this case.
		if (load < 0) {
			return new TestResult("load",0,0,System.nanoTime() - start);
		}

		final int cpus = osMxBean.getAvailableProcessors();
		final int errors = load / cpus >= 0.75 ? 1 : 0;
		return new TestResult("load",1,errors,System.nanoTime() - start);

	}
}
