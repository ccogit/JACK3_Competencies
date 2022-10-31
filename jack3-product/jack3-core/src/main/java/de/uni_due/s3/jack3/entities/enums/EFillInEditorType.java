package de.uni_due.s3.jack3.entities.enums;

public enum EFillInEditorType {

	NONE(""),
	TEXT("Text"),
	NUMBER("Number"),
	MATHDOX_FORMULAR_EDITOR("");
	
	private String typeLabel;
	
	EFillInEditorType(String typeLabel){
		this.typeLabel = typeLabel;
	}
	
	public String getTypeLabel() {
		return typeLabel;
	}
}
