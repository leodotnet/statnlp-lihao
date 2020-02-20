package org.statnlp.abbr.basic;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.AbstractMap.SimpleImmutableEntry;

import org.statnlp.abbr.common.AbbrCompiler.NodeType;
import org.statnlp.abbr.common.AbbrFeatureManager;
import org.statnlp.abbr.common.AbbrGlobal;
import org.statnlp.abbr.common.AbbrInstance;
import org.statnlp.abbr.common.Utils;
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


public class AbbrLinearFeatureManager extends AbbrFeatureManager {

	/**
	 * 
	 */
	private static final long serialVersionUID = -3784918454446952439L;

	public AbbrLinearFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}

	public AbbrLinearFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
		super(param_g, neuralType, moreBinaryFeatures);
	}

	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k, int children_k_index) {
		if (children_k.length > 2)
			throw new RuntimeException("The number of children should be at most 2, but it is " + children_k.length);

		AbbrInstance inst = ((AbbrInstance) network.getInstance());

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

		
	
		

		ArrayList<Integer> featureList = new ArrayList<Integer>();
		ArrayList<Integer> continuousFeatureList = new ArrayList<Integer>();
		ArrayList<Double> continuousFeatureValueList = new ArrayList<Double>();

		FeatureArray fa = FeatureArray.EMPTY;

		int tagId = -1;
		String tagStr = null;
		String word = null;

		ArrayList<String[]> featureArr = new ArrayList<String[]>();

		if (nodetype_parent == NodeType.Root.ordinal()) {
			featureList.add(this._param_g.toFeature(network, "transition_", START, NodeType.values()[nodetype_parent] + "=>" + NodeType.values()[nodetype_child]));

		} else {
			tagId = tag_parent;
			tagStr = this.compiler._labels[tagId].getForm();

			if (nodetype_child == NodeType.Node.ordinal())
				featureList.add(this._param_g.toFeature(network, "transition_", tagStr, NodeType.values()[nodetype_parent] + "=>" + NodeType.values()[nodetype_child]));
			else
				featureList.add(this._param_g.toFeature(network, "transition_", tagStr, NodeType.values()[nodetype_parent] + "=>" + END));
			
			String w = Utils.getToken(inputs, pos_parent, AbbrInstance.FEATURE_TYPES.token.ordinal());
			String lw = Utils.getToken(inputs, pos_parent - 1, AbbrInstance.FEATURE_TYPES.token.ordinal());
			String llw = Utils.getToken(inputs, pos_parent - 2, AbbrInstance.FEATURE_TYPES.token.ordinal());
			String rw = Utils.getToken(inputs, pos_parent + 1, AbbrInstance.FEATURE_TYPES.token.ordinal());
			String rrw = Utils.getToken(inputs, pos_parent + 2, AbbrInstance.FEATURE_TYPES.token.ordinal());
			
			word =  Utils.getToken(inputs, pos_parent, AbbrInstance.FEATURE_TYPES.token.ordinal());
			
			String ct = tagStr;
			String nt = (nodetype_child == NodeType.Node.ordinal()) ? this.compiler._labels[tag_child].getForm() : this.END;

			if (AbbrGlobal.ENABLE_DISCRETE_FEATURE) {
				
				
				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, w));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, w + rw));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, lw + w));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, lw + w + rw));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, llw + lw + w + rw + rrw));
				
				int[] featureCandicate = new int[]{3, 4, 5, 6};
				
				
				
				
			
			
				
				//for(int i = AbbrInstance.FEATURE_TYPES.word.ordinal(); i <= AbbrInstance.FEATURE_TYPES.syntax.ordinal(); i++) 
				for(int i : featureCandicate)
				{
					String featureType = AbbrInstance.FEATURE_TYPES.values()[i].name();
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "curr_" + featureType + ":" + Utils.getToken(inputs, pos_parent    , i)));
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "next_" + featureType + ":" + Utils.getToken(inputs, pos_parent + 1, i)));
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "last_" + featureType + ":" + Utils.getToken(inputs, pos_parent - 1, i)));
					
					
					
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "next_" + featureType + ":" + Utils.getToken(inputs, pos_parent    , i) + Utils.getToken(inputs, pos_parent + 1, i)));
					featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "last_" + featureType + ":" + Utils.getToken(inputs, pos_parent - 1, i) + Utils.getToken(inputs, pos_parent + 0, i)));
					
					
				
				}
				
			

				//featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "Is1stCap:" + Utils.isFirstCap(w)));
				//featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "Is1stCap:" + Utils.isFirstCap(w) + w));

				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "IsAllCap:" + Utils.isAllCap(w)));
				//featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "IsAllCap:" + Utils.isAllCap(w) + w));

				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "IsPunc:" + Utils.isPunctuation(w)));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "IsPunc:" + Utils.isPunctuation(w) + w));
				
				featureList.add(this._param_g.toFeature(network, FeaType.word + "before", tagStr, "IsPunc:" + Utils.isPunctuation(lw)));
				featureList.add(this._param_g.toFeature(network, FeaType.word + "after", tagStr, "IsPunc:" + Utils.isPunctuation(rw)));
				
				
				
				
				//for(int i = AbbrInstance.FEATURE_TYPES.word.ordinal(); i <= AbbrInstance.FEATURE_TYPES.syntax.ordinal(); i++)
				for(int i : featureCandicate)
				{
					String featureType = AbbrInstance.FEATURE_TYPES.values()[i].name();
					
					featureList.add(this._param_g.toFeature(network, featureType + "before", tagStr, Utils.getToken(inputs, pos_parent - 1, i)));
					featureList.add(this._param_g.toFeature(network, featureType + "after", tagStr, Utils.getToken(inputs, pos_parent + 1, i)));
					
					featureList.add(this._param_g.toFeature(network, featureType + "", tagStr, Utils.getToken(inputs, pos_parent, i)));
				}
				


				
			}
			
			

		}

		if (NetworkConfig.USE_NEURAL_FEATURES) {
			if (tagId != -1) {
				Object input = null;
				if (neuralType.startsWith("lstm")) {
					String sentenceInput = sentence;
					input = new SimpleImmutableEntry<String, Integer>(sentenceInput, pos_parent);

					this.addNeural(network, 0, parent_k, children_k_index, input, tagId);
				} else if (neuralType.equals("continuous0")) { 
					
					
					if (AbbrGlobal.EMBEDDING_WORD_LOWERCASE)
						word = word.toLowerCase();
					
					double[] vec = AbbrGlobal.Word2Vec.getVector(word);
					if (vec == null) {
						vec = AbbrGlobal.Word2Vec.getVector(AbbrGlobal.UNK);
						
						if (vec == null) {
							vec = new double[AbbrGlobal.Word2Vec.ShapeSize];
							Arrays.fill(vec, 0);
						}
					}
					
					for(int i = 0; i < AbbrGlobal.Word2Vec.ShapeSize; i++) {
						continuousFeatureList.add(this._param_g.toFeature(network, "continuous-emb:", tagStr, "dim" + i  + ":"));
						continuousFeatureValueList.add(vec[i]);
						
					}
				
				} else {
					input = word;//.toLowerCase();
					
					if (tagId != -1) {
					
						if (AbbrGlobal.EMBEDDING_WORD_LOWERCASE)
							input = word.toLowerCase();
						this.addNeural(network, 0, parent_k, children_k_index, input, tagId);
					}
				}

			}
		} else {
			// featureList.add(this._param_g.toFeature(network,
			// FeaType.word.name(), entity, word));
		}

		FeatureArray contFa = this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList);
		fa = this.createFeatureArray(network, featureList, contFa);
		return fa;

	}

}
