package de.uni_due.s3.jack3.entities.maintenance;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;

import javax.persistence.Column;
import javax.persistence.Entity;

import org.hibernate.envers.Audited;

import de.uni_due.s3.jack3.annotations.ToString;
import de.uni_due.s3.jack3.entities.AbstractEntity;

@Audited
@Entity
public class TempDir extends AbstractEntity {
	private static final long serialVersionUID = 7602080311028673021L;

	@ToString
	@Column
	String path;

	@ToString
	@Column
	LocalDateTime created;

	public TempDir() {

	}

	public TempDir(Path path, LocalDateTime created) {
		this.path = path.toString();
		this.created = created;
	}

	public Path getPath() {
		return Paths.get(path);
	}

	public void setPath(Path path) {
		this.path = path.toString();
	}

	public LocalDateTime getCreated() {
		return created;
	}

	public void setCreated(LocalDateTime created) {
		this.created = created;
	}

}
