package org.entityrelation.semi;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.entityrelation.common.AttributedWord;
import org.entityrelation.common.Entity;
import org.entityrelation.common.EntityRelationGlobal;
import org.entityrelation.common.EntityRelationInstance;
import org.entityrelation.common.EntityRelationOutput;
import org.entityrelation.common.Relation;
import org.entityrelation.common.Utils;
import org.entityrelation.common.EntityRelationCompiler.NodeType;
import org.entityrelation.common.EntityRelationFeatureManager;
import org.entityrelation.common.EntityRelationFeatureManager.FeatureType;
import org.entityrelation.common.EntityRelationFeatureManager.RelFeaType;
import org.entityrelation.linear.EntityRelationLinearFeatureManager;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkIDMapper;

public class EntityRelationSemiBTFeatureManager extends EntityRelationFeatureManager {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8186940850252607291L;

	public EntityRelationSemiBTFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}

	public EntityRelationSemiBTFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
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
		ArrayList<Integer> continuousFeatureList = new ArrayList<Integer>();
		ArrayList<Double> continuousFeatureValueList = new ArrayList<Double>();

		
		FeatureArray fa = this.createFeatureArray(network, new int[] {});
		FeatureArray fp = fa;
		
	
		
		
		if(nodetypeParent == NodeType.X || nodetypeParent == NodeType.A || nodetypeParent == NodeType.E || nodetypeParent == NodeType.Root || nodetypeParent ==NodeType.I1 || nodetypeParent == NodeType.I2){
			return fa;
		}
		
		int i = size - ids_parent[0];
		int k = size - ids_parent[1];
		int r = ids_parent[5];
		int t1 = ids_parent[6];
		int t2 = ids_parent[7];
		
		
		boolean isSelfRelation = Relation.isSelfRelation(r);
		
		
		String relationType = EntityRelationOutput.getRELATIONS(r).form;
		String entity1Type = EntityRelationOutput.getENTITY(t1).form;
		String entity2Type = EntityRelationOutput.getENTITY(t2).form;
		

		
		if (EntityRelationGlobal.ENABLE_DISCRETE_FEATURE) {
			

			if (nodetypeParent == NodeType.T) {
				
				if (nodetypeChild == NodeType.X) {
					
					featureList.add(this._param_g.toFeature(network, FeatureType.TRANSITION + "", "T-X-", ""));
					
					//Relation Features
					fp = addPartialRelFeatures(fp, network, inst, nodetypeParent, nodetypeChild, i, k, Relation.NO_RELATION, parent_k, children_k_index);
					
				} else {
					
					featureList.add(this._param_g.toFeature(network, FeatureType.TRANSITION + "", "T-I-I-", ""));
					
					int child1_k = children_k[0];
					int child2_k = children_k[1];
					
					int[] ids_child1 = network.getNodeArray(child1_k);
					int[] ids_child2 = network.getNodeArray(child2_k);
					
					
					int j = size - ids_child1[0];
					int l = size - ids_child2[1];
					
					Entity entity1 = new Entity(new int[] {i, j}, new int[] {-1, -1}, EntityRelationOutput.getENTITY(t1));
					Entity.setHeadIdx(inputs, entity1, lastWordAsHead);
					Entity entity2 = new Entity(new int[] {k, l}, new int[] {-1, -1}, EntityRelationOutput.getENTITY(t2));
					Entity.setHeadIdx(inputs, entity2, lastWordAsHead);
					//Relation Features
					fp = addRelFeatures(fp, network, inst, entity1, entity2, relationType, parent_k, children_k_index);
					
					//Span Features
					for(int m = i; m < j; m++) {
						String indicator = "I1-I1-";
						if (m == i) {
							indicator = "T-I1-";
						} else if (m == j - 1) {
							indicator = "I1-X-";
						}
						
						if (EntityRelationGlobal.I_GENERAL) {
							indicator = indicator.replaceAll("I1", "I");
						}
						ArrayList<Integer> featureAtPos = extractSpanFeatures(inst, network, inputs, m, size, nodetypeParent, nodetypeChild, indicator, entity1Type);
						featureList.addAll(featureAtPos);
						
					}
					
					for(int m = k; m < l; m++) {
						String indicator = "I2-I2-";
						if (m == k) {
							indicator = "T-I2-";
						} else if (m == l - 1) {
							indicator = "I2-X-";
						}
						
						if (EntityRelationGlobal.I_GENERAL) {
							indicator = indicator.replaceAll("I2", "I");
						}
						
						ArrayList<Integer> featureAtPos = extractSpanFeatures(inst, network, inputs, m, size, nodetypeParent, nodetypeChild, indicator, entity2Type);
						featureList.addAll(featureAtPos);
					}
					
				}
				
				
			}
			
		}
		
		fp = fp.addNext(this.createFeatureArray(network, featureList));
		fp = fp.addNext(this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList));
		
		return fa;

	}	
	


}
