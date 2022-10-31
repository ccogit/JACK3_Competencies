package de.uni_due.s3.jack3.beans;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.StringJoiner;

import javax.faces.application.FacesMessage;
import javax.inject.Inject;

import org.primefaces.PrimeFaces;
import org.primefaces.model.TreeNode;

import de.uni_due.s3.jack3.business.AuthorizationBusiness;
import de.uni_due.s3.jack3.entities.AbstractEntity;
import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.interfaces.Namable;
import de.uni_due.s3.jack3.utils.StopWatch;

/**
 * This class contains common methods for the tree views in {@link MyWorkspaceView} (Exercises, Courses and Content
 * Folders) and {@link AvailableCoursesView} (Course Offers and Presentation Folders).
 * 
 * @author lukas.glaser
 */
public abstract class AbstractContentTreeView<T extends Folder> extends AbstractView implements Serializable {

	private static final long serialVersionUID = 4912483282634614843L;

	@Inject
	private UserRightsDialogView userRightsDialog;

	@Inject
	protected AuthorizationBusiness authorizationBusiness;
	
	protected final HashMap<Folder, String> shownAccessRights = new HashMap<>();
	protected final HashMap<Long, String> searchStrings = new HashMap<>();

	// ---------------------------------
	// ---------- User rights ----------
	// ---------------------------------

	/**
	 * Should return Folders with the rights the currently logged in user has on them.
	 */
	protected abstract Map<T, AccessRight> computeFolderRightsMap();

	/**
	 * Should return the current selected folder.
	 */
	protected abstract Optional<T> getSelectedFolder();

	public String getShownAccessRight(final T folder) {
		return shownAccessRights.get(folder);
	}

	public boolean isShowRightsLegend() {
		return !shownAccessRights.isEmpty();
	}

	protected void refreshShownAccessRights() {
		shownAccessRights.clear();
		final Map<T, AccessRight> folderRightsMap = computeFolderRightsMap();
		for (final Entry<T, AccessRight> entry : folderRightsMap.entrySet()) {

			// Only show the right if it differs from the parent right
			final Folder parent = entry.getKey().getParentFolder();
			if (parent == null || !entry.getValue().equals(folderRightsMap.get(parent))) {

				// Produces a concatenation of rights in the form of single letters
				// e.g. "(R,E)" for READ and EXTEDED_READ
				final StringJoiner rightJoiner = new StringJoiner(",", " (", ")");
				if (entry.getValue().isRead())
					rightJoiner.add(getLocalizedMessage("AccessRight.letter.READ"));
				if (entry.getValue().isExtendedRead())
					rightJoiner.add(getLocalizedMessage("AccessRight.letter.EXTENDED_READ"));
				if (entry.getValue().isWrite())
					rightJoiner.add(getLocalizedMessage("AccessRight.letter.WRITE"));
				if (entry.getValue().isGrade())
					rightJoiner.add(getLocalizedMessage("AccessRight.letter.GRADE"));
				if (entry.getValue().isManage())
					rightJoiner.add(getLocalizedMessage("AccessRight.letter.MANAGE"));

				shownAccessRights.put(entry.getKey(), rightJoiner.toString());
			}
		}
	}

	public void openUserRightsDialog() {
		final var folder = getSelectedFolder();
		if (folder.isEmpty()) {
			return;
		}
		if (authorizationBusiness.canManage(getCurrentUser(), folder.get())) {
			userRightsDialog.loadDialog(folder.get());
			PrimeFaces.current().executeScript("PF('editRightsDialog').show();");
		} else {
			addGlobalFacesMessage(FacesMessage.SEVERITY_ERROR, "start.missingEditRights",
					"start.missingEditRightsDetails");
		}
	}

	// ----------------------------
	// ---------- Search ----------
	// ----------------------------

	/**
	 * Should return all Entities that are saved in the tree.
	 */
	protected abstract List<AbstractEntity> computeEntitiesInTree();

	/**
	 * This method can be overridden if it is possible that the name of a namable Entity does not match the search
	 * string name.
	 */
	protected String getRealNameOfEntity(Namable namableEntity) {
		return namableEntity.getName();
	}

	/**
	 * Computes a search string for an Entity. By default, the name of the Entity is added
	 */
	protected void generateSearchString(AbstractEntity entity, StringJoiner joiner) {
		if (entity instanceof Namable) {
			joiner.add(getRealNameOfEntity((Namable) entity));
		}
	}

	public void updateAllSearchStrings() {
		searchStrings.clear();
		StopWatch watch = new StopWatch().start();
		for (AbstractEntity entity : computeEntitiesInTree()) {
			StringJoiner joiner = new StringJoiner("\n");
			generateSearchString(entity, joiner);
			searchStrings.put(entity.getId(), joiner.toString());
		}
		getLogger().debugf("Updating the search strings took %s.", watch.stop().getElapsedMilliseconds());
	}

	protected void updateSingleSearchString(AbstractEntity entity) {
		StringJoiner joiner = new StringJoiner("\n");
		generateSearchString(entity, joiner);
		searchStrings.put(entity.getId(), joiner.toString());
	}

	/**
	 * Returns the search String for an object that was computed by
	 * {@link #generateSearchString(AbstractEntity, StringJoiner)} previously.
	 */
	public String getSearchString(Object nodeData) {
		if (nodeData instanceof TreeNode) {
			nodeData = ((TreeNode) nodeData).getData();
		}
		if (!(nodeData instanceof AbstractEntity)) {
			return null;
		}

		final AbstractEntity entity = (AbstractEntity) nodeData;
		return searchStrings.get(entity.getId());
	}

}
