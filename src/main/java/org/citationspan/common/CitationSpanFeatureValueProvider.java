package org.citationspan.common;

import org.statnlp.hypergraph.neural.ContinuousFeatureValueProvider;

public class CitationSpanFeatureValueProvider extends ContinuousFeatureValueProvider {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2811282457964664411L;

	WordEmbedding embedding = null;
	
	public String UNK = "<UNK>";
	
	
	public CitationSpanFeatureValueProvider(WordEmbedding embedding, int numLabels)
	{
		super(embedding.ShapeSize, numLabels);
		this.embedding = embedding;
	}
	
	public CitationSpanFeatureValueProvider setUNK(String UNK)
	{
		this.UNK = UNK;
		return this;
	}

	
	public void getFeatureValue(Object input, double[] featureValue) {
		String inputStr = (String)input;
		double[] vector = embedding.getVector(inputStr);
		if (vector == null)
		{
			vector = embedding.getVector(UNK);
		}

		System.arraycopy(vector, 0, featureValue, 0, vector.length);
	}

	@Override
	public double[] getFeatureValue(Object input) {
		String inputStr = (String)input;
		double[] vector = embedding.getVector(inputStr);
		if (vector == null)
		{
			vector = embedding.getVector(UNK);
		}
		
		return vector;
	}
	
	

}
