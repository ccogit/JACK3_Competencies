package de.uni_due.s3.jack3.business.xstream.converter;

import java.util.Base64;

import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

import de.uni_due.s3.jack3.entities.tenant.ExerciseResource;

public class ExerciseResourceConverter implements Converter {

	@Override
	// The parameter "type" cannot be parameterized because its signature is defined by an interface.
	public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
		return type.equals(ExerciseResource.class);
	}

	@Override
	public void marshal(Object source, HierarchicalStreamWriter writer, MarshallingContext context) {
		ExerciseResource exerciseResource = (ExerciseResource) source;

		writer.startNode("content");
		writer.setValue(Base64.getEncoder().encodeToString(exerciseResource.getContent()));
		writer.endNode();

		writer.startNode("filename");
		writer.setValue(exerciseResource.getFilename());
		writer.endNode();

		writer.startNode("description");
		writer.setValue(exerciseResource.getDescription());
		writer.endNode();

		writer.startNode("replacePlaceholder");
		writer.setValue(Boolean.toString(exerciseResource.isReplacePlaceholder()));
		writer.endNode();

	}

	@Override
	public Object unmarshal(HierarchicalStreamReader reader, UnmarshallingContext context) {
		String content = "";
		String filename = "";
		String description = "";
		boolean replacePlaceholder = false;
		while (reader.hasMoreChildren()) {
			reader.moveDown();
			switch (reader.getNodeName()) {
			case "content":
				content = reader.getValue();
				break;
			case "filename":
				filename = reader.getValue();
				break;
			case "description":
				description = reader.getValue();
				break;
			case "replacePlaceholder":
				replacePlaceholder = Boolean.parseBoolean(reader.getValue());
				break;
			default:
				throw new UnsupportedOperationException(
						"Exercise Resource Converter encountered unknown node type: " + reader.getNodeName());
			}
			reader.moveUp();
		}

		// Setting User as null, see REVIEW comment at Resource.lastEditor
		return new ExerciseResource(filename, Base64.getDecoder().decode(content), null, description,
				replacePlaceholder);
	}

}
