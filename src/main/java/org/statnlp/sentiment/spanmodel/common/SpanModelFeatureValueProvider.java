package org.statnlp.sentiment.spanmodel.common;

import org.statnlp.hypergraph.neural.ContinuousFeatureValueProvider;

public class SpanModelFeatureValueProvider extends ContinuousFeatureValueProvider {

	/**
	 * 
	 */
	private static final long serialVersionUID = 426990175647496517L;

	WordEmbedding embedding = null;
	
	public String UNK = "<UNK>";
	
	public static String SEPARATOR = "<\\|>";
	
	
	public static void setSEPARATOR(String SEPARATOR)
	{
		SpanModelFeatureValueProvider.SEPARATOR = SEPARATOR;
	}
	
	/*public TSFeatureValueProvider(int numFeatureValues, int numLabels) {
		super(numFeatureValues, numLabels);
	}
	
	public TSFeatureValueProvider(int numLabels) {
		super(numLabels);
	}*/
	
	//public static String[] LABELS_CONNINUOUS = new String[] {"O", "E-positive", "E-neutral", "E-negative"};
	
	
	public SpanModelFeatureValueProvider(WordEmbedding embedding, int numLabels)
	{
		super(embedding.ShapeSize, numLabels);
		this.embedding = embedding;
	}
	
	public SpanModelFeatureValueProvider setUNK(String UNK)
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
		
		String[] words = inputStr.split(SEPARATOR);
		
		double[] sum = null;
		
		for(String word : words)
		{
			double[] vector = embedding.getVector(word);
			if (vector == null)
			{
				vector = embedding.getVector(UNK);
			}
			
			
			if (sum == null) {
				sum = vector;
			}
			else
			{
				sum = this.vectorAdd(sum, vector);
			}
		}
		
		sum = this.vectorMulScalar(sum, 1.0 / words.length);
		
		return sum;
	}
	
	public double[] vectorAdd(double[] a, double[] b)
	{
		if (a.length != b.length) return null;
		
		double[] sum = new double[a.length];
		
		for(int i = 0; i < sum.length; i++)
			sum[i] = a[i] + b[i];
		
		return sum;
	}
	
	public double[] vectorMulScalar(double[] a, double b)
	{
		double[] c = new double[a.length];
		for(int i = 0; i < c.length; i++)
			c[i] = a[i] * b;
		
		return c;
	}
	
	

}
