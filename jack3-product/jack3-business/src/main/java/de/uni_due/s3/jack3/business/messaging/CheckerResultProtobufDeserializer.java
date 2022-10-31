package de.uni_due.s3.jack3.business.messaging;


import org.apache.kafka.common.serialization.Deserializer;

import com.google.protobuf.InvalidProtocolBufferException;

import de.uni_due.s3.jack.dto.generated.BackendResultData.BackendResult;
import de.uni_due.s3.jack3.exceptions.JackRuntimeException;

/**
 * This class is configured in microprofile-config.properties, so our KafkaListener automagically gets the deserialzed
 * ojects and not the byte arrays!
 *
 * @author Benjamin Otto
 *
 */
public class CheckerResultProtobufDeserializer implements Deserializer<BackendResult> {

	@Override
	public BackendResult deserialize(String topic, byte[] data) {
		if (data == null) {
			return null;
		}

		try {
			return BackendResult.parseFrom(data);
		} catch (InvalidProtocolBufferException e) {
			throw new JackRuntimeException(e);
		}
	}

}