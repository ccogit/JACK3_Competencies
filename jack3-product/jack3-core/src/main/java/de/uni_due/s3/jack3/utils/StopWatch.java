package de.uni_due.s3.jack3.utils;

import java.text.DecimalFormat;

/**
 * Provides a stopwatch to do simple performance-measuring. Usage:
 * 
 * <pre>
 * StopWatch stopWatch = new StopWatch().start();
 * doStuff();
 * System.out.println("doStuff() took " + stopWatch.stop().getElapsedSeconds());
 * </pre>
 * 
 * @author Benjamin.Otto
 */
public class StopWatch {
	long start = 0;
	long stop = 0;

	public StopWatch start() {
		start = System.nanoTime();
		return this;
	}

	public StopWatch stop() {
		stop = System.nanoTime();
		return this;
	}

	public String getElapsedSeconds() {
		if (start == 0 || stop == 0) {
			return "start() or stop() has not been called!";
		}

		double elapsedTime = (double) stop - (double) start;
		return new DecimalFormat("#0.0000").format(elapsedTime / 1_000_000_000.0) + "s";
	}

	public String getElapsedMilliseconds() {
		if (start == 0 || stop == 0) {
			return "start() or stop() has not been called!";
		}
		long elapsedTime = stop - start;
		return Math.round((double) elapsedTime / 1_000_000) + "ms";
	}

	public double getCurrentSeconds() {
		if (start == 0) {
			throw new AssertionError("start() has not been called!");
		}
		long elapsedTime = System.nanoTime() - start;
		return (double) elapsedTime / 1_000_000_000;
	}

	public double getCurrentMilliseconds() {
		if (start == 0) {
			throw new AssertionError("start() has not been called!");
		}
		long elapsedTime = System.nanoTime() - start;
		return (double) elapsedTime / 1_000_000;
	}

	public StopWatch reset() {
		start = 0;
		stop = 0;
		return this;
	}

	@Override
	public String toString() {
		if (start == 0) {
			return "StopWatch [not started]";
		}
		return "StopWatch [" + getCurrentMilliseconds() + "ms]";
	}

	/**
	 * Stops the time needed for an operation.
	 * 
	 * @param r
	 *            Performed action
	 * @param message
	 *            Identifier which is used for message string
	 * @return Message indicating how long the action took. Can be used directly for loggers.
	 */
	public static String stopTime(Runnable r, String message) {
		final StopWatch stopWatch = new StopWatch().start();
		r.run();
		stopWatch.stop();
		return String.format("%-40s: %s", message, stopWatch.getElapsedSeconds());
	}

}
