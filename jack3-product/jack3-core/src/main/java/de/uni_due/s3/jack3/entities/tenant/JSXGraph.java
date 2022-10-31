package de.uni_due.s3.jack3.entities.tenant;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;

import org.hibernate.annotations.Type;
import org.hibernate.envers.Audited;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import de.uni_due.s3.jack3.interfaces.DeepCopyable;

import de.uni_due.s3.jack3.entities.AbstractEntity;

@Audited
@Entity
@XStreamAlias("JSXGraph")
@Inheritance(strategy = InheritanceType.TABLE_PER_CLASS)
public class JSXGraph extends AbstractEntity implements DeepCopyable<JSXGraph>{
	
	private static final long serialVersionUID = -420361994452685699L;
	
	public static final int DEFAULT_WIDTH = 400;
	public static final int DEFAULT_HEIGHT = 400;

	@Column(nullable = false)
	@Type(type = "text")
	protected String name;
	
	@Column
	protected int orderIndex;
	
	@Column
	private int width;
	
	@Column
	private int height;
	
	@Column
	@Type(type = "text")
	private String text;
	
	public JSXGraph() {
		
	}
	
	public JSXGraph(String name, int orderIndex) {
		setName(name);
		this.orderIndex = orderIndex;
		width = DEFAULT_WIDTH;
		height = DEFAULT_HEIGHT;
	}
	
	public String getName() {
		return name;
	}
	
	public void setName(String name) {
		this.name = requireIdentifier(name, "name must be a non empty string.");
	}
	
	public int getOrderIndex() {
		return orderIndex;
	}
	
	public void setOrderIndex(int orderIndex) {
		this.orderIndex = orderIndex;
	}
	
	public int getWidth() {
		return width;
	}
	
	public void setWidth(int width) {
		this.width = width;
	}
	
	public int getHeight() {
		return height;
	}
	
	public void setHeight(int height) {
		this.height = height;
	}
	
	public String getText() {
		return text;
	}
	
	public void setText(String text) {
		this.text = text;
	}
	
	@Override
	public JSXGraph deepCopy() {
		JSXGraph deepCopy = new JSXGraph();
		
		deepCopy.name = name;
		deepCopy.orderIndex = orderIndex;
		deepCopy.width = width;
		deepCopy.height = height;
		deepCopy.text = text;
		
		return deepCopy;
	}
}