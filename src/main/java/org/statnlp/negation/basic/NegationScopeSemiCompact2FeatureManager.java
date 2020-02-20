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
import org.statnlp.negation.common.NegationInstance.FEATURE_TYPES;
import org.statnlp.negation.common.Utils;
import org.statnlp.negation.basic.NegationScopeSemiCompact2Compiler.*;

public class NegationScopeSemiCompact2FeatureManager extends NegationFeatureManager {

	public NegationScopeSemiCompact2FeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}

	public NegationScopeSemiCompact2FeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
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

		int pos_parent = 1000 - ids_parent[0];
		int pos_child = 1000 - ids_child[0];

		/*int tag_parent = ids_parent[1];
		int tag_child = ids_child[1];
		
		int numspan_parent = ids_parent[2];
		int numspan_child = ids_child[2];*/
		
		//int latentIdx_parent = ids_parent[3];
		//int latentIdx_child = ids_child[3];

		int nodetype_parent = ids_parent[3];
		int nodetype_child = ids_child[3];

		Negation neg = inst.negation;

	
		

		ArrayList<Integer> featureList = new ArrayList<Integer>();
		ArrayList<Integer> continuousFeatureList = new ArrayList<Integer>();
		ArrayList<Double> continuousFeatureValueList = new ArrayList<Double>();

		
		FeatureArray fa = this.createFeatureArray(network, new int[] {});
		FeatureArray fp = fa;


		//ArrayList<String[]> featureArr = new ArrayList<String[]>();
		
		if (nodetype_child == NodeType.Y.ordinal()) {
			continuousFeatureList.add(this._param_g.toFeature(network, "DEADEND:", "", ""));
			continuousFeatureValueList.add(Double.NEGATIVE_INFINITY);
		} else {
			long node_child2 = network.getNode(children_k[1]);
			int[] ids_child2 = NetworkIDMapper.toHybridNodeArray(node_child2);
			
			
			int pos_child2 = 1000 - ids_child2[0];
			int latentIdx_child2 = ids_child2[3];
			int nodetype_child2 = ids_child2[2];
			
			
			String nodetypeParent = NodeType.values()[nodetype_parent].name();
			String nodetypeChild = NodeType.values()[nodetype_child].name();
			String nodetypeChild2 = NodeType.values()[nodetype_child2].name();
			
			//edge: Root O X
			if (nodetypeParent.startsWith("Root") && nodetypeChild.startsWith("O") && nodetypeChild2.startsWith("X")) {
				featureList.add(this._param_g.toFeature(network, "transition_", "O-X", START));
				for(int i = 0; i < size; i++) {
					String nt = (i == size - 1 ? END : "O");
					featureList.add(this._param_g.toFeature(network, "transition_", "O", "O=>" + nt));
					fp = fp.addNext(this.getFeatureAtPos(network, inst, i, "O", nt));
				}
			}
			//edge: Root O I1
			else if (nodetypeParent.startsWith("Root") && nodetypeChild.startsWith("O") && nodetypeChild2.startsWith("I")) {
				
				featureList.add(this._param_g.toFeature(network, "transition_", "O-I", START));
				for(int i = 0; i < pos_child2; i++) {
					String nt = (i == pos_child2 - 1 ? "I" : "O");
					featureList.add(this._param_g.toFeature(network, "transition_", "O", "O=>" + nt));
					fp = fp.addNext(this.getFeatureAtPos(network, inst, i, "O", nt));
				}
				
			} 
			//edge: I O (I, X)
			else if (nodetypeParent.startsWith("I") && nodetypeChild.startsWith("O") ) {
				
				
				for(int i = pos_parent; i < pos_child; i++) {	
					
					String nt = (i == pos_child - 1 ? "O" : "I");
					featureList.add(this._param_g.toFeature(network, "transition_", "I", "I=>" + nt));
					fp = fp.addNext(this.getFeatureAtPos(network, inst, i, "I", nt));
				}
				
				
				//boundary
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_parent, "I", "I", "I-Start"));
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_child - 1, "I", "O", "I-End"));
				
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_child, "O", "O", "Gap-Start"));
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_child2 - 1, "O", "I", "Gap-End"));
				
				if (pos_child2 < size)
					fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_child2, "I", "I", "IC-Start"));
				
				
				if (nodetypeChild2.startsWith("I")) {
					
					
					featureList.add(this._param_g.toFeature(network, "transition_", "O-I", "I"));
					
					for(int i = pos_child; i < pos_child2; i++) {
						String nt = (i == pos_child2 - 1 ? "I" : "O");
						featureList.add(this._param_g.toFeature(network, "transition_", "O", "O=>" + nt));
						fp = fp.addNext(this.getFeatureAtPos(network, inst, i, "O", nt));
						
						featureList.add(this._param_g.toFeature(network, "gap_", "", Utils.getToken(inputs, i, FEATURE_TYPES.word.ordinal())));
						featureList.add(this._param_g.toFeature(network, "gap_", "", Utils.getToken(inputs, i, FEATURE_TYPES.pos_tag.ordinal())));
					}
					
					for(int i = pos_parent; i < pos_child; i++) {
						featureList.add(this._param_g.toFeature(network, "far_", "I", "I=>I" + Utils.getToken(inputs, i, FEATURE_TYPES.word.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.word.ordinal())));
						featureList.add(this._param_g.toFeature(network, "far_", "I", "I=>I" + Utils.getToken(inputs, i, FEATURE_TYPES.pos_tag.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.pos_tag.ordinal())));
						featureList.add(this._param_g.toFeature(network, "far_", "I", "I=>I" + Utils.getToken(inputs, i, FEATURE_TYPES.pos_tag.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.word.ordinal())));
						featureList.add(this._param_g.toFeature(network, "far_", "I", "I=>I" + Utils.getToken(inputs, i, FEATURE_TYPES.word.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.pos_tag.ordinal())));
						
					}
					
					//long distance dependency
					String output = "I=>I";
					
					//partial scope and next partial scope
					for(int i = pos_parent; i < pos_child; i++) {
						
						featureList.add(this._param_g.toFeature(network, "far_", output, Utils.getToken(inputs, i, FEATURE_TYPES.word.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.word.ordinal())));
						featureList.add(this._param_g.toFeature(network, "far_", output, Utils.getToken(inputs, i, FEATURE_TYPES.pos_tag.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.pos_tag.ordinal())));
						featureList.add(this._param_g.toFeature(network, "far_", output, Utils.getToken(inputs, i, FEATURE_TYPES.pos_tag.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.word.ordinal())));
						featureList.add(this._param_g.toFeature(network, "far_", output, Utils.getToken(inputs, i, FEATURE_TYPES.word.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.pos_tag.ordinal())));
					}
					
					//boundary	
					featureList.add(this._param_g.toFeature(network, "far_boundary_", output, Utils.getToken(inputs, pos_child -1, FEATURE_TYPES.word.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.pos_tag.ordinal())));
					
				}
				else if (nodetypeChild2.startsWith("X")) {
					for(int i = pos_child; i < size; i++) {
						String nt = (i == size - 1 ? END : "O");
						featureList.add(this._param_g.toFeature(network, "transition_", "O", "O=>" + nt));
						fp = fp.addNext(this.getFeatureAtPos(network, inst, i, "O", nt));
						
					}
				}
			}
			
			
			
		}
						
			
		
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			
			if (nodetype_child != NodeType.Y.ordinal()) {
				
				long node_child2 = network.getNode(children_k[1]);
				int[] ids_child2 = NetworkIDMapper.toHybridNodeArray(node_child2);
				
				
				int pos_child2 = 1000 - ids_child2[0];
				int latentIdx_child2 = ids_child2[3];
				int nodetype_child2 = ids_child2[2];
				
				
				String nodetypeParent = NodeType.values()[nodetype_parent].name();
				String nodetypeChild = NodeType.values()[nodetype_child].name();
				String nodetypeChild2 = NodeType.values()[nodetype_child2].name();
			
				
				if (nodetypeParent.startsWith("Root") && nodetypeChild.startsWith("O")) {
					
					int tagId = 0;
					String tagStr = "O";
					
					if (nodetypeChild2.startsWith("X")) {
						pos_child2 = size;
					}
					
					fp = fp.addNext(this.getNeuralFeature(network, parent_k, children_k, children_k_index, inst, tagId, tagStr, 0, pos_child2, sentence));
					
					//edge: I O (I, X)
				} else if (nodetypeParent.startsWith("I") && nodetypeChild.startsWith("O") ) {
					
					int tagId = 1;
					String tagStr = "I";
					
					fp = fp.addNext(this.getNeuralFeature(network, parent_k, children_k, children_k_index, inst, tagId, tagStr, pos_parent, pos_child, sentence));
					
					tagId = 0;
					tagStr = "O";
					fp = fp.addNext(this.getNeuralFeature(network, parent_k, children_k, children_k_index, inst, tagId, tagStr, pos_parent, pos_child, sentence));
					
				
				}
				
			
			}
		
			
	
			
		} 
		/*else {
			// featureList.add(this._param_g.toFeature(network,
			// FeaType.word.name(), entity, word));
		}*/

		
		fp = fp.addNext(this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList));
		fp = fp.addNext(this.createFeatureArray(network, featureList));
		
		return fa;

	}

}
