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


public class EntityRelationLinearShareCompiler extends EntityRelationCompiler {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5689984204595365160L;
	
	public static boolean DEBUG = false;

	public EntityRelationLinearShareCompiler() {
		super();
	}
	
	BaseNetwork[] label = new BaseNetwork[1000];
	BaseNetwork[] unlabel = new BaseNetwork[1000];

	@Override
	public EntityRelationInstance decompile(Network network) {
		BaseNetwork msnetwork = (BaseNetwork) network;
		EntityRelationInstance inst = (EntityRelationInstance) network.getInstance();
		int size = inst.size();
		List<String[]> inputs = (List<String[]>) inst.getInput();
		EntityRelationOutput output = (EntityRelationOutput)inst.getOutput();
		

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
		
			int nodetype_parent = parent_ids[2];
			
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
				
				for(int i = 0; i < childs.length; i++) {
					queue.add(childs[i]); //node_E node_A
				}
			}	
			else if (nodetype_parent == NodeType.E.ordinal()) {
				for(int i = 0; i < childs.length; i++) {
					queue.add(childs[i]); //node_T node_X
				}
				
			} else if (nodetype_parent == NodeType.T.ordinal()) {
				
				int i = size - parent_ids[0];
				int k = size - parent_ids[1];
				int r = parent_ids[5];
				int t1 = parent_ids[6];
				int t2 = parent_ids[7];

				
				if (childs.length == 1) { //X
					continue;
				}
				
				
				
				int child1 = childs[0];
				int[] child1_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(child1));
				int nodetype_child1 = child1_ids[2];
				
				int child2 = childs[1];
				int[] child2_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(child2));
				int nodetype_child2 = child2_ids[2];
				
				int j = i;
				int l = k;
				
				
				ArrayList<Entity> entities1 = new ArrayList<Entity>();
				ArrayList<Entity> entities2 = new ArrayList<Entity>();
				
				if (EntityRelationGlobal.FIX_ENTITY) {
					entities1 = EntityRelationOutput.getEntities(output.entities, i, t1);
					entities2 = EntityRelationOutput.getEntities(output.entities, k, t2);
				} else {
				
					//go right
					while(nodetype_child1 != NodeType.X.ordinal()) {
						int[] child1s = network.getMaxPath(child1);
						child1 = child1s[0];
						child1_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(child1));
						nodetype_child1 = child1_ids[2];
						if (nodetype_child1 == NodeType.X.ordinal()) {
							//j = size - child1_ids[0];
							Entity entity1 = new Entity(new int[] {i, j + 1}, new int[]{-1, -1}, EntityRelationOutput.getENTITY(t1));
							
//							if (EntityRelationGlobal.FIX_ENTITY) {
//							int p = EntityRelationOutput.getEntityIdx(output.entities, entity1);
//								if (p >= 0) {
//									entities1.add(entity1);
//								}
//							} else {
								entities1.add(entity1);
//							}
							
							if (child1s.length > 1) {
								child1 = child1s[1];
								child1_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(child1));
								nodetype_child1 = child1_ids[2];
								j++;
							} else {
								break;
							}
						} else {
							j++;
						}
					}
					
					
					
					//go down
					while(nodetype_child2 != NodeType.X.ordinal()) {
						int[] child2s = network.getMaxPath(child2);
						child2 = child2s[0];
						child2_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(child2));
						nodetype_child2 = child2_ids[2];
						if (nodetype_child2 == NodeType.X.ordinal()) {
							//l = size - child2_ids[1];
							Entity entity2 = new Entity(new int[] {k, l + 1}, new int[]{-1, -1}, EntityRelationOutput.getENTITY(t2));
							
//							if (EntityRelationGlobal.FIX_ENTITY) {
//								int p = EntityRelationOutput.getEntityIdx(output.entities, entity2);
//								if (p >= 0) {
//									entities2.add(entity2);
//								}
//							} else {
								entities2.add(entity2);
//							}
							
							
							if (child2s.length > 1) {
								child2 = child2s[1];
								child2_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(child2));
								nodetype_child2 = child2_ids[2];
								l++;
							} else {
								break;
							}
						} else {
							l++;
						}
					}
				}
				
//				Entity entity1 = new Entity(new int[] {i, j + 1}, new int[]{-1, -1}, EntityRelationOutput.getENTITY(t1));
//				Entity entity2 = new Entity(new int[] {k, l + 1}, new int[]{-1, -1}, EntityRelationOutput.getENTITY(t2));
//				
//				if (!entities.contains(entity1)) {
//					entities.add(entity1);
//				}
//				
//				if (!entities.contains(entity2)) {
//					entities.add(entity2);
//				}
				
				
				for(Entity entity : entities1) {
					int p = EntityRelationOutput.getEntityIdx(entities, entity);
					if (p < 0) {
						entities.add(entity);
					}
				}
				
				for(Entity entity : entities2) {
					int p = EntityRelationOutput.getEntityIdx(entities, entity);
					if (p < 0) {
						entities.add(entity);
					}
				}
				
				
				for(Entity entity1 : entities1) {
					for(Entity entity2 : entities2) {
						Relation relation = new Relation(entities, r, entity1, entity2);
						
						if (relation.isSelfRelation())
							continue;
						
						int p = EntityRelationOutput.getRelationIdx(relations, relation);
						if (p < 0) {
							relations.add(relation);
						}
					}
				}
				
				
				
			}
	
				
		}

		
		EntityRelationOutput predication = new EntityRelationOutput(entities, relations);
		
		inst.setPrediction(predication);
		
		return inst;
	}
	
	
	
	public Network compileUnlabeledFixEntity(int networkId, Instance instance, LocalNetworkParam param) {
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
		
		/*
		if (EntityRelationGlobal.ADD_SELF_RELATION) {
			//Add self-relation
			for(int i = 0; i < output.entities.size(); i++) {
				Entity entity = output.entities.get(i);
				
				Relation selfRelation = Relation.buildSelfRelation(output.entities, entity);
				relations.add(selfRelation);
			}
		}*/
		//Add no-relation
		//.....

		ArrayList<Entity>[][] entitiesOfLeftBoundary = new ArrayList[size][EntityRelationGlobal.ENTITY_TYPE_MAX];
		for(int leftBoundary = 0; leftBoundary < size; leftBoundary++) {
			for(int t = 0; t < EntityRelationGlobal.ENTITY_TYPE_MAX; t++) {
				entitiesOfLeftBoundary[leftBoundary][t] = EntityRelationOutput.getEntities(output.entities, leftBoundary, t);
			}
		}
		
		for(int i = 0; i < size; i++) {
			for(int k = 0; k < size; k++) {
				long node_A = this.toNode_A(size, i, k);
				ernetwork.addNode(node_A);
				
				long node_E = this.toNode_E(size, i, k);
				ernetwork.addNode(node_E);
							
				ArrayList<Relation> relationsCurr = EntityRelationOutput.getRelations(relations, i, k);
				
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
					
					if (entitiesOfLeftBoundary[i][t1].size() == 0 || entitiesOfLeftBoundary[k][t2].size() == 0) {
						
					} else {
						for(Entity entity1 : entitiesOfLeftBoundary[i][t1])
						for(Entity entity2 : entitiesOfLeftBoundary[k][t2])	
						{
							
							int j = entity1.span[1];
							int l = entity2.span[1];
							
							
							
							long node_I1 = this.toNode_I1(size, i, k, r, t1, t2);
							long node_I2 = this.toNode_I2(size, i, k, r, t1, t2);
							ernetwork.addNode(node_I1);
							ernetwork.addNode(node_I2);
							ernetwork.addEdge(node_T, new long[] {node_I1, node_I2});
							
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
						
						long node_I1 = this.toNode_I1(size, i, k, r, t1, t2);
						long node_I2 = this.toNode_I2(size, i, k, r, t1, t2);
						
						
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
									ernetwork.addEdge(node_I1, new long[] { node_X });
									ernetwork.addEdge(node_I1, new long[] { next_node_I1 });
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
									ernetwork.addEdge(node_I2, new long[] { node_X });
									ernetwork.addEdge(node_I2, new long[] { next_node_I2 });
									ernetwork.addEdge(node_I2, new long[] { node_X, next_node_I2 });
								}
							}
						}
						
						
					}
					
				}

				ernetwork.addEdge(node_E, T);
				
			}
		}
		
		ArrayList<Long> toSet = new ArrayList<Long>();
		long[] toSetArr = null;
		
		for(int k = 0; k < size; k++) {
			for(int i = 0; i < size; i++) {
				long node_A = this.toNode_A(size, i, k);
				long node_E = this.toNode_E(size, i, k);
				
				toSet.clear();
				toSet.add(node_E);
				
				
				if (k < size - 1) {
					long node_A_down = this.toNode_A(size, i, k + 1);
					toSet.add(node_A_down);
				}
				
				
				if (k == 0 && i < size - 1) {
					long node_A_right = this.toNode_A(size, i + 1, k);
					toSet.add(node_A_right);
				}
				
				toSetArr = new long[toSet.size()];
				
				for(int p = 0; p < toSetArr.length; p++) {
					toSetArr[p] = toSet.get(p);
				}
				
				ernetwork.addEdge(node_A, toSetArr);
				
			}
		}
		
		
		ernetwork.addEdge(node_Root, new long[] {this.toNode_A(size, 0, 0)});

		
		 

		BaseNetwork network = ernetwork.build(networkId, inst, param, this);
		
		if (DEBUG) {
		int id = inst.getInstanceId();
		if (id < 0) id = -id;
		unlabel[id] = network;
		}
		return network;
	}
	
	
	
	
	public Network compileUnlabeledFixEntityPair(int networkId, Instance instance, LocalNetworkParam param) {
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

		
		for(int i = 0; i < size; i++) {
			for(int k = 0; k < size; k++) {
				long node_A = this.toNode_A(size, i, k);
				ernetwork.addNode(node_A);
				
				long node_E = this.toNode_E(size, i, k);
				ernetwork.addNode(node_E);
							
				ArrayList<Relation> relationsCurr = EntityRelationOutput.getRelations(relations, i, k);
				
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
					
					if (relationsCurr.size() == 0) {
						
					} else {
						for(int pRel = 0; pRel < relationsCurr.size(); pRel ++) {
							
							Relation relation = relationsCurr.get(pRel);
							
							int j = relation.arg1.span[1];
							int l = relation.arg2.span[1];
							
							if (t1 != relation.arg1.type.id || t2 != relation.arg2.type.id)
								continue;
							
							
							long node_I1 = this.toNode_I1(size, i, k, r, t1, t2);
							long node_I2 = this.toNode_I2(size, i, k, r, t1, t2);
							ernetwork.addNode(node_I1);
							ernetwork.addNode(node_I2);
							ernetwork.addEdge(node_T, new long[] {node_I1, node_I2});
							
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
						
						long node_I1 = this.toNode_I1(size, i, k, r, t1, t2);
						long node_I2 = this.toNode_I2(size, i, k, r, t1, t2);
						
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

//									ernetwork.addEdge(node_I1, new long[] { next_node_I1 });
//									ernetwork.addEdge(node_I1, new long[] { node_X });
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
									
//									ernetwork.addEdge(node_I2, new long[] { next_node_I2 });
//									ernetwork.addEdge(node_I2, new long[] { node_X });
									ernetwork.addEdge(node_I2, new long[] { node_X, next_node_I2 });
								}
							}
						}
						
						
					}
					
				}

				ernetwork.addEdge(node_E, T);
				
			}
		}
		
		ArrayList<Long> toSet = new ArrayList<Long>();
		long[] toSetArr = null;
		
		for(int k = 0; k < size; k++) {
			for(int i = 0; i < size; i++) {
				long node_A = this.toNode_A(size, i, k);
				long node_E = this.toNode_E(size, i, k);
				
				toSet.clear();
				toSet.add(node_E);
				
				
				if (k < size - 1) {
					long node_A_down = this.toNode_A(size, i, k + 1);
					toSet.add(node_A_down);
				}
				
				
				if (k == 0 && i < size - 1) {
					long node_A_right = this.toNode_A(size, i + 1, k);
					toSet.add(node_A_right);
				}
				
				toSetArr = new long[toSet.size()];
				
				for(int p = 0; p < toSetArr.length; p++) {
					toSetArr[p] = toSet.get(p);
				}
				
				ernetwork.addEdge(node_A, toSetArr);
				
			}
		}
		
		
		ernetwork.addEdge(node_Root, new long[] {this.toNode_A(size, 0, 0)});

		
		 

		BaseNetwork network = ernetwork.build(networkId, inst, param, this);
		
		/*
		int id = inst.getInstanceId();
		if (id < 0) id = -id;
		unlabel[id] = network;
		*/
		return network;
	}
	
	

	@Override
	public Network compileUnlabeled(int networkId, Instance instance, LocalNetworkParam param) {
		
		if (EntityRelationGlobal.FIX_ENTITY_PAIR)
			return this.compileUnlabeledFixEntityPair(networkId, instance, param);
		
		if (EntityRelationGlobal.FIX_ENTITY)
			return this.compileUnlabeledFixEntity(networkId, instance, param);
		
		EntityRelationInstance inst = (EntityRelationInstance) instance;
		NetworkBuilder<BaseNetwork> ernetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		
		int size = inst.size();

		long node_Root = this.toNode_Root(size);
		ernetwork.addNode(node_Root);
		long node_X = this.toNode_X(size);
		ernetwork.addNode(node_X);
		
		
		//ArrayList<int[]> relations = EntityRelationGlobal.relationEntityTriples;
		
		int L_MAX = EntityRelationGlobal.L_SPAN_MAX;
		
		
		
		for(int i = 0; i < size; i++) {
			for(int k = 0; k < size; k++) {
				long node_A = this.toNode_A(size, i, k);
				ernetwork.addNode(node_A);
				
				long node_E = this.toNode_E(size, i, k);
				ernetwork.addNode(node_E);
				
				
				
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
					
					if (EntityRelationGlobal.ENABLE_ENTITY_ORDER_CONSTRAINT) {
						if (i > k)
							continue;
					}
					
					if (EntityRelationGlobal.ENABLE_ENTITY_DIST_CONSTRAINT) {
						int dist = k - i;
						if (dist > EntityRelationGlobal.RELATION_ENTITY_DISTANCE_MAX[1] || dist < EntityRelationGlobal.RELATION_ENTITY_DISTANCE_MAX[0] ) {
							continue;
						}
					}
					
					if (EntityRelationGlobal.ADD_SELF_RELATION && r == 0) {
						if (i != k)
							continue;
					}
					
					
					long node_I1 = this.toNode_I1(size, i, k, r, t1, t2);
					long node_I2 = this.toNode_I2(size, i, k, r, t1, t2);
					ernetwork.addNode(node_I1);
					ernetwork.addNode(node_I2);
					ernetwork.addEdge(node_T, new long[] {node_I1, node_I2});
					
					
					//go left
					//for(int x = i; x < i + L_MAX && x < size - 1; x++) {
					for(int x = i; x < size - 1; x++) {
						ernetwork.addEdge(node_I1, new long[] {node_X});

						long next_node_I1 = this.toNode_I1(size, x + 1, k, r, t1, t2);
						ernetwork.addNode(next_node_I1);
						ernetwork.addEdge(node_I1, new long[] {next_node_I1});
						
						ernetwork.addEdge(node_I1, new long[] {node_X, next_node_I1});
						
						node_I1 = next_node_I1;
					}
					ernetwork.addEdge(node_I1, new long[] {node_X});
					
					//go down
					//for(int y = k; y < k + L_MAX && y < size - 1; y++) {
					for(int y = k; y < size - 1; y++) {
						ernetwork.addEdge(node_I2, new long[] {node_X});
						long next_node_I2 = this.toNode_I2(size, i, y + 1, r, t1, t2);
						ernetwork.addNode(next_node_I2);
						ernetwork.addEdge(node_I2, new long[] {next_node_I2});
						
						ernetwork.addEdge(node_I2, new long[] {node_X, next_node_I2});
						
						node_I2 = next_node_I2;
					}
					ernetwork.addEdge(node_I2, new long[] {node_X});
					
					
					
					
					
				}
				
				ernetwork.addEdge(node_E, T);
				
				
			}
		}
		

		ArrayList<Long> toSet = new ArrayList<Long>();
		long[] toSetArr = null;
		
		for(int k = 0; k < size; k++) {
			for(int i = 0; i < size; i++) {
				long node_A = this.toNode_A(size, i, k);
				long node_E = this.toNode_E(size, i, k);
				
				toSet.clear();
				toSet.add(node_E);
				
				
				if (k < size - 1) {
					long node_A_down = this.toNode_A(size, i, k + 1);
					toSet.add(node_A_down);
				}
				
				
				if (k == 0 && i < size - 1) {
					long node_A_right = this.toNode_A(size, i + 1, k);
					toSet.add(node_A_right);
				}
				
				toSetArr = new long[toSet.size()];
				
				for(int p = 0; p < toSetArr.length; p++) {
					toSetArr[p] = toSet.get(p);
				}
				
				ernetwork.addEdge(node_A, toSetArr);
				
			}
		}
		
		ernetwork.addEdge(node_Root, new long[] {this.toNode_A(size, 0, 0)});
		
		BaseNetwork network = ernetwork.build(networkId, inst, param, this);
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
		
		
		
		for(int i = 0; i < size; i++) {
			for(int k = 0; k < size; k++) {
				long node_A = this.toNode_A(size, i, k);
				ernetwork.addNode(node_A);
				
				long node_E = this.toNode_E(size, i, k);
				ernetwork.addNode(node_E);
							
				
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
				
				ernetwork.addEdge(node_E, T);
				
				
				
			}
		}
		
		ArrayList<Long> toSet = new ArrayList<Long>();
		long[] toSetArr = null;
		
		for(int k = 0; k < size; k++) {
			for(int i = 0; i < size; i++) {
				long node_A = this.toNode_A(size, i, k);
				long node_E = this.toNode_E(size, i, k);
				
				toSet.clear();
				toSet.add(node_E);
				
				
				if (k < size - 1) {
					long node_A_down = this.toNode_A(size, i, k + 1);
					toSet.add(node_A_down);
				}
				
				
				if (k == 0 && i < size - 1) {
					long node_A_right = this.toNode_A(size, i + 1, k);
					toSet.add(node_A_right);
				}
				
				toSetArr = new long[toSet.size()];
				
				for(int p = 0; p < toSetArr.length; p++) {
					toSetArr[p] = toSet.get(p);
				}
				
				ernetwork.addEdge(node_A, toSetArr);
				
			}
		}
		
		
		ernetwork.addEdge(node_Root, new long[] {this.toNode_A(size, 0, 0)});


		BaseNetwork network = ernetwork.build(networkId, inst, param, this);
		
		
		if (DEBUG) {
			int id = inst.getInstanceId();
			if (id < 0)
				id = -id;
			label[id] = network;

			if (unlabel[id] != null) {
				System.out.println(unlabel[id].contains(network));
				System.out.println();
			}
		}
		return network;

	}

}
