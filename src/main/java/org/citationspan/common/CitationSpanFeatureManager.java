package org.citationspan.common;



import org.citationspan.common.CitationSpanCompiler.NodeType;
import org.statnlp.hypergraph.FeatureManager;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.neural.MLP;
import org.statnlp.hypergraph.neural.NeuralNetworkCore;



public abstract class CitationSpanFeatureManager extends FeatureManager {

	/**
	 * 
	 */
	private static final long serialVersionUID = 592253662868854534L;
	protected NeuralNetworkCore net;
	public String neuralType;
	public boolean moreBinaryFeatures = false;

	public enum FeaType {
		word, tag, lw, lt, ltt, rw, rt, prefix, suffix, transition, contain_citation
	};

	public static String OUT_SEP = MLP.OUT_SEP;
	public static String IN_SEP = MLP.IN_SEP;
	protected final String START = "STR";
	protected final String END = "END";

	public CitationSpanFeatureManager(GlobalNetworkParam param_g) {
		super(param_g, null);
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			this.net = (NeuralNetworkCore) param_g.getNNParamG().getNet(0);
		}
	}

	public CitationSpanFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
		super(param_g);
		if (NetworkConfig.USE_NEURAL_FEATURES && !CitationSpanGlobal.neuralType.equals("continuous0")) {
			this.net = (NeuralNetworkCore) param_g.getNNParamG().getNet(0);
		}
		this.neuralType = neuralType;
		this.moreBinaryFeatures = moreBinaryFeatures;
	}


	int NodeTypeSize = NodeType.values().length;
	int WordFeatureTypeSize = CitationSpanInstance.FEATURE_TYPES.values().length;

	protected CitationSpanCompiler compiler = null;
	
	public void setCompiler(CitationSpanCompiler compiler) {
		this.compiler = compiler;
	}

}
