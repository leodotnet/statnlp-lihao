package org.statnlp.example.descriptor;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class SpanType implements Comparable<SpanType>, Serializable{
	
	private static final long serialVersionUID = -3314363044582374266L;
	public static final Map<String, SpanType> SPANS = new HashMap<String, SpanType>();
	public static final Map<Integer, SpanType> SPANS_INDEX = new HashMap<Integer, SpanType>();
	public static boolean locked = false;
	
	public static SpanType get(String form){
		if(!SPANS.containsKey(form)){
			if (!locked) {
				SpanType label = new SpanType(form, SPANS.size());
				SPANS.put(form, label);
				SPANS_INDEX.put(label.id, label);
			} else {
				throw new RuntimeException("the map is locked");
			}
		}
		return SPANS.get(form);
	}
	
	public static SpanType get(int id){
		if (!SPANS_INDEX.containsKey(id))
			throw new RuntimeException("the map does not have id: "+ id);
		return SPANS_INDEX.get(id);
	}
	
	public static void lock () {
		locked = true;
	}
	
	public String form;
	public int id;
	
	private SpanType(String form, int id) {
		this.form = form;
		this.id = id;
	}

	@Override
	public int hashCode() {
		return form.hashCode();
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!(obj instanceof SpanType))
			return false;
		SpanType other = (SpanType) obj;
		if (form == null) {
			if (other.form != null)
				return false;
		} else if (!form.equals(other.form))
			return false;
		return true;
	}
	
	public String toString(){
		return String.format("%s(%d)", form, id);
	}

	@Override
	public int compareTo(SpanType o) {
		return Integer.compare(id, o.id);
	}
	
	public static int compare(SpanType o1, SpanType o2){
		if(o1 == null){
			if(o2 == null) return 0;
			else return -1;
		} else {
			if(o2 == null) return 1;
			else return o1.compareTo(o2);
		}
	}
}
