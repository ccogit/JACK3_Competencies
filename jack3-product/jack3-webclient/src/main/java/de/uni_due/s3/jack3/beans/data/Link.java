package de.uni_due.s3.jack3.beans.data;

public class Link {

	private String url;
	private String value;

	public Link(String url) {
		setUrl(url);
	}

	public Link(String url, String value) {
		setUrl(url);
		setValue(value);
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getValue() {
		return value;
	}

	public void setValue(String value) {
		this.value = value;
	}

}
