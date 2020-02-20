package org.statnlp.targetedsentiment.ncrf.semi;

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
import org.statnlp.hypergraph.TableLookupNetwork;
import org.statnlp.targetedsentiment.common.TSInstance;
import org.statnlp.targetedsentiment.common.TargetSentimentGlobal;



/*******
 * 
 * @author Li Hao
 * @assumption: Only neighbor entities may have overlapping scopes
 *
 */

public class SentimentParsingSemiHybridCompiler extends SentimentParsingSemiCompiler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3384719392383824211L;

	public SentimentParsingSemiHybridCompiler() {
		super();
		setCapacity();
	}
	
	public static BaseNetwork[] labelNetworks = new BaseNetwork[3000];
	public static BaseNetwork[] unlabelNetworks = new BaseNetwork[3000];
	
	void visualize(TableLookupNetwork network, String title, int networkId)
	{
		/*
		if (labelNetworks[networkId] != null && unlabelNetworks[networkId - 1] != null)
		{
			System.out.println("contains:" + unlabelNetworks[networkId - 1].contains(labelNetworks[networkId]));
			System.out.println();
		}*/
		
	}
	
	/*
	public void setCapacity() {
		int MAX_SENTENCE_LENGTH = TargetSentimentGlobal.MAX_LENGTH_LENGTH;
		NetworkIDMapper.setCapacity(new int[] { MAX_SENTENCE_LENGTH,
				NodeTypeSize + 1, PolarityTypeSize + 1 });

	}

	

	public enum NodeType {
		X, B0, A0, O, B, Root
	};

	// public enum TType {positive, neutral, negative}

	public enum PolarityType {
		positive, neutral, negative
	}

	int NodeTypeSize = NodeType.values().length;

	int PolarityTypeSize = PolarityType.values().length;



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
*/
	

	@Override
	public Network compileLabeled(int networkId, Instance instance, LocalNetworkParam param) {
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
			int maxPolar = (node_type == NodeType.B.ordinal()) ? PolarityTypeSize : 1;
			for (int polar = 0; polar < maxPolar; polar++) {
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
		
		long[] B0 = node_arr[NodeType.B0.ordinal()][0];
		long[][] B = node_arr[NodeType.B.ordinal()];
		long[] O = node_arr[NodeType.O.ordinal()][0];
		long[] A0 = node_arr[NodeType.A0.ordinal()][0];


		// //////////////////////////////////////////

		int last_entity_pos = -2;
		PolarityType last_polar = null;
		PolarityType polar = null;
		long from = root;
		long to = -1;
		long to1 = -1;
		int entity_begin = -1;
		HashSet<String> edgeSet = new HashSet<String>();

		// Count the 1st entity for each polarity
		int[] firstEntity = new int[PolarityTypeSize];
		Arrays.fill(firstEntity, -1);
		int numEntity = 0;
		
		int fieldSize = inputs.get(0).length;
					

		int entityIdx = -1;
		for (int pos = 0; pos < size; pos++) {
			String word = inputs.get(pos)[0];
			Label label = outputs.get(pos);

			boolean start_entity = TargetSentimentGlobal.startOfEntity(pos, size, outputs);
			boolean end_entity = TargetSentimentGlobal.endofEntity(pos, size, outputs);

			if (start_entity) {
				entityIdx++;
				polar = PolarityType.valueOf(label.getForm().substring(2));
								

				for (int i = last_entity_pos + 2; i < pos; i++) {
					to = O[i];
					lcrfNetwork.addEdge(from, new long[] { to });					
					
					from = to;
				}
				
				if (last_entity_pos + 1 < pos) {
					to = B[polar.ordinal()][pos];					
					lcrfNetwork.addEdge(from, new long[] { to });
					from = to;
				}

				entity_begin = pos;

			}

			if (end_entity) {
				
				String nextTag = (pos + 1 >= size) ? "X" : outputs.get(pos + 1).getForm();
				
				ArrayList<Integer> sentimentWords = new ArrayList<Integer>(inst.potentialSentWord);
				
				/*
				if (sentimentWords.isEmpty()) {
					for(int w = 1; w <= 3; w++) {
						if (entity_begin - w >= 0)
							sentimentWords.add(entity_begin - w);
						
						if (pos + w < size)
							sentimentWords.add(pos + w);
					}
				}*/
				
				
				if (nextTag.startsWith("O")) {
					to = O[pos + 1];
				} else if (nextTag.startsWith("X")) {
					to = X;
				} else if (nextTag.startsWith("B")) {
					PolarityType nextPolar = PolarityType.valueOf(nextTag.substring(2));
					to = B[nextPolar.ordinal()][pos + 1];
				}
				
				if (sentimentWords.isEmpty()) { //no sentiment word found
					lcrfNetwork.addEdge(from, new long[] { to});
				} else {
					for (Integer sentWordPos : sentimentWords) {
						if (sentWordPos >= pos) {
							to1 = A0[sentWordPos];
						} else {
							to1 = B0[sentWordPos];
						}
						lcrfNetwork.addEdge(from, new long[] { to, to1 });

						lcrfNetwork.addEdge(to1, new long[] { X });
					}
				}
				
				from = to;
				last_entity_pos = pos;
				last_polar = polar;

			}

		}

		// add the last column node to end
		if (polar != null) {
			
			if (from != X) {
				
				for(int i = last_entity_pos + 2; i < size; i++) {
					to = O[i];
					lcrfNetwork.addEdge(from, new long[] { to });
					from = to;
				}
				
				to = X;
				lcrfNetwork.addEdge(from, new long[] { to });
			}
		} else {
			
			System.err.println("No Entity found in this Instance, Discard!");
			try {
				throw new Exception();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		
		labelNetworks[networkId] = network;
		visualize(network, "Sentiment Model:labeled", networkId);
		return network;
	}
	
	
	
	public Network compileUnlabeledFixNE(int networkId, Instance instance, LocalNetworkParam param) {

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
			int maxPolar = (node_type == NodeType.B.ordinal()) ? PolarityTypeSize : 1;
			for (int polar = 0; polar < maxPolar; polar++) {
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
		
		long[] B0 = node_arr[NodeType.B0.ordinal()][0];
		long[][] B = node_arr[NodeType.B.ordinal()];
		long[] O = node_arr[NodeType.O.ordinal()][0];
		long[] A0 = node_arr[NodeType.A0.ordinal()][0];


		// //////////////////////////////////////////

		int last_entity_pos = -2;
		//PolarityType last_polar = null;
		//PolarityType polar = null;
		//long from = root;
		//long to = -1;
		
		int entity_begin = -1;
		HashSet<String> edgeSet = new HashSet<String>();

		// Count the 1st entity for each polarity
		int[] firstEntity = new int[PolarityTypeSize];
		Arrays.fill(firstEntity, -1);
		
		ArrayList<Long> lastCandicates = new ArrayList<Long>();
		ArrayList<Long> currCandicates = new ArrayList<Long>();
		
		lastCandicates.add(root);
	
		boolean B2B = false;

		int entityIdx = -1;
		for (int pos = 0; pos < size; pos++) {
			String word = inputs.get(pos)[0];
			Label label = outputs.get(pos);

			boolean start_entity = TargetSentimentGlobal.startOfEntity(pos, size, outputs);
			boolean end_entity = TargetSentimentGlobal.endofEntity(pos, size, outputs);

			if (start_entity) {
				entityIdx++;
				//polar = PolarityType.valueOf(label.getForm().substring(2));
				
				if (!B2B){
					for (int i = last_entity_pos + 2; i < pos; i++) {
						
						currCandicates.clear();
						currCandicates.add(O[i]);
						
						for(Long from : lastCandicates) {
							for(Long to : currCandicates) {
								lcrfNetwork.addEdge(from, new long[] { to });
							}
						}
						
						lastCandicates.clear();
						lastCandicates.addAll(currCandicates);
						
					}

					currCandicates.clear();
					
					for (PolarityType polar : PolarityType.values()) {
						if (last_entity_pos + 1 < pos) {
							currCandicates.add(B[polar.ordinal()][pos]);
							//lcrfNetwork.addEdge(from, new long[] { to });
							//from = to;
						}
					}
					
					for(Long from : lastCandicates) {
						for(Long to : currCandicates) {
							lcrfNetwork.addEdge(from, new long[] { to });
						}
					}
					
					lastCandicates.clear();
					lastCandicates.addAll(currCandicates);
				

				entity_begin = pos;
				}

			}

			if (end_entity) {
				
				String nextTag = (pos + 1 >= size) ? "X" : outputs.get(pos + 1).getForm();
				
				ArrayList<Integer> sentimentWords = new ArrayList<Integer>(inst.potentialSentWord);
				
				currCandicates.clear();
				
				B2B = false;
				
				if (nextTag.startsWith("O")) {
					currCandicates.add(O[pos + 1]);
				} else if (nextTag.startsWith("X")) {
					currCandicates.add(X);
				} else if (nextTag.startsWith("B")) {
					//PolarityType nextPolar = PolarityType.valueOf(nextTag.substring(2));
					for (PolarityType polar : PolarityType.values()) {
						currCandicates.add(B[polar.ordinal()][pos + 1]);
					}
					
					B2B = true;
					
				}
				
				for (Long from : lastCandicates) {
					for (Long to : currCandicates) {

						if (sentimentWords.isEmpty()) { // no sentiment word
														// found
							lcrfNetwork.addEdge(from, new long[] { to });
						} else {
							for (Integer sentWordPos : sentimentWords) {
								long to1 = -1;
								if (sentWordPos >= pos) {
									to1 = A0[sentWordPos];
								} else {
									to1 = B0[sentWordPos];
								}
								lcrfNetwork.addEdge(from, new long[] { to, to1 });

								lcrfNetwork.addEdge(to1, new long[] { X });
							}
						}
					}
				}
				
				lastCandicates.clear();
				lastCandicates.addAll(currCandicates);
				
				last_entity_pos = pos;
				
				if (nextTag.startsWith("B")) {
					
					entity_begin = pos + 1;
					//pos = pos + 1;
					
				}

			}

		}

		// add the last column node to end
		if (last_entity_pos < size - 1) {
			
			if (!lastCandicates.contains(X)) {
				
				for(int i = last_entity_pos + 2; i < size; i++) {
					
					currCandicates.clear();
					currCandicates.add(O[i]);
					
					for(Long from : lastCandicates) {
						for(Long to : currCandicates) {
							
							lcrfNetwork.addEdge(from, new long[] { to });
						}
					}
					
					lastCandicates.clear();
					lastCandicates.addAll(currCandicates);
					
				}
				
				currCandicates.clear();
				currCandicates.add(X);
				
				for(Long from : lastCandicates) {
					for(Long to : currCandicates) {
						lcrfNetwork.addEdge(from, new long[] { to });
					}
				}
				
				lastCandicates.clear();
				lastCandicates.addAll(currCandicates);
				
			}
		} 
		
		/*else {
			
			System.err.println("No Entity found in this Instance, Discard!");
			try {
				throw new Exception();
			} catch (Exception e) {
				e.printStackTrace();
			}

		}*/

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		
		labelNetworks[networkId] = network;
		visualize(network, "Sentiment Model:unlabeled_fixne", networkId);
		return network;
	}
	
	String node2Str(long node)
	{
		int[] ids = NetworkIDMapper.toHybridNodeArray(node);
		return Arrays.toString(ids);
	}

	@Override
	public Network compileUnlabeled(int networkId, Instance instance, LocalNetworkParam param) {
		
		if (TargetSentimentGlobal.FIXNE)
			return this.compileUnlabeledFixNE(networkId, instance, param);

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
			int maxPolar = (node_type == NodeType.B.ordinal()) ? PolarityTypeSize : 1;
			for (int polar = 0; polar < maxPolar; polar++) {
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
		
		long[] B0 = node_arr[NodeType.B0.ordinal()][0];
		long[][] B = node_arr[NodeType.B.ordinal()];
		long[] O = node_arr[NodeType.O.ordinal()][0];
		long[] A0 = node_arr[NodeType.A0.ordinal()][0];


		// //////////////////////////////////////////

		long from = -1, to = -1, to1 = -1;

		// add first column of span node from start
		from = root;
		for (int j = 0; j < PolarityTypeSize; j++) {
			to = B[j][0];
			lcrfNetwork.addEdge(from, new long[] { to });
		}
		
		to = O[0];
		lcrfNetwork.addEdge(from, new long[] { to });
		
		
		int LNER = TargetSentimentGlobal.NER_SPAN_MAX;
		int LO = TargetSentimentGlobal.O_SPAN_MAX;

		for (int pos = 0; pos < size; pos++) {
			
			//B
			for (int j = 0; j < PolarityTypeSize; j++) {
				from = B[j][pos];
				
				ArrayList<Integer> sentimentWords = new ArrayList<Integer>(inst.potentialSentWord);
				
				/*
				if (sentimentWords.isEmpty()) {
					for(int w = 1; w <= 3; w++) {
						if (pos - w >= 0)
							sentimentWords.add(pos - w);
						
						if (pos + w < size)
							sentimentWords.add(pos + w);
					}
				}*/
				
				if (sentimentWords.isEmpty()) {
					
					for (int L = 1; L <= LNER; L++) {

						if (pos + L < size) {
							// to B+
							to = B[PolarityType.positive.ordinal()][pos + L];
							lcrfNetwork.addEdge(from, new long[] { to });

							// to B0
							to = B[PolarityType.neutral.ordinal()][pos + L];
							lcrfNetwork.addEdge(from, new long[] { to });

							// to B-
							to = B[PolarityType.negative.ordinal()][pos + L];
							lcrfNetwork.addEdge(from, new long[] { to });

							// to O
							// to B0
							to = O[pos + L];
							lcrfNetwork.addEdge(from, new long[] { to });


						} else if (pos + L == size) {

							to = X;
							lcrfNetwork.addEdge(from, new long[] { to });


						} else {
							break;
						}

					}

				} else {

					for (Integer sentWordPos : sentimentWords) {

						if (sentWordPos >= pos) {
							to1 = A0[sentWordPos];
						} else {
							to1 = B0[sentWordPos];
						}

						for (int L = 1; L <= LNER; L++) {

							if (pos + L < size) {
								// to B+
								to = B[PolarityType.positive.ordinal()][pos + L];
								lcrfNetwork.addEdge(from, new long[] { to, to1 });

								// to B0
								to = B[PolarityType.neutral.ordinal()][pos + L];
								lcrfNetwork.addEdge(from, new long[] { to, to1 });

								// to B-
								to = B[PolarityType.negative.ordinal()][pos + L];
								lcrfNetwork.addEdge(from, new long[] { to, to1 });

								// to O
								// to B0
								to = O[pos + L];
								lcrfNetwork.addEdge(from, new long[] { to, to1 });

								lcrfNetwork.addEdge(to1, new long[] { X });

							} else if (pos + L == size) {

								to = X;
								lcrfNetwork.addEdge(from, new long[] { to, to1 });

								lcrfNetwork.addEdge(to1, new long[] { X });

							} else {
								break;
							}

						}
					}
				}

			}
			
			//O
			from = O[pos];
			if (pos + 1 < size) {
				to = O[pos + 1];
				lcrfNetwork.addEdge(from, new long[] { to }); 
				
				to = B[PolarityType.positive.ordinal()][pos + 1];
				lcrfNetwork.addEdge(from, new long[] { to });

				// to B0
				to = B[PolarityType.neutral.ordinal()][pos + 1];
				lcrfNetwork.addEdge(from, new long[] { to });

				// to B-
				to = B[PolarityType.negative.ordinal()][pos + 1];
				lcrfNetwork.addEdge(from, new long[] { to });
				
			} else if (pos + 1 == size){
				to = X;
				lcrfNetwork.addEdge(from, new long[] { to }); 
			} else {
				break;
			}
			
		}


		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		
		unlabelNetworks[networkId] = network;
		visualize(network, "Sentiment Model:unlabeled", networkId);
		
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
			
			if (child_node_type ==  NodeType.X.ordinal()) {
				child_pos = size;
			}
			
			
			if (parent_node_type == NodeType.O.ordinal() && 
					(child_node_type == NodeType.B.ordinal() || child_node_type == NodeType.O.ordinal() || child_node_type == NodeType.X.ordinal())) {
				
				for(int i = parent_pos; i < child_pos; i++) {
					prediction.add(new Label("O", 0));
				}
			} 
			
			else if (parent_node_type == NodeType.B.ordinal() && 
					(child_node_type == NodeType.B.ordinal() || child_node_type == NodeType.O.ordinal() || child_node_type == NodeType.X.ordinal())) {
				
				String polar = PolarityType.values()[parent_polar].name();
				
				prediction.add(new Label("B-" + polar, 0));
				for(int i = parent_pos + 1; i < child_pos; i++) {
					prediction.add(new Label("I-" + polar, 0));
				}

				if (child.length > 1) {
					int[] sentWord_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(child[1]));

					int sentWordPos = -1;

					if (sentWord_ids[1] == NodeType.B0.ordinal()) {
						sentWordPos = sentWord_ids[0] - 1;
					} else {
						sentWordPos = 2 * size - sentWord_ids[0];
					}

					scopes.add(new int[] { sentWordPos, sentWordPos + 1 });
				} else {
					scopes.add(new int[] { parent_pos, child_pos });
				}
			}
			
			
			if (child_node_type == NodeType.X.ordinal()) {
				break;
			}

			
			parent = child[0];

		}
		
		
		/*
		String[] pred_arr = new String[prediction.size()];
		for(int k = 0; k < prediction.size(); k++)
		{
			pred_arr[k] = prediction.get(k).getForm();
		}*/
		
		inst.setPrediction(prediction);
		inst.setScopes(scopes);
		return inst;
	}

}
