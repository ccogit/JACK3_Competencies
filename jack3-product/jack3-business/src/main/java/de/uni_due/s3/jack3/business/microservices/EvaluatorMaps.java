package de.uni_due.s3.jack3.business.microservices;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import de.uni_due.s3.jack3.business.microservices.variableutils.VariableValueFactory;
import de.uni_due.s3.jack3.entities.tenant.VariableValue;

public class EvaluatorMaps {

	Map<String, VariableValue> exerciseVariableMap = new HashMap<>();
	Map<String, VariableValue> inputVariableMap = new HashMap<>();
	Map<String, VariableValue> metaVariableMap = new HashMap<>();
	Map<String, VariableValue> checkVariableMap = new HashMap<>();

	public EvaluatorMaps() {
	}

	public Map<String, VariableValue> getExerciseVariableMap() {
		return exerciseVariableMap;
	}

	public void setExerciseVariableMap(Map<String, VariableValue> exerciseVariableMap) {
		this.exerciseVariableMap = exerciseVariableMap;
	}

	public Map<String, VariableValue> getInputVariableMap() {
		return inputVariableMap;
	}

	public void setInputVariableMap(Map<String, VariableValue> inputVariableMap) {
		this.inputVariableMap = inputVariableMap;
	}

	public Map<String, VariableValue> getMetaVariableMap() {
		return metaVariableMap;
	}

	public void setMetaVariableMap(Map<String, VariableValue> metaVariableMap) {
		this.metaVariableMap = metaVariableMap;
	}

	public Map<String, VariableValue> getCheckVariableMap() {
		return checkVariableMap;
	}

	public void setCheckVariableMap(Map<String, VariableValue> checkVariableMap) {
		this.checkVariableMap = checkVariableMap;
	}

	public VariableValue getExerciseVariableFor(String name) throws VariableNotDefinedException {
		return getOrElseThrow(exerciseVariableMap, name, "var");
	}

	public VariableValue getInputVariableFor(String name) throws VariableNotDefinedException {
		return getOrElseThrow(inputVariableMap, name, "input");
	}

	public VariableValue getMetaVariableFor(String name) throws VariableNotDefinedException {
		return getOrElseThrow(metaVariableMap, name, "meta");
	}

	public VariableValue getCheckVariableFor(String name) throws VariableNotDefinedException {
		return getOrElseThrow(checkVariableMap, name, "check");

	}

	private VariableValue getOrElseThrow(Map<String, VariableValue> map, String name, String mapName)
			throws VariableNotDefinedException {
		return Optional.ofNullable(map.get(name)).orElseThrow(() -> new VariableNotDefinedException(name, mapName));
	}

	public void addCheckVariable(String keyName, int value) {
		this.checkVariableMap.put(keyName, VariableValueFactory.createVariableValueForOpenMathInteger(value));
	}

	public void addCheckVariable(String keyName, double value) {
		this.checkVariableMap.put(keyName, VariableValueFactory.createVariableValueForOpenMathFloat(value));
	}

	public void addCheckVariable(String keyName, String value) {
		this.checkVariableMap.put(keyName, VariableValueFactory.createVariableValueForOpenMathString(value));
	}

	public void addMetaVariable(String keyName, int value) {
		this.metaVariableMap.put(keyName, VariableValueFactory.createVariableValueForOpenMathInteger(value));
	}

	public void addMetaVariable(String keyName, double value) {
		this.metaVariableMap.put(keyName, VariableValueFactory.createVariableValueForOpenMathFloat(value));
	}

	public void addMetaVariable(String keyName, String value) {
		this.metaVariableMap.put(keyName, VariableValueFactory.createVariableValueForOpenMathString(value));
	}

	public static class VariableNotDefinedException extends Exception {

		private static final long serialVersionUID = 2198444219580857553L;

		private String variableName;
		private String variableMapName;

		public VariableNotDefinedException(String variableName, String variableMapName) {
			super();
			this.variableName = variableName;
			this.variableMapName = variableMapName;
		}

		public String getVariableName() {
			return variableName;
		}

		public String getVaribaleMapName() {
			return variableMapName;
		}

	}
}
