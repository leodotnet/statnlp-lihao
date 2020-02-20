package org.entityrelation.crf;

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


public class EntityRelationLinearCRFCompiler extends EntityRelationCompiler {
	/**
	 * 
	 */
	private static final long serialVersionUID = -666195635788496150L;
	public static boolean DEBUG = false;
	
	public enum NodeType {
		X, O, I, B, Root
	};
	
	int NodeTypeSize = NodeType.values().length;
	
	public BaseNetwork unlabeledNetwork = null;

	public EntityRelationLinearCRFCompiler() {
		super();
		NetworkIDMapper.setCapacity(new int[] {EntityRelationGlobal.SENTENCE_LENGTH_MAX + 2,  NodeTypeSize, 10 });
		//unlabeledNetwork = compileUnlabelGeneric();
	}
	
	/*
	public static long getNode_Root() {
		return NetworkIDMapper.toHybridNodeID(new int[] {NodeType.Root.ordinal(), 0 });
	}
	*/

	protected long to_Root(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] {EntityRelationGlobal.SENTENCE_LENGTH_MAX + 1, NodeType.Root.ordinal(), 0});
	}

	protected long to_B(int pos, int size, int t) {
		return NetworkIDMapper.toHybridNodeID(new int[] {size - pos, NodeType.B.ordinal(), t});
	}
	
	protected long to_I(int pos, int size, int t) {
		return NetworkIDMapper.toHybridNodeID(new int[] {size - pos, NodeType.I.ordinal(), t});
	}
	
	protected long to_O(int pos, int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] {size - pos, NodeType.O.ordinal(), 0});
	}

	protected long to_X() {
		return NetworkIDMapper.toHybridNodeID(new int[] {0,  NodeType.X.ordinal(), 0});
	}
	
	

	@Override
	public EntityRelationInstance decompile(Network network) {
		BaseNetwork ernetwork = (BaseNetwork) network;
		EntityRelationInstance inst = (EntityRelationInstance) network.getInstance();
		int size = inst.size();
		List<String[]> inputs = (List<String[]>) inst.getInput();
		EntityRelationOutput output = (EntityRelationOutput)inst.getOutput();
		

		
		ArrayList<Entity> entities = new ArrayList<Entity>();
		ArrayList<Relation> relations = new ArrayList<Relation>();
		Entity e = null;
		
		long root = this.to_Root(size);
		int node_k = Arrays.binarySearch(ernetwork.getAllNodes(), root);
		
		while(node_k >= 0) {
			int[] childs = network.getMaxPath(node_k);
			int[] child_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(childs[0]));
			
			int pos = size - child_ids[0];
			NodeType nodeType = NodeType.values()[child_ids[1]];
			int entityTypeId = child_ids[2];
			String entityType =  EntityRelationOutput.getENTITY(entityTypeId).form;
			
			if (nodeType == NodeType.B) {
				e = new Entity();
				e.type = EntityRelationOutput.getENTITY(entityType);
				e.span[0] = pos;
				e.head[0] = pos;
				e.span[1] = pos + 1;
				e.head[1] = pos + 1;
				entities.add(e);
			} else if (nodeType == NodeType.I) {
				e.span[1] = pos + 1;
				e.head[1] = pos + 1;
				assert (e.type.id == entityTypeId);
			} else if (nodeType == NodeType.O) {
				e = null;
			} else if (nodeType == NodeType.X) {
				break;
			}
			
			node_k = childs[0];
		}
		
	
		EntityRelationOutput predication = new EntityRelationOutput(entities, relations);
		
		inst.setPrediction(predication);
		
		return inst;
	}
	
	
	

	@Override
	public Network compileUnlabeled(int networkId, Instance instance, LocalNetworkParam param) {
		
//		int size = instance.size();
//		long root = to_Root(size);
//		long[] allNodes = unlabeledNetwork.getAllNodes();
//		int[][][] allChildren = unlabeledNetwork.getAllChildren();
//		int root_k  = unlabeledNetwork.getNodeIndex(root);
//		int numNodes = root_k+1;
//		BaseNetwork network = NetworkBuilder.quickBuild(networkId, instance, allNodes, allChildren, numNodes, param, this);
//		return network;
		
		EntityRelationInstance inst = (EntityRelationInstance) instance;
		NetworkBuilder<BaseNetwork> ernetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		
		int size = inst.size();

		long node_Root = this.to_Root(size);
		ernetwork.addNode(node_Root);
		
		long node_X = this.to_X();
		ernetwork.addNode(node_X);
		
		for(int pos = 0; pos < size; pos++) {
			for(int t = 0; t < EntityRelationOutput.ENTITYTYPE.size(); t++) {
				long node_B = this.to_B(pos, size, t);
				ernetwork.addNode(node_B);
				long node_I = this.to_I(pos, size, t);
				ernetwork.addNode(node_I);
			}
			long node_O = this.to_O(pos, size);
			ernetwork.addNode(node_O);
		}
		
		ArrayList<Long> fromSet = new ArrayList<Long>();
		ArrayList<Long> toSet = new ArrayList<Long>();
		
		//from Root to pos 0:
		for(int t = 0; t < EntityRelationOutput.ENTITYTYPE.size(); t++) {
			long node_B = this.to_B(0, size, t);
			ernetwork.addEdge(node_Root, new long[] {node_B});
			fromSet.add(node_B);
			long node_I = this.to_I(0, size, t);
			ernetwork.addEdge(node_Root, new long[] {node_I});
			fromSet.add(node_I);
			
		}
		{
			long node_O = this.to_O(0, size);
			ernetwork.addEdge(node_Root, new long[] {node_O});
			fromSet.add(node_O);
		}
		
		
		for(int pos = 1; pos < size; pos++) {
			
			toSet.clear();
			for(int t = 0; t < EntityRelationOutput.ENTITYTYPE.size(); t++) {
				long node_B = this.to_B(pos, size, t);
				toSet.add(node_B);
				long node_I = this.to_I(pos, size, t);
				toSet.add(node_I);
				
			}
			{
				long node_O = this.to_O(pos, size);
				toSet.add(node_O);
			}
			
			
			for(Long fromNode : fromSet) {
				for(Long toNode : toSet) {
					ernetwork.addEdge(fromNode, new long[] {toNode});
				}
			}
			
			fromSet.clear();
			fromSet.addAll(toSet);
		}
		
		
		for(Long fromNode : fromSet) {
			ernetwork.addEdge(fromNode, new long[] {node_X});
		}
		
		BaseNetwork network = ernetwork.build(networkId, inst, param, this);
		return network;
	}

	

	public BaseNetwork compileUnlabelGeneric() {
		
		
		
		NetworkBuilder<BaseNetwork> ernetwork = NetworkBuilder.builder();

		
		
		int size = EntityRelationGlobal.SENTENCE_LENGTH_MAX;

		long node_Root = this.to_Root(size);
		ernetwork.addNode(node_Root);
		
		long node_X = this.to_X();
		ernetwork.addNode(node_X);
		
		for(int pos = 0; pos < size; pos++) {
			for(int t = 0; t < EntityRelationOutput.ENTITYTYPE.size(); t++) {
				long node_B = this.to_B(pos, size, t);
				ernetwork.addNode(node_B);
				long node_I = this.to_I(pos, size, t);
				ernetwork.addNode(node_I);
			}
			long node_O = this.to_O(pos, size);
			ernetwork.addNode(node_O);
		}
		
		ArrayList<Long> fromSet = new ArrayList<Long>();
		ArrayList<Long> toSet = new ArrayList<Long>();
		
		//from Root to pos 0:
		for(int t = 0; t < EntityRelationOutput.ENTITYTYPE.size(); t++) {
			long node_B = this.to_B(0, size, t);
			ernetwork.addEdge(node_Root, new long[] {node_B});
			fromSet.add(node_B);
			long node_I = this.to_I(0, size, t);
			ernetwork.addEdge(node_Root, new long[] {node_I});
			fromSet.add(node_I);
			
		}
		{
			long node_O = this.to_O(0, size);
			ernetwork.addEdge(node_Root, new long[] {node_O});
			fromSet.add(node_O);
		}
		
		
		for(int pos = 1; pos < size; pos++) {
			
			toSet.clear();
			for(int t = 0; t < EntityRelationOutput.ENTITYTYPE.size(); t++) {
				long node_B = this.to_B(pos, size, t);
				toSet.add(node_B);
				long node_I = this.to_I(pos, size, t);
				toSet.add(node_I);
				
			}
			{
				long node_O = this.to_O(pos, size);
				toSet.add(node_O);
			}
			
			
			for(Long fromNode : fromSet) {
				for(Long toNode : toSet) {
					ernetwork.addEdge(fromNode, new long[] {toNode});
				}
			}
			
			fromSet.clear();
			fromSet.addAll(toSet);
		}
		
		
		for(Long fromNode : fromSet) {
			ernetwork.addEdge(fromNode, new long[] {node_X});
		}
		
		BaseNetwork network = ernetwork.buildRudimentaryNetwork();
		return network;
	}

	
	@Override
	public Network compileLabeled(int networkId, Instance instance, LocalNetworkParam param) {

		EntityRelationInstance inst = (EntityRelationInstance) instance;
		NetworkBuilder<BaseNetwork> ernetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		EntityRelationOutput output = (EntityRelationOutput)inst.getOutput();
		
		int size = inst.size();


		long node_Root = this.to_Root(size);
		ernetwork.addNode(node_Root);
		
		long node_X = this.to_X();
		ernetwork.addNode(node_X);
		
		long[] chain = new long[size];
		Arrays.fill(chain, -1);
		
		for(Entity e : output.entities) {
			int t = e.type.id;
			long node_B = this.to_B(e.span[0], size, t);
			ernetwork.addNode(node_B);
			chain[e.span[0]] = node_B;
			
			for(int i = e.span[0] + 1; i < e.span[1]; i++) {
				long node_I = this.to_I(i, size, t);
				ernetwork.addNode(node_I);
				chain[i] = node_I;
			}
		}
		
		long from = node_Root;
		for(int i = 0; i < size; i++) {
			if (chain[i] == -1) {
				long node_O = this.to_O(i, size);
				chain[i] = node_O;
				ernetwork.addNode(node_O);
			}
			
			ernetwork.addEdge(from, new long[] {chain[i]});
			from = chain[i];
		}
		
		ernetwork.addEdge(from, new long[] {node_X});

		BaseNetwork network = ernetwork.build(networkId, inst, param, this);

		return network;

	}
	
	/*
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
*/


}
