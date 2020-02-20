package org.entityrelation.logisticregression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.entityrelation.common.Entity;
import org.entityrelation.common.EntityRelationCompiler;
import org.entityrelation.common.EntityRelationGlobal;
import org.entityrelation.common.EntityRelationInstance;
import org.entityrelation.common.EntityRelationOutput;
import org.entityrelation.common.Relation;
import org.entityrelation.common.EntityRelationCompiler.NodeType;
import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.example.base.BaseNetwork;
import org.statnlp.example.base.BaseNetwork.NetworkBuilder;
import org.statnlp.hypergraph.LocalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.NetworkIDMapper;
import org.statnlp.hypergraph.TableLookupNetwork;


public class EntityRelationLRCompiler extends EntityRelationCompiler {
	/**
	 * 
	 */
	private static final long serialVersionUID = -666195635788496150L;
	public static boolean DEBUG = false;
	
	public enum NodeType {
		X, T, Root
	};
	
	int NodeTypeSize = NodeType.values().length;

	public EntityRelationLRCompiler() {
		super();
		NetworkIDMapper.setCapacity(new int[] {  NodeTypeSize, 10 });
	}
	
	/*
	public static long getNode_Root() {
		return NetworkIDMapper.toHybridNodeID(new int[] {NodeType.Root.ordinal(), 0 });
	}
	*/

	protected long toRoot() {
		return NetworkIDMapper.toHybridNodeID(new int[] {NodeType.Root.ordinal(), 0});
	}

	protected long toT(int r) {
		return NetworkIDMapper.toHybridNodeID(new int[] { NodeType.T.ordinal(), r});
	}

	protected long toX() {
		return NetworkIDMapper.toHybridNodeID(new int[] { NodeType.X.ordinal(), 0});
	}
	
	

	@Override
	public EntityRelationInstance decompile(Network network) {
		BaseNetwork msnetwork = (BaseNetwork) network;
		EntityRelationInstance inst = (EntityRelationInstance) network.getInstance();
		int size = inst.size();
		List<String[]> inputs = (List<String[]>) inst.getInput();
		EntityRelationOutput output = (EntityRelationOutput)inst.getOutput();
		Relation goldRelation = output.relations.get(0);

		long root = this.toRoot();
		int node_root = Arrays.binarySearch(msnetwork.getAllNodes(), root);
		
		ArrayList<Entity> entities = new ArrayList<Entity>();
		ArrayList<Relation> relations = new ArrayList<Relation>();
		
		
		Queue<Integer> queue = new LinkedList<Integer>();
		
		queue.clear();
		queue.add(node_root);
		
		int[] childs = network.getMaxPath(node_root);
		int[] child_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(childs[0]));
		
		int r = child_ids[1];
		
		entities.add(goldRelation.arg1);
		entities.add(goldRelation.arg2);
		
		Relation relation = new Relation(entities, r ,goldRelation.arg1, goldRelation.arg2);
		relations.add(relation);
		
		EntityRelationOutput predication = new EntityRelationOutput(entities, relations);
		
		inst.setPrediction(predication);
		
		return inst;
	}
	
	
	

	@Override
	public Network compileUnlabeled(int networkId, Instance instance, LocalNetworkParam param) {
		
		
		EntityRelationInstance inst = (EntityRelationInstance) instance;
		NetworkBuilder<BaseNetwork> ernetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		
		int size = inst.size();

		long node_Root = this.toRoot();
		ernetwork.addNode(node_Root);
		
		long node_X = this.toX();
		ernetwork.addNode(node_X);
		
		for(int r = 0; r < EntityRelationOutput.RELATIONS.size(); r++) {
		
			long node_T = this.toT(r);
			ernetwork.addNode(node_T);
			
			ernetwork.addEdge(node_Root, new long[] {node_T});
			ernetwork.addEdge(node_T, new long[] {node_X});
		}
		
		BaseNetwork network = ernetwork.build(networkId, inst, param, this);
		return network;
	}

	@Override
	public Network compileLabeled(int networkId, Instance instance, LocalNetworkParam param) {

		EntityRelationInstance inst = (EntityRelationInstance) instance;
		NetworkBuilder<BaseNetwork> ernetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		EntityRelationOutput output = (EntityRelationOutput)inst.getOutput();
		Relation relation = output.relations.get(0);
		


		long node_Root = this.toRoot();
		ernetwork.addNode(node_Root);
		
		long node_X = this.toX();
		ernetwork.addNode(node_X);
		
		int r = relation.type.id;
		
		
		long node_T = this.toT(r);
		ernetwork.addNode(node_T);
		
		
		
		ernetwork.addEdge(node_Root, new long[] {node_T});
		ernetwork.addEdge(node_T, new long[] {node_X});
		

		BaseNetwork network = ernetwork.build(networkId, inst, param, this);

		return network;

	}
	
	
	@Override
	public double costAt(Network network, int parent_k, int[] child_k){
		EntityRelationInstance inst = (EntityRelationInstance)network.getInstance();
		int size = inst.size();
		Network labeledNet = network.getLabeledNetwork();
		long parentNode = network.getNode(parent_k);
		//int parentNode_k = labeledNet.getNodeIndex(parentNode);
		
		
		int[] parentIds = NetworkIDMapper.toHybridNodeArray(parentNode);
		int nodetype_parent = parentIds[0];
		int r = parentIds[1];
		NodeType nodetypeParent = NodeType.values()[nodetype_parent];
		
		int goldR = ((EntityRelationOutput)(inst.output)).relations.get(0).type.id;
		
		double cost = 0.0;
		
		if (nodetypeParent == NodeType.T) {
			
			double beta = EntityRelationGlobal.RECALL_BETA[1];
			
			if (EntityRelationGlobal.ADD_NO_RELATION) {
			
				if (r == EntityRelationGlobal.NoRelationIndex) { // predicted T-X
					
					if (goldR == EntityRelationGlobal.NoRelationIndex) { // gold T-X
						cost = 0.0;
					} else {  //gold T-I
						cost = beta;
					}
					
				} else { //predicted T-I   child_k.length > 1
					
					if (goldR == EntityRelationGlobal.NoRelationIndex) { // gold T-X
						cost = 1.0;
					} else { // predicted T-I (children.length > 1
						cost = r == goldR ? 0 : 1;
					}
					
				}
			}
		}
		
		
		return cost;
	}



}
