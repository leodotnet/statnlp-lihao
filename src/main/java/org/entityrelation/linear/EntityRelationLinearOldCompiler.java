package org.entityrelation.linear;

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
import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.example.base.BaseNetwork;
import org.statnlp.example.base.BaseNetwork.NetworkBuilder;
import org.statnlp.hypergraph.LocalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkIDMapper;
import org.statnlp.hypergraph.TableLookupNetwork;


public class EntityRelationLinearOldCompiler extends EntityRelationCompiler {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5689984204595365160L;

	public EntityRelationLinearOldCompiler() {
		super();
	}
	
	TableLookupNetwork[] label = new TableLookupNetwork[100];
	TableLookupNetwork[] unlabel = new TableLookupNetwork[100];

	@Override
	public EntityRelationInstance decompile(Network network) {
		BaseNetwork msnetwork = (BaseNetwork) network;
		EntityRelationInstance inst = (EntityRelationInstance) network.getInstance();
		int size = inst.size();
		List<String[]> inputs = (List<String[]>) inst.getInput();

		

		long root = this.toNode_Root(size);
		int node_root = Arrays.binarySearch(msnetwork.getAllNodes(), root);
		
		
	    //// x, y, origfrom, relation, entity1type, entity2type, nodetype
		// node_k = 0;
		
		ArrayList<Entity> entities = new ArrayList<Entity>();
		ArrayList<Relation> relations = new ArrayList<Relation>();
		
		
		Queue<Integer> queue = new LinkedList<Integer>();
		
		queue.clear();
		queue.add(node_root);
		
		while (!queue.isEmpty()) {
			// System.out.print(node_k + " ");
			int node_k = queue.poll();
			
			int[] parent_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(node_k));
		
			int nodetype_parent = parent_ids[3];
			
			if (nodetype_parent == NodeType.X.ordinal()) {
				break;
			} 
			
			int[] childs = network.getMaxPath(node_k);
			
			
			if (nodetype_parent == NodeType.Root.ordinal()) {
				
				for(int i = 0; i < childs.length; i++) {
					//int[] child_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(childs[i]));
					queue.add(childs[i]);
				}
				
			} else if (nodetype_parent == NodeType.A.ordinal()) {
				int[] child_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(childs[0]));
				
				int nodetype_child = child_ids[3];
				if (nodetype_child == NodeType.T.ordinal()) {
					queue.add(childs[0]); //node_T
				}
				
			} else if (nodetype_parent == NodeType.T.ordinal()) {
				
				int i = size - parent_ids[0];
				int k = size - parent_ids[1];
				int r = parent_ids[4];
				int t1 = parent_ids[5];
				int t2 = parent_ids[6];
				
				if (childs.length == 1) {
					continue;
				}
				
				
				int child1 = childs[0];
				int[] child1_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(child1));
				int nodetype_child1 = child1_ids[3];
				
				int child2 = childs[1];
				int[] child2_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(child2));
				int nodetype_child2 = child2_ids[3];
				
				int j = i;
				int l = k;
				
				//go right
				while(nodetype_child1 != NodeType.X.ordinal()) {
					child1 = network.getMaxPath(child1)[0];
					child1_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(child1));
					nodetype_child1 = child1_ids[3];
					if (nodetype_child1 == NodeType.X.ordinal()) {
						j = size - child1_ids[0];
						break;
					}
				}
				
				
				
				//go down
				while(nodetype_child2 != NodeType.X.ordinal()) {
					child2 = network.getMaxPath(child2)[0];
					child2_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(child2));
					nodetype_child2 = child2_ids[3];
					if (nodetype_child2 == NodeType.X.ordinal()) {
						l = size - child2_ids[1];
						break;
					}
				}
				
				
				Entity entity1 = new Entity(new int[] {i, j + 1}, new int[]{-1, -1}, EntityRelationOutput.getENTITY(t1));
				Entity entity2 = new Entity(new int[] {k, l + 1}, new int[]{-1, -1}, EntityRelationOutput.getENTITY(t2));
				
				if (!entities.contains(entity1)) {
					entities.add(entity1);
				}
				
				if (!entities.contains(entity2)) {
					entities.add(entity2);
				}
				
				Relation relation = new Relation(entities, r, entity1, entity2);
				
				if (!relations.contains(relation)) {
					relations.add(relation);
				}
				
				
			}
	
				
		}

		
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

		long node_Root = this.toNode_Root(size);
		ernetwork.addNode(node_Root);
		long node_X = this.toNode_X(size);
		ernetwork.addNode(node_X);
		
		long[] A = new long[size * size];
		int pToSetArr = 0;
		Arrays.fill(A,0);
		
		//ArrayList<int[]> relations = EntityRelationGlobal.relationEntityTriples;
		
		int L_MAX = EntityRelationGlobal.L_SPAN_MAX;
		
		for(int i = 0; i < size; i++) {
			for(int k = 0; k < size; k++) {
				long node_A = this.toNode_A(size, i, k);
				ernetwork.addNode(node_A);
				A[pToSetArr++] = node_A;
				
				
				long[] T = new long[EntityRelationGlobal.relationEntityTriples.size()];
				
				for(int pTriple = 0; pTriple < T.length; pTriple++) {
					int[] relationArr = EntityRelationGlobal.relationEntityTriples.get(pTriple);
					int r = relationArr[0];
					int t1 = relationArr[1];
					int t2 = relationArr[2];
					long node_T = this.toNode_T(size, i, k, r, t1, t2);
					ernetwork.addNode(node_T);
					T[pTriple] = node_T;
					
					ernetwork.addEdge(node_T, new long[]{node_X});
					
					long node_I1 = this.toNode_I1(size, i, k, r, t1, t2);
					long node_I2 = this.toNode_I2(size, i, k, r, t1, t2);
					ernetwork.addNode(node_I1);
					ernetwork.addNode(node_I2);
					ernetwork.addEdge(node_T, new long[] {node_I1, node_I2});
					
					
					//go left
					for(int x = i; x < i + L_MAX && x < size - 1; x++) {
						ernetwork.addEdge(node_I1, new long[] {node_X});

						long next_node_I1 = this.toNode_I1(size, x + 1, k, r, t1, t2);
						ernetwork.addNode(next_node_I1);
						ernetwork.addEdge(node_I1, new long[] {next_node_I1});
						
						ernetwork.addEdge(node_I1, new long[] {node_X, next_node_I1});
						
						node_I1 = next_node_I1;
					}
					ernetwork.addEdge(node_I1, new long[] {node_X});
					
					//go down
					for(int y = k; y < k + L_MAX && y < size - 1; y++) {
						ernetwork.addEdge(node_I2, new long[] {node_X});
						long next_node_I2 = this.toNode_I2(size, i, y + 1, r, t1, t2);
						ernetwork.addNode(next_node_I2);
						ernetwork.addEdge(node_I2, new long[] {next_node_I2});
						
						ernetwork.addEdge(node_I2, new long[] {node_X, next_node_I2});
						
						node_I2 = next_node_I2;
					}
					ernetwork.addEdge(node_I2, new long[] {node_X});
					
					
					
					
					
				}
				
				ernetwork.addEdge(node_A, T);
				
				
			}
		}
		ernetwork.addEdge(node_Root, A);
		

		BaseNetwork network = ernetwork.build(networkId, inst, param, this);
		
		/*
		unlabel[-instance.getInstanceId()] = network;
		
		if (label[-instance.getInstanceId()] != null) {
			System.out.println(network.contains(label[-instance.getInstanceId()]));
		}*/
		
		return network;


	}

	@Override
	public Network compileLabeled(int networkId, Instance instance, LocalNetworkParam param) {

		EntityRelationInstance inst = (EntityRelationInstance) instance;
		NetworkBuilder<BaseNetwork> ernetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		EntityRelationOutput output = (EntityRelationOutput)inst.getOutput();
		
		int size = inst.size();

		long node_Root = this.toNode_Root(size);
		ernetwork.addNode(node_Root);
		long node_X = this.toNode_X(size);
		ernetwork.addNode(node_X);
		
		
		ArrayList<Relation> relations = new ArrayList<Relation>(output.relations);
		
		if (EntityRelationGlobal.ADD_SELF_RELATION) {
			//Add self-relation
			for(int i = 0; i < output.entities.size(); i++) {
				Entity entity = output.entities.get(i);
				
				Relation selfRelation = Relation.buildSelfRelation(output.entities, entity);
				relations.add(selfRelation);
			}
		}
		//Add no-relation
		//.....
		
		
		
		long[] A = new long[size * size];
		int pToSetArr = 0;
		Arrays.fill(A,0);
		
		for(int i = 0; i < size; i++) {
			for(int k = 0; k < size; k++) {
				long node_A = this.toNode_A(size, i, k);
				ernetwork.addNode(node_A);
				A[pToSetArr++] = node_A;
				
				
				long[] T = new long[EntityRelationGlobal.relationEntityTriples.size()];
				
				for(int pTriple = 0; pTriple < T.length; pTriple++) {
					int[] relationArr = EntityRelationGlobal.relationEntityTriples.get(pTriple);
					int r = relationArr[0];
					int t1 = relationArr[1];
					int t2 = relationArr[2];
					long node_T = this.toNode_T(size, i, k, r, t1, t2);
					ernetwork.addNode(node_T);
					T[pTriple] = node_T;
					
					ArrayList<Relation> relationsCurr = EntityRelationOutput.getRelations(relations, i, k, r, t1, t2);
					
					if (relationsCurr.size() == 0) {
						ernetwork.addEdge(node_T, new long[]{node_X});
					} else {
						
						long node_I1 = this.toNode_I1(size, i, k, r, t1, t2);
						long node_I2 = this.toNode_I2(size, i, k, r, t1, t2);
						ernetwork.addNode(node_I1);
						ernetwork.addNode(node_I2);
						ernetwork.addEdge(node_T, new long[] {node_I1, node_I2});
						
						
						for(int pRel = 0; pRel < relationsCurr.size(); pRel ++) {
							
							Relation relation = relationsCurr.get(pRel);
							
							int j = relation.arg1.span[1];
							int l = relation.arg2.span[1];
							
							node_I1 = this.toNode_I1(size, i, k, r, t1, t2);
							node_I2 = this.toNode_I2(size, i, k, r, t1, t2);
								
							//go left
							for(int x = i; x < j - 1; x++) {
								long next_node_I1 = this.toNode_I1(size, x + 1, k, r, t1, t2);
								ernetwork.addNode(next_node_I1);
								ernetwork.addEdge(node_I1, new long[] {next_node_I1});
								
								node_I1 = next_node_I1;
							}
							ernetwork.addEdge(node_I1, new long[] {node_X});
							
							//go down
							for(int y = k; y < l - 1; y++) {
								long next_node_I2 = this.toNode_I2(size, i, y + 1, r, t1, t2);
								ernetwork.addNode(next_node_I2);
								ernetwork.addEdge(node_I2, new long[] {next_node_I2});
								node_I2 = next_node_I2;
							}
							ernetwork.addEdge(node_I2, new long[] {node_X});
								
						}
						
						
						//check left
						for(int x = i; x < size - 1; x++) {
							node_I1 = this.toNode_I1(size, x, k, r, t1, t2);
							if (ernetwork.contains(node_I1)) {
								// Convert two edges (one to X one to next I) to a
								// single hyperedge
								List<long[]> childrenList = ernetwork.getChildren_tmp(node_I1);
								if (childrenList.size() > 1) {
									childrenList.clear();
									long next_node_I1 = this.toNode_I1(size, x + 1, k, r, t1, t2);
									//ernetwork.addEdge(node_I1, new long[] { node_X });
									ernetwork.addEdge(node_I1, new long[] { node_X, next_node_I1 });
								}
							}
						}
						
						
						//check down
						for(int y = k; y < size - 1; y++) {
							node_I2 = this.toNode_I2(size, i, y, r, t1, t2);
							if (ernetwork.contains(node_I2)) {
								// Convert two edges (one to X one to next I) to a
								// single hyperedge
								List<long[]> childrenList = ernetwork.getChildren_tmp(node_I2);
								if (childrenList.size() > 1) {
									childrenList.clear();
									long next_node_I2 = this.toNode_I2(size, i, y + 1, r, t1, t2);
									//ernetwork.addEdge(node_I2, new long[] { node_X });
									ernetwork.addEdge(node_I2, new long[] { node_X, next_node_I2 });
								}
							}
						}
						
						
						
					}
					
					
				}
				
				ernetwork.addEdge(node_A, T);
				
				
			}
		}
		
		ernetwork.addEdge(node_Root, A);
		

		BaseNetwork network = ernetwork.build(networkId, inst, param, this);
		
		/*
		label[instance.getInstanceId()] = network;
		
		
		if (unlabel[instance.getInstanceId()] != null) {
			System.out.println(unlabel[instance.getInstanceId()].contains(network));
			System.out.println("label:");
			System.out.println(network);
			
			System.out.println("unlabel:");
			System.out.println(unlabel[instance.getInstanceId()]);
		}*/
		
		return network;

	}

	


}
