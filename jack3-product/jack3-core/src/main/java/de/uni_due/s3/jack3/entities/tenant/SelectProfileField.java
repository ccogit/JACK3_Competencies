package de.uni_due.s3.jack3.entities.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.envers.Audited;

/**
 * This class implements a profile field where the user can select one or more items of several
 * predefined options.
 */
@Audited
@Entity
public class SelectProfileField extends ProfileField {

	private static final long serialVersionUID = 3207177987654602046L;

	@Column
	boolean allowMultipleSelections;

	@Column
	int defaultSelection;

	/** This map maps language codes to a map of available options in the corresponding language. */
	// TODO We need a mapping for this field. Unfortunately there is no native mean to do this in jpa or hibernate.
	// Maybe this is helpful: http://blog.xebia.com/mapping-multimaps-with-hibernate/
//	@ManyToAny(metaColumn = @Column)
//	private Map<String, Map<Integer, String>> options;

	public SelectProfileField() {
		super();
	}
}
