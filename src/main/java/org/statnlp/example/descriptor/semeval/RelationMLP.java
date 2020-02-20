package org.statnlp.example.descriptor.semeval;

import org.statnlp.hypergraph.neural.NeuralNetworkCore;

public class RelationMLP extends NeuralNetworkCore {

	private static final long serialVersionUID = -5997015642979630505L;

	public RelationMLP(String className, int numLabels, int hiddenSize, String embedding, 
			int gpuId, int embeddingSize, int secondHiddenSize, 
			double dropout, boolean nnDescriptorFeature, int gruHiddenSize, boolean fixEmbedding, String testVocabFile,
			boolean bilinear) {
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
        config.put("bilinear", bilinear);
	}

	@Override
	public int hyperEdgeInput2OutputRowIndex(Object edgeInput) {
		int sentID = this.getNNInputID(edgeInput);
		return sentID;
	}

	/**
	 * For now, the edge input is just a sentence
	 */
	@Override
	public Object hyperEdgeInput2NNInput(Object edgeInput) {
		return edgeInput;
	}

}
