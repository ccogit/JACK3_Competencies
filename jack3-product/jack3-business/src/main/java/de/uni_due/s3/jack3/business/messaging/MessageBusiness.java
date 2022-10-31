package de.uni_due.s3.jack3.business.messaging;

import java.time.LocalDateTime;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;

import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.eclipse.microprofile.reactive.messaging.Message;
import org.eclipse.microprofile.reactive.messaging.Outgoing;

import com.google.common.base.VerifyException;

import de.uni_due.s3.jack.dto.generated.BackendResultData.BackendResult;
import de.uni_due.s3.jack.dto.generated.ConsoleEvaluationResponse.ConsoleEvalResponse;
import de.uni_due.s3.jack3.business.AbstractBusiness;
import de.uni_due.s3.jack3.business.ExercisePlayerBusiness;
import de.uni_due.s3.jack3.business.stagetypes.AbstractStageBusiness;
import de.uni_due.s3.jack3.entities.tenant.ConsoleResult;
import de.uni_due.s3.jack3.entities.tenant.Job;
import de.uni_due.s3.jack3.entities.tenant.Stage;
import de.uni_due.s3.jack3.entities.tenant.StageSubmission;
import de.uni_due.s3.jack3.entities.tenant.Submission;
import de.uni_due.s3.jack3.exceptions.NoSuchJackEntityException;
import de.uni_due.s3.jack3.multitenancy.TenantConfigSource;
import de.uni_due.s3.jack3.services.BaseService;
import de.uni_due.s3.jack3.services.StageSubmissionService;
import de.uni_due.s3.jack3.services.SubmissionService;
import io.smallrye.reactive.messaging.kafka.api.KafkaMetadataUtil;
import io.smallrye.reactive.messaging.kafka.api.OutgoingKafkaRecordMetadata;

/**
 * Business that uses
 * <a href="https://www.wildfly.org/news/2021/10/14/MicroProfile-Reactive-Messaging-2.0-in-WildFly-25/">MicroProfile
 * Reactive Messaging 2.0</a> to send and receive asynchronous messages. The underlying technology can be changed but
 * at the moment we use the smallrye-kafka connector. This is configured in <a
 * href="file:../resources/META-INF/microprofile-config.properties">microprofile-config.properties</a>
 *
 * @author Benjamin Otto
 *
 */
@ApplicationScoped
public class MessageBusiness extends AbstractBusiness {

	@Inject
	@Channel("check-request")
	private Emitter<byte[]> messageEmitter;

	@Inject
	private BaseService baseService;

	@Inject
	private ExercisePlayerBusiness exercisePlayerBusiness;

	@Inject
	private StageSubmissionService stageSubmissionService;

	@Inject
	private SubmissionService submissionService;

	/**
	 * Kafka is configured via <a
	 * href="file:../resources/META-INF/microprofile-config.properties">microprofile-config.properties</a>. The actual
	 * topic name is customized by {@link TenantConfigSource} to add a tenant and hostname specific suffix. This can be
	 * overwritten globaly (i.e. for all Tenants!) by providing the VM argument like:
	 *
	 * <pre>
	 * -Dmp.messaging.incoming.checker-results.topic=my-topic
	 * </pre>
	 *
	 * You can however specify a custom hostname by appending the "JackCustomHostname" VM argument, in this case this
	 * will still be prefixed by "deplyomentname-results."
	 *
	 * <pre>
	 * -DJackCustomHostname="my-awesome-name"
	 * </pre>
	 *
	 */
	@Incoming("checker-results")
	public void checkerResult(BackendResult backendResult) {
		// We catch all exceptions while processing the result here and just log them to prevent the messaging system to
		// stop working if uncaught exceptions occur
		try {
			handleIncomingCheckerResult(backendResult);
		} catch (Exception e) {
			// Beware: By catching and aborting here we won't get the same result deliverd to us again
			getLogger().error("Error occured while receiving checker result from kafka. Aborting...", e);
		}
	}

	void handleIncomingCheckerResult(BackendResult backendResult) {
		getLogger().info("New result '" + backendResult.getResult() + "' received from Kafka: "
				+ backendResult.getJobMetaInfo());

		long jobId = backendResult.getJobMetaInfo().getJobId();
		Job job = baseService.findById(Job.class, jobId, true)
				.orElseThrow(() -> new NoSuchJackEntityException("Job with ID " + jobId + " not found in the db!"));

		job.setFinished();
		baseService.merge(job);

		AbstractStageBusiness stageBusiness = getStageBusiness(job);
		stageBusiness.handleAsyncCheckerResult(backendResult, job);
		Submission submission = job.getSubmission();
		StageSubmission stageSubmission = stageSubmissionService
				.getStageSubmissionWithLazyData(job.getStageSubmission().getId()) //
				.orElseThrow(); //

		stageBusiness.updateStatus(stageSubmission, submission);

		if (!stageSubmission.hasPendingChecks() && !stageSubmission.hasInternalErrors()) {
			Stage stage = baseService.findById(Stage.class, stageSubmission.getStageId(), false).orElseThrow();
			// Calculate variable updates (phase "after check")
			exercisePlayerBusiness.doVariableUpdates(submission, stageSubmission, stage,
					stage.getVariableUpdatesAfterCheck());
			submission = submissionService.mergeSubmission(submission);
		}
		exercisePlayerBusiness.updateTotalResult(submission);
	}

	/**
	 * See {@link #checkerResult(BackendResult)}
	 *
	 * @param consoleEvalResponse
	 */
	@Incoming("console-results")
	public void consoleResult(ConsoleEvalResponse consoleEvalResponse) {
		getLogger().info("Received Console response: " + consoleEvalResponse);
		handleConsoleResponse(consoleEvalResponse);
	}

	/**
	 * Generic handling of the response of a console evaluation.
	 *
	 */
	private void handleConsoleResponse(ConsoleEvalResponse consoleEvalResponse) {
		String output = consoleEvalResponse.getEvalResponse();

		long consoleResultId = Long.parseLong(consoleEvalResponse.getId());
		ConsoleResult consoleResult = baseService //
				.findById(ConsoleResult.class, consoleResultId, false) //
				.orElseThrow(() -> new NoSuchJackEntityException("Console result with id " + consoleResultId
						+ " was requested to be updated, but was not found in the DB!"));

		if (consoleResult.isResponseReceived()) {
			throw new VerifyException("The response of a console eval request has already been received, "
					+ "but was requested to be set again! " + consoleEvalResponse);
		}

		consoleResult.setResponse(output);
		consoleResult.setResponseReceived(true);
		consoleResult.setFinishedAt(LocalDateTime.now());
		consoleResult.setGraderId(consoleEvalResponse.getGraderID());
		baseService.merge(consoleResult);
	}

	/**
	 * Since the Emitter Interface doesn't provide a direct way to change the outgoing topic, we add the topic as
	 * metadata to the message and then emit it to the channel "check-request".
	 */
	public void sendSerializedDtoToKafka(byte[] serializedDto, String topic) {
		OutgoingKafkaRecordMetadata<byte[]> topicMetaData = OutgoingKafkaRecordMetadata.<byte[]>builder()
				.withTopic(topic) //
				.build();
		Message<byte[]> serializedDtoMsg = Message.of(serializedDto);
		messageEmitter.send(KafkaMetadataUtil.writeOutgoingKafkaMetadata(serializedDtoMsg, topicMetaData));
	}

	/**
	 * User code needs to use an emitter to send messages to kafka. To use this, add an emitter to the channel
	 * "check-request" to your code like this:
	 *
	 * <pre>
	 * &#64;Inject
	 * &#64;Channel("check-request")
	 * private Emitter&lt;byte[]&gt; checkEmitter;
	 * </pre>
	 *
	 * Even though the Emitter above is parametrized with byte[], you can (and in our case actually must) send a
	 * Message.of(byte[]) with added Metadata. In the Metadata you need to set the target kafka topic like this:
	 *
	 * <pre>
	 * checkEmitter.send(KafkaMetadataUtil.writeOutgoingKafkaMetadata( //
	 * 		Message.of(dto.toByteArray()), //
	 * 		OutgoingKafkaRecordMetadata.&lt;byte[]&gt;builder().withTopic(topic).build() //
	 * ));
	 * </pre>
	 *
	 * Ideally we would do above code in the method below, but since there is currently no easy way (other than to write
	 * code generator plugins) to add interfaces to protobuf dtos we would have to write the same code essentially for
	 * all dtos. So here we just take the alredy serialized dto and return it to the outgoing channel, which is defined
	 * in <a href="file:../resources/META-INF/microprofile-config.properties">microprofile-config.properties</a>.
	 */
	@Incoming("check-request")
	@Outgoing("to-kafka")
	private Message<byte[]> sendToKafka(Message<byte[]> serializedDto) {
		return serializedDto;
	}

	private AbstractStageBusiness getStageBusiness(Job job) {
		final String receiverBusinessName = "de.uni_due.s3.jack3.business.stagetypes." + job.getStageTypeName()
				+ "Business";

		// Load stage specific business bean
		try {
			final Class<?> stageBusinessClass = getClass().getClassLoader().loadClass(receiverBusinessName);
			return (AbstractStageBusiness) (CDI.current().select(stageBusinessClass).get());
		} catch (ClassNotFoundException e) {
			throw new VerifyException(e);
		}
	}

}