package org.entityrelation.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.entityrelation.common.EntityRelationCompiler.NodeType;
import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.example.base.BaseNetwork;
import org.statnlp.example.base.BaseNetwork.NetworkBuilder;
import org.statnlp.hypergraph.LocalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkCompiler;
import org.statnlp.hypergraph.NetworkIDMapper;



public abstract class EntityRelationCompiler extends NetworkCompiler {

	public Map<Integer, Label> _labelsMap;
	public Label[] _labels;
	
	public static Label[] LABELS;
	
	
	
	public EntityRelationCompiler() {
		super(null);
		int M = EntityRelationGlobal.SENTENCE_LENGTH_MAX+1;
		NetworkIDMapper.setCapacity(new int[] { M, M,NodeTypeSize, M, M, 20, 10, 10 });
		
		/*
		_labels = this.getLabels();
		LABELS = _labels;
		this._labelsMap = new HashMap<Integer, Label>();
		for(Label label: _labels){
			this._labelsMap.put(label.getId(), new Label(label));
		}*/
	}


	/**
	 * 
	 */
	private static final long serialVersionUID = 2100499563741744475L;

	public enum NodeType {
		X, I ,I2, I1, T, E, A, Root
	};
	
	


	int NodeTypeSize = NodeType.values().length;
	
	int EntityTypeSize = EntityRelationOutput.ENTITYTYPE.size();
	
	int RelationTypeSize = EntityRelationOutput.RELATIONS.size();
	
	public static long getNode_Root(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] {size + 1, 0, NodeType.Root.ordinal(), 0, 0, 0, 0, 0 });
													//// x, y, origfrom, relation, entity1type, entity2type, nodetype
	}

	protected long toNode_Root(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] {size + 1, 0, NodeType.Root.ordinal(),0, 0,  0, 0, 0 });
													//// x, y, origfrom, relation, entity1type, entity2type, nodetype
	}

	protected long toNode_A(int size, int i, int k) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - i, size - k, NodeType.A.ordinal(), 0, 0, 0, 0, 0});
	}
	
	protected long toNode_E(int size, int i, int k) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - i, size - k, NodeType.E.ordinal(), 0, 0, 0, 0, 0});
	}
	
	protected long toNode_T(int size, int i, int k, int r, int t1, int t2) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - i, size - k, NodeType.T.ordinal(), 0, 0,  r, t1, t2, });
	}
	
	protected long toNode_I1(int size, int i, int k, int r, int t1, int t2) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - i, size - k, NodeType.I1.ordinal(), 0, 0, r, t1, t2, });
	}
	
	protected long toNode_I2(int size, int i, int k, int r, int t1, int t2) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - i, size - k, NodeType.I2.ordinal(), 0, 0, r , t1, t2});
	}
	
	protected long toNode_Iself(int size, int i, int k, int r, int t1, int t2) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - i, size - k, NodeType.I.ordinal(), 0, 0, r , t1, t2});
	}


	protected long toNode_X(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 0, 0, NodeType.X.ordinal(), 0, 0, 0,  0, 0 });
	}
	
	
	@Override
	public double costAt(Network network, int parent_k, int[] child_k){
		int size = network.getInstance().size();
		Network labeledNet = network.getLabeledNetwork();
		long parentNode = network.getNode(parent_k);
		int parentNode_k = labeledNet.getNodeIndex(parentNode);
		
		
		int[] parentIds = NetworkIDMapper.toHybridNodeArray(parentNode);
		int nodetype_parent = parentIds[2];
		int r = parentIds[5];
		NodeType nodetypeParent = NodeType.values()[nodetype_parent];
		
		
		
		double cost = 0.0;
		
	
		//if (EntityRelationGlobal.ONLY_SELF_RELATION || (EntityRelationGlobal.ADD_SELF_RELATION && r == 0)) 
		{
			
			if (nodetypeParent == NodeType.T) {
				
				double beta = EntityRelationGlobal.RECALL_BETA[1];
				
				if (EntityRelationGlobal.ADD_SELF_RELATION && r == 0) {
					beta = EntityRelationGlobal.RECALL_BETA[0];
				}
				
				
				int[][] children = labeledNet.getChildren(parentNode_k);
				
				if (child_k.length == 1) { // predicted T-X
					
					if (children[0].length == 1) { // gold T-X
						cost = 0.0;
					} else {  //gold T-I
						cost = beta;
					}
					
				} else { //predicted T-I   child_k.length > 1
					
					if (children[0].length == 1) { // gold T-X
						cost = 1.0;
					} else { // predicted T-I (children.length > 1
						cost = 0.0;
					}
					
				}
			}
		} 
		
		return cost;
	}


}
