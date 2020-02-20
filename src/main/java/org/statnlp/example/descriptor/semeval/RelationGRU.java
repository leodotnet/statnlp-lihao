package org.statnlp.example.descriptor.semeval;

import org.statnlp.hypergraph.neural.NeuralNetworkCore;

public class RelationGRU extends NeuralNetworkCore {
	
	private static final long serialVersionUID = 1008226102286195355L;

	public RelationGRU(String className, int numLabels, int hiddenSize, String embedding, 
			int gpuId, int embeddingSize, int secondHiddenSize, 
			double dropout, boolean nnDescriptorFeature, int gruHiddenSize, boolean fixEmbedding, String testVocabFile,
			String model, boolean useHeadWord, boolean tanhGRU, double gruDropout, boolean positionIndicator, double embDropout,
			boolean add, boolean twoLayerGRU, boolean bilinear) {
		super(numLabels, gpuId);
		config.put("class", className);
        config.put("hiddenSize", hiddenSize);
        config.put("numLabels", numLabels);
        config.put("embedding", embedding);
        config.put("gpuid", gpuId);
        config.put("embeddingSize", embeddingSize);
        config.put("layer2hiddenSize", secondHiddenSize);
        config.put("dropout", dropout);
        config.put("nnDescriptorFeature", nnDescriptorFeature);
        config.put("gruHiddenSize", gruHiddenSize);
        config.put("fixEmbedding", fixEmbedding);
        config.put("testVocab", testVocabFile);
        config.put("model", model);
        config.put("headword", useHeadWord);
        config.put("tanhGRU", tanhGRU);
        config.put("gruDropout", gruDropout);
        config.put("positionIndicator", positionIndicator);
        config.put("embDropout", embDropout);
        config.put("add", add); //add the hidden size of the two hidden vector
        config.put("twoLayerGRU", twoLayerGRU);
        config.put("bilinear", bilinear);
	}

	@Override
	public Object hyperEdgeInput2NNInput(Object edgeInput) {
		return edgeInput;
	}
	
	@Override
	public int hyperEdgeInput2OutputRowIndex (Object edgeInput) {
		int sentID = this.getNNInputID(edgeInput);
		return sentID;
	}

}
