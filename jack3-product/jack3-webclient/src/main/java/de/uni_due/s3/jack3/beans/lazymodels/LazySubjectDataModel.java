package de.uni_due.s3.jack3.beans.lazymodels;

import de.uni_due.s3.jack3.entities.tenant.Subject;
import de.uni_due.s3.jack3.services.BaseService;
import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import javax.enterprise.inject.spi.CDI;
import java.util.List;
import java.util.Map;

/**
 *
 * @author Christopher.Ridder
 *
 */
public class LazySubjectDataModel extends LazyDataModel<Subject> {

	private static final long serialVersionUID = 1L;

	@Override
	public List<Subject> load(int first, int pageSize, String sortField, SortOrder sortOrder,
			Map<String, FilterMeta> filterBy) {
		String sortOrderString = sortOrder == SortOrder.DESCENDING ? "DESC" : "ASC";

		BaseService baseService = CDI.current().select(BaseService.class).get();
		setRowCount(Math.toIntExact(baseService.countAll(Subject.class)));
		return baseService.findAllInRange(Subject.class, first, pageSize, sortOrderString);
	}
}
