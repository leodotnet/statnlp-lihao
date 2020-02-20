package org.statnlp.targetedsentiment.overlap.cont;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.example.base.BaseNetwork;
import org.statnlp.example.base.BaseNetwork.NetworkBuilder;
import org.statnlp.hypergraph.LocalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkCompiler;
import org.statnlp.hypergraph.NetworkIDMapper;
import org.statnlp.targetedsentiment.common.TSInstance;
import org.statnlp.targetedsentiment.common.TargetSentimentGlobal;



/*******
 * 
 * @author Li Hao
 * @assumption: Only neighbor entities may have overlapping scopes
 *
 */

public class SSOverlapContCompiler extends NetworkCompiler {

	public SSOverlapContCompiler() {
		super(null);
		setCapacity();
	}

	public void setCapacity() {
		int MAX_SENTENCE_LENGTH = TargetSentimentGlobal.MAX_LENGTH_LENGTH;
		NetworkIDMapper.setCapacity(new int[] { MAX_SENTENCE_LENGTH,
				NodeTypeSize + 1, PolarityTypeSize + 1 });

	}

	private static final long serialVersionUID = 2100499563741744475L;

	public enum NodeType {
		X, B0, A0, A, E, B, Root
	};

	// public enum TType {positive, neutral, negative}

	public enum PolarityType {
		positive, neutral, negative
	}

	int NodeTypeSize = NodeType.values().length;

	int PolarityTypeSize = PolarityType.values().length;

	// int TTypeSize = TType.values().length;

	private long toNode_root(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size + 1 + size,
				NodeType.Root.ordinal(), 0 });
	}

	private long toNode_X() {
		return NetworkIDMapper.toHybridNodeID(new int[] { 0, 0, 0 });
	}

	// pos: position of word in the sentence starting from 0 to size - 1
	private long toNode_Span(int size, int pos, int node_type, int type) {
		if (node_type == NodeType.B0.ordinal()) {
			return NetworkIDMapper.toHybridNodeID(new int[] { pos + 1,
					node_type, type });
		} else {
			return NetworkIDMapper.toHybridNodeID(new int[] { size - pos + size,
					node_type, type });
		}
	}

	/*
	private long toNode_B(int size, int pos) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - pos,
				NodeType.B.ordinal(), 0 });
	}

	private long toNode_E(int size, int pos) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - pos,
				NodeType.E.ordinal(), 0 });
	}

	private long toNode_A(int size, int pos, int type) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - pos,
				NodeType.A.ordinal(), type });
	}

	private long toNode_B0(int size, int pos, int type) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - pos,
				NodeType.B0.ordinal(), type });
	}

	private long toNode_A0(int size, int pos, int type) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - pos,
				NodeType.A0.ordinal(), type });
	}*/

	@Override
	public Network compileLabeled(int networkId, Instance instance,
			LocalNetworkParam param) {
		TSInstance inst = (TSInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		ArrayList<String[]> inputs = (ArrayList<String[]>) inst.getInput();
		ArrayList<Label> outputs = (ArrayList<Label>) inst.getOutput();

		int size = inst.size();

		long root = this.toNode_root(size);
		lcrfNetwork.addNode(root);

		long[][][] node_arr = new long[NodeTypeSize][PolarityTypeSize][size];

		
		/****** build node array ******/
		long node = -1;

		for (int node_type = NodeType.X.ordinal() + 1; node_type < NodeType.Root
				.ordinal(); node_type++) {

			for (int polar = 0; polar < PolarityTypeSize; polar++) {
				for (int pos = 0; pos < size; pos++) {
					node = this.toNode_Span(size, pos, node_type, polar);
					node_arr[node_type][polar][pos] = node;
					lcrfNetwork.addNode(node);
				}
			}
		}

		long X = this.toNode_X();
		lcrfNetwork.addNode(X);
		/****** build node array ******/
		
		long[][] B0 = node_arr[NodeType.B0.ordinal()];
		long[][] B = node_arr[NodeType.B.ordinal()];
		long[][] E = node_arr[NodeType.E.ordinal()];
		long[][] A = node_arr[NodeType.A.ordinal()];
		long[][] A0 = node_arr[NodeType.A0.ordinal()];


		// //////////////////////////////////////////

		int last_entity_pos = -1;
		PolarityType last_polar = null;
		PolarityType polar = null;
		long from = -1;
		long to = -1;
		long to1 = -1;
		int entity_begin = -1;
		HashSet<String> edgeSet = new HashSet<String>();

		// Count the 1st entity for each polarity
		int[] firstEntity = new int[PolarityTypeSize];
		Arrays.fill(firstEntity, -1);
		int numEntity = 0;
		int lastEntityPos = -1;
		for (int pos = 0; pos < size; pos++) {
			String word = inputs.get(pos)[0];
			Label label = outputs.get(pos);
			boolean start_entity = TargetSentimentGlobal.startOfEntity(pos, size, outputs);

			if (start_entity) {
				lastEntityPos = pos;
				polar = PolarityType.valueOf(label.getForm().substring(2));

				if (firstEntity[polar.ordinal()] == -1) {
					firstEntity[polar.ordinal()] = numEntity;
				}
				
				if (numEntity == 0) {
					// A0->A0, A0->X
					for(int j = 0; j < PolarityTypeSize; j++) {
					for (int i = pos; i < size; i++) {
						
						if (i < size - 1) {
						from = A0[j][i];
						to = A0[j][i + 1];
						lcrfNetwork.addEdge(from, new long[] { to });
						}
						from = A0[j][i];
						to = X;
						lcrfNetwork.addEdge(from, new long[] { to });

					}
					}
				}

				numEntity++;
			}

		}
		
		// last entity
		// B0-B0 B0-X
		for(int j = 0; j < PolarityTypeSize; j++) {
			for (int i = lastEntityPos; i >= 0; i--) {
				if (i > 0) {
				from = B0[j][i];
				to = B0[j][i - 1];
				lcrfNetwork.addEdge(from, new long[] { to });
				}
				from = B0[j][i];
				to = X;
				lcrfNetwork.addEdge(from, new long[] { to });
			}
		}

		int entityIdx = -1;
		for (int pos = 0; pos < size; pos++) {
			String word = inputs.get(pos)[0];
			Label label = outputs.get(pos);

			boolean start_entity = TargetSentimentGlobal.startOfEntity(pos, size, outputs);
			boolean end_entity = TargetSentimentGlobal.endofEntity(pos, size, outputs);

			if (start_entity) {
				entityIdx++;
				polar = PolarityType.valueOf(label.getForm().substring(2));
				from = B[polar.ordinal()][pos];
				to = E[polar.ordinal()][pos];
				lcrfNetwork.addEdge(from, new long[] { to });

				if (last_entity_pos == -1) {
					from = root;
					to = B[polar.ordinal()][0];
					lcrfNetwork.addEdge(from, new long[] { to });

					// / directly from left to right
					for (int i = 0; i < pos; i++) {
						// from before node[pos] to before node at [pos+1]
						from = B[polar.ordinal()][i];
						to = B[polar.ordinal()][i + 1];
						lcrfNetwork.addEdge(from, new long[] { to });

					}

					

				} else {

					// latent path
					for (int i = last_entity_pos + 1; i < pos; i++) {
						// add A->A
						from = A[last_polar.ordinal()][i - 1];
						to = A[last_polar.ordinal()][i];
						lcrfNetwork.addEdge(from, new long[] { to });

						// add B->B
						from = B[polar.ordinal()][i];
						to = B[polar.ordinal()][i + 1];
						lcrfNetwork.addEdge(from, new long[] { to });

					}

					// add A->B
					for (int i = last_entity_pos; i < pos; i++) {

						// if neighbor entities are of different polarity
						if (!last_polar.equals(polar)) {

							from = A[last_polar.ordinal()][i];
							to = B[polar.ordinal()][i + 1];
							lcrfNetwork.addEdge(from, new long[] { to });

						} else { // if neighbor entities are of same polarity
							
							//no overlapping
							from = A[last_polar.ordinal()][i];
							to = B[polar.ordinal()][i + 1];
							lcrfNetwork.addEdge(from, new long[] { to });
							
							//overlapping
							from = A[last_polar.ordinal()][i];
							to = B[polar.ordinal()][i + 1];
							long overlap_B0 = B0[polar.ordinal()][i];
							long overlap_A0 = A0[polar.ordinal()][i];
							lcrfNetwork.addEdge(from, new long[] { to,
									overlap_B0, overlap_A0 });
						}
					}

				}

				

				entity_begin = pos;

			}

			if (end_entity) {

				// add links between entity
				for (int i = entity_begin; i < pos; i++) {
					from = E[polar.ordinal()][i];
					to = E[polar.ordinal()][i + 1];
					lcrfNetwork.addEdge(from, new long[] { to });

				}

				// add link from entity to After
				from = E[polar.ordinal()][pos];
				to = A[polar.ordinal()][pos];
				lcrfNetwork.addEdge(from, new long[] { to });

				last_entity_pos = pos;
				last_polar = polar;

			}

		}

		// add the last column node to end
		if (polar != null) {
			for (int pos = last_entity_pos + 1; pos < size; pos++) {
				from = A[polar.ordinal()][pos - 1];
				to = A[polar.ordinal()][pos];
				lcrfNetwork.addEdge(from, new long[] { to });
			}

			from = A[polar.ordinal()][size - 1]; // node_array[size -
												// 1][polar.ordinal()][SubNodeType.A.ordinal()];
			to = X;
			lcrfNetwork.addEdge(from, new long[] { to });
		} else {
			// polar = PolarityType._;
			// from = start;
			// to = node_array[0][polar.ordinal()][SubNodeType.B.ordinal()];
			// network.addEdge(from, new long[]{to});

			System.err.println("No Entity found in this Instance, Discard!");
			try {
				throw new Exception();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

		}

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		return network;
	}

	@Override
	public Network compileUnlabeled(int networkId, Instance instance,
			LocalNetworkParam param) {

		TSInstance inst = (TSInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		ArrayList<String[]> inputs = (ArrayList<String[]>) inst.getInput();
		// ArrayList<String> outputs = (ArrayList<String>) inst.getOutput();
		

		int size = inst.size();

		long root = this.toNode_root(size);
		lcrfNetwork.addNode(root);

		long[][][] node_arr = new long[NodeTypeSize][PolarityTypeSize][size];

		

		/****** build node array ******/
		long node = -1;

		for (int node_type = NodeType.X.ordinal() + 1; node_type < NodeType.Root
				.ordinal(); node_type++) {

			for (int polar = 0; polar < PolarityTypeSize; polar++) {
				for (int pos = 0; pos < size; pos++) {
					node = this.toNode_Span(size, pos, node_type, polar);
					node_arr[node_type][polar][pos] = node;
					lcrfNetwork.addNode(node);
				}
			}
		}

		long X = this.toNode_X();
		lcrfNetwork.addNode(X);
		
		long[][] B0 = node_arr[NodeType.B0.ordinal()];
		long[][] B = node_arr[NodeType.B.ordinal()];
		long[][] E = node_arr[NodeType.E.ordinal()];
		long[][] A = node_arr[NodeType.A.ordinal()];
		long[][] A0 = node_arr[NodeType.A0.ordinal()];
		/****** build node array ******/

		// //////////////////////////////////////////

		long from = -1, to = -1;

		// add first column of span node from start
		for (int j = 0; j < PolarityTypeSize; j++) {
			from = root;
			to = B[j][0];
			lcrfNetwork.addEdge(from, new long[] { to });
		}
		
		
		for(int j = 0; j < PolarityTypeSize; j++) {
			for (int i = 0; i < size; i++) {
				
				if (i < size - 1) {
					from = A0[j][i];
					to = A0[j][i + 1];
					lcrfNetwork.addEdge(from, new long[] { to });
				}
				
				from = A0[j][i];
				to = X;
				lcrfNetwork.addEdge(from, new long[] { to });

			}
			
			// last entity
			// B0-B0 B0-X
			
			for (int i = size - 1; i >= 0; i--) {
				
				if (i > 0) {
					from = B0[j][i];
					to = B0[j][i - 1];
					lcrfNetwork.addEdge(from, new long[] { to });
				}

					from = B0[j][i];
					to = X;
					lcrfNetwork.addEdge(from, new long[] { to });
			}
			
		}

		for (int pos = 0; pos < size; pos++) {
			for (int j = 0; j < PolarityTypeSize; j++) {
				// before to next before
				if (pos < size - 1) {
					from = B[j][pos];
					to = B[j][pos + 1];
					lcrfNetwork.addEdge(from, new long[] { to });
				}

				// before to current entity
				from = B[j][pos];
				to = E[j][pos];
				lcrfNetwork.addEdge(from, new long[] { to });

				// entity to after
				from = E[j][pos]; // node_array[pos][j][SubNodeType.e.ordinal()];
				to = A[j][pos]; // node_array[pos][j][SubNodeType.A.ordinal()];
				lcrfNetwork.addEdge(from, new long[] { to });

				// entity to next entity
				if (pos < size - 1) {
					from = E[j][pos]; // node_array[pos][j][SubNodeType.e.ordinal()];
					to = E[j][pos + 1]; // node_array[pos +
										// 1][j][SubNodeType.e.ordinal()];
					lcrfNetwork.addEdge(from, new long[] { to });
				}

				// after to next after
				if (pos < size - 1) {
					from = A[j][pos]; // node_array[pos][j][SubNodeType.A.ordinal()];
					to = A[j][pos + 1]; // node_array[pos +
										// 1][j][SubNodeType.A.ordinal()];
					lcrfNetwork.addEdge(from, new long[] { to });
				}

				// after to next before
				if (pos < size - 1) {

					for (int k = 0; k < PolarityTypeSize; k++) {
						/********** new cases!!!!!!!! ***********/

						if (j != k) {
							from = A[j][pos];
							to = B[k][pos + 1];
							lcrfNetwork.addEdge(from, new long[] { to });
						} else {
							
							//no overlapping
							from = A[j][pos];
							to = B[k][pos + 1];
							lcrfNetwork.addEdge(from, new long[] { to });
							
							//overlapping
							from = A[j][pos];
							to = B[k][pos + 1];
							long overlap_B0 = B0[j][pos];
							long overlap_A0 = A0[k][pos];
							try{
							lcrfNetwork.addEdge(from, new long[] { to,
									overlap_B0, overlap_A0 });} catch (Exception e) {
										System.err.println();
									}
						}

					}
				}

			}

		}

		// add last column of span node to end
		for (int j = 0; j < PolarityTypeSize; j++) {
			from = A[j][size - 1]; // node_array[size -
									// 1][j][SubNodeType.A.ordinal()];
			to = X;
			lcrfNetwork.addEdge(from, new long[] { to });
		}

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		return network;
	}

	@Override
	public Instance decompile(Network network) {
		BaseNetwork lcrfNetwork = (BaseNetwork) network;

		TSInstance inst = (TSInstance) network.getInstance();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		//ArrayList<String> outputs = (ArrayList<String>) inst.getOutput();
		ArrayList<Label> prediction = new ArrayList<Label>();
		
		int size = inst.size();


		ArrayList<int[]> preds = new ArrayList<int[]>();
		ArrayList<int[]> preds_refine = new ArrayList<int[]>();
		
		
		long rootNode = toNode_root(size);
		int root = Arrays.binarySearch(lcrfNetwork.getAllNodes(), rootNode);
		
		int[] root_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(root));
		//int[] current = new int[]{root};// network.getMaxPath(root);

		//int[] ids = NetworkIDMapper.toHybridNodeArray(network.getNode(current[0]));

		int parent = root;
		int[] child = null;
		
		ArrayList<int[]> scopes = new ArrayList<int[]>(); //for each entity
		scopes.add(new int[]{0, -1});
	
		while (true) {
			
			child = network.getMaxPath(parent);

			int[] parent_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(parent));
			int[] child_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(child[0]));
			// System.out.println("ids:" + Arrays.toString(ids));

			int parent_pos = 2 * size - parent_ids[0];
			int parent_node_type = parent_ids[1];
			int parent_polar = parent_ids[2];
			
			int child_pos = 2 * size - child_ids[0];
			int child_node_type = child_ids[1];
			int child_polar = child_ids[2];
			
			if (child_node_type == NodeType.X.ordinal()) {
				break;
			}

			if (child_node_type == NodeType.B.ordinal()) {
				prediction.add(new Label("O", 0));
				
				//sentiment scope boundary
				if (parent_node_type == NodeType.A.ordinal()) {
					if (child.length == 1) {
						scopes.get(scopes.size() - 1)[1] = child_pos;   //boundary [,)
						scopes.add(new int[]{child_pos, -1});
					} else {
						
						int A0 = child[2];
						int rightBoundary = child_pos;
						
						while(true) {
							int[] ids = NetworkIDMapper.toHybridNodeArray(network.getNode(A0));
							
							if (ids[1] == NodeType.A0.ordinal()) {
								rightBoundary = 2 * size - ids[0] + 1;
							} else if (ids[1] == NodeType.X.ordinal()) {
								break;
							}
							
							A0 = network.getMaxPath(A0)[0];
							
						}
						
						scopes.get(scopes.size() - 1)[1] = rightBoundary;   //boundary [,)
						
						
						int B0 = child[1];
						int leftBoundary = child_pos;
						
						
						while(true) {
							int[] ids = NetworkIDMapper.toHybridNodeArray(network.getNode(B0));
							
							if (ids[1] == NodeType.B0.ordinal()) {
								leftBoundary = ids[0] - 1;
							} else if (ids[1] == NodeType.X.ordinal()) {
								break;
							}
							
							B0 = network.getMaxPath(B0)[0];
							
						}
						
						
						scopes.add(new int[]{leftBoundary, -1});
						
					}
				}
				
			}
			else if (child_node_type == NodeType.E.ordinal()) {
				
				PolarityType polar = PolarityType.values()[child_polar];
				
				if (parent_node_type == NodeType.B.ordinal()) {
					//try{
					prediction.set(prediction.size() - 1, new Label("B-" + polar.name(), 0));
					//} catch (Exception e) {
					//	System.err.println();
					//}
				} else {
					prediction.add(new Label("I-" + polar.name(), 0));
				}
			}
			else if (child_node_type == NodeType.A.ordinal()) {
				if (parent_node_type == NodeType.E.ordinal()) {
					//do nothing
				} else {
					prediction.add(new Label("O", 0));
				}
			}
			
			
			parent = child[0];

		}
		
		
		scopes.get(scopes.size() - 1)[1] = size;
		
		String[] pred_arr = new String[prediction.size()];
		for(int k = 0; k < prediction.size(); k++)
		{
			pred_arr[k] = prediction.get(k).getForm();
		}
		
		inst.setPrediction(prediction);
		inst.setScopes(scopes);
		return inst;
	}

}
