package org.statnlp.sentiment.spanmodel.common;

import org.statnlp.hypergraph.FeatureManager;
import org.statnlp.hypergraph.GlobalNetworkParam;

public abstract class SpanModelSuperFeatureManager  extends FeatureManager {

	public SpanModelSuperFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = -8994331082431732758L;

	public static int NGRAM = 3;
	
	public SentimentDict dict;
	
	public void setSentimentDict(SentimentDict dict) {
		this.dict = dict;
	}

}
