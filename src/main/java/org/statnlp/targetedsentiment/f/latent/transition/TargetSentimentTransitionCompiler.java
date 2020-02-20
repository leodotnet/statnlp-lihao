package org.statnlp.targetedsentiment.f.latent.transition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

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
import org.statnlp.targetedsentiment.ncrf.linear.SentimentParsingLinearCompiler.NodeType;
import org.statnlp.targetedsentiment.ncrf.linear.SentimentParsingLinearCompiler.PolarityType;

public class TargetSentimentTransitionCompiler extends NetworkCompiler {

	int NEMaxLength = 3;
	int SpanMaxLength = 10;
	boolean visual = true;

	// TargetSentimentViewer viewer = new TargetSentimentViewer(this, null, 5);
	// void visualize(LinearNetwork network, String title, int networkId)
	// {
	// viewer.visualizeNetwork(network, null, title + "[" + networkId + "]");
	// }

	public TargetSentimentTransitionCompiler() {
		super(null);
		// TODO Auto-generated constructor stub
	}

	public TargetSentimentTransitionCompiler(int NEMaxLength, int SpanMaxLength) {
		super(null);
		this.NEMaxLength = NEMaxLength;
		this.SpanMaxLength = SpanMaxLength;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2100499563741744475L;

	public enum NodeType {
		Start, Span, End
	};

	public enum SubNodeType {
		Bpp, Bp0, Bpn, B0p, B00, B0n, Bnp, Bn0, Bnn, e, App, Ap0, Apn, A0p, A00, A0n, Anp, An0, Ann, NULL
	}

	public enum PolarityType {
		positive, neutral, negative
	}

	int PolarityTypeSize = PolarityType.values().length;

	int SubNodeTypeSize = SubNodeType.values().length;

	@Override
	public TSInstance decompile(Network network) {
		BaseNetwork lcrfNetwork = (BaseNetwork) network;
		TSInstance inst = (TSInstance) network.getInstance();
		int size = inst.size();
		//List<String[]> inputs = (List<String[]>) inst.getInput();

		ArrayList<int[]> preds = new ArrayList<int[]>();
		boolean NoVisibleTarget = false;

		ArrayList<int[]> target_pred = new ArrayList<int[]>();
		ArrayList<Label> predication_array = new ArrayList<Label>();
		
		PolarityType polar = null;
		ArrayList<int[]> scopes = new ArrayList<int[]>(); // for each entity
		
		
		

		if (TargetSentimentGlobal.ALLOW_NULL_TARGET) {

			for (int i = 0; i < inst.targets.size(); i++) {
				int[] target = ((int[]) inst.targets.get(i)).clone();
				target[2] = -1;
				target_pred.add(target);
			}
		}

		// int node_k = network.countNodes() - 1;

		long rootNode = toNode_start(size);
		int node_k = Arrays.binarySearch(lcrfNetwork.getAllNodes(), rootNode);
		// node_k = 0;
		while (true) {
			// System.out.print(node_k + " ");
			int[] parent_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(node_k));

			int[] childs = network.getMaxPath(node_k);

			int[] child_ids = null;
			child_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(childs[0]));
			/*
			try { child_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(childs[0])); } catch (Exception e) {
				System.out.println(inst.getSentence());
				System.out.println();
			}*/

			if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.NULLTarget != null) {
				if (parent_ids[4] == NodeType.Start.ordinal() && childs.length == 2) {
					int[] nullTarget_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(childs[1]));
					int nullTarget_polar = PolarityTypeSize - nullTarget_ids[1];
					inst.NULLTargetPred = PolarityType.values()[nullTarget_polar].name();

					for (int i = 0; i < target_pred.size(); i++) {
						int[] target = target_pred.get(i);
						if (target[0] == -1) {
							target[2] = nullTarget_polar;
						}
					}

					if (child_ids[4] == NodeType.End.ordinal()) {
						NoVisibleTarget = true;

						for (int pos = 0; pos < size; pos++) {
							preds.add(new int[] { pos, 0, SubNodeType.B00.ordinal() });
						}

						break;
					}

				}
			}

			if (child_ids[4] == NodeType.End.ordinal()) {
				break;
			}

			int child_pos = size - child_ids[0];
			int child_polar = PolarityTypeSize - child_ids[1];
			int child_subnode = SubNodeTypeSize - child_ids[2];
			int child_node_type = child_ids[4];

			if (child_node_type == NodeType.Span.ordinal()) {

				preds.add(new int[] { child_pos, child_polar, child_subnode });

			}

			node_k = childs[0];

		}


		scopes.add(new int[] { 0, -1 });
		int numEntity = 0;

		for (int i = 0; i < preds.size(); i++) {
			int[] ids = preds.get(i);
			int pos = ids[0];
			int polar_index = ids[1];
			int subnode_index = ids[2];

			// left node
			if (SubNodeType.values()[subnode_index].name().startsWith("B")) {

				int[] next_ids = (i + 1 < preds.size()) ? preds.get(i + 1) : new int[] { size, polar_index, -1 };
				int next_pos = next_ids[0];
				int next_polar_index = next_ids[1];
				int next_subnode_index = next_ids[2];

				// next node is before node
				if (next_subnode_index == -1) { //B=>X
					predication_array.add(new Label("O", 0));
				} else if (SubNodeType.values()[next_subnode_index].name().startsWith("B")) { //B=>B
					predication_array.add(new Label("O", 0));

				} else if (next_subnode_index == SubNodeType.e.ordinal()) {
					// entity_begin = pos;

					polar = PolarityType.values()[next_polar_index];

					predication_array.add(new Label("B-" + polar.name(), 1));
					
					numEntity++;
				} 

			} else if (subnode_index == SubNodeType.e.ordinal()) {
				int[] next_ids = preds.get(i + 1);
				int next_pos = next_ids[0];
				int next_polar_index = next_ids[1];
				int next_subnode_index = next_ids[2];

				if (next_subnode_index == SubNodeType.e.ordinal()) {
					predication_array.add(new Label("I-" + polar.name(), 2));
				} else if (SubNodeType.values()[next_subnode_index].name().startsWith("A")) {
					// nothing
					i++;
				}

			} else if (SubNodeType.values()[subnode_index].name().startsWith("A")) {
				
				int[] next_ids =  (i + 1 < preds.size()) ? preds.get(i + 1) : new int[] { size, polar_index, -1 };
				int next_pos = next_ids[0];
				int next_polar_index = next_ids[1];
				int next_subnode_index = next_ids[2];

				// from After to next After
				if (next_subnode_index == -1) { //A=>X
					predication_array.add(new Label("O", 0));
				} else if (SubNodeType.values()[next_subnode_index].name().startsWith("A")) {
					predication_array.add(new Label("O", 0));
				} else if (SubNodeType.values()[next_subnode_index].name().startsWith("B")) {
					predication_array.add(new Label("O", 0));
					
					if (numEntity > 0) {
						scopes.get(scopes.size() - 1)[1] = pos + 1;
						scopes.add(new int[] { pos + 1, -1 });
					} else {
						scopes.get(0)[0] = pos + 1;
					}

				}
				
			}

		}
		
		scopes.get(scopes.size() - 1)[1] = size;
		inst.setScopes(scopes);

		if (TargetSentimentGlobal.ALLOW_NULL_TARGET) {
			int entityBeginIdx = -1;
			int entityEndIdx = -1;
			int polarIdx = -1;

			for (int pos = 0; pos < predication_array.size(); pos++) {

				boolean start_entity = TargetSentimentGlobal.startOfEntity(pos, size, predication_array);
				boolean end_entity = TargetSentimentGlobal.endofEntity(pos, size, predication_array);

				if (start_entity) {
					entityBeginIdx = pos;
					polarIdx = PolarityType.valueOf(predication_array.get(pos).getForm().substring(2)).ordinal();
				}

				if (end_entity) {
					entityEndIdx = pos;

					for (int i = 0; i < target_pred.size(); i++) {
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



		inst.setTargetPrediction(target_pred);
		inst.setPrediction(predication_array);

		return inst;
	}

	public Network compileUnLabeledFixNE(int networkId, Instance instance, LocalNetworkParam param) {

		TSInstance inst = (TSInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		ArrayList<Label> outputs = (ArrayList<Label>) inst.getOutput();

		int size = inst.size();

		long start = this.toNode_start(size);
		lcrfNetwork.addNode(start);

		long[][][] node_array = new long[size][PolarityTypeSize][SubNodeTypeSize];

		// build node array
		for (int pos = 0; pos < size; pos++) {
			for (int polar = 0; polar < PolarityTypeSize; polar++) {
				for (int sub = 0; sub < SubNodeTypeSize; sub++) {
					long node = this.toNode_Span(size, pos, polar, sub);
					lcrfNetwork.addNode(node);
					node_array[pos][polar][sub] = node;
				}
			}
		}

		long end = this.toNode_end(inst.size());
		lcrfNetwork.addNode(end);

		long[] NULL = this.getNULLNodes(size);

		// //////////////////////////////////////////

		int last_entity_pos = -1;
		// PolarityType last_polar = null;
		// PolarityType polar = null;
		long from = -1;
		long to = -1;
		int entity_begin = -1;

		if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
			for (int i = 0; i < NULL.length; i++)
				lcrfNetwork.addNode(NULL[i]);
		}

		for (int pos = 0; pos < size; pos++) {
			String word = inputs.get(pos)[0];
			String label = outputs.get(pos).getForm();

			boolean start_entity = this.startOfEntity(pos, size, outputs);
			boolean end_entity = this.endofEntity(pos, size, outputs);

			if (start_entity) {

				PolarityType[] last_polar_set = null;
				if (last_entity_pos == -1) {
					last_polar_set = new PolarityType[] { PolarityType.neutral };
				} else {
					last_polar_set = PolarityType.values();
				}

				for (PolarityType polar : PolarityType.values()) {
					for (PolarityType last_polar : last_polar_set) {

						// polar = PolarityType.valueOf(label.substring(2));
						int transitionIdx = last_polar.ordinal() * PolarityTypeSize + polar.ordinal();
						int BIdx = SubNodeType.Bpp.ordinal() + transitionIdx;
						int AIdx = SubNodeType.App.ordinal() + transitionIdx;

						from = node_array[pos][polar.ordinal()][BIdx];
						to = node_array[pos][polar.ordinal()][SubNodeType.e.ordinal()];
						lcrfNetwork.addEdge(from, new long[] { to });

						for (int i = last_entity_pos + 1; i <= pos; i++) {
							if (i - 1 < 0) {
								from = start;
								if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
									PolarityType NullTargetPolar = PolarityType.valueOf(inst.NULLTarget);
									to = node_array[0][polar.ordinal()][BIdx];
									lcrfNetwork.addEdge(from, new long[] { to, NULL[NullTargetPolar.ordinal()] });

									if (pos > 0) { // if the entity appear at the beginning, we don't connect start => A
										to = node_array[0][last_polar.ordinal()][AIdx];
										lcrfNetwork.addEdge(from, new long[] { to, NULL[NullTargetPolar.ordinal()] });
									}

								} else {
									to = node_array[0][polar.ordinal()][BIdx];
									lcrfNetwork.addEdge(from, new long[] { to });

									if (pos > 0) { // if the entity appear at the beginning, we don't connect start => A
										to = node_array[0][last_polar.ordinal()][AIdx];
										lcrfNetwork.addEdge(from, new long[] { to });
									}
								}

							} else {

								// B=>B
								if (i > last_entity_pos + 1) {
									from = node_array[i - 1][polar.ordinal()][BIdx];
									to = node_array[i][polar.ordinal()][BIdx];
									lcrfNetwork.addEdge(from, new long[] { to });
								}

								// A=>A
								if (i < pos) {
									from = node_array[i - 1][last_polar.ordinal()][AIdx];
									to = node_array[i][last_polar.ordinal()][AIdx];
									lcrfNetwork.addEdge(from, new long[] { to });
								}

								// A=>B
								from = node_array[i - 1][last_polar.ordinal()][AIdx];
								to = node_array[i][polar.ordinal()][BIdx];
								lcrfNetwork.addEdge(from, new long[] { to });
							}

						}
					}
				}
				entity_begin = pos;
			}

			if (end_entity) {

				for (PolarityType polar : PolarityType.values()) {

					// add links between entity
					for (int i = entity_begin; i < pos; i++) {
						from = node_array[i][polar.ordinal()][SubNodeType.e.ordinal()];
						to = node_array[i + 1][polar.ordinal()][SubNodeType.e.ordinal()];
						lcrfNetwork.addEdge(from, new long[] { to });

					}

					PolarityType[] next_polar_set = new PolarityType[]{PolarityType.neutral};
					for (int i = pos + 1; i < size; i++) {
						if (this.startOfEntity(i, size, outputs)) {
							next_polar_set = PolarityType.values();
							break;
						}
					}
					
					for(PolarityType next_polar : next_polar_set) {
						int transitionIdx = polar.ordinal() * PolarityTypeSize + next_polar.ordinal();
						int BIdx = SubNodeType.Bpp.ordinal() + transitionIdx;
						int AIdx = SubNodeType.App.ordinal() + transitionIdx;
	
						// add link from entity to After
						from = node_array[pos][polar.ordinal()][SubNodeType.e.ordinal()];
						to = node_array[pos][polar.ordinal()][AIdx];
	
						lcrfNetwork.addEdge(from, new long[] { to });
					}

					last_entity_pos = pos;

					// last_polar = polar;
				}

			}

		}

		// add the last column node to end
		if (last_entity_pos >= 0) {

			for (PolarityType polar : PolarityType.values()) {
				PolarityType next_polar = PolarityType.neutral;
				int transitionIdx = polar.ordinal() * PolarityTypeSize + next_polar.ordinal();
				int BIdx = SubNodeType.Bpp.ordinal() + transitionIdx;
				int AIdx = SubNodeType.App.ordinal() + transitionIdx;

				for (int pos = last_entity_pos + 1; pos < size; pos++) {

					from = node_array[pos - 1][polar.ordinal()][AIdx];

					// A[pos - 1] => B[pos]
					to = node_array[pos][next_polar.ordinal()][BIdx];
					lcrfNetwork.addEdge(from, new long[] { to });

					// A[pos - 1] => A[pos]
					to = node_array[pos][polar.ordinal()][AIdx];
					lcrfNetwork.addEdge(from, new long[] { to });

					if (pos > last_entity_pos + 1) {
						// B[pos - 1] => B[pos]
						from = node_array[pos - 1][next_polar.ordinal()][BIdx];
						to = node_array[pos][next_polar.ordinal()][BIdx];
						lcrfNetwork.addEdge(from, new long[] { to });
					}

				}

				if (last_entity_pos < size - 1) {
					// B[size-1] = > end
					from = node_array[size - 1][next_polar.ordinal()][BIdx];
					to = end;
					lcrfNetwork.addEdge(from, new long[] { to });

					// A[size-1] = > end
					from = node_array[size - 1][polar.ordinal()][AIdx];
					to = end;
					lcrfNetwork.addEdge(from, new long[] { to });
				} else {

					// A[size-1] = > end
					from = node_array[size - 1][polar.ordinal()][AIdx];
					to = end;
					lcrfNetwork.addEdge(from, new long[] { to });

				}
			}
		} else {

			// System.out.println("No Entity found in this Instance, Discard!");
			// No visible Targets, only NULL Targets
			if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {

				for (PolarityType nullTargetPolar : PolarityType.values()) {
					lcrfNetwork.addEdge(start, new long[] { end, NULL[nullTargetPolar.ordinal()] });
				}
			}

		}

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);

		// System.err.println(lcrfNetwork.countNodes()+" nodes.");
		// System.exit(1);

		return network;

	}

	@Override
	public Network compileUnlabeled(int networkId, Instance instance, LocalNetworkParam param) {

		if (TargetSentimentGlobal.FIXNE)
			return this.compileUnLabeledFixNE(networkId, instance, param);

		TSInstance inst = (TSInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		ArrayList<Label> outputs = (ArrayList<Label>) inst.getOutput();

		// OutputToken[] outputs = inst.getOutput();

		int size = inst.size();

		long start = this.toNode_start(size);
		lcrfNetwork.addNode(start);

		long[][][] node_array = new long[size][PolarityTypeSize][SubNodeTypeSize];

		// build node array
		for (int pos = 0; pos < size; pos++) {
			for (int polar = 0; polar < PolarityTypeSize; polar++) {
				for (int sub = 0; sub < SubNodeTypeSize; sub++) {
					long node = this.toNode_Span(size, pos, polar, sub);
					lcrfNetwork.addNode(node);
					node_array[pos][polar][sub] = node;
				}
			}
		}

		long end = this.toNode_end(inst.size());
		lcrfNetwork.addNode(end);

		long[] NULL = this.getNULLNodes(size);

		long from = -1, to = -1;

		// add first column of span node from start
		for (int j = 0; j < PolarityTypeSize; j++) {
			from = start;

			int transitionIdx = PolarityType.neutral.ordinal() * PolarityTypeSize + j;
			int BIdx = SubNodeType.Bpp.ordinal() + transitionIdx;
			int AIdx = SubNodeType.App.ordinal() + transitionIdx;

			if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
				for (int i = 0; i < NULL.length; i++) {
					lcrfNetwork.addNode(NULL[i]);

					to = node_array[0][j][BIdx];
					lcrfNetwork.addEdge(from, new long[] { to, NULL[i] });

					to = node_array[0][j][AIdx];
					lcrfNetwork.addEdge(from, new long[] { to, NULL[i] });

				}
			} else {
				to = node_array[0][j][BIdx];
				lcrfNetwork.addEdge(from, new long[] { to });

				to = node_array[0][j][AIdx];
				lcrfNetwork.addEdge(from, new long[] { to });
			}
		}

		for (int pos = 0; pos < size; pos++) {
			for (int last_polar_Idx = 0; last_polar_Idx < PolarityTypeSize; last_polar_Idx++)
				for (int j = 0; j < PolarityTypeSize; j++) {

					int transitionIdx = last_polar_Idx * PolarityTypeSize + j;
					int BIdx = SubNodeType.Bpp.ordinal() + transitionIdx;
					int AIdx = SubNodeType.App.ordinal() + transitionIdx;

					// before to next before
					if (pos < size - 1) {
						from = node_array[pos][j][BIdx];
						to = node_array[pos + 1][j][BIdx];
						lcrfNetwork.addEdge(from, new long[] { to });
					}

					// before to current entity
					from = node_array[pos][j][BIdx];
					to = node_array[pos][j][SubNodeType.e.ordinal()];
					lcrfNetwork.addEdge(from, new long[] { to });

					// entity to after entity
					from = node_array[pos][j][SubNodeType.e.ordinal()];
					to = node_array[pos][j][AIdx];
					lcrfNetwork.addEdge(from, new long[] { to });

					// entity to next entity
					if (pos < size - 1) {
						from = node_array[pos][j][SubNodeType.e.ordinal()];
						to = node_array[pos + 1][j][SubNodeType.e.ordinal()];
						lcrfNetwork.addEdge(from, new long[] { to });
					}

					// after to next after
					if (pos < size - 1) {
						from = node_array[pos][j][AIdx];
						to = node_array[pos + 1][j][AIdx];
						lcrfNetwork.addEdge(from, new long[] { to });
					}

					// after to next before
					if (pos < size - 1) {

						from = node_array[pos][last_polar_Idx][AIdx];
						to = node_array[pos + 1][j][BIdx];
						lcrfNetwork.addEdge(from, new long[] { to });

					}

				}

		}

		// add last column of span node to end
		for (int j = 0; j < PolarityTypeSize; j++) {

			int transitionIdx = j * PolarityTypeSize + PolarityType.neutral.ordinal();
			int BIdx = SubNodeType.Bpp.ordinal() + transitionIdx;
			int AIdx = SubNodeType.App.ordinal() + transitionIdx;

			from = node_array[size - 1][j][AIdx];
			to = end;
			lcrfNetwork.addEdge(from, new long[] { to });
		}

		if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
			for (PolarityType nullTargetPolar : PolarityType.values())
				lcrfNetwork.addEdge(start, new long[] { end, NULL[nullTargetPolar.ordinal()] });
		}

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);

		// System.err.println(lcrfNetwork.countNodes()+" nodes.");
		// System.exit(1);

		return network;
	}

	@Override
	public Network compileLabeled(int networkId, Instance instance, LocalNetworkParam param) {

		TSInstance inst = (TSInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		ArrayList<Label> outputs = (ArrayList<Label>) inst.getOutput();

		int size = inst.size();

		long start = this.toNode_start(size);
		lcrfNetwork.addNode(start);

		long[][][] node_array = new long[size][PolarityTypeSize][SubNodeTypeSize];

		// build node array
		for (int pos = 0; pos < size; pos++) {
			for (int polar = 0; polar < PolarityTypeSize; polar++) {
				for (int sub = 0; sub < SubNodeTypeSize; sub++) {
					long node = this.toNode_Span(size, pos, polar, sub);
					lcrfNetwork.addNode(node);
					node_array[pos][polar][sub] = node;
				}
			}
		}

		long end = this.toNode_end(inst.size());
		lcrfNetwork.addNode(end);

		long[] NULL = this.getNULLNodes(size);
		if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
			for (int i = 0; i < NULL.length; i++)
				lcrfNetwork.addNode(NULL[i]);
		}

		// //////////////////////////////////////////

		int last_entity_pos = -1;
		PolarityType last_polar = PolarityType.neutral;
		PolarityType polar = null;
		long from = -1;
		long to = -1;
		int entity_begin = -1;

		for (int pos = 0; pos < size; pos++) {
			String word = inputs.get(pos)[0];
			String label = outputs.get(pos).getForm();

			boolean start_entity = this.startOfEntity(pos, size, outputs);
			boolean end_entity = this.endofEntity(pos, size, outputs);

			if (start_entity) {
				polar = PolarityType.valueOf(label.substring(2));

				int transitionIdx = last_polar.ordinal() * PolarityTypeSize + polar.ordinal();
				int BIdx = SubNodeType.Bpp.ordinal() + transitionIdx;
				int AIdx = SubNodeType.App.ordinal() + transitionIdx;

				from = node_array[pos][polar.ordinal()][SubNodeType.Bpp.ordinal() + transitionIdx];
				to = node_array[pos][polar.ordinal()][SubNodeType.e.ordinal()];
				lcrfNetwork.addEdge(from, new long[] { to });

				for (int i = last_entity_pos + 1; i <= pos; i++) {
					if (i - 1 < 0) {
						from = start;
						if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
							PolarityType NullTargetPolar = PolarityType.valueOf(inst.NULLTarget);
							to = node_array[0][polar.ordinal()][BIdx];
							lcrfNetwork.addEdge(from, new long[] { to, NULL[NullTargetPolar.ordinal()] });

							if (pos > 0) { // if the entity appear at the beginning, we don't connect start => A
								to = node_array[0][last_polar.ordinal()][AIdx];
								lcrfNetwork.addEdge(from, new long[] { to, NULL[NullTargetPolar.ordinal()] });
							}

						} else {
							to = node_array[0][polar.ordinal()][BIdx];
							lcrfNetwork.addEdge(from, new long[] { to });

							if (pos > 0) { // if the entity appear at the beginning, we don't connect start => A
								to = node_array[0][last_polar.ordinal()][AIdx];
								lcrfNetwork.addEdge(from, new long[] { to });
							}
						}

					} else {

						// B=>B
						if (i > last_entity_pos + 1) {
							from = node_array[i - 1][polar.ordinal()][BIdx];
							to = node_array[i][polar.ordinal()][BIdx];
							lcrfNetwork.addEdge(from, new long[] { to });
						}

						// A=>A
						if (i < pos) {
							from = node_array[i - 1][last_polar.ordinal()][AIdx];
							to = node_array[i][last_polar.ordinal()][AIdx];
							lcrfNetwork.addEdge(from, new long[] { to });
						}

						// A=>B
						from = node_array[i - 1][last_polar.ordinal()][AIdx];
						to = node_array[i][polar.ordinal()][BIdx];
						lcrfNetwork.addEdge(from, new long[] { to });
					}

				}

				entity_begin = pos;

			}

			if (end_entity) {

				// add links between entity
				for (int i = entity_begin; i < pos; i++) {
					from = node_array[i][polar.ordinal()][SubNodeType.e.ordinal()];
					to = node_array[i + 1][polar.ordinal()][SubNodeType.e.ordinal()];
					lcrfNetwork.addEdge(from, new long[] { to });

				}

				PolarityType next_polar = PolarityType.neutral;
				for (int i = pos + 1; i < size; i++) {
					if (this.startOfEntity(i, size, outputs)) {
						next_polar = PolarityType.valueOf(outputs.get(i).getForm().substring(2));
						break;
					}
				}

				int transitionIdx = polar.ordinal() * PolarityTypeSize + next_polar.ordinal();
				int BIdx = SubNodeType.Bpp.ordinal() + transitionIdx;
				int AIdx = SubNodeType.App.ordinal() + transitionIdx;

				// add link from entity to After
				from = node_array[pos][polar.ordinal()][SubNodeType.e.ordinal()];
				to = node_array[pos][polar.ordinal()][AIdx];

				lcrfNetwork.addEdge(from, new long[] { to });

				last_entity_pos = pos;
				last_polar = polar;

			}

		}

		// add the last column node to end
		if (last_entity_pos >= 0) {
			PolarityType next_polar = PolarityType.neutral;
			
			int transitionIdx = polar.ordinal() * PolarityTypeSize + next_polar.ordinal();
			int BIdx = SubNodeType.Bpp.ordinal() + transitionIdx;
			int AIdx = SubNodeType.App.ordinal() + transitionIdx;

			for (int pos = last_entity_pos + 1; pos < size; pos++) {

				from = node_array[pos - 1][polar.ordinal()][AIdx];

				// A[pos - 1] => B[pos]
				to = node_array[pos][next_polar.ordinal()][BIdx];
				lcrfNetwork.addEdge(from, new long[] { to });

				// A[pos - 1] => A[pos]
				to = node_array[pos][polar.ordinal()][AIdx];
				lcrfNetwork.addEdge(from, new long[] { to });

				if (pos > last_entity_pos + 1) {
					// B[pos - 1] => B[pos]
					from = node_array[pos - 1][next_polar.ordinal()][BIdx];
					to = node_array[pos][next_polar.ordinal()][BIdx];
					lcrfNetwork.addEdge(from, new long[] { to });
				}

			}

			if (last_entity_pos < size - 1) {
				// B[size-1] = > end
				from = node_array[size - 1][next_polar.ordinal()][BIdx];
				to = end;
				lcrfNetwork.addEdge(from, new long[] { to });

				// A[size-1] = > end
				from = node_array[size - 1][polar.ordinal()][AIdx];
				to = end;
				lcrfNetwork.addEdge(from, new long[] { to });
			} else {

				// A[size-1] = > end
				from = node_array[size - 1][polar.ordinal()][AIdx];
				to = end;
				lcrfNetwork.addEdge(from, new long[] { to });

			}
		} else {

			// System.out.println("No Entity found in this Instance, Discard!");
			// No visible Targets, only NULL Targets
			if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
				PolarityType nullTargetPolar = PolarityType.valueOf(inst.NULLTarget);
				lcrfNetwork.addEdge(start, new long[] { end, NULL[nullTargetPolar.ordinal()] });
			}

		}

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		return network;

	}

	boolean startOfEntity(int pos, int size, ArrayList<Label> outputs) {
		String label = outputs.get(pos).getForm();
		if (label.startsWith("B"))
			return true;

		if (pos == 0 && label.startsWith("I"))
			return true;

		if (pos > 0) {
			String prev_label = outputs.get(pos - 1).getForm();
			if (label.startsWith("I") && prev_label.startsWith("O"))
				return true;
		}

		return false;
	}

	boolean endofEntity(int pos, int size, ArrayList<Label> outputs) {
		String label = outputs.get(pos).getForm();
		if (!label.startsWith("O")) {
			if (pos == size - 1)
				return true;
			else {
				String next_label = outputs.get(pos + 1).getForm();
				if (next_label.startsWith("O") || next_label.startsWith("B"))
					return true;
			}
		}

		return false;
	}

	private long toNode_start(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size + 1, 0, 0, 0, NodeType.Start.ordinal() });
	}

	private long toNode_Span(int size, int bIndex, int polar, int subnode) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - bIndex, PolarityTypeSize - polar, SubNodeTypeSize - subnode, 0, NodeType.Span.ordinal() });
	}

	private long toNode_end(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 0, 0, 0, 0, NodeType.End.ordinal() });
	}

	private long[] getNULLNodes(int size) {
		long[] NULLNodes = new long[PolarityTypeSize];
		for (int i = 0; i < PolarityTypeSize; i++)
			NULLNodes[i] = this.toNode_NULL(size, i);
		return NULLNodes;
	}

	private long toNode_NULL(int size, int polar) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size, PolarityTypeSize - polar, SubNodeTypeSize - SubNodeType.NULL.ordinal(), 0, NodeType.Span.ordinal() });
	}

}
