package org.statnlp.targetedsentiment.common;

import org.statnlp.hypergraph.neural.ContinuousFeatureValueProvider;

public class TSFeatureValueProvider extends ContinuousFeatureValueProvider {

	WordEmbedding embedding = null;
	
	public String UNK = "<UNK>";
	
	/*public TSFeatureValueProvider(int numFeatureValues, int numLabels) {
		super(numFeatureValues, numLabels);
	}
	
	public TSFeatureValueProvider(int numLabels) {
		super(numLabels);
	}*/
	
	public static String[] LABELS_CONNINUOUS = new String[] {"O", "E-positive", "E-neutral", "E-negative"};
	
	
	public TSFeatureValueProvider(WordEmbedding embedding, int numLabels)
	{
		super(embedding.ShapeSize, numLabels);
		this.embedding = embedding;
	}
	
	public TSFeatureValueProvider setUNK(String UNK)
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
