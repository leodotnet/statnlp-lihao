package org.statnlp.example.descriptor.semeval;

import org.statnlp.example.descriptor.Config;
import org.statnlp.example.descriptor.emb.WordEmbedding;
import org.statnlp.hypergraph.neural.ContinuousFeatureValueProvider;

public class SemEvalContinuousFeatureProvider extends ContinuousFeatureValueProvider {

	private static final long serialVersionUID = -2616865725072223725L;

	protected transient WordEmbedding emb;
	private int totalDim;
	
	public SemEvalContinuousFeatureProvider(int numLabels) {
		super(numLabels);
	}

	public SemEvalContinuousFeatureProvider(int numFeatureValues, int mlpHiddenSize, int mlpSecondLayer, double dropout, int numLabels, WordEmbedding emb) {
		super(numFeatureValues, numLabels);
		this.emb = emb;
		config.put("class", "SemEvalContinuousFeature");
		config.put("hiddenSize", mlpHiddenSize);
		config.put("layer2hiddenSize", mlpSecondLayer);
		config.put("dropout", dropout);
		this.totalDim = numFeatureValues;
	}

	@Override
	public double[] getFeatureValue(Object input) {
		//current word only.
		String words = (String)input;
		String[] ws = words.split(Config.NEURAL_SEP);
		double[] avgs = new double[this.totalDim];
		for (int s = 0; s < ws.length; s++) {
			String[] subs = ws[s].split(" ");
			int start = s * emb.getDimension();
			int end = (s + 1) * emb.getDimension();
			for (String w : subs) {
				double[] wemb = emb.getEmbedding(w);
				for (int i = start; i < end; i++) {
					avgs[i] += wemb[i - start];
				}
			}
			for (int i = start; i < end; i++) {
				avgs[i] /= subs.length;
			}
		}
		return avgs;
	}

}
