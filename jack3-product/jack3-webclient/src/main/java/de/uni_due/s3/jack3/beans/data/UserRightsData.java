package de.uni_due.s3.jack3.beans.data;

import java.io.Serializable;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;

import de.uni_due.s3.jack3.entities.AccessRight;
import de.uni_due.s3.jack3.entities.tenant.Folder;
import de.uni_due.s3.jack3.entities.tenant.User;
import de.uni_due.s3.jack3.entities.tenant.UserGroup;

public class UserRightsData implements Serializable {

	private static final long serialVersionUID = -1827848554824122190L;

	private User user;
	private UserGroup userGroup;

	private Folder folder;
	private String folderAlias;

	private boolean readRights;
	private boolean extendedReadRights;
	private boolean writeRights;
	private boolean gradeRights;
	private boolean manageRights;
	private boolean inheritedReadRights;
	private boolean inheritedExtendedReadRights;
	private boolean inheritedWriteRights;
	private boolean inheritedGradeRights;
	private boolean inheritedManageRights;

	private final boolean immutable;

	public UserRightsData(User user, Folder folder, String folderAlias, @CheckForNull AccessRight ownRights,
			@CheckForNull AccessRight inheritedRights, boolean immutable) {
		this.user = user;
		this.folder = folder;
		this.folderAlias = folderAlias;
		this.immutable = immutable;

		updateData(ownRights, inheritedRights);
	}

	public UserRightsData(UserGroup userGroup, Folder folder, String folderAlias, @CheckForNull AccessRight ownRights,
			@CheckForNull AccessRight inheritedRights, boolean immutable) {
		this.userGroup = userGroup;
		this.folder = folder;
		this.folderAlias = folderAlias;
		this.immutable = immutable;

		updateData(ownRights, inheritedRights);

	}

	public Folder getFolder() {
		return folder;
	}

	public String getFolderAlias() {
		return folderAlias;
	}

	@Nonnull
	public AccessRight getRights() {
		AccessRight result = AccessRight.getNone();
		if (readRights)
			result = result.add(AccessRight.READ);
		if (extendedReadRights)
			result = result.add(AccessRight.EXTENDED_READ);
		if (writeRights)
			result = result.add(AccessRight.WRITE);
		if (gradeRights)
			result = result.add(AccessRight.GRADE);
		if (manageRights)
			result = result.add(AccessRight.MANAGE);
		return result;
	}

	public User getUser() {
		return user;
	}

	public UserGroup getUserGroup() {
		return userGroup;
	}

	public boolean isExtendedReadRights() {
		return extendedReadRights;
	}

	public boolean isImmutable() {
		return immutable;
	}

	public boolean isInheritedExtendedReadRights() {
		return inheritedExtendedReadRights;
	}

	public boolean isInheritedReadRights() {
		return inheritedReadRights;
	}

	public boolean isInheritedWriteRights() {
		return inheritedWriteRights;
	}

	public boolean isInheritedGradeRights() {
		return inheritedGradeRights;
	}

	public boolean isInheritedManageRights() {
		return inheritedManageRights;
	}

	public boolean isReadRights() {
		return readRights;
	}

	public boolean isWriteRights() {
		return writeRights;
	}

	public boolean isGradeRights() {
		return gradeRights;
	}

	public boolean isManageRights() {
		return manageRights;
	}

	public void setExtendedReadRights(boolean extendedReadRights) {
		this.extendedReadRights = extendedReadRights;
	}

	public void setFolder(Folder folder) {
		this.folder = folder;
	}

	public void setFolderAlias(String folderAlias) {
		this.folderAlias = folderAlias;
	}

	public void setInheritedExtendedReadRights(boolean inheritedExtendedReadRights) {
		this.inheritedExtendedReadRights = inheritedExtendedReadRights;
	}

	public void setInheritedReadRights(boolean inheritedReadRights) {
		this.inheritedReadRights = inheritedReadRights;
	}

	public void setInheritedWriteRights(boolean inheritedWriteRights) {
		this.inheritedWriteRights = inheritedWriteRights;
	}

	public void setInheritedGradeRights(boolean inheritedGradeRights) {
		this.inheritedGradeRights = inheritedGradeRights;
	}

	public void setInheritedManageRights(boolean inheritedManageRights) {
		this.inheritedManageRights = inheritedManageRights;
	}

	public void setReadRights(boolean readRights) {
		this.readRights = readRights;
	}

	public void setUser(User user) {
		this.user = user;
	}

	public void setUserGroup(UserGroup userGroup) {
		this.userGroup = userGroup;
	}

	public void setWriteRights(boolean writeRights) {
		this.writeRights = writeRights;
	}

	public void setGradeRights(boolean gradeRights) {
		this.gradeRights = gradeRights;
	}

	public void setManageRights(boolean manageRights) {
		this.manageRights = manageRights;
	}

	public void updateData(@CheckForNull AccessRight ownRights, @CheckForNull AccessRight inheritedRights) {
		if (ownRights == null)
			ownRights = AccessRight.getNone();
		if (inheritedRights == null)
			inheritedRights = AccessRight.getNone();

		readRights = ownRights.isRead();
		extendedReadRights = ownRights.isExtendedRead();
		writeRights = ownRights.isWrite();
		gradeRights = ownRights.isGrade();
		manageRights = ownRights.isManage();

		inheritedReadRights = inheritedRights.isRead();
		inheritedExtendedReadRights = inheritedRights.isExtendedRead();
		inheritedWriteRights = inheritedRights.isWrite();
		inheritedGradeRights = inheritedRights.isGrade();
		inheritedManageRights = inheritedRights.isManage();
	}

	public boolean isReadRightImmutable() {
		return extendedReadRights || writeRights || gradeRights || manageRights;
	}

	public boolean isExtendedReadRightImmutable() {
		return manageRights;
	}

	public boolean isWriteRightImmutable() {
		return manageRights;
	}

	public boolean isGradeRightImmutable() {
		return manageRights;
	}

}
