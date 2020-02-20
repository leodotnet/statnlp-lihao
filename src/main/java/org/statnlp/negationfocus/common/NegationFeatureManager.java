package org.statnlp.negationfocus.common;



import org.statnlp.hypergraph.FeatureManager;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.neural.MLP;
import org.statnlp.hypergraph.neural.NeuralNetworkCore;
import org.statnlp.negationfocus.common.NegationCompiler.*;


public abstract class NegationFeatureManager extends FeatureManager {

	/**
	 * 
	 */
	private static final long serialVersionUID = 592253662868854534L;
	protected NeuralNetworkCore net;
	public String neuralType;
	public boolean moreBinaryFeatures = false;

	public enum FeaType {
		word, tag, lw, lt, ltt, rw, rt, prefix, suffix, transition, SR
	};

	public static String OUT_SEP = MLP.OUT_SEP;
	public static String IN_SEP = MLP.IN_SEP;
	protected final String START = "STR";
	protected final String END = "END";

	public NegationFeatureManager(GlobalNetworkParam param_g) {
		super(param_g, null);
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			this.net = (NeuralNetworkCore) param_g.getNNParamG().getNet(0);
		}
	}

	public NegationFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
		super(param_g);
		if (NetworkConfig.USE_NEURAL_FEATURES && !NegationGlobal.neuralType.equals("continuous0")) {
			this.net = (NeuralNetworkCore) param_g.getNNParamG().getNet(0);
		}
		this.neuralType = neuralType;
		this.moreBinaryFeatures = moreBinaryFeatures;
	}


	int NodeTypeSize = NodeType.values().length;
	int WordFeatureTypeSize = NegationInstance.FEATURE_TYPES.values().length;

	protected NegationCompiler compiler = null;
	
	public void setCompiler(NegationCompiler compiler) {
		this.compiler = compiler;
	}

}
