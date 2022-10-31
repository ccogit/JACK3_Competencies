package de.uni_due.s3.jack3.services;

import static de.uni_due.s3.jack3.services.utils.DBHelper.getOneOrZero;

import java.util.List;
import java.util.Optional;

import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import de.uni_due.s3.jack3.entities.tenant.Tag;
import de.uni_due.s3.jack3.utils.JackStringUtils;

/**
 * Service for managing {@link Tag} entities.
 */
@Stateless
public class TagService extends AbstractServiceBean {

	@Inject
	BaseService baseService;

	public List<Tag> getAllTags() {
		return baseService.findAll(Tag.class);
	}

	public Optional<Tag> getTagByName(String name) {
		final EntityManager em = getEntityManager();
		final TypedQuery<Tag> query = em.createNamedQuery(Tag.TAG_BY_NAME, Tag.class);
		query.setParameter("name", name);

		return getOneOrZero(query);
	}

	/**
	 * Lists all available tag names, alphabetically ordered.
	 */
	public List<String> getAllTagsAsStrings() {

		final EntityManager em = getEntityManager();
		final TypedQuery<String> q = em.createNamedQuery(Tag.ALL_TAGS_AS_STRINGS, String.class);
		return q.getResultList();
	}

	public Tag getOrCreateByName(String tagName) {
		if (JackStringUtils.isBlank(tagName)) {
			throw new IllegalArgumentException("Tag-Name must not be empty!");
		}

		tagName = tagName.strip();
		Optional<Tag> res = getTagByName(tagName);
		if (!res.isPresent()) {
			getLogger().infof("Writing new tag with name %s to database.", tagName);
			Tag newTag = new Tag(tagName);
			baseService.persist(newTag);
			return newTag;
		}

		return res.get();
	}
}
