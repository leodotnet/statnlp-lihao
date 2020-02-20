package org.entityrelation.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;


public class Entity implements Serializable {
	
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 2761383967533952691L;
	
	public int[] span = new int[] {-1 , -1};
	public int[] head = new int[] {-1 , -1};
	public Label type = null;
	public boolean ol = false;
	public int entityIdx = -1;
	public int headIdx = -1;
	
	public Entity() {
		
	}
	
	public Entity(int[] span, int[] head, Label type) {
		this.span = span;
		this.head = head;
		this.type = type;
	}
	
	
	
	public Entity(String entityStr) {
		String[] fields = entityStr.split(" ");
		String[] boundary = fields[0].split(",");
		span[0] = Integer.parseInt(boundary[0]);
		span[1] = Integer.parseInt(boundary[1]);
		head[0] = Integer.parseInt(boundary[2]);
		head[1] = Integer.parseInt(boundary[3]);
		
		if (EntityRelationGlobal.HEAD_AS_SPAN) {
			span[0] = head[0];
			span[1] = head[1];
		}
		
		String typeStr = fields[1];
		
		type = EntityRelationOutput.getENTITY(typeStr);
	}
	
	public void setFlip(int size) {
		this.span = new int[] {size - span[1], size  - span[0] };
		this.head = new int[] {size  - head[1] , size - head[1] };
	}
	
	@Override
	public boolean equals(Object e) {
		Entity o = (Entity)e;
		return Arrays.equals(span, o.span) && Arrays.equals(head, o.head) && type.equals(o.type);
	}
	
	public boolean spanEquals(Object e) {
		Entity o = (Entity)e;
		return Arrays.equals(span, o.span) && type.equals(o.type);
	}
	
	public boolean onlySpanEquals(Object e) {
		Entity o = (Entity)e;
		return Arrays.equals(span, o.span);
	}
	
	public void setOL() {
		setOL(true);
	}
	
	public void setOL(boolean ol) {
		this.ol = ol;
	}
	
	
	public boolean isSpanOL(Entity e) {
		return (this.span[0] > e.span[0] && this.span[1] <= e.span[1]) || (this.span[0] >= e.span[0] && this.span[1] < e.span[1]);
	}
	
	
	
	public int getSpanLength() {
		return span[1] - span[0];
	}
	
	public int getHeadLength() {
		return head[1] - head[0];
	}
	
	@Override
	public String toString() {
		return span[0] + "," + span[1] + "," + head[0] + "," + head[1] + " " + type.form;
	}
	
	public int comparePosition(Entity o) {
		if(span[0] < o.span[0]) return -1;
		if(span[0] > o.span[0]) return 1;
		if(span[1] < o.span[1]) return -1;
		if(span[1] > o.span[1]) return 1;
		return 0;
	}
	
	public static void setHeadIdx (ArrayList<String[]> input, Entity entity, boolean lastWordAsHead) {
		//a mention is from the start to the head end according to Zhou 2005
		if (!lastWordAsHead) {
			for (int i = entity.span[0]; i < entity.span[1]; i++) {
				if (i > entity.span[0] && input.get(i)[1].equals("IN")) {
					entity.headIdx = i - 1;
					return;
				}
			}
		}
		entity.headIdx = entity.span[1] - 1;
	}
	
	
	public Entity setHeadIdx(ArrayList<String[]> input, boolean lastWordAsHead) {
		//a mention is from the start to the head end according to Zhou 2005
		if (!lastWordAsHead) {
			for (int i = this.span[0]; i < this.span[1]; i++) {
				if (i > this.span[0] && input.get(i)[1].equals("IN")) {
					this.headIdx = i - 1;
					return this;
				}
			}
		}
		this.headIdx = this.span[1] - 1;
		return this;
	}
	
}
