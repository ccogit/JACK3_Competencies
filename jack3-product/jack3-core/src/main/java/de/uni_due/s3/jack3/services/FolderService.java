package de.uni_due.s3.jack3.services;

import static de.uni_due.s3.jack3.services.utils.DBHelper.getOneOrZero;
import static de.uni_due.s3.jack3.services.utils.DBHelper.getOneOrZeroConvertToTypeRemovingDuplicates;
import static de.uni_due.s3.jack3.services.utils.DBHelper.getOneOrZeroRemovingDuplicates;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.annotation.Nonnull;
import javax.ejb.Stateless;
import javax.inject.Inject;
import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;

import org.hibernate.envers.AuditReader;
import org.hibernate.envers.AuditReaderFactory;

import de.uni_due.s3.jack3.entities.tenant.ContentFolder;
import de.uni_due.s3.jack3.entities.tenant.Exercise;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.PresentationFolder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;
import de.uni_due.s3.jack3.utils.EntityReflectionHelper;

/**
 * Service for managaging entities derived from {@link Folder}.
 */
@Stateless
public class FolderService extends AbstractServiceBean {
	@Inject
	private BaseService baseService;

	private static final String MISSING_FOLDER_EXCEPTION_MESSAGE = "Folder does not exist in database!";

	public void persistFolder(Folder folder) {
		baseService.persist(folder);
	}

	public void deleteFolder(Folder folder) {
		baseService.deleteEntity(folder);
	}

	/**
	 * Returns all content folders for given user.
	 *
	 * @return Content folders with lazy rights data, ordered by name
	 */
	public List<ContentFolder> getAllContentFoldersForUser(User user) {
		final EntityManager em = getEntityManager();
		final TypedQuery<ContentFolder> query = em.createNamedQuery(ContentFolder.ALL_CONTENT_FOLDERS_FOR_USER,
				ContentFolder.class);
		query.setParameter("user", user);
		return query.getResultList();
	}

	/**
	 * Returns all content folders for given user group.
	 *
	 * @return Content folders with lazy rights data, ordered by name
	 */
	public List<ContentFolder> getAllContentFoldersForUserGroup(UserGroup userGroup) {
		final EntityManager em = getEntityManager();
		final TypedQuery<ContentFolder> query = em.createNamedQuery(ContentFolder.ALL_CONTENT_FOLDERS_FOR_USERGROUP,
				ContentFolder.class);
		query.setParameter("usergroup", userGroup);
		return query.getResultList();
	}

	/**
	 * Returns all presentation folder, which are not deleted, ordered alphabetical by name.
	 *
	 * @return Presentation folders <strong>without</strong> lazy data, ordered by name
	 */
	public List<PresentationFolder> getAllPresentationFolders() {
		final EntityManager em = getEntityManager();
		final TypedQuery<PresentationFolder> query = em.createNamedQuery(PresentationFolder.ALL_PRESENTATION_FOLDERS,
				PresentationFolder.class);
		return query.getResultList();
	}

	/**
	 * Returns all presentation folder for given user.
	 *
	 * @return Presentation folders with lazy rights data, ordered by name
	 */
	public List<PresentationFolder> getAllPresentationFoldersForUser(User user) {
		final EntityManager em = getEntityManager();
		final TypedQuery<PresentationFolder> query = em
				.createNamedQuery(PresentationFolder.ALL_PRESENTATION_FOLDERS_FOR_USER, PresentationFolder.class);
		query.setParameter("user", user);
		return query.getResultList();
	}

	/**
	 * Returns all presentation folder for given user group.
	 *
	 * @return Presentation folders with lazy rights data, ordered by name
	 */
	public List<PresentationFolder> getAllPresentationFoldersForUserGroup(UserGroup userGroup) {
		final EntityManager em = getEntityManager();
		final TypedQuery<PresentationFolder> query = em
				.createNamedQuery(PresentationFolder.ALL_PRESENTATION_FOLDERS_FOR_USERGROUP, PresentationFolder.class);
		query.setParameter("usergroup", userGroup);
		return query.getResultList();
	}

	/**
	 * Returns the content folder that has no parent. At any time there must be exactly one such folder.
	 *
	 * @return Folder without lazy data
	 */
	@Nonnull
	public ContentFolder getContentRoot() {
		final EntityManager em = getEntityManager();
		final TypedQuery<ContentFolder> query = em.createNamedQuery(ContentFolder.QUERY_ROOT, ContentFolder.class);
		return getOneOrZero(query).orElseThrow(() -> new AssertionError("No content root folder found."));
	}

	/**
	 * Loads lazy rights data for a given folder
	 */
	public <T extends Folder> Optional<T> getFolderWithManagingRights(final T folder, final Class<T> clazz) {
		Objects.requireNonNull(folder);

		final TypedQuery<Folder> query = getEntityManager().createNamedQuery(Folder.FOLDER_WITH_MANAGING_RIGHS_BY_ID,
				Folder.class);
		query.setParameter("id", folder.getId());
		return getOneOrZeroConvertToTypeRemovingDuplicates(query, clazz);
	}

	public Optional<Folder> getFolderWithManagingRights(final Folder folder) {
		return getFolderWithManagingRights(folder, Folder.class);
	}

	/**
	 * @return Presentation folder without lazy data
	 */
	public Optional<PresentationFolder> getPresentationFolderById(long id) {
		final EntityManager em = getEntityManager();
		final TypedQuery<PresentationFolder> query = em.createNamedQuery(PresentationFolder.PRESENTATION_FOLDER_BY_ID,
				PresentationFolder.class);
		query.setParameter("id", id);
		return getOneOrZero(query);
	}

	/**
	 * @return Content folder without lazy data
	 */
	public Optional<ContentFolder> getContentFolderById(long id) {
		final EntityManager em = getEntityManager();
		final TypedQuery<ContentFolder> query = em.createNamedQuery(ContentFolder.CONTENT_FOLDER_BY_ID,
				ContentFolder.class);
		query.setParameter("id", id);
		return getOneOrZero(query);
	}
	
	/**
	 * Load the Content folder with lazy data by id from envers.
	 */
	public Optional<ContentFolder> getContentFolderWithLazyDataByIdFromEnvers(long folderId) {
		AuditReader auditReader = AuditReaderFactory.get(getEntityManager());
		List<Number> revisionNumbers = auditReader.getRevisions(ContentFolder.class, folderId);
		Number revisionNumber;
		if(revisionNumbers.size()>1) {
			//we don't take the last revision (revisionNumbers.size()-1) because if the folder was deleted the last revision would be null.
			revisionNumber = revisionNumbers.get(revisionNumbers.size() - 2); 
		}else {
			//there is only one revision
			revisionNumber = revisionNumbers.get(0);
		}
		
		ContentFolder result = auditReader.find(ContentFolder.class, folderId, revisionNumber);
		if (result == null) {
			return Optional.empty();
		}
		EntityReflectionHelper.hibernateInitializeObjectGraph(result);

		Folder parent = result.getParentFolder();
		while(parent!=null) {
			parent.getId();
			parent = parent.getParentFolder();
		}

		return Optional.of(result);
	}

	/**
	 * Returns the presentation folder that has no parent. At any time there must be exactly one such folder.
	 *
	 * @return Folder without lazy data
	 */
	@Nonnull
	public PresentationFolder getPresentationRoot() {
		final EntityManager em = getEntityManager();
		final TypedQuery<PresentationFolder> query = em.createNamedQuery(PresentationFolder.QUERY_ROOT,
				PresentationFolder.class);

		return getOneOrZero(query).orElseThrow(() -> new AssertionError("No presentation root folder found."));
	}

	/**
	 * Counts all content folders
	 */
	public long getNoOfContentFolders() {
		final EntityManager em = getEntityManager();
		final TypedQuery<Long> query = em.createNamedQuery(ContentFolder.QUERY_COUNT, Long.class);
		return query.getSingleResult(); // We don't need an Optional here, since count() returns at least 0.
	}

	/**
	 * Counts all presentation folders
	 */
	public long getNoOfPresentationFolder() {
		final EntityManager em = getEntityManager();
		final TypedQuery<Long> query = em.createNamedQuery(PresentationFolder.QUERY_COUNT, Long.class);
		return query.getSingleResult(); // We don't need an Optional here, since count() returns at least 0.
	}

	/**
	 * Returns all folders which are <strong>direct</strong> children of given folder.
	 *
	 * @return Folder list without lazy data
	 * @see #getChildrenContentFolder(ContentFolder)
	 * @see #getChildrenPresentationFolder(PresentationFolder)
	 */
	public List<Folder> getChildrenFolder(Folder parent) {
		Objects.requireNonNull(parent);

		final EntityManager em = getEntityManager();
		final TypedQuery<Folder> query = em.createNamedQuery(Folder.ALL_CHILDREN, Folder.class);
		query.setParameter("parent", parent);

		return query.getResultList();
	}

	/**
	 * Returns all folders which are <strong>direct</strong> children of given folder.
	 *
	 * @return Folder list without lazy data
	 * @see #getChildrenFolder(Folder)
	 * @see #getChildrenPresentationFolder(PresentationFolder)
	 */
	public List<ContentFolder> getChildrenContentFolder(ContentFolder parent) {
		Objects.requireNonNull(parent);

		final EntityManager em = getEntityManager();
		final TypedQuery<ContentFolder> query = em.createNamedQuery(ContentFolder.ALL_CHILDREN, ContentFolder.class);
		query.setParameter("parent", parent);

		return query.getResultList();
	}

	/**
	 * Returns all folders which are <strong>direct</strong> children of given folder.
	 *
	 * @return Folder list without lazy data
	 * @see #getChildrenFolder(Folder)
	 * @see #getChildrenContentFolder(ContentFolder)
	 */
	public List<PresentationFolder> getChildrenPresentationFolder(PresentationFolder parent) {
		Objects.requireNonNull(parent);

		final EntityManager em = getEntityManager();
		final TypedQuery<PresentationFolder> query = em.createNamedQuery(PresentationFolder.ALL_CHILDREN,
				PresentationFolder.class);
		query.setParameter("parent", parent);

		return query.getResultList();
	}

	/**
	 * Loads lazy data for a folder
	 * @see #getPresentationFolderWithLazyData(Folder)
	 */
	public ContentFolder getContentFolderWithLazyData(Folder folder) {
		Objects.requireNonNull(folder);
		return getContentFolderWithLazyData(folder.getId());
	}

	/**
	 * Loads lazy data for a folder using an Id
	 */
	public ContentFolder getContentFolderWithLazyData(long folderId) {
		final EntityManager em = getEntityManager();
		final TypedQuery<ContentFolder> query = em.createNamedQuery(ContentFolder.CONTENT_FOLDER_WITH_LAZY_DATA,
				ContentFolder.class);
		query.setParameter("id", folderId);
		return getOneOrZeroRemovingDuplicates(query)
				.orElseThrow(() -> new IllegalStateException(MISSING_FOLDER_EXCEPTION_MESSAGE));
	}

	/**
	 * Loads lazy data for a folder
	 * @see #getContentFolderWithLazyData(Folder)
	 */
	public PresentationFolder getPresentationFolderWithLazyData(Folder folder) {
		Objects.requireNonNull(folder);

		final EntityManager em = getEntityManager();
		final TypedQuery<PresentationFolder> query = em
				.createNamedQuery(PresentationFolder.PRESENTATION_FOLDER_WITH_LAZY_DATA, PresentationFolder.class);
		query.setParameter("id", folder.getId());

		return getOneOrZeroRemovingDuplicates(query)
				.orElseThrow(() -> new IllegalStateException(MISSING_FOLDER_EXCEPTION_MESSAGE));
	}

	public ContentFolder mergeContentFolder(ContentFolder parent) {
		return baseService.merge(parent);
	}

	public PresentationFolder mergePresentationFolder(PresentationFolder parent) {
		return baseService.merge(parent);
	}

	public Folder mergeFolder(Folder folder) {
		return baseService.merge(folder);
	}

	public Optional<ContentFolder> getContentFolderFor(Exercise exercise) {
		Objects.requireNonNull(exercise);
		return getOneOrZeroRemovingDuplicates( //
				getEntityManager() //
						.createNamedQuery(ContentFolder.CONTENTFOLDER_BY_EXERCISE, ContentFolder.class) //
						.setParameter("exercise", exercise) //
		);
	}
}