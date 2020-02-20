package org.entityrelation.common;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntityRelationOutput implements Serializable {
	
	public static Map<String, Label> ENTITYTYPE = new HashMap<String, Label>();
	public static Map<Integer, Label> ENTITYTYPE_INDEX = new HashMap<Integer, Label>();
	
	public static Label getENTITY(String form){
		if(!ENTITYTYPE.containsKey(form)){
			Label label = new Label(form, ENTITYTYPE.size());
			ENTITYTYPE.put(form, label);
			ENTITYTYPE_INDEX.put(label.id, label);
		}
		return ENTITYTYPE.get(form);
	}
	
	public static Label getENTITY(int id){
		return ENTITYTYPE_INDEX.get(id);
	}
	
	
	public static Map<String, Label> RELATIONS = new HashMap<String, Label>();
	public static Map<Integer, Label> RELATIONS_INDEX = new HashMap<Integer, Label>();
	
	public static Label getRELATIONS(String form){
		if(!RELATIONS.containsKey(form)){
			Label label = new Label(form, RELATIONS.size());
			RELATIONS.put(form, label);
			RELATIONS_INDEX.put(label.id, label);
		}
		return RELATIONS.get(form);
	}
	
	public static Label getRELATIONS(int id){
		return RELATIONS_INDEX.get(id);
	}
	
	public static int getEntityIdx(ArrayList<Entity> entities, Entity entity) {
		for(int i = 0; i < entities.size(); i++) {
			if (entities.get(i).spanEquals(entity))
				return i;
		}
		
		return -1;
	}
	
	
	public static ArrayList<Entity> getEntities(ArrayList<Entity> entities, int leftBoundary, int t) {
		ArrayList<Entity> ret = new ArrayList<Entity>();
		for(Entity entity : entities) {
			if (entity.span[0] == leftBoundary && entity.type.id == t)
				ret.add(entity);
		}
		
		return ret;
	}
	
	public static boolean isOLEntity(ArrayList<Entity> entities, Entity entity) {
		for(int i = 0; i < entities.size(); i++) {
			Entity e = entities.get(i);
			if (!entity.spanEquals(e) && entity.isSpanOL(e))
				return true;
		}
		
		return false;
	}
	
	public void setFlip(int size) {
		for(Entity e : this.entities) {
			e.setFlip(size);
		}
	}
	
	
	
	public static int getRelationIdx(ArrayList<Relation> relations, Relation relation) {
		for(int i = 0; i < relations.size(); i++) {
			if (relations.get(i).spanEquals(relation))
				return i;
		}
		
		return -1;
	}
	
	
	public static ArrayList<Relation> getRelations(ArrayList<Relation> relations, int i, int k, int r, int t1, int t2) {
		ArrayList<Relation> ret = new ArrayList<Relation>();
		for(Relation relation : relations) {
			if (relation.matched(i, k, r, t1, t2))
				ret.add(relation);
		}
		
		return ret;
	}
	
	
	public static ArrayList<Relation> getRelations(ArrayList<Relation> relations, int i, int k) {
		ArrayList<Relation> ret = new ArrayList<Relation>();
		for(Relation relation : relations) {
			if (relation.matched(i, k))
				ret.add(relation);
		}
		
		return ret;
	}
	
	
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 4537195002806101847L;
	
	public ArrayList<Entity> entities = null;
	public ArrayList<Relation> relations = null;

	EntityRelationOutput() {
		this.entities = new ArrayList<Entity>();
		this.relations = new ArrayList<Relation>();
	}
	
	public EntityRelationOutput(ArrayList<Entity> entities, ArrayList<Relation> relations) {
		this.entities = entities;
		this.relations = relations;
	}
	
	public EntityRelationOutput duplicate() {
		return new EntityRelationOutput(this.entities, this.relations);
	}
	
	@Override
	public String toString() {
		return Utils.join(entities, "|") + "\n" + Utils.join(relations, "|") + "\n";
	}
	
	public String toString(boolean removeNoRelation) {
		String retEntityStr = Utils.join(entities, "|") + "\n";
		List<Relation> rel = new ArrayList<Relation>();
		for(Relation relation : relations) {
			if (relation.isNORelation() || relation.isSelfRelation()) {
				rel.add(relation);
			}
		}
		String retRelationStr = Utils.join(rel, "|") + "\n";
		return retEntityStr + retRelationStr;
	}
	
	/*
	public void addEntityRelation(EntityRelationOutput o, boolean removeNORelation) {
		for(Entity entity : o.entities) {
			int p = this.getEntityIdx(this.entities, entity);
			if (p < 0) {
				this.entities.add(entity);
			}
		}
		
		for(Relation relation : o.relations) {
			
			if (relation.isNORelation())
				continue;
			
			int p = this.getEntityIdx(this.entities, relation.arg1);
			if (p < 0) {
				this.entities.add(relation.arg1);
			} else {
				relation.arg1 = this.entities.get(p);
			}
			
			p = this.getEntityIdx(this.entities, relation.arg2);
			if (p < 0) {
				this.entities.add(relation.arg2);
			} else {
				relation.arg2 = this.entities.get(p);
			}
			
			p = this.getRelationIdx(this.relations, relation);
			if (p < 0) {
				this.relations.add(relation);
			}
		}
	}
	*/
	
	public void addRelations(ArrayList<Relation> relations, boolean removeNORelation) {
		for(Relation relation : relations) {
			
			if (removeNORelation)
				if (relation.isNORelation())
					continue;
			
			int p = -1;
			if (EntityRelationGlobal.REMOVE_DUPLICATE) {
				p = this.getRelationIdx(this.relations, relation);
			}
			if (p < 0) {
				this.relations.add(relation);
			}
		}
	}

}
