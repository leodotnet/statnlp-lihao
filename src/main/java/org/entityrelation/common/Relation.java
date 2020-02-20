package org.entityrelation.common;

import java.io.Serializable;
import java.util.ArrayList;

public class Relation implements Serializable {
	
	public static String SELF_RELATION = "Self-Relation";
	
	public static String NO_RELATION = "No-Relation";
	
	public static String REVERSE_PREFIX = "R-";
	/**
	 * 
	 */
	private static final long serialVersionUID = 5983237650795179875L;
	public Label type = null;
	public Entity arg1 = null;
	public Entity arg2 = null;
	
	public int arg1Idx = -1;
	public int arg2Idx = -1;

	
	public Relation() {
		
	}
	
	/*
	public Relation(ArrayList<Entity> entities, String relationStr) {
		String[] fields = relationStr.split(" ");
		String relationTypeStr = fields[0].split("::")[0];
		type = EntityRelationOutput.getRELATIONS(relationTypeStr);
		
		Entity entity1 = new Entity(fields[1] + " " + fields[2]);
		Entity entity2 = new Entity(fields[3] + " " + fields[4]);
		
		arg1Idx = EntityRelationOutput.getEntityIdx(entities, entity1);
		arg1 = entities.get(arg1Idx);
		
		arg2Idx = EntityRelationOutput.getEntityIdx(entities, entity2);
		arg2 = entities.get(arg2Idx);
	}
	*/
	
	public static Relation parseRelation(ArrayList<Entity> entities, String relationStr) {
		
		Relation relation = new Relation();
		
		String[] fields = relationStr.split(" ");
		String relationTypeStr = fields[0].split("::")[0];
		relation.type = EntityRelationOutput.getRELATIONS(relationTypeStr);
		
		Entity entity1 = new Entity(fields[1] + " " + fields[2]);
		Entity entity2 = new Entity(fields[3] + " " + fields[4]);
		
		relation.arg1Idx = EntityRelationOutput.getEntityIdx(entities, entity1);
		if (relation.arg1Idx < 0)
			return null;
		relation.arg1 = entities.get(relation.arg1Idx);
		
		relation.arg2Idx = EntityRelationOutput.getEntityIdx(entities, entity2);
		if (relation.arg2Idx < 0)
			return null;
		relation.arg2 = entities.get(relation.arg2Idx);
		
		return relation;
	}
	
	
	public Relation(ArrayList<Entity> entities, String typeStr, Entity entity1, Entity entity2) {
		
		arg1Idx = EntityRelationOutput.getEntityIdx(entities, entity1);
		arg1 = entities.get(arg1Idx);
		
		arg2Idx = EntityRelationOutput.getEntityIdx(entities, entity2);
		arg2 = entities.get(arg2Idx);
		
		type = EntityRelationOutput.getRELATIONS(typeStr);  
		
	}
	
	
	public Relation(ArrayList<Entity> entities, int r, Entity entity1, Entity entity2) {
		
		arg1Idx = EntityRelationOutput.getEntityIdx(entities, entity1);
		arg1 = entities.get(arg1Idx);
		
		arg2Idx = EntityRelationOutput.getEntityIdx(entities, entity2);
		arg2 = entities.get(arg2Idx);
		
		type = EntityRelationOutput.getRELATIONS(r);  
		
		if (type == null) {
			System.out.println("r=" + r);
			try {
				throw new Exception();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(-1);
			}
		}
		
	}
	
	public int[] getRelationArr() {
		return new int[] {this.type.id, this.arg1.type.id, this.arg2.type.id};
	}
	
	public static Relation buildRelation(ArrayList<Entity> entities, int r, Entity entity1, Entity entity2) {
		return new Relation(entities, r, entity1, entity2);
	}
	
	public static Relation buildSelfRelation(ArrayList<Entity> entities, Entity entity) {
		return new Relation(entities, SELF_RELATION, entity, entity);
	}
	
	public static Relation buildNoRelation(ArrayList<Entity> entities, Entity entity1, Entity entity2) {
		return new Relation(entities, NO_RELATION, entity1, entity2);
	}
	
	public Relation getReverseRelation() {
		
		Relation relation = new Relation();
		relation.arg1 = this.arg2;
		relation.arg1Idx = this.arg2Idx;
		relation.arg2 = this.arg1;
		relation.arg2Idx = this.arg1Idx;
		String typeStr = this.type.form;
		if (!typeStr.startsWith(REVERSE_PREFIX)) {
			relation.type = EntityRelationOutput.getRELATIONS(REVERSE_PREFIX + typeStr);
		} else {
			relation.type = EntityRelationOutput.getRELATIONS(typeStr.replace(REVERSE_PREFIX, ""));
		}
		return relation;
	}
	
	public boolean isReversedRelation() {
		return this.type.form.startsWith(REVERSE_PREFIX);
	}
	
	public void normalizeRelation() {
		String typeStr = this.type.form;
		if (this.isReversedRelation()){	
			Entity arg1 = this.arg2;
			int arg1Idx = this.arg2Idx;
			Entity arg2 = this.arg1;
			int arg2Idx = this.arg1Idx;
			
			this.arg1 = arg1;
			this.arg1Idx = arg1Idx;
			this.arg2 = arg2;
			this.arg2Idx = arg2Idx;
			
			this.type = EntityRelationOutput.getRELATIONS(typeStr.replace(REVERSE_PREFIX, ""));
		}
	}
	
	@Override
	public boolean equals(Object relation) {
		Relation o = (Relation)relation;
		return this.type.equals(o.type) && this.arg1.equals(o.arg1) && this.arg2.equals(o.arg2);
	}
	
	public boolean spanEquals(Relation relation) {
		Relation o = (Relation)relation;
		return this.type.equals(o.type) && this.arg1.spanEquals(o.arg1) && this.arg2.spanEquals(o.arg2);
	}
	
	@Override
	public String toString() {
		return type.form + " " + arg1 + " " + arg2;
	}
	
	public boolean matched(int i, int k, int r, int t1, int t2) {
		Label relationType = EntityRelationOutput.getRELATIONS(r);
		Label entity1Type = EntityRelationOutput.getENTITY(t1);
		Label entity2Type = EntityRelationOutput.getENTITY(t2);
		return (arg1.span[0] == i && arg2.span[0] == k && type.equals(relationType) && arg1.type.equals(entity1Type) && arg2.type.equals(entity2Type));
	}
	
	public boolean matched(int i, int k) {
		return (arg1.span[0] == i && arg2.span[0] == k);
	}
	
	
	public boolean isSelfRelation() {
		return EntityRelationGlobal.ADD_SELF_RELATION && type.form.equals(SELF_RELATION);
	}

	public boolean isNORelation() {
		return EntityRelationGlobal.ADD_NO_RELATION && type.form.equals(NO_RELATION);
	}
	
	public static boolean isNORelation(int r) {
		return EntityRelationGlobal.ADD_NO_RELATION && EntityRelationOutput.getRELATIONS(r).form.equals(NO_RELATION);
	}
	
	public static boolean isSelfRelation(int r) {
		return EntityRelationGlobal.ADD_SELF_RELATION && EntityRelationOutput.getRELATIONS(r).form.equals(SELF_RELATION);
	}

	
}
