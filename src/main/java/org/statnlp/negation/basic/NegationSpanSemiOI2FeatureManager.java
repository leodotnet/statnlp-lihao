package org.statnlp.negation.basic;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.Arrays;

import org.statnlp.commons.types.Label;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.NetworkIDMapper;
import org.statnlp.negation.common.Negation;
import org.statnlp.negation.common.NegationCompiler.NodeType;
import org.statnlp.negation.common.NegationFeatureManager;
import org.statnlp.negation.common.NegationGlobal;
import org.statnlp.negation.common.NegationInstance;
import org.statnlp.negation.common.Utils;

public class NegationSpanSemiOI2FeatureManager extends NegationFeatureManager {

	/**
	 * 
	 */
	private static final long serialVersionUID = -1777202275474581371L;

	public NegationSpanSemiOI2FeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}

	public NegationSpanSemiOI2FeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
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

		FeatureArray fa = this.createFeatureArray(network, new int[] {});
		FeatureArray fp = fa;


		String tagStr = null;

		if (nodetype_parent == NodeType.Root.ordinal()) {
			featureList.add(this._param_g.toFeature(network, "transition_", START, NodeType.values()[nodetype_parent] + "=>" + NodeType.values()[nodetype_child]));

		} else {
			
			String ct = this.compiler._labels[tag_parent].getForm();
			
			tagStr = ct;

			if (nodetype_child == NodeType.Node.ordinal())
				featureList.add(this._param_g.toFeature(network, "transition_", ct, NodeType.values()[nodetype_parent] + "=>" + NodeType.values()[nodetype_child]));
			else
				featureList.add(this._param_g.toFeature(network, "transition_", ct, NodeType.values()[nodetype_parent] + "=>" + END));
			
			
			if (ct.startsWith("O")) {
				
				if (pos_parent < size) {
					String nt = (pos_parent == size - 1) ? "<END>" :  this.compiler._labels[tag_child].getForm();
					fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_parent, ct, nt, ""));
				}
				
			} else if (ct.startsWith("I")) {
				
				for(int pos = pos_parent; pos < pos_child; pos++) {
					
					String nt = (pos == pos_child - 1) ? (pos < size ? "O" : "<END>") :  ct;
					fp = fp.addNext(this.getFeatureAtPos(network, inst, pos, ct, nt, ""));
				}
				
				//Semi Features
				
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_parent - 1, "", "", "scope_begin_-1"));
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_parent, "", "", "scope_begin_0"));
				
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_parent + 1, "", "", "scope_begin_+1"));
				
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_child - 2, "", "", "scope_end_-1"));
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_child - 1, "", "", "scope_end_0"));
				
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_child, "", "", "scope_end_+1"));
				
				
			}
		}

		
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			if (tagStr != null) {
				Object input = null;
				if (neuralType.equals("continuous0")) { 
					
					for(int pos = pos_parent; pos < pos_child; pos++) {
						
						String word =  Utils.getToken(inputs, pos, NegationInstance.FEATURE_TYPES.word.ordinal());
					
						if (NegationGlobal.EMBEDDING_WORD_LOWERCASE)
							word = word.toLowerCase();
						
						double[] vec = NegationGlobal.Word2Vec.getVector(word);
						if (vec == null) {
							vec = NegationGlobal.Word2Vec.getVector(NegationGlobal.UNK);
							
							if (vec == null) {
								vec = new double[NegationGlobal.Word2Vec.ShapeSize];
								Arrays.fill(vec, 0);
							}
						}
						
						for(int i = 0; i < NegationGlobal.Word2Vec.ShapeSize; i++) {
							continuousFeatureList.add(this._param_g.toFeature(network, "continuous-emb:", tagStr, "dim" + i  + ":"));
							continuousFeatureValueList.add(vec[i]);
							
						}
					}
				
				} 

			}
		} 

		fp = fp.addNext(this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList));
		fp = fp.addNext(this.createFeatureArray(network, featureList));
		
		return fa;

	}

}
