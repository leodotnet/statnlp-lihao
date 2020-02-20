package org.statnlp.sentiment.spanmodel.common;

import org.statnlp.hypergraph.neural.NeuralNetworkCore;

public class EmbeddingLayer extends NeuralNetworkCore {

	private static final long serialVersionUID = 4951822203204790448L;
	
	
	public EmbeddingLayer(int numLabels, int hiddenSize) {
		this(numLabels,hiddenSize, "random", false);
	}

	public EmbeddingLayer(int numLabels, int hiddenSize, String emb, boolean fixEmbedding) {
		super(numLabels);
		config.put("class", "EmbeddingLayer");
        config.put("hiddenSize", hiddenSize);
        config.put("embedding", emb);
        config.put("fixEmbedding", fixEmbedding);
        config.put("numLabels", numLabels);
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
