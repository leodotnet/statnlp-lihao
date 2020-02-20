package org.statnlp.abbr.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.AbstractMap.SimpleImmutableEntry;

import org.statnlp.abbr.common.AbbrCompiler.NodeType;
import org.statnlp.commons.types.Label;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.FeatureManager;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.NetworkIDMapper;
import org.statnlp.hypergraph.neural.MLP;
import org.statnlp.hypergraph.neural.MultiLayerPerceptron;
import org.statnlp.hypergraph.neural.NeuralNetworkCore;


public abstract class AbbrFeatureManager extends FeatureManager {

	/**
	 * 
	 */
	private static final long serialVersionUID = 592253662868854534L;
	protected NeuralNetworkCore net;
	public String neuralType;
	public boolean moreBinaryFeatures = false;

	public enum FeaType {
		word, tag, lw, lt, ltt, rw, rt, prefix, suffix, transition, MP
	};

	protected String OUT_SEP = MLP.OUT_SEP;
	protected String IN_SEP = MLP.IN_SEP;
	protected final String START = "STR";
	protected final String END = "END";

	public AbbrFeatureManager(GlobalNetworkParam param_g) {
		super(param_g, null);
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			this.net = (NeuralNetworkCore) param_g.getNNParamG().getNet(0);
		}
	}

	public AbbrFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
		super(param_g);
		if (NetworkConfig.USE_NEURAL_FEATURES && !AbbrGlobal.neuralType.equals("continuous0")) {
			this.net = (NeuralNetworkCore) param_g.getNNParamG().getNet(0);
		}
		this.neuralType = neuralType;
		this.moreBinaryFeatures = moreBinaryFeatures;
	}


	int NodeTypeSize = NodeType.values().length;
	int WordFeatureTypeSize = AbbrInstance.FEATURE_TYPES.values().length;

	protected AbbrCompiler compiler = null;
	
	public void setCompiler(AbbrCompiler compiler) {
		this.compiler = compiler;
	}

}
