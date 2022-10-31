package de.uni_due.s3.jack3.entities.enums;

public enum EFormularEditorPalette {
	MATHDOX_FORMULAR_EDITOR_PALETTE_NOT_SELECTED(""),
	MATHDOX_FORMULAR_EDITOR_NO_PALETTE("invisible"),
	MATHDOX_FORMULAR_EDITOR_BASIC("basic"),
	MATHDOX_FORMULAR_EDITOR_TRIGONOMETRY("trigonometry"),
	MATHDOX_FORMULAR_EDITOR_PALETTE("palette"),
	MATHDOX_FORMULAR_EDITOR_PALETTE_FULL("palette_full"),
	MATHDOX_FORMULAR_EDITOR_PALETTE_SPLIT("palette_split"),
	MATHDOX_FORMULAR_EDITOR_PALETTE_2("palette2"),
	MATHDOX_FORMULAR_EDITOR_PALETTE_3("palette3"),
	MATHDOX_FORMULAR_EDITOR_PALETTE_4("palette4"),
	MATHDOX_FORMULAR_EDITOR_PALETTE_5("palette5"),
	MATHDOX_FORMULAR_EDITOR_PALETTE_6("palette6"),
	MATHDOX_FORMULAR_EDITOR_LINALG("linalg"),
	MATHDOX_FORMULAR_EDITOR_3X6_MATRIX("3x6Matrix"),
	MATHDOX_FORMULAR_EDITOR_SET_OPERATIONS("setoperations"),
	MATHDOX_FORMULAR_EDITOR_UDE_MATH_DIDACTICS("ude_math_didactics");

	
	
	private String typeLabel;
	
	EFormularEditorPalette(String typeLabel){
		this.typeLabel = typeLabel;
	}
	
	public String getTypeLabel() {
		return typeLabel;
	}
	
	public static EFormularEditorPalette getEFormularEditorPaletteForTypeLabel(String typeLabel) {
		EFormularEditorPalette palette = MATHDOX_FORMULAR_EDITOR_PALETTE_NOT_SELECTED;
		for(EFormularEditorPalette item : EFormularEditorPalette.values()) {
			if(typeLabel.equals(item.getTypeLabel())) {
				return item;
			}
		}
		return palette;
	}
}
