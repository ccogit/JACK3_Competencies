package de.uni_due.s3.jack3.business.messaging;


import org.apache.kafka.common.serialization.Serializer;

/**
 * This class is configured in microprofile-config.properties
 *
 * @author Benjamin Otto
 *
 */
public class ResultProtobufSerializer implements Serializer<byte[]> {

	@Override
	public byte[] serialize(String topic, byte[] data) {
		// Since there is currently no easy way to let protobuf implement interfaces (other than to write code generator
		// plugins) we just return the already serialized byte arrays here (so the usercode has to call
		// dto.toByteArray()). Otherwise we would need to write a serializer for all dto classes and repeate one line of
		// code for all of these.
		return data;
	}
}