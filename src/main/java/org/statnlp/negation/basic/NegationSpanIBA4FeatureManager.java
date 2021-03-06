package org.statnlp.negation.basic;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.AbstractMap.SimpleImmutableEntry;

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
import org.statnlp.negation.common.Negation;
import org.statnlp.negation.common.NegationFeatureManager;
import org.statnlp.negation.common.NegationGlobal;
import org.statnlp.negation.common.NegationInstance;
import org.statnlp.negation.common.NegationFeatureManager.FeaType;
import org.statnlp.negation.common.NegationInstance.FEATURE_TYPES;
import org.statnlp.negation.common.NegationCompiler.*;
import org.statnlp.negation.common.Utils;
import org.statnlp.targetedsentiment.common.TargetSentimentGlobal;

public class NegationSpanIBA4FeatureManager extends NegationSpan2FeatureManager {

	public NegationSpanIBA4FeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}

	public NegationSpanIBA4FeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
		super(param_g, neuralType, moreBinaryFeatures);
	}

	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k, int children_k_index) {
		if (children_k.length > 2)
			throw new RuntimeException("The number of children should be at most 2, but it is " + children_k.length);

		NegationInstance inst = ((NegationInstance) network.getInstance());

		long node_parent = network.getNode(parent_k);

		if (children_k.length == 0)
			return FeatureArray.EMPTY;

		int size = inst.size();
		String sentence = inst.getSentence();

		long node_child = network.getNode(children_k[0]);

		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(node_parent);
		int[] ids_child = NetworkIDMapper.toHybridNodeArray(node_child);

		ArrayList<String[]> inputs = (ArrayList<String[]>) inst.getInput();
		ArrayList<Label> outputs = (ArrayList<Label>) inst.getOutput();

		int pos_parent = size - ids_parent[0];
		int pos_child = size - ids_child[0];

		int tag_parent = ids_parent[1];
		int tag_child = ids_child[1];

		int nodetype_parent = ids_parent[2];
		int nodetype_child = ids_child[2];

		Negation neg = inst.negation;

	
		

		ArrayList<Integer> featureList = new ArrayList<Integer>();
		ArrayList<Integer> continuousFeatureList = new ArrayList<Integer>();
		ArrayList<Double> continuousFeatureValueList = new ArrayList<Double>();

		FeatureArray fa = FeatureArray.EMPTY;
		
		

		int tagId = -1;
		String tagStr = null;
		String word = null;

		ArrayList<String[]> featureArr = new ArrayList<String[]>();

		if (nodetype_parent == NodeType.Root.ordinal()) {

		} else {
			tagId = tag_parent;
			tagStr = this.compiler._labels[tagId].getForm();


			
			String w = Utils.getToken(inputs, pos_parent, NegationInstance.FEATURE_TYPES.word.ordinal());
			String lw = Utils.getToken(inputs, pos_parent - 1, NegationInstance.FEATURE_TYPES.word.ordinal());
			String llw = Utils.getToken(inputs, pos_parent - 2, NegationInstance.FEATURE_TYPES.word.ordinal());
			String rw = Utils.getToken(inputs, pos_parent + 1, NegationInstance.FEATURE_TYPES.word.ordinal());
			String rrw = Utils.getToken(inputs, pos_parent + 2, NegationInstance.FEATURE_TYPES.word.ordinal());
			
			word =  Utils.getToken(inputs, pos_parent, NegationInstance.FEATURE_TYPES.word.ordinal());
			
			String ct = tagStr;
			String nt = (nodetype_child == NodeType.Node.ordinal()) ? this.compiler._labels[tag_child].getForm() : this.END;

			if (NegationGlobal.ENABLE_DISCRETE_FEATURE) {
				
				
				int[] featureCandicate = new int[]{3, 5, 6};
				
				if (this.compiler._labels[tag_parent].getForm().endsWith("B") ) {
					
					
				}
			

			}
			
			

		}

		
		FeatureArray standard_fa = super.extract_helper(network, parent_k, children_k, children_k_index);
		FeatureArray contFa = this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList, standard_fa);
		fa = this.createFeatureArray(network, featureList, contFa);
		return fa;

	}

}
