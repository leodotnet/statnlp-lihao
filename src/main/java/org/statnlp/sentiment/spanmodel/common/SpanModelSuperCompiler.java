package org.statnlp.sentiment.spanmodel.common;

import org.statnlp.hypergraph.NetworkCompiler;

public abstract class SpanModelSuperCompiler extends NetworkCompiler{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 7986946714708267844L;
	public SentimentDict dict;
	
	public SpanModelSuperCompiler() {
		
	}
	
	public void setSentimentDict(SentimentDict dict) {
		this.dict = dict;
	}
	
	public String getExplanation(SentimentInstance inst) {
		return inst.getExplanation();
	}

}
