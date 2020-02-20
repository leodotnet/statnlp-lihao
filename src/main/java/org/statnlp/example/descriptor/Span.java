package org.statnlp.example.descriptor;


public class Span implements Comparable<Span>{

	public String entity;
	public int start;
	public int end;
	
	public int headIdx;
	
	public SpanType type;
	
	public static boolean HEAD_AS_SPAN = false;
	
	/**
	 * Span constructor
	 * @param start: inclusive
	 * @param end: inclusive
	 * @param entity
	 */
	public Span(int start, int end, String entity) {
		this.start = start;
		this.end = end;
		this.entity = entity;
	}
	
	public Span(int start, int end, String entity, SpanType type) {
		this.start = start;
		this.end = end;
		this.entity = entity;
		this.type = type;
	}
	
	public boolean equals(Object o){
		if(o instanceof Span){
			Span s = (Span)o;
			if(start != s.start) return false;
			if(end != s.end) return false;
			return entity.equals(s.entity);
		}
		return false;
	}

	@Override
	public int compareTo(Span o) {
		if(start < o.start) return -1;
		if(start > o.start) return 1;
		if(end < o.end) return -1;
		if(end > o.end) return 1;
		return entity.compareTo(o.entity);
	}
	
	public int comparePosition(Span o) {
		if(start < o.start) return -1;
		if(start > o.start) return 1;
		if(end < o.end) return -1;
		if(end > o.end) return 1;
		return 0;
	}
	
	public boolean overlap(Span other) {
		if (other.start > this.end) return false;
		if (other.end < this.start) return false;
		return true;
	}
	
	public String toString(){
		return String.format("%d,%d %s", start, end, entity);
	}
	
	public static Span parseSpan(String spanStr, boolean endInclusive) {
		String[] tmp = spanStr.split(" ");
		SpanType type = SpanType.get(tmp[1]);
		String[] posInfo = tmp[0].split(",");
		int start = Integer.parseInt(posInfo[0]);
		int end = Integer.parseInt(posInfo[1]) + (endInclusive ? 0 : -1);
		int headStart = Integer.parseInt(posInfo[2]);
		int headEnd = Integer.parseInt(posInfo[3]) + (endInclusive ? 0 : -1);
		
		if (HEAD_AS_SPAN) {
			start = headStart;
			end = headEnd;
		}
		
		Span span = new Span(start, end ,type.form, type);
		return span;
		
	}
	
}
