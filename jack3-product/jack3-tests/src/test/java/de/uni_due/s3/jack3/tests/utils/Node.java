package de.uni_due.s3.jack3.tests.utils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * This simple class simulates a Tree and can be used to navigate through the children without explicit lazy-loading.
 * 
 * @author lukas.glaser
 */
public class Node<T> {

	/**
	 * Constructs a new node with the given data.
	 */
	public Node(T data) {
		this.data = data;
		this.parent = null;
	}

	private Node(T data, Node<T> parent) {
		this.data = data;
		this.parent = parent;
	}
	
	private final T data;
	private final Node<T> parent;
	private List<Node<T>> children = new ArrayList<>();
	
	public T getData() {
		return data;
	}

	public Node<T> getParent() {
		return parent;
	}

	/**
	 * Adds a new child to the node.
	 * 
	 * @return New constructed node from the passed data.
	 */
	public Node<T> addChild(T data) {
		final Node<T> newNode = new Node<>(data, this);
		children.add(newNode);
		return newNode;
	}

	/**
	 * @return All direct children of the node.
	 */
	public List<Node<T>> getChildren() {
		return Collections.unmodifiableList(children);
	}

	/**
	 * @return All direct and indirect children of the node.
	 */
	public List<Node<T>> getAllChildren() {
		final List<Node<T>> result = new ArrayList<>();

		final Queue<Node<T>> toProcess = new LinkedList<>();
		toProcess.add(this);

		Node<T> currentNode;
		while ((currentNode = toProcess.poll()) != null) {
			result.addAll(currentNode.children);
			toProcess.addAll(currentNode.children);
		}
		return result;
	}

	/**
	 * @return All direct and indirect children data of the node.
	 */
	public List<T> getAllChildrenData() {
		final List<T> result = new ArrayList<>();

		final Queue<Node<T>> toProcess = new LinkedList<>();
		toProcess.add(this);

		Node<T> currentNode;
		while ((currentNode = toProcess.poll()) != null) {
			for (Node<T> child : currentNode.getChildren()) {
				result.add(child.data);
			}
			toProcess.addAll(currentNode.getChildren());
		}
		return result;
	}

}
