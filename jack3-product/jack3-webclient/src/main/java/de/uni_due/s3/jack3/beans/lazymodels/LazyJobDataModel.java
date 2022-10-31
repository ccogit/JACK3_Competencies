package de.uni_due.s3.jack3.beans.lazymodels;

import java.util.List;
import java.util.Map;

import javax.enterprise.inject.spi.CDI;

import org.primefaces.model.FilterMeta;
import org.primefaces.model.LazyDataModel;
import org.primefaces.model.SortOrder;

import de.uni_due.s3.jack3.entities.tenant.Job;
import de.uni_due.s3.jack3.services.BaseService;

/**
 *
 * @author Benjamin.Otto
 *
 */
public class LazyJobDataModel extends LazyDataModel<Job> {

	private static final long serialVersionUID = 1L;

	@Override
	public List<Job> load(int first, int pageSize, String sortField, SortOrder sortOrder,
			Map<String, FilterMeta> filterBy) {
		String sortOrderString = sortOrder == SortOrder.DESCENDING ? "DESC" : "ASC";

		BaseService baseService = CDI.current().select(BaseService.class).get();
		setRowCount(Math.toIntExact(baseService.countAll(Job.class)));
		return baseService.findAllInRange(Job.class, first, pageSize, sortOrderString);
	}
}
