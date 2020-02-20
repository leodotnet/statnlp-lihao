package org.statnlp.example.descriptor.semeval;

import org.statnlp.hypergraph.neural.NeuralNetworkCore;

public class RelationCNN extends NeuralNetworkCore {

	private static final long serialVersionUID = -1526975811458300727L;

	public RelationCNN(String className, int numLabels, int hiddenSize, String embedding, 
			int gpuId, int windowSize, int embeddingSize, int secondHiddenSize, double dropout,
			boolean usePositionEmbeddings, int posEmbeddingSize, boolean nnDescriptorFeature, int gruHiddenSize) {
		super(numLabels, gpuId);
		config.put("class", className);
        config.put("hiddenSize", hiddenSize);
        config.put("numLabels", numLabels);
        config.put("embedding", embedding);
        config.put("gpuid", gpuId);
        config.put("windowSize", windowSize);
        config.put("embeddingSize", embeddingSize);
        config.put("layer2hiddenSize", secondHiddenSize);
        config.put("dropout", dropout);
        config.put("positionEmbedding", usePositionEmbeddings);
        config.put("posEmbeddingSize", posEmbeddingSize);
        config.put("nnDescriptorFeature", nnDescriptorFeature);
        config.put("gruHiddenSize", gruHiddenSize);
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
