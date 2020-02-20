package org.citationspan.basic;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;

import org.citationspan.basic.CitationSpanSemiCompactNCompiler.NodeType;
import org.citationspan.common.CitationSpanFeatureManager;
import org.citationspan.common.CitationSpanGlobal;
import org.citationspan.common.CitationSpanInstance;
import org.citationspan.common.CitationSpanInstance.FEATURE_TYPES;
import org.citationspan.common.Utils;
import org.statnlp.commons.types.Label;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkIDMapper;



public class CitationSpanSemiCompactNFeatureManager extends CitationSpanFeatureManager {

	/**
	 * 
	 */
	private static final long serialVersionUID = 651614281501396833L;

	public CitationSpanSemiCompactNFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}

	public CitationSpanSemiCompactNFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
		super(param_g, neuralType, moreBinaryFeatures);
	}
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k, int children_k_index) {
		if (children_k.length > 2)
			throw new RuntimeException("The number of children should be at most 2, but it is " + children_k.length);

		CitationSpanInstance inst = ((CitationSpanInstance) network.getInstance());

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
		*/
		int spannum_parent = 100 - ids_parent[2];
		int spannum_child = 100 - ids_child[2];
		
		//int latentIdx_parent = ids_parent[3];
		//int latentIdx_child = ids_child[3];

		int nodetype_parent = ids_parent[3];
		int nodetype_child = ids_child[3];

		

	
		

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
			int spannum_child2 = 100 - ids_child2[2];
			int nodetype_child2 = ids_child2[3];
			
			
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
			else if (nodetypeParent.startsWith("Root") && nodetypeChild.startsWith("O") && nodetypeChild2.startsWith("I1")) {
				featureList.add(this._param_g.toFeature(network, "transition_", "O-I1", START));
				featureList.add(this._param_g.toFeature(network, "transition_", "O-I", START));
				for(int i = 0; i < pos_child2; i++) {
					String nt = (i == pos_child2 - 1 ? "I1" : "O");
					featureList.add(this._param_g.toFeature(network, "transition_", "O", "O=>" + nt));
					fp = fp.addNext(this.getFeatureAtPos(network, inst, i, "O", nt));
					
					nt = (i == pos_child2 - 1 ? "I" : "O");
					featureList.add(this._param_g.toFeature(network, "transition_", "O", "O=>" + nt));
					fp = fp.addNext(this.getFeatureAtPos(network, inst, i, "O", nt));
				}
				
			} 
			//edge: I O (I, X)
			else if (nodetypeParent.startsWith("I") && nodetypeChild.startsWith("O") ) {
				
				
				
				for(int i = pos_parent; i < pos_child; i++) {
					String nt = (i == pos_child - 1 ? "O" : "I" + spannum_parent);
					featureList.add(this._param_g.toFeature(network, "transition_", "I" + spannum_parent, "I" + spannum_parent + "=>" + nt));
					fp = fp.addNext(this.getFeatureAtPos(network, inst, i, "I" + spannum_parent, nt));
					
					nt = (i == pos_child - 1 ? "O" : "I");
					featureList.add(this._param_g.toFeature(network, "transition_", "I", "I=>" + nt));
					fp = fp.addNext(this.getFeatureAtPos(network, inst, i, "I", nt));
				}
				
				
				//boundary
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_parent, "I", "I", "I-Start"));
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_child - 1, "I", "O", "I-End"));
				
				if (pos_child < size) {
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_child, "O", "O", "Gap-Start"));
				fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_child2 - 1, "O", "I", "Gap-End"));
				}
				
				if (pos_child2 < size)
					fp = fp.addNext(this.getFeatureAtPos(network, inst, pos_child2, "I", "I", "IC-Start"));
				
				
				if (nodetypeChild2.startsWith("I")) {
					
					//String spannum_child2 = nodetypeChild2.substring(1);
					
					featureList.add(this._param_g.toFeature(network, "transition_", "O-I" + spannum_child2, "I" + spannum_parent));
					featureList.add(this._param_g.toFeature(network, "transition_", "O-I", "I"));
					
					for(int i = pos_child; i < pos_child2; i++) {
						String nt = (i == pos_child2 - 1 ? "I2" : "O");
						featureList.add(this._param_g.toFeature(network, "transition_", "O", "O=>" + nt));
						fp = fp.addNext(this.getFeatureAtPos(network, inst, i, "O", nt));
						
						nt = (i == pos_child2 - 1 ? "I" : "O");
						featureList.add(this._param_g.toFeature(network, "transition_", "O", "O=>" + nt));
						fp = fp.addNext(this.getFeatureAtPos(network, inst, i, "O", nt));
						
						//featureList.add(this._param_g.toFeature(network, "gap_", "", Utils.getToken(inputs, i, FEATURE_TYPES.word.ordinal())));
						//featureList.add(this._param_g.toFeature(network, "gap_", "", Utils.getToken(inputs, i, FEATURE_TYPES.pos_tag.ordinal())));
					}
					
					for(int i = pos_parent; i < pos_child; i++) {
						//featureList.add(this._param_g.toFeature(network, "far_", "I", "I=>I" + Utils.getToken(inputs, i, FEATURE_TYPES.word.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.word.ordinal())));
						//featureList.add(this._param_g.toFeature(network, "far_", "I", "I=>I" + Utils.getToken(inputs, i, FEATURE_TYPES.pos_tag.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.pos_tag.ordinal())));
						//featureList.add(this._param_g.toFeature(network, "far_", "I", "I=>I" + Utils.getToken(inputs, i, FEATURE_TYPES.pos_tag.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.word.ordinal())));
						//featureList.add(this._param_g.toFeature(network, "far_", "I", "I=>I" + Utils.getToken(inputs, i, FEATURE_TYPES.word.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.pos_tag.ordinal())));
						
					}
					
					//long distance dependency
					String output = "I" + spannum_parent + "=>I" + spannum_child2;
					
					//partial scope and next partial scope
					for(int i = pos_parent; i < pos_child; i++) {
						
						//featureList.add(this._param_g.toFeature(network, "far_", output, Utils.getToken(inputs, i, FEATURE_TYPES.word.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.word.ordinal())));
						//featureList.add(this._param_g.toFeature(network, "far_", output, Utils.getToken(inputs, i, FEATURE_TYPES.pos_tag.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.pos_tag.ordinal())));
						//featureList.add(this._param_g.toFeature(network, "far_", output, Utils.getToken(inputs, i, FEATURE_TYPES.pos_tag.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.word.ordinal())));
						//featureList.add(this._param_g.toFeature(network, "far_", output, Utils.getToken(inputs, i, FEATURE_TYPES.word.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.pos_tag.ordinal())));
					}
					
					//boundary	
					//featureList.add(this._param_g.toFeature(network, "far_boundary_", output, Utils.getToken(inputs, pos_child -1, FEATURE_TYPES.word.ordinal()) + Utils.getToken(inputs, pos_child2, FEATURE_TYPES.pos_tag.ordinal())));
					
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
						
			
		/*
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			if (tagId != -1) {
				Object input = null;
				if (neuralType.startsWith("lstm")) {
					String sentenceInput = sentence;
					input = new SimpleImmutableEntry<String, Integer>(sentenceInput, pos_parent);

					this.addNeural(network, 0, parent_k, children_k_index, input, tagId);
				} else if (neuralType.equals("continuous0")) { 
					
					
					if (CitationSpanGlobal.EMBEDDING_WORD_LOWERCASE)
						word = word.toLowerCase();
					
					double[] vec = CitationSpanGlobal.Word2Vec.getVector(word);
					if (vec == null) {
						vec = CitationSpanGlobal.Word2Vec.getVector(CitationSpanGlobal.UNK);
						
						if (vec == null) {
							vec = new double[CitationSpanGlobal.Word2Vec.ShapeSize];
							Arrays.fill(vec, 0);
						}
					}
					
					for(int i = 0; i < CitationSpanGlobal.Word2Vec.ShapeSize; i++) {
						continuousFeatureList.add(this._param_g.toFeature(network, "continuous-emb:", tagStr, "dim" + i  + ":"));
						continuousFeatureValueList.add(vec[i]);
						
					}
				
				} else {
					input = word;//.toLowerCase();
					
					if (tagId != -1) {
					
						if (CitationSpanGlobal.EMBEDDING_WORD_LOWERCASE)
							input = word.toLowerCase();
						this.addNeural(network, 0, parent_k, children_k_index, input, tagId);
					}
				}

			}
		} else {
			// featureList.add(this._param_g.toFeature(network,
			// FeaType.word.name(), entity, word));
		}*/

		
		fp = fp.addNext(this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList));
		fp = fp.addNext(this.createFeatureArray(network, featureList));
		
		return fa;

	}
	
	
	public FeatureArray getFeatureAtPos(Network network, CitationSpanInstance inst, int pos_parent, String ct, String nt) {
		
		return getFeatureAtPos(network, inst, pos_parent, ct, nt, "");
	}
	
	public FeatureArray getFeatureAtPos(Network network, CitationSpanInstance inst, int pos_parent, String ct, String nt, String featureType) {
		
		if (pos_parent < 0 || pos_parent >= inst.size())
			return this.createFeatureArray(network, new int[] {});
		
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		ArrayList<Integer> continuousFeatureList = new ArrayList<Integer>();
		ArrayList<Double> continuousFeatureValueList = new ArrayList<Double>();
		
		ArrayList<String[]> inputs = (ArrayList<String[]>) inst.getInput();
		
		for (int pos = 0; pos < inputs.get(pos_parent).length; pos++) {
			//System.out.println("inputs.get(pos_parent).length:" + inputs.get(pos_parent).length);
			String w = Utils.getToken(inputs, pos_parent, pos ).toLowerCase();
			String lw = Utils.getToken(inputs, pos_parent , pos - 1);
			String llw = Utils.getToken(inputs, pos_parent , pos - 2);
			String rw = Utils.getToken(inputs, pos_parent , pos + 1);
			String rrw = Utils.getToken(inputs, pos_parent , pos + 2);

			String word = w;

			String tagStr = ct;

			if (CitationSpanGlobal.ENABLE_DISCRETE_FEATURE) {

				featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "_w_", tagStr, w));
				featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "_wrw_", tagStr, w + rw));
				featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "_lww_", tagStr, lw + w));
				featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "_lwwrw_", tagStr, lw + w + rw));
				featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "_llwlwwrwrrw_", tagStr, llw + lw + w + rw + rrw));
				
				if (inst.citationArr[pos] == 1) {
					featureList.add(this._param_g.toFeature(network, featureType + FeaType.contain_citation + "", tagStr, ""));
//					featureList.add(this._param_g.toFeature(network, featureType + FeaType.contain_citation + "_wrw_", tagStr, w + rw));
//					featureList.add(this._param_g.toFeature(network, featureType + FeaType.contain_citation + "_lww_", tagStr, lw + w));
//					featureList.add(this._param_g.toFeature(network, featureType + FeaType.contain_citation + "_lwwrw_", tagStr, lw + w + rw));
//					featureList.add(this._param_g.toFeature(network, featureType + FeaType.contain_citation + "_llwlwwrwrrw_", tagStr, llw + lw + w + rw + rrw));
				}
				
				

//				int[] featureCandicate = new int[] { 0 };
//
//				for (int i : featureCandicate) {
//					String featuretype = CitationSpanInstance.FEATURE_TYPES.values()[i].name();
//					featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", ct + "=>" + nt, "curr_" + featuretype + ":" + Utils.getToken(inputs, pos_parent, i)));
//					featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", ct + "=>" + nt, "next_" + featuretype + ":" + Utils.getToken(inputs, pos_parent + 1, i)));
//					featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", ct + "=>" + nt, "last_" + featuretype + ":" + Utils.getToken(inputs, pos_parent - 1, i)));
//
//					featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", ct + "=>" + nt, "next_" + featuretype + ":" + Utils.getToken(inputs, pos_parent, i) + Utils.getToken(inputs, pos_parent + 1, i)));
//					featureList.add(this._param_g.toFeature(network, featureType + FeaType.tag + "", ct + "=>" + nt, "last_" + featuretype + ":" + Utils.getToken(inputs, pos_parent - 1, i) + Utils.getToken(inputs, pos_parent + 0, i)));
//
//				}
//
//				featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "", tagStr, "IsAllCap:" + Utils.isAllCap(w)));
//				// featureList.add(this._param_g.toFeature(network, FeaType.word
//				// + "", tagStr, "IsAllCap:" + Utils.isAllCap(w) + w));
//
//				featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "", tagStr, "IsPunc:" + Utils.isPunctuation(w)));
//				featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "", tagStr, "IsPunc:" + Utils.isPunctuation(w) + w));
//
//				featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "before", tagStr, "IsPunc:" + Utils.isPunctuation(lw)));
//				featureList.add(this._param_g.toFeature(network, featureType + FeaType.word + "after", tagStr, "IsPunc:" + Utils.isPunctuation(rw)));
//
//				// for(int i =
//				// CitationSpanInstance.FEATURE_TYPES.word.ordinal(); i <=
//				// CitationSpanInstance.FEATURE_TYPES.syntax.ordinal(); i++)
//				for (int i : featureCandicate) {
//					String featuretype = CitationSpanInstance.FEATURE_TYPES.values()[i].name();
//
//					featureList.add(this._param_g.toFeature(network, featureType + featuretype + "before", tagStr, Utils.getToken(inputs, pos_parent - 1, i)));
//					featureList.add(this._param_g.toFeature(network, featureType + featuretype + "after", tagStr, Utils.getToken(inputs, pos_parent + 1, i)));
//
//					featureList.add(this._param_g.toFeature(network, featureType + featuretype + "", tagStr, Utils.getToken(inputs, pos_parent, i)));
//				}

			}
		}
	
		
		FeatureArray contFa = this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList);
		FeatureArray fa = this.createFeatureArray(network, featureList, contFa); 
		
		return fa;

	}
	
	public FeatureArray getNeuralFeature(Network network, int parent_k, int[] children_k, int children_k_index, CitationSpanInstance inst, int tagId, String tagStr, int leftBoundary, int rightRoundary, String sentence ) {
		
		ArrayList<String[]> inputs = (ArrayList<String[]>) inst.getInput();
		ArrayList<Integer> continuousFeatureList = new ArrayList<Integer>();
		ArrayList<Double> continuousFeatureValueList = new ArrayList<Double>();
		
		if (neuralType.startsWith("lstm")) {
			for(int pos = leftBoundary; pos < rightRoundary; pos++) {
				Object input = new SimpleImmutableEntry<String, Integer>(sentence, pos);		
				this.addNeural(network, 0, parent_k, children_k_index, input, tagId);
			}
		}  else if (neuralType.equals("continuous0")) { 

			for(int pos = leftBoundary; pos < rightRoundary; pos++) {
				String word = Utils.getToken(inputs, pos, CitationSpanInstance.FEATURE_TYPES.word.ordinal());
				if (CitationSpanGlobal.EMBEDDING_WORD_LOWERCASE)
					word = word.toLowerCase();
				
				double[] vec = CitationSpanGlobal.Word2Vec.getVector(word);
				if (vec == null) 
					vec = CitationSpanGlobal.Word2Vec.getVector(CitationSpanGlobal.UNK);
					
				
				if (vec != null)
					for(int i = 0; i < CitationSpanGlobal.Word2Vec.ShapeSize; i++) {
						continuousFeatureList.add(this._param_g.toFeature(network, "continuous-emb:", tagStr, "dim" + i  + ":"));
						continuousFeatureValueList.add(vec[i]);
					}
			}
		
		}  else {
			
			for(int pos = leftBoundary; pos < rightRoundary; pos++) {
				String word = Utils.getToken(inputs, pos, CitationSpanInstance.FEATURE_TYPES.word.ordinal());
				Object input = word;
				
				if (CitationSpanGlobal.EMBEDDING_WORD_LOWERCASE)
					input = word.toLowerCase();
				
				this.addNeural(network, 0, parent_k, children_k_index, input, tagId);
			}
		}
		
		FeatureArray contFa = this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList);
		return contFa;
	}

}
