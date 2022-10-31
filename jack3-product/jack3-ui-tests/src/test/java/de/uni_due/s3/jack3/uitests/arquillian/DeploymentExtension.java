package de.uni_due.s3.jack3.uitests.arquillian;

/*
 * Copyright (C) 2017 GitHub user famod.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,  
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.jboss.arquillian.container.spi.client.deployment.DeploymentScenario;
import org.jboss.arquillian.container.spi.event.DeployManagedDeployments;
import org.jboss.arquillian.container.spi.event.UnDeployManagedDeployments;
import org.jboss.arquillian.container.spi.event.container.BeforeStop;
import org.jboss.arquillian.container.test.impl.client.deployment.event.GenerateDeployment;
import org.jboss.arquillian.core.api.Event;
import org.jboss.arquillian.core.api.InstanceProducer;
import org.jboss.arquillian.core.api.annotation.Inject;
import org.jboss.arquillian.core.api.annotation.Observes;
import org.jboss.arquillian.core.spi.EventContext;
import org.jboss.arquillian.core.spi.LoadableExtension;
import org.jboss.arquillian.test.spi.TestClass;
import org.jboss.arquillian.test.spi.annotation.ClassScoped;
import org.jboss.arquillian.test.spi.annotation.SuiteScoped;

/**
 * <p>
 * Arquillian extension for running the tests. It checks each test class for the <code>@ExtendedDeployment</code>
 * annotation and runs the test separately or use an existing multi-deployment if there is one.
 * </p>
 * 
 * <p>
 * From <a href="https://gist.github.com/famod/a423cadbe976401e02002b9103d1d2d5">GitHub user <i>famod</i></a>.
 * </p>
 */
public class DeploymentExtension implements LoadableExtension {

	private static final Logger LOG = Logger.getLogger(DeploymentExtension.class.getName());

	@Override
	public void register(ExtensionBuilder builder) {
		builder.observer(DeploymentHandler.class);
	}

	public static class DeploymentHandler {

		/** Deployment scenario for reusing as multi deployment */
		private DeploymentScenario multiDeployment;

		/** Multi deployment is active when the active test class uses it */
		private boolean multiDeploymentIsActive;

		/** Block deployment if multi deployment is deployed and is to use */
		private boolean blockDeploy;

		/** Block undeployment if multi deployment is active */
		private boolean blockUnDeploy;

		@Inject
		private Event<GenerateDeployment> generateDeploymentEvent;

		@Inject
		private Event<UnDeployManagedDeployments> undeployEvent;

		@Inject
		@ClassScoped
		private InstanceProducer<DeploymentScenario> deploymentScenarioProducer;

		@Inject
		@SuiteScoped
		private InstanceProducer<DeploymentScenario> suiteScopedDeploymentScenarioProducer;

		/**
		 * Called when deployment is generated.
		 */
		public void generateDeployment(@Observes EventContext<GenerateDeployment> eventContext) {

			// Proceed deployment if deployment event has been fired to generate a new reusable deployment
			// (see below)
			if (eventContext.getEvent() instanceof GenerateMultiDeployment) {
				eventContext.proceed();
				return;
			}
			blockDeploy = false;
			blockUnDeploy = false;

			// Evaluate @ReuseDeployment on test class (if present), otherwise false
			final TestClass testClass = eventContext.getEvent().getTestClass();
			final EDeploymentType deploymentType = Optional //
					.ofNullable(testClass.getAnnotation(ExtendedDeployment.class)) //
					.map(ExtendedDeployment::value) //
					.orElse(EDeploymentType.SINGLE);
			final boolean activateMultiDeployment = deploymentType == EDeploymentType.MULTI;

			// Skip deployment / undeployment if multi deployment is enabled for this test class and multi deployment is
			// activated from the last test class
			if (activateMultiDeployment && multiDeploymentIsActive) {
				LOG.log(Level.FINE, "Reusing deployed Deployment for {0}.", testClass.getName());
				deploymentScenarioProducer.set(multiDeployment);
				blockDeploy = true;
				blockUnDeploy = true;
				return;
			}

			// Undeploy reusable deployment because test class requires a non-reusable deployment
			if (multiDeploymentIsActive) {
				LOG.log(Level.FINE, "Undeploying multi deployment for {0}.", testClass.getName());
				deploymentScenarioProducer.set(multiDeployment);
				undeployEvent.fire(new UnDeployManagedDeployments());
			}

			// Just proceed without any futher special handling in case the test class requires a single deployment
			if (!activateMultiDeployment) {
				LOG.log(Level.FINE, "Performing default deployment procedure for {0}", testClass.getName());
				eventContext.proceed();
				return;
			}

			// Multi deployment is activated for this test class but deployment is not yet deployed or generated
			final DeploymentScenario deploymentScenario = multiDeployment;
			if (deploymentScenario != null) {
				LOG.log(Level.FINE, "Reusing generated Deployment for {0}.", testClass.getName());
				deploymentScenarioProducer.set(deploymentScenario);
			} else {
				LOG.log(Level.FINE, "Generating reusable deployment for {0}.", testClass.getName());

				// Force deployment of this archive (see above)
				generateDeploymentEvent.fire(new GenerateMultiDeployment(new TestClass(Deployments.class)));
				multiDeployment = deploymentScenarioProducer.get();
			}
			multiDeploymentIsActive = true;
			blockUnDeploy = true;
		}

		/**
		 * Called when deployment is to be deployed.
		 */
		public void deploy(@Observes EventContext<DeployManagedDeployments> eventContext) {
			if (blockDeploy) {
				LOG.log(Level.FINE, "Blocking deployment.");
			} else {
				eventContext.proceed();
			}
		}

		/**
		 * Called when deployment is to be undeployed.
		 */
		public void undeploy(@Observes EventContext<UnDeployManagedDeployments> eventContext) {
			if (blockUnDeploy) {
				LOG.log(Level.FINE, "Blocking undeployment.");
			} else {
				eventContext.proceed();
				multiDeploymentIsActive = false;
			}
		}

		/**
		 * Called before stopping container.
		 */
		public void undeploy(@Observes BeforeStop event) {
			if (multiDeploymentIsActive) {
				LOG.log(Level.FINE, "Undeploying reusable deployment before stopping container.");
				blockUnDeploy = false;
				suiteScopedDeploymentScenarioProducer.set(multiDeployment);
				undeployEvent.fire(new UnDeployManagedDeployments());
			}
		}

		/**
		 * Helper class: This deployment generation forces deploy event (see above)
		 */
		private static class GenerateMultiDeployment extends GenerateDeployment {
			public GenerateMultiDeployment(TestClass testClass) {
				super(testClass);
			}
		}

	}

}
