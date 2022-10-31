package de.uni_due.s3.jack3.beans;

import java.io.Serializable;
import java.util.List;

import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.inject.Inject;
import javax.inject.Named;
import javax.persistence.Transient;
import javax.transaction.Transactional;

import de.uni_due.s3.jack3.services.LiveHqlService;
import de.uni_due.s3.jack3.utils.StopWatch;

@Named
@ViewScoped
public class LiveHqlBean extends AbstractView implements Serializable {

	/**
	 * generated serial
	 */
	private static final long serialVersionUID = 8481340927878339334L;

	@Inject
	private LiveHqlService liveHqlService;

	private String query;
	private boolean singleResult;

	@Transient
	private List<?> results;

	private final StopWatch sw = new StopWatch();

	@Transactional
	public void executeQuery() {
		try {
			if (singleResult) {
				sw.reset().start();
				results = liveHqlService.getSingleResult(query);
				sw.stop();
			} else {
				sw.reset().start();
				results = liveHqlService.getResultList(query);
				sw.stop();
			}

			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_INFO, "Success!",
							"The query '" + query + "' was successful executed as "
									+ ((singleResult) ? "single" : "list") + " result."));

		}
		catch (final Exception e) {
			FacesContext.getCurrentInstance().addMessage(null,
					new FacesMessage(FacesMessage.SEVERITY_ERROR, "Error!", e.getMessage()));
			getLogger().error("Error on executing query '" + query + "'", e);
			results = null;
		}
	}

	public String getQuery() {
		return query;
	}

	public List<?> getResults() {
		return results;
	}

	public boolean isSingleResult() {
		return singleResult;
	}

	public void setQuery(final String query) {
		this.query = query;
	}

	public void setSingleResult(final boolean singleResult) {
		this.singleResult = singleResult;
	}

	public String getDuration() {
		return sw.getElapsedMilliseconds();
	}
}