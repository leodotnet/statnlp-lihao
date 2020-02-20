package org.statnlp.negationfocus.common;

import java.util.Arrays;

import org.statnlp.hypergraph.neural.ContinuousFeatureValueProvider;

public class NegationFeatureValueProvider extends ContinuousFeatureValueProvider {

	/**
	 * 
	 */
	private static final long serialVersionUID = -2811282457964664411L;

	WordEmbedding embedding = null;
	
	public String UNK = "<UNK>";
	
	
	public NegationFeatureValueProvider(WordEmbedding embedding, int numLabels)
	{
		super(embedding.ShapeSize, numLabels);
		this.embedding = embedding;
	}
	
	public NegationFeatureValueProvider setUNK(String UNK)
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
		
		double[] sum = new double[embedding.ShapeSize];
		
		Arrays.fill(sum, 0);
		
		for(String str : inputStr.split(NegationFeatureManager.IN_SEP)) {
			double[] vector = embedding.getVector(str);
			if (vector == null)
			{
				vector = embedding.getVector(UNK);
			}
			
			if (vector != null) {
				sum = Utils.vectorAdd(sum, vector);
			}
		}
		
		
		return sum;
	}
	
	

}
