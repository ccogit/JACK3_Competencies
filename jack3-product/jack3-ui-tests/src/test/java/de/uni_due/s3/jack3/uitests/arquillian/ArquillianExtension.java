package de.uni_due.s3.jack3.uitests.arquillian;

/*
   Copyright 2021 Johannes Beck (kifj)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 */

// source: https://github.com/arquillian/arquillian-core/pull/301/files

import java.lang.reflect.Method;
import java.util.function.Predicate;

import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ReflectiveInvocationContext;

/**
 * This fix enables the before and after methods to run in container-mode.
 * 
 * @author lukas.glaser, kifj
 */
public class ArquillianExtension extends org.jboss.arquillian.junit5.ArquillianExtension {

	private static Predicate<ExtensionContext> isInsideArquillian = context -> Boolean
			.parseBoolean(context.getConfigurationParameter(RUNNING_INSIDE_ARQUILLIAN).orElse("false"));

	@Override
	public void interceptBeforeEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {

		// In selenium, we execute the method only in client mode!
		if (isInsideArquillian.test(extensionContext)) {
			invocation.skip();
		} else {
			invocation.proceed();
		}
	}

	@Override
	public void interceptAfterEachMethod(Invocation<Void> invocation,
			ReflectiveInvocationContext<Method> invocationContext, ExtensionContext extensionContext) throws Throwable {

		// In selenium, we execute the method only in client mode!
		if (isInsideArquillian.test(extensionContext)) {
			invocation.skip();
		} else {
			invocation.proceed();
		}
	}

}
