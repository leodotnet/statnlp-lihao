package org.statnlp.example.descriptor.semeval;

import org.statnlp.hypergraph.neural.NeuralNetworkCore;

public class RNNPool extends NeuralNetworkCore {

	private static final long serialVersionUID = -4765814829979998318L;

	public RNNPool(String className, int numLabels, int hiddenSize, String embedding, 
			int gpuId, int embeddingSize, int secondHiddenSize, 
			double dropout, boolean nnDescriptorFeature, int gruHiddenSize, boolean fixEmbedding, String testVocabFile,
			String model, boolean useHeadWord, boolean tanhGRU, double gruDropout, boolean positionIndicator, double embDropout,
			boolean add, boolean attn) {
		super(numLabels, gpuId);
		config.put("class", className);
        config.put("hiddenSize", hiddenSize);
        config.put("numLabels", numLabels);
        config.put("embedding", embedding);
        config.put("gpuid", gpuId);
        config.put("embeddingSize", embeddingSize);
        config.put("layer2hiddenSize", secondHiddenSize);
        config.put("dropout", dropout);
        config.put("gruHiddenSize", gruHiddenSize);
        config.put("fixEmbedding", fixEmbedding);
        config.put("testVocab", testVocabFile);
        config.put("model", model);
        config.put("tanhGRU", tanhGRU);
        config.put("gruDropout", gruDropout);
        config.put("positionIndicator", positionIndicator);
        config.put("embDropout", embDropout);
        config.put("add", add);
        config.put("attn", attn);
	}

	@Override
	public int hyperEdgeInput2OutputRowIndex(Object edgeInput) {
		return this.getNNInputID(edgeInput);
	}

	@Override
	public Object hyperEdgeInput2NNInput(Object edgeInput) {
		return edgeInput;
	}

}
