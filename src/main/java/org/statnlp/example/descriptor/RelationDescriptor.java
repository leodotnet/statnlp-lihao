package org.statnlp.example.descriptor;

public class RelationDescriptor {
	
	public RelationType type;
	// the left and right index of the relation descriptor.
	protected int left;
	protected int right;
	
	public RelationDescriptor(RelationType type) {
		this(type, -1, -1);
	}
	
	public RelationDescriptor(RelationType type, int left, int right) {
		this.type = type;
		this.left = left;
		this.right = right;
	}

	public RelationType getType() {
		return type;
	}

	public int getLeft() {
		return left;
	}

	public int getRight() {
		return right;
	}

	public void setType(RelationType type) {
		this.type = type;
	}

	public void setLeft(int left) {
		this.left = left;
	}

	public void setRight(int right) {
		this.right = right;
	}

	@Override
	public String toString() {
		return "RelationDescriptor [type=" + type + ", left=" + left + ", right=" + right + "]";
	}
	
	
	
}