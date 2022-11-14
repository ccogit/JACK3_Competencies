package de.uni_due.s3.jack3.entities.enums;

public enum ECompetenceDimension {

    PROCESS("Process"),
    CONTENT("Content");

    private String dimensionLabel;

    ECompetenceDimension(String dimensionLabel) {
        this.dimensionLabel = dimensionLabel;
    }

    public String getDimensionLabel() {
        return dimensionLabel;
    }
}
