package de.uni_due.s3.jack3.services;

import java.util.List;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.TypedQuery;

import de.uni_due.s3.jack3.entities.tenant.IdentityProfileField;
import de.uni_due.s3.jack3.entities.tenant.ProfileField;
import de.uni_due.s3.jack3.services.utils.DBHelper;

/**
 * Service for managing entities derived from {@link ProfileField}.
 * 
 * @author Bj&ouml;rn Zurmaar
 */
@Stateless
public class ProfileFieldService extends AbstractServiceBean {

	@Inject
	private BaseService baseService;

	public <T extends ProfileField> T persistProfileField(T field) {
		return baseService.merge(field);
	}

	public Optional<IdentityProfileField> getIdentityField(final String attributeName) {
		final TypedQuery<IdentityProfileField> q = getEntityManager().createNamedQuery(
				IdentityProfileField.GET_BY_NAME,IdentityProfileField.class);
		q.setParameter("attributeName",attributeName);
		return DBHelper.getOneOrZero(q);
	}

	/**
	 * Ensures that there is an identity profile field with the specified name. Returns an existing field or creates a
	 * new one.
	 */
	@Nonnull
	public IdentityProfileField getOrCreateIdentityField(final String attributeName) {
		return getIdentityField(attributeName)
				.orElseGet(() -> persistProfileField(new IdentityProfileField(attributeName)));
	}

	public List<ProfileField> getAllFields() {
		return getEntityManager()
			.createNamedQuery(ProfileField.GET_ALL,ProfileField.class)
			.getResultList();
	}

	public List<ProfileField> getAllPublicFields() {
		return getEntityManager()
			.createNamedQuery(ProfileField.GET_ALL_PUBLIC,ProfileField.class)
			.getResultList();
	}

	public List<ProfileField> getAllMandatoryFields() {
		return getEntityManager()
			.createNamedQuery(ProfileField.GET_ALL_MANDATORY,ProfileField.class)
			.getResultList();
	}

	public ProfileField updateProfileField(ProfileField profileField) {
		return baseService.merge(profileField);
	}
}
