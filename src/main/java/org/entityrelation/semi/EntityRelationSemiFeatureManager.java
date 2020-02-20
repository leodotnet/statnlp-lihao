package org.entityrelation.semi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.AbstractMap.SimpleImmutableEntry;

import org.entityrelation.common.AttributedWord;
import org.entityrelation.common.Entity;
import org.entityrelation.common.EntityRelationGlobal;
import org.entityrelation.common.EntityRelationInstance;
import org.entityrelation.common.EntityRelationOutput;
import org.entityrelation.common.Label;
import org.entityrelation.common.Relation;
import org.entityrelation.common.Utils;
import org.entityrelation.common.EntityRelationCompiler.NodeType;
import org.entityrelation.common.EntityRelationFeatureManager;
import org.entityrelation.common.EntityRelationFeatureManager.FeatureType;
import org.entityrelation.linear.EntityRelationLinearFeatureManager;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.NetworkIDMapper;

public class EntityRelationSemiFeatureManager extends EntityRelationFeatureManager {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8186940850252607291L;

	public EntityRelationSemiFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}

	public EntityRelationSemiFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
		super(param_g, neuralType, moreBinaryFeatures);
	}
	
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k, int children_k_index) {
		//if (children_k.length > 2) throw new RuntimeException("The number of children should be at most 2, but it is " + children_k.length);

		EntityRelationInstance inst = ((EntityRelationInstance) network.getInstance());

		long node_parent = network.getNode(parent_k);

		if (children_k.length == 0)
			return FeatureArray.EMPTY;

		int size = inst.size();
		String sentence = inst.getSentence();

		long node_child = network.getNode(children_k[0]);

		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(node_parent);
		int[] ids_child = NetworkIDMapper.toHybridNodeArray(node_child);

		ArrayList<String[]> inputs = (ArrayList<String[]>) inst.getInput();
	

		int nodetype_parent = ids_parent[2];
		int nodetype_child = ids_child[2];

		NodeType nodetypeParent = NodeType.values()[nodetype_parent];
		NodeType nodetypeChild = NodeType.values()[nodetype_child];
		

		ArrayList<Integer> featureList = new ArrayList<Integer>();
//		ArrayList<Integer> continuousFeatureList = new ArrayList<Integer>();
//		ArrayList<Double> continuousFeatureValueList = new ArrayList<Double>();

		
		FeatureArray fa = this.createFeatureArray(network, new int[] {});
		FeatureArray fp = fa;
		
		
		
		if(nodetypeParent == NodeType.X || nodetypeParent == NodeType.A || nodetypeParent == NodeType.E || nodetypeParent == NodeType.Root){
			return fa;
		}
		
		int i = size - ids_parent[0];
		int k = size - ids_parent[1];
		int r = ids_parent[5];
		int t1 = ids_parent[6];
		int t2 = ids_parent[7];
		int iFrom = size - ids_parent[3];
		int kFrom = size - ids_parent[4];
		
		boolean isSelfRelation = Relation.isSelfRelation(r);// (EntityRelationGlobal.ADD_SELF_RELATION && r== 0);
		
		
		String relationType = EntityRelationOutput.getRELATIONS(r).form;
		Label e1Type = EntityRelationOutput.getENTITY(t1);
		Label e2Type = EntityRelationOutput.getENTITY(t2);
		String entity1Type = e1Type.form;
		String entity2Type = e2Type.form;
		
		
		String type =  nodetypeParent + " ";
		for(int childIdx = 0; childIdx < children_k.length; childIdx++) {
			long child = network.getNode(children_k[childIdx]);
			int[] child_ids = NetworkIDMapper.toHybridNodeArray(child);
			
			
			String nodeType = NodeType.values()[child_ids[2]].toString();
			
			if (EntityRelationGlobal.I_GENERAL) {
				type = type.replaceAll("I1", "I").replaceAll("I2", "I");
			}

			type +=  "-" + nodeType;
		}

		
		featureList.add(this._param_g.toFeature(network, FeatureType.TRANSITION + "", type, ""));
		
		
		
		if (EntityRelationGlobal.ENABLE_DISCRETE_FEATURE) {
			
			//Relation Features
			if (nodetypeParent == NodeType.T) {
				
				if (children_k.length == 1) {
					relationType = Relation.NO_RELATION;
				} 
				
				fp = addPartialRelFeatures(fp, network, inst, nodetypeParent, nodetypeChild, i, k, relationType, parent_k, children_k_index);//extractRelationFeatures(inst, network, inputs, i, k, indicator, relationType, entity1Type, entity2Type);
			}
			
			
			//Span Features
			
			if (nodetypeParent == NodeType.T) {
				
				if (children_k.length == 1) {//X
					if (isSelfRelation) { 
						String indicator = "T-X-";
						ArrayList<Integer> featureAtPos = extractSpanFeatures(inst, network, inputs, i, size, nodetypeParent, nodetypeChild, indicator, entity1Type);
						featureList.addAll(featureAtPos);
					}
				} else {
					/*
					String indicator = "T-I1-";
					if (EntityRelationGlobal.I_GENERAL) {
						indicator = indicator.replaceAll("I1", "I");
					}
					ArrayList<Integer> featureAtPos = extractSpanFeatures(inst, network, inputs, i, size, nodetypeParent, nodetypeChild, indicator, entity1Type);
					featureList.addAll(featureAtPos);
					
					indicator = "T-I2-";
					if (EntityRelationGlobal.I_GENERAL) {
						indicator = indicator.replaceAll("I2", "I");
					}
					featureAtPos = extractSpanFeatures(inst, network, inputs, k, size, nodetypeParent, nodetypeChild, indicator, entity2Type);
					featureList.addAll(featureAtPos);*/
				}
				
				
			} else if (nodetypeParent == NodeType.I1 && nodetypeChild == NodeType.X) {
				
				for(int m = iFrom + 1; m <= i; m++) {
					String indicator = "I1-I1-";
					
					if (EntityRelationGlobal.I_GENERAL) {
						indicator = indicator.replaceAll("I1", "I");
					}
					
					ArrayList<Integer> featureAtPos = extractSpanFeatures(inst, network, inputs, m, size, nodetypeParent, nodetypeChild, indicator, entity1Type);
					featureList.addAll(featureAtPos);
					
				}
				
				String indicator = "T-I1-";
				if (EntityRelationGlobal.I_GENERAL) {
					indicator = indicator.replaceAll("I1", "I");
				}
				ArrayList<Integer> featureAtPos = extractSpanFeatures(inst, network, inputs, iFrom, size, nodetypeParent, nodetypeChild, indicator, entity1Type);
				featureList.addAll(featureAtPos);
				
				indicator = "I1-X-";
				if (EntityRelationGlobal.I_GENERAL) {
					indicator = indicator.replaceAll("I1", "I");
				}
				featureAtPos = extractSpanFeatures(inst, network, inputs, i, size, nodetypeParent, nodetypeChild, indicator, entity1Type);
				featureList.addAll(featureAtPos);
				
				
			} else if (nodetypeParent == NodeType.I2 && nodetypeChild == NodeType.X) {
				
				for(int m = kFrom + 1; m <= k; m++) {
					String indicator = "I2-I2-";
					
					if (EntityRelationGlobal.I_GENERAL) {
						indicator = indicator.replaceAll("I2", "I");
					}
					
					ArrayList<Integer> featureAtPos = extractSpanFeatures(inst, network, inputs, m, size, nodetypeParent, nodetypeChild, indicator, entity2Type);
					featureList.addAll(featureAtPos);
					
				}
				
				{
					String indicator = "T-I2-";
					if (EntityRelationGlobal.I_GENERAL) {
						indicator = indicator.replaceAll("I2", "I");
					}
					
					ArrayList<Integer> featureAtPos = extractSpanFeatures(inst, network, inputs, kFrom, size, nodetypeParent, nodetypeChild, indicator, entity2Type);
					featureList.addAll(featureAtPos);
				}
				
				{
					String indicator = "I2-X-";
					if (EntityRelationGlobal.I_GENERAL) {
						indicator = indicator.replaceAll("I2", "I");
					}
					ArrayList<Integer> featureAtPos = extractSpanFeatures(inst, network, inputs, k, size, nodetypeParent, nodetypeChild, indicator, entity2Type);
					featureList.addAll(featureAtPos);
				}
				
			}
			

			
		}
		
		
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			
			//Relation
			if (nodetypeParent == NodeType.T) {
				
				if (children_k.length == 1) {
					relationType = Relation.NO_RELATION;
				} 
				
				if (neuralType.equals("continuous0")) { 
					
					fp = addContinuousEmbeddingRelationFeatures(fp, network, inst, i, k, relationType);
					

				} else if (neuralType.equals("cnn")) {
					Entity entity1 = (new Entity(new int[] {i, i + 1}, new int[] {-1 , -1}, e1Type)).setHeadIdx(inputs, lastWordAsHead);
					Entity entity2 = (new Entity(new int[] {k, k + 1}, new int[] {-1 , -1}, e2Type)).setHeadIdx(inputs, lastWordAsHead);
					addCNNFirstNeuralFeatures(network, parent_k, children_k_index, r, inst, entity1, entity2);
				}

			}
			
			
			//Span
			if (nodetypeParent == NodeType.T) {
				
				if (isSelfRelation && children_k.length == 1) {
					
					int tagId = EntityRelationGlobal.NUM_ENTITY_TYPE;
					
					if (neuralType.startsWith("lstm")) {
						String sentenceInput = sentence;
						Object input = new SimpleImmutableEntry<String, Integer>(sentenceInput, i);

						this.addNeural(network, 0, parent_k, children_k_index, input, tagId);
					}
					
				}
				
			} else if ( nodetypeParent == NodeType.I1 || nodetypeParent == NodeType.I2) {
				int tagId = -1;
				String entityType =null;
				int pos = -1;
				
				if (nodetype_parent == NodeType.I1.ordinal()) {
					tagId = t1;
					entityType = entity1Type;
					pos = i;
					
				} else  {
					tagId = t2;
					entityType = entity2Type;
					pos = k;
				} 
				
				String word = inputs.get(pos)[0];
				
				if (EntityRelationGlobal.EMBEDDING_WORD_LOWERCASE)
					word = word.toLowerCase();
				
				Object input = null;
				if (neuralType.startsWith("lstm")) {
					String sentenceInput = sentence;
					input = new SimpleImmutableEntry<String, Integer>(sentenceInput, pos);

					this.addNeural(network, 0, parent_k, children_k_index, input, tagId);
				} else if (neuralType.equals("continuous0")) { 
					
					fp = addContinuousEmbeddingSpanFeatures(fp, network, word, entityType);

				} 
				
				
			}
		}
		

		
		fp = fp.addNext(this.createFeatureArray(network, featureList));
		//fp = fp.addNext(this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList));
		
		return fa;

	}	

}
