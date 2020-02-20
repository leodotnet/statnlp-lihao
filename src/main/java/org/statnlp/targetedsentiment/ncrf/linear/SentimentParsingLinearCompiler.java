package org.statnlp.targetedsentiment.ncrf.linear;

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
import org.statnlp.hypergraph.NetworkConfig;
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

public class SentimentParsingLinearCompiler extends NetworkCompiler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 3384719392383824211L;

	public SentimentParsingLinearCompiler() {
		super();
		setCapacity();
	}

	public static BaseNetwork[] labelNetworks = new BaseNetwork[3000];
	public static BaseNetwork[] unlabelNetworks = new BaseNetwork[3000];

	void visualize(TableLookupNetwork network, String title, int networkId) {
		/*
		 * if (labelNetworks[networkId] != null && unlabelNetworks[networkId -
		 * 1] != null) { System.out.println("contains:" +
		 * unlabelNetworks[networkId - 1].contains(labelNetworks[networkId]));
		 * System.out.println(); }
		 */

	}

	public void setCapacity() {
		int MAX_SENTENCE_LENGTH = TargetSentimentGlobal.MAX_LENGTH_LENGTH;
		NetworkIDMapper.setCapacity(new int[] { MAX_SENTENCE_LENGTH, NodeTypeSize + 1, PolarityTypeSize + 1 });

	}

	public enum NodeType {
		X, B0, A0, O, I, B, NULL, Root
	};

	// public enum TType {positive, neutral, negative}

	public enum PolarityType {
		positive, neutral, negative
	}

	int NodeTypeSize = NodeType.values().length;

	int PolarityTypeSize = PolarityType.values().length;

	private long toNode_root(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size + 2 + size, NodeType.Root.ordinal(), 0 });
	}
	
	private long toNode_NULL(int size, int type) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size + 1 + size, NodeType.NULL.ordinal(), type });
	}
	
	private long[] getNULLNodes(int size) {
		long[] NULLNodes = new long[PolarityTypeSize];
		for(int i = 0; i < PolarityTypeSize; i++)
			NULLNodes[i] = this.toNode_NULL(size, i);
		return NULLNodes;
	}

	private long toNode_X() {
		return NetworkIDMapper.toHybridNodeID(new int[] { 0, 0, 0 });
	}

	// pos: position of word in the sentence starting from 0 to size - 1
	private long toNode_Span(int size, int pos, int node_type, int type) {
		if (node_type == NodeType.B0.ordinal()) {
			return NetworkIDMapper.toHybridNodeID(new int[] { pos + 1, node_type, type });
		} else {
			return NetworkIDMapper.toHybridNodeID(new int[] { size - pos + size, node_type, type });
		}
	}

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

		for (int node_type = NodeType.X.ordinal() + 1; node_type < NodeType.Root.ordinal(); node_type++) {
			if (node_type == NodeType.NULL.ordinal()) continue;
			int maxPolar = (node_type == NodeType.B.ordinal() || node_type == NodeType.I.ordinal()) ? PolarityTypeSize : 1;
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
		long[][] I = node_arr[NodeType.I.ordinal()];
		long[] O = node_arr[NodeType.O.ordinal()][0];
		long[] A0 = node_arr[NodeType.A0.ordinal()][0];

		long[] NULL = this.getNULLNodes(size);
		
		if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
			for(int i = 0; i < NULL.length; i++)
				lcrfNetwork.addNode(NULL[i]);
			ArrayList<Integer> sentimentWords = new ArrayList<Integer>(inst.potentialSentWord);
			if (sentimentWords.isEmpty()) { // no sentiment word found

				if (inst.NULLTarget != null) {
					PolarityType NullTargetPolar = PolarityType.valueOf(inst.NULLTarget);
					lcrfNetwork.addEdge(NULL[NullTargetPolar.ordinal()], new long[] { X });
				}
				else {
				for(int i = 0; i < NULL.length; i++) 
					lcrfNetwork.addEdge(NULL[i], new long[] { X });
				}

			} else { // enumerate all the possible sentiment words

				for (Integer sentWordPos : sentimentWords) {
					
					// At B node, add hyperedge
					if (inst.NULLTarget != null) {
						PolarityType NullTargetPolar = PolarityType.valueOf(inst.NULLTarget);
						lcrfNetwork.addEdge(NULL[NullTargetPolar.ordinal()], new long[] {A0[sentWordPos] });
						lcrfNetwork.addEdge(A0[sentWordPos], new long[] { X });
					} else {
					
						for(int i = 0; i < NULL.length; i++) {
							lcrfNetwork.addEdge(NULL[i], new long[] {A0[sentWordPos] });
							lcrfNetwork.addEdge(A0[sentWordPos], new long[] { X });
						}
					}

				}

			}

		}
		
		// //////////////////////////////////////////

		int last_entity_pos = -2;
		PolarityType last_polar = null;
		PolarityType polar = null;
		long from = root;
		long to = -1;
		long to1 = -1;
		int entity_begin = -1;
		boolean isLastPosEntityBegin = false;

		// Count the 1st entity for each polarity
		int[] firstEntity = new int[PolarityTypeSize];
		Arrays.fill(firstEntity, -1);

		for (int pos = 0; pos < size; pos++) {
			String word = inputs.get(pos)[0];
			Label label = outputs.get(pos);

			boolean start_entity = TargetSentimentGlobal.startOfEntity(pos, size, outputs);
			boolean end_entity = TargetSentimentGlobal.endofEntity(pos, size, outputs);

			if (label.getForm().charAt(0) == 'O') {
				to = O[pos];
			} else {
				polar = PolarityType.valueOf(label.getForm().substring(2));

				if (start_entity) {
					to = B[polar.ordinal()][pos];
				} else {
					to = I[polar.ordinal()][pos];
				}
			}

			if (isLastPosEntityBegin) {

				ArrayList<Integer> sentimentWords = new ArrayList<Integer>(inst.potentialSentWord);
				if (sentimentWords.isEmpty()) { // no sentiment word found

					lcrfNetwork.addEdge(from, new long[] { to });

				} else { // enumerate all the possible sentiment words

					for (Integer sentWordPos : sentimentWords) {
						if (sentWordPos >= pos) {
							to1 = A0[sentWordPos];
						} else {
							to1 = B0[sentWordPos];
						}

						// At B node, add hyperedge

						lcrfNetwork.addEdge(from, new long[] { to, to1 });
						lcrfNetwork.addEdge(to1, new long[] { X });

					}

				}

				from = to;
			} else {
				
				if (TargetSentimentGlobal.ALLOW_NULL_TARGET && pos == 0 && inst.numNULLTarget > 0) 
				{
					if (inst.NULLTarget != null) {
						PolarityType NullTargetPolar = PolarityType.valueOf(inst.NULLTarget);
						lcrfNetwork.addEdge(from, new long[] {to, NULL[NullTargetPolar.ordinal()] });
					} else {
						for(int i = 0; i < PolarityTypeSize; i++) 
							lcrfNetwork.addEdge(from, new long[] {to, NULL[i] });
					}
					
				} else {
					lcrfNetwork.addEdge(from, new long[] { to });
				}
				from = to;
			}

			from = to;
			last_entity_pos = pos;
			last_polar = polar;
			isLastPosEntityBegin = start_entity;
		}

		// add the last column node to end
		if (polar != null) {

			to = X;

			if (isLastPosEntityBegin) {

				ArrayList<Integer> sentimentWords = new ArrayList<Integer>(inst.potentialSentWord);
				if (sentimentWords.isEmpty()) { // no sentiment word found

					lcrfNetwork.addEdge(from, new long[] { to });

				} else { // enumerate all the possible sentiment words

					for (Integer sentWordPos : sentimentWords) {
						if (sentWordPos >= size - 1) {
							to1 = A0[sentWordPos];
						} else {
							to1 = B0[sentWordPos];
						}

						// At B node, add hyperedge

						lcrfNetwork.addEdge(from, new long[] { to, to1 });
						lcrfNetwork.addEdge(to1, new long[] { X });

					}

				}

				from = to;
			} else {
				lcrfNetwork.addEdge(from, new long[] { to });
				from = to;
			}

		} else {

			if (NetworkConfig.STATUS == NetworkConfig.ModelStatus.TRAINING) {
				
				if (TargetSentimentGlobal.ALLOW_NULL_TARGET) {
					lcrfNetwork.addEdge(from, new long[] { X });
				} else {
				
					System.err.println("No Entity found in this Instance, Discard!");
					System.exit(0);
					try {
						throw new Exception();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
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

		for (int node_type = NodeType.X.ordinal() + 1; node_type < NodeType.Root.ordinal(); node_type++) {
			if (node_type == NodeType.NULL.ordinal()) continue;
			int maxPolar = (node_type == NodeType.B.ordinal() || node_type == NodeType.I.ordinal()) ? PolarityTypeSize : 1;
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
		long[][] I = node_arr[NodeType.I.ordinal()];
		long[] O = node_arr[NodeType.O.ordinal()][0];
		long[] A0 = node_arr[NodeType.A0.ordinal()][0];
		
		long[] NULL = this.getNULLNodes(size);
		
		
		if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
			for(int i = 0; i < NULL.length; i++)
				lcrfNetwork.addNode(NULL[i]);
			ArrayList<Integer> sentimentWords = new ArrayList<Integer>(inst.potentialSentWord);
			if (sentimentWords.isEmpty()) { // no sentiment word found

				for(int i = 0; i < NULL.length; i++) 
					lcrfNetwork.addEdge(NULL[i], new long[] { X });

			} else { // enumerate all the possible sentiment words

				for (Integer sentWordPos : sentimentWords) {
					
					// At B node, add hyperedge
					for(int i = 0; i < NULL.length; i++) {
						lcrfNetwork.addEdge(NULL[i], new long[] {A0[sentWordPos] });
						lcrfNetwork.addEdge(A0[sentWordPos], new long[] { X });
					}

				}

			}

		}
		

		int last_entity_pos = -2;
		// PolarityType last_polar = null;
		// PolarityType polar = null;
		long from = root;
		long to = -1;
		long to1 = -1;
		int entity_begin = -1;
		NodeType lastNodeType = null;
		boolean isLastPosEntityBegin = false;

		// Count the 1st entity for each polarity
		int[] firstEntity = new int[PolarityTypeSize];
		Arrays.fill(firstEntity, -1);
		ArrayList<Long> lastCandicates = new ArrayList<Long>();
		ArrayList<Long> currCandicates = new ArrayList<Long>();

		lastCandicates.add(root);

		for (int pos = 0; pos <= size; pos++) {
			//String word = inputs.get(pos)[0];
			

			//boolean start_entity = TargetSentimentGlobal.startOfEntity(pos, size, outputs);
			//boolean end_entity = TargetSentimentGlobal.endofEntity(pos, size, outputs);

			currCandicates.clear();

			boolean isLastEntityBegin = false;

			if (pos < size) {
				Label label = outputs.get(pos);
				if (label.getForm().charAt(0) == 'O') {
					currCandicates.add(O[pos]);
				} else if (label.getForm().charAt(0) == 'B') {

					for (PolarityType polar : PolarityType.values()) {
						currCandicates.add(B[polar.ordinal()][pos]);
					}


				} else { // 'I'

					for (PolarityType polar : PolarityType.values()) {
						currCandicates.add(I[polar.ordinal()][pos]);
					}
				}
				
				
				
			} else {
				currCandicates.add(X);
			}
			
			Label lastLabel = pos > 0 ? outputs.get(pos - 1) : new Label("Root", 0);
			if (lastLabel.getForm().charAt(0) == 'B')
				isLastEntityBegin = true;

			for (Long lastNode : lastCandicates) {

				for (Long currNode : currCandicates) {

					if (isLastEntityBegin) { // last node is B nodde

						ArrayList<Integer> sentimentWords = new ArrayList<Integer>(inst.potentialSentWord);
						if (sentimentWords.isEmpty()) { // no sentiment word
														// found

							lcrfNetwork.addEdge(lastNode, new long[] { currNode });

						} else { // enumerate all the possible sentiment words

							for (Integer sentWordPos : sentimentWords) {
								if (sentWordPos >= pos) {
									to1 = A0[sentWordPos];
								} else {
									to1 = B0[sentWordPos];
								}

								// At B node, add hyperedge
								lcrfNetwork.addEdge(lastNode, new long[] { currNode, to1 });
								lcrfNetwork.addEdge(to1, new long[] { X });

							}

						}

					} else {
						
						if (TargetSentimentGlobal.ALLOW_NULL_TARGET && pos == 0 && inst.numNULLTarget > 0) 
						{
							for(int i = 0; i < PolarityTypeSize; i++) 
								lcrfNetwork.addEdge(lastNode, new long[] {currNode, NULL[i] });
							
						} else {
							lcrfNetwork.addEdge(lastNode, new long[] { currNode });
						}

					}

				}
			}

			lastCandicates.clear();
			lastCandicates.addAll(currCandicates);

		}

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);

		return network;

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

		for (int node_type = NodeType.X.ordinal() + 1; node_type < NodeType.Root.ordinal(); node_type++) {
			if (node_type == NodeType.NULL.ordinal()) continue;
			int maxPolar = (node_type == NodeType.B.ordinal() || node_type == NodeType.I.ordinal()) ? PolarityTypeSize : 1;
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
		long[][] I = node_arr[NodeType.I.ordinal()];
		long[] O = node_arr[NodeType.O.ordinal()][0];
		long[] A0 = node_arr[NodeType.A0.ordinal()][0];
		
		long[] NULL = this.getNULLNodes(size);
		if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
			for(int i = 0; i < NULL.length; i++)
				lcrfNetwork.addNode(NULL[i]);
			
			ArrayList<Integer> sentimentWords = new ArrayList<Integer>(inst.potentialSentWord);
			if (sentimentWords.isEmpty()) { // no sentiment word found

				for(int i = 0; i < NULL.length; i++) 
					lcrfNetwork.addEdge(NULL[i], new long[] { X });

			} else { // enumerate all the possible sentiment words

				for (Integer sentWordPos : sentimentWords) {
					
					// At B node, add hyperedge
					for(int i = 0; i < NULL.length; i++) {
						lcrfNetwork.addEdge(NULL[i], new long[] {A0[sentWordPos] });
						lcrfNetwork.addEdge(A0[sentWordPos], new long[] { X });
					}

				}

			}

		}

		// //////////////////////////////////////////

		long from = -1, to = -1, to1 = -1;

		// add first column of span node from start
		from = root;
		for (int j = 0; j < PolarityTypeSize; j++) {
			to = B[j][0];
			if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
				for(int i = 0; i < PolarityTypeSize; i++) {
					lcrfNetwork.addEdge(from, new long[] { to, NULL[i] });
				}
			} else {
				lcrfNetwork.addEdge(from, new long[] { to });
			}
		}

		to = O[0];
		if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
			for(int i = 0; i < PolarityTypeSize; i++) {
				lcrfNetwork.addEdge(from, new long[] { to, NULL[i] });
			}
		} else {
			lcrfNetwork.addEdge(from, new long[] { to });
		}
		// add first column of span node from start

		for (int pos = 0; pos < size; pos++) {

			// B
			for (int j = 0; j < PolarityTypeSize; j++) {
				from = B[j][pos];

				ArrayList<Integer> sentimentWords = new ArrayList<Integer>(inst.potentialSentWord);

				if (sentimentWords.isEmpty()) {

					if (pos + 1 < size) {
						// to B+

						for (int k = 0; k < PolarityTypeSize; k++) {
							to = B[k][pos + 1];
							lcrfNetwork.addEdge(from, new long[] { to });
						}

						to = I[j][pos + 1];
						lcrfNetwork.addEdge(from, new long[] { to });

						// to O
						// to B0
						to = O[pos + 1];
						lcrfNetwork.addEdge(from, new long[] { to });

					} else if (pos + 1 == size) {

						to = X;
						lcrfNetwork.addEdge(from, new long[] { to });

					} else {
						break;
					}

				} else {

					for (Integer sentWordPos : sentimentWords) {

						if (sentWordPos >= pos) {
							to1 = A0[sentWordPos];
						} else {
							to1 = B0[sentWordPos];
						}

						if (pos + 1 < size) {

							for (int k = 0; k < PolarityTypeSize; k++) {
								// to B+
								to = B[k][pos + 1];
								lcrfNetwork.addEdge(from, new long[] { to, to1 });
								lcrfNetwork.addEdge(to1, new long[] { X });
							}

							// I
							to = I[j][pos + 1];
							lcrfNetwork.addEdge(from, new long[] { to, to1 });
							lcrfNetwork.addEdge(to1, new long[] { X });

							// to O
							to = O[pos + 1];
							lcrfNetwork.addEdge(from, new long[] { to, to1 });
							lcrfNetwork.addEdge(to1, new long[] { X });

						} else if (pos + 1 == size) {

							to = X;
							lcrfNetwork.addEdge(from, new long[] { to, to1 });

							lcrfNetwork.addEdge(to1, new long[] { X });

						} else {
							break;
						}

					}
				}

				if (pos > 0) {
					from = I[j][pos];
					if (pos + 1 < size) {
						// to B+

						for (int k = 0; k < PolarityTypeSize; k++) {
							to = B[k][pos + 1];
							lcrfNetwork.addEdge(from, new long[] { to });
						}

						to = I[j][pos + 1];
						lcrfNetwork.addEdge(from, new long[] { to });

						// to O
						// to B0
						to = O[pos + 1];
						lcrfNetwork.addEdge(from, new long[] { to });

					} else if (pos + 1 == size) {

						to = X;
						lcrfNetwork.addEdge(from, new long[] { to });

					} else {
						break;
					}
				}

			}

			// O
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

			} else if (pos + 1 == size) {
				to = X;
				lcrfNetwork.addEdge(from, new long[] { to });
			} else {
				break;
			}

		}

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);

		// unlabelNetworks[networkId] = network;
		// visualize(network, "Sentiment Model:unlabeled", networkId);

		return network;
	}

	@Override
	public Instance decompile(Network network) {
		BaseNetwork lcrfNetwork = (BaseNetwork) network;

		TSInstance inst = (TSInstance) network.getInstance();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		// ArrayList<String> outputs = (ArrayList<String>) inst.getOutput();
		ArrayList<Label> prediction = new ArrayList<Label>();

		int size = inst.size();

		ArrayList<int[]> preds = new ArrayList<int[]>();
		ArrayList<int[]> preds_refine = new ArrayList<int[]>();

		long rootNode = toNode_root(size);
		int root = Arrays.binarySearch(lcrfNetwork.getAllNodes(), rootNode);

		int[] root_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(root));
		// int[] current = new int[]{root};// network.getMaxPath(root);

		// int[] ids =
		// NetworkIDMapper.toHybridNodeArray(network.getNode(current[0]));

		int parent = root;
		int[] child = null;

		ArrayList<int[]> scopes = new ArrayList<int[]>(); // for each entity
		
		ArrayList<int[]> target_pred = new ArrayList<int[]>();
		
		if (TargetSentimentGlobal.ALLOW_NULL_TARGET) {
		
			for(int i = 0; i < inst.targets.size(); i++) {
				int[] target = ((int[])inst.targets.get(i)).clone();
				target[2] = -1;
				target_pred.add(target);
			}
		}
		
		int NULLTargetSentiWordPos = -1;
		
		String polar = null;

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
			
			if (parent_node_type == NodeType.Root.ordinal()) {
				if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.NULLTarget != null) {
					int[] nullTarget_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(child[1]));
					int nullTarget_polar = nullTarget_ids[2];
					inst.NULLTargetPred = PolarityType.values()[nullTarget_polar].name();
					
					for(int i = 0; i < target_pred.size(); i++) {
						int[] target = target_pred.get(i);
						if (target[0] == -1) {
							target[2] = nullTarget_polar;
						}
					}
					
					int[] sentWord = network.getMaxPath(child[1]);
					int[] sentWord_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(sentWord[0]));
					
					
					int sentWordPos = -1;
					if (sentWord_ids[1] == NodeType.X.ordinal()) {
						sentWordPos = -1;
					} else if (sentWord_ids[1] == NodeType.B0.ordinal()) {
						sentWordPos = sentWord_ids[0] - 1;
					} else if (sentWord_ids[1] == NodeType.A0.ordinal()){
						sentWordPos = 2 * size - sentWord_ids[0];
					} else {
						System.err.println(Arrays.toString(sentWord_ids));
						System.err.println("NOT possible!!!!!");
						System.exit(0);
					}
					
					NULLTargetSentiWordPos = sentWordPos;
				}
			}

			if (child_node_type == NodeType.X.ordinal()) {
				child_pos = size;
			}

			if (parent_node_type == NodeType.O.ordinal()) {

				for (int i = parent_pos; i < child_pos; i++) {
					prediction.add(new Label("O", 0));
				}
			} else if (parent_node_type == NodeType.B.ordinal()) {

				polar = PolarityType.values()[parent_polar].name();

				prediction.add(new Label("B-" + polar, 0));

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
					scopes.add(new int[] { parent_pos, -1 });
				}
			} else if (parent_node_type == NodeType.I.ordinal()) {


				prediction.add(new Label("I-" + polar, 0));

			}

			if (child_node_type == NodeType.X.ordinal()) {
				break;
			}

			parent = child[0];

		}
		
	
		
		if (TargetSentimentGlobal.ALLOW_NULL_TARGET){
			int entityBeginIdx = -1;
			int entityEndIdx = -1;
			int polarIdx = -1;
		
			for(int pos = 0; pos < prediction.size(); pos++) {
				
				boolean start_entity = TargetSentimentGlobal.startOfEntity(pos, size, prediction);
				boolean end_entity = TargetSentimentGlobal.endofEntity(pos, size, prediction);
				
				if (start_entity) {
					entityBeginIdx = pos;
					polarIdx = PolarityType.valueOf(prediction.get(pos).getForm().substring(2)).ordinal();
				}
				
				if (end_entity) {
					entityEndIdx = pos;
					
					for(int i = 0; i < target_pred.size(); i++) {
						int[] target = target_pred.get(i);
						if (target[0] == entityBeginIdx && target[1] == entityEndIdx) {
							target[2] = polarIdx;
						}
					}
					
					entityBeginIdx = -1;
					entityEndIdx = -1;
					polarIdx = -1;
					
					
				}
				
			}
		}
		
		if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
			if (NULLTargetSentiWordPos == -1)
				scopes.add(new int[]{-1, -1});
			else
				scopes.add(new int[]{NULLTargetSentiWordPos, NULLTargetSentiWordPos+1});
		}
		
		/*
		System.out.println("==");
		System.out.println(inst.getSentence());
		System.out.println(prediction);
		System.out.println(inst.output);*/
		
		inst.setTargetPrediction(target_pred);
		inst.setPrediction(prediction);
		inst.setScopes(scopes);
		
		/*
		if (TargetSentimentGlobal.ALLOW_NULL_TARGET){
			for(int j = 0; j < inst.targets_pred.size(); j++) {
				
				int[] pred_entity = (int[])inst.targets_pred.get(j);
				int[] gold_entity = (int[])inst.targets.get(j);
				//System.out.println(Arrays.toString(pred_entity) + "\t" + Arrays.toString(gold_entity));
			}
			//System.out.println();
		}*/
		
		
		
		return inst;
	}

}
