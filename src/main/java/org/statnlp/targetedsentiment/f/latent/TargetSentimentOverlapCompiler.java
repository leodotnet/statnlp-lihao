package org.statnlp.targetedsentiment.f.latent;

import java.util.ArrayList;
import java.util.Arrays;
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


public class TargetSentimentOverlapCompiler extends NetworkCompiler {

	int NEMaxLength = 3;
	int SpanMaxLength = 10;
	boolean visual = true;

	// TargetSentimentViewer viewer = new TargetSentimentViewer(this, null, 5);
	// void visualize(LinearNetwork network, String title, int networkId)
	// {
	// viewer.visualizeNetwork(network, null, title + "[" + networkId + "]");
	// }

	public TargetSentimentOverlapCompiler() {
		super(null);
		// TODO Auto-generated constructor stub
	}

	public TargetSentimentOverlapCompiler(int NEMaxLength, int SpanMaxLength) {
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
		BS, BD, e, AS, AD, NULL
	}

	public enum PolarityType {
		positive, neutral,negative
	}

	TargetSentimentOverlapFeatureManager fm;

	int PolarityTypeSize = PolarityType.values().length;

	int SubNodeTypeSize = SubNodeType.values().length;

	public void setFeatureManager(TargetSentimentOverlapFeatureManager fm) {
		this.fm = fm;
	}

	@Override
	public TSInstance decompile(Network network) {
		BaseNetwork lcrfNetwork = (BaseNetwork) network;
		TSInstance inst = (TSInstance) network.getInstance();
		int size = inst.size();
		List<String[]> inputs = (List<String[]>) inst.getInput();

		ArrayList<int[]> preds = new ArrayList<int[]>();
		ArrayList<int[]> preds_refine = new ArrayList<int[]>();
		
		
		ArrayList<int[]> target_pred = new ArrayList<int[]>();
		
		if (TargetSentimentGlobal.ALLOW_NULL_TARGET) {
		
			for(int i = 0; i < inst.targets.size(); i++) {
				int[] target = ((int[])inst.targets.get(i)).clone();
				target[2] = -1;
				target_pred.add(target);
			}
		}

		//int node_k = network.countNodes() - 1;
		
		long rootNode = toNode_start(size);
		int node_k = Arrays.binarySearch(lcrfNetwork.getAllNodes(), rootNode);
		// node_k = 0;
		while (true) {
			// System.out.print(node_k + " ");
			int[] childs =  network.getMaxPath(node_k);
			node_k = childs[0];
			
			if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.NULLTarget != null) {
				if (childs.length == 2) {
					int[] nullTarget_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(childs[1]));
					int nullTarget_polar = nullTarget_ids[2];
					inst.NULLTargetPred = PolarityType.values()[nullTarget_polar].name();
					
					for(int i = 0; i < target_pred.size(); i++) {
						int[] target = target_pred.get(i);
						if (target[0] == -1) {
							target[2] = nullTarget_polar;
						}
					}

				}
			}
			

			int[] ids = NetworkIDMapper.toHybridNodeArray(network.getNode(node_k));
			// System.out.println("ids:" + Arrays.toString(ids));

			if (ids[4] == NodeType.End.ordinal()) {
				break;
			}

			int pos = size - ids[0];
			int polar = PolarityTypeSize - ids[1];
			int subnode = SubNodeTypeSize - ids[2];
			int node_type = ids[4];

			if (ids[4] == NodeType.Span.ordinal()) {

				preds.add(new int[] { pos, polar, subnode });

			}

		}

		ArrayList<Label> predication_array = new ArrayList<Label>();
		PolarityType polar = null;
		int entity_begin = -1;
		ArrayList<int[]> scopes = new ArrayList<int[]>(); //for each entity
		scopes.add(new int[]{0, -1});

		for (int i = 0; i < preds.size(); i++) {
			int[] ids = preds.get(i);
			int pos = ids[0];
			int polar_index = ids[1];
			int subnode_index = ids[2];

			// System.out.println(pos + "," +
			// SubNodeType.values()[subnode_index].name()
			// + "," + PolarityType.values()[polar_index].name());

			// left node
			if (SubNodeType.values()[subnode_index].name().startsWith("B")) {

				int[] next_ids  = (i + 1 < preds.size()) ? preds.get(i + 1) : new int[]{size, polar_index, subnode_index};
				int next_pos = next_ids[0];
				int next_polar_index = next_ids[1];
				int next_subnode_index = next_ids[2];

				// next node is before node
				if (SubNodeType.values()[next_subnode_index].name().startsWith("B")) {
					predication_array.add(new Label("O", 0));

				} else if (next_subnode_index == SubNodeType.e.ordinal()) {
					// entity_begin = pos;

					polar = PolarityType.values()[next_polar_index];

					predication_array.add(new Label("B-" + polar.name(), 1));
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
				}

			} else if (SubNodeType.values()[subnode_index].name().startsWith("A")) {
				if (pos < size - 1) {
					int[] next_ids = preds.get(i + 1);
					int next_pos = next_ids[0];
					int next_polar_index = next_ids[1];
					int next_subnode_index = next_ids[2];

					// from After to next After
					if (SubNodeType.values()[next_subnode_index].name().startsWith("A")) {
						predication_array.add(new Label("O", 0));
					} else if (SubNodeType.values()[next_subnode_index].name().startsWith("B")) {
						
							scopes.get(scopes.size() - 1)[1] = pos + 1;
							scopes.add(new int[]{pos + 1, -1});
						
					}
				}
			}

		}
		
		
		if (TargetSentimentGlobal.ALLOW_NULL_TARGET){
			int entityBeginIdx = -1;
			int entityEndIdx = -1;
			int polarIdx = -1;
		
			for(int pos = 0; pos < predication_array.size(); pos++) {
				
				boolean start_entity = TargetSentimentGlobal.startOfEntity(pos, size, predication_array);
				boolean end_entity = TargetSentimentGlobal.endofEntity(pos, size, predication_array);
				
				if (start_entity) {
					entityBeginIdx = pos;
					polarIdx = PolarityType.valueOf(predication_array.get(pos).getForm().substring(2)).ordinal();
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


		
		scopes.get(scopes.size() - 1)[1] = size;
		inst.setScopes(scopes);
		


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
			for(int i = 0; i < NULL.length; i++)
				lcrfNetwork.addNode(NULL[i]);
		}

		for (int pos = 0; pos < size; pos++) {
			String word = inputs.get(pos)[0];
			String label = outputs.get(pos).getForm();

			boolean start_entity = this.startOfEntity(pos, size, outputs);
			boolean end_entity = this.endofEntity(pos, size, outputs);

			if (start_entity) {

				for (PolarityType polar : PolarityType.values()) {

					for (PolarityType last_polar : PolarityType.values()) {

						// polar = PolarityType.valueOf(label.substring(2));

						if (polar == last_polar || last_polar == null) {
							from = node_array[pos][polar.ordinal()][SubNodeType.BS.ordinal()];
						} else {
							from = node_array[pos][polar.ordinal()][SubNodeType.BD.ordinal()];
						}
						to = node_array[pos][polar.ordinal()][SubNodeType.e.ordinal()];
						lcrfNetwork.addEdge(from, new long[] { to });

						if (last_entity_pos == -1) {
							from = start;
							to = node_array[0][polar.ordinal()][SubNodeType.BS.ordinal()];
							
							if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
								for(int i = 0; i < NULL.length; i++) {
									lcrfNetwork.addEdge(start, new long[] { to, NULL[i] });
								}
							} else {
							
								lcrfNetwork.addEdge(start, new long[] { to });
							}

							// / directly from left to right
							for (int i = 0; i < pos; i++) {
								// from before node[pos] to before node at
								// [pos+1]
								from = node_array[i][polar.ordinal()][SubNodeType.BS.ordinal()];
								to = node_array[i + 1][polar.ordinal()][SubNodeType.BS.ordinal()];
								
								lcrfNetwork.addEdge(from, new long[] { to });

							}

						} else {

							// latent path
							for (int i = last_entity_pos + 1; i < pos; i++) {
								if (last_polar == polar) {
									// add AS->AS
									from = node_array[i - 1][last_polar.ordinal()][SubNodeType.AS.ordinal()];
									to = node_array[i][last_polar.ordinal()][SubNodeType.AS.ordinal()];
									lcrfNetwork.addEdge(from, new long[] { to });
								} else {
									// add AD->AD
									from = node_array[i - 1][last_polar.ordinal()][SubNodeType.AD.ordinal()];
									to = node_array[i][last_polar.ordinal()][SubNodeType.AD.ordinal()];
									lcrfNetwork.addEdge(from, new long[] { to });
								}

								if (last_polar == polar) {
									// add BS->BS
									from = node_array[i][polar.ordinal()][SubNodeType.BS.ordinal()];
									to = node_array[i + 1][polar.ordinal()][SubNodeType.BS.ordinal()];
									lcrfNetwork.addEdge(from, new long[] { to });
								} else {
									// add BD->BD
									from = node_array[i][polar.ordinal()][SubNodeType.BD.ordinal()];
									to = node_array[i + 1][polar.ordinal()][SubNodeType.BD.ordinal()];
									lcrfNetwork.addEdge(from, new long[] { to });
								}

							}

							for (int i = last_entity_pos; i < pos; i++) {
								// add AS/AD->BS/BD
								if (last_polar == polar) {
									from = node_array[i][last_polar.ordinal()][SubNodeType.AS.ordinal()];
									to = node_array[i + 1][polar.ordinal()][SubNodeType.BS.ordinal()];
								} else {
									from = node_array[i][last_polar.ordinal()][SubNodeType.AD.ordinal()];
									to = node_array[i + 1][polar.ordinal()][SubNodeType.BD.ordinal()];
								}
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

					boolean nextSame = true;
					for (int i = pos + 1; i < size; i++) {
						if (this.startOfEntity(i, size, outputs)) {
							nextSame = PolarityType.valueOf(outputs.get(i).getForm().substring(2)) == polar;
							break;
						}
					}

					// add link from entity to After
					from = node_array[pos][polar.ordinal()][SubNodeType.e.ordinal()];
					if (nextSame) {
						to = node_array[pos][polar.ordinal()][SubNodeType.AS.ordinal()];
					} else {
						to = node_array[pos][polar.ordinal()][SubNodeType.AD.ordinal()];
					}
					lcrfNetwork.addEdge(from, new long[] { to });

					last_entity_pos = pos;
					// last_polar = polar;
				}

			}

		}

		// add the last column node to end
		// if (polar != null)
		// {
		if (last_entity_pos != -1) {
			for (PolarityType polar : PolarityType.values()) {
				for (int pos = last_entity_pos + 1; pos < size; pos++) {
					from = node_array[pos - 1][polar.ordinal()][SubNodeType.AS.ordinal()];
					to = node_array[pos][polar.ordinal()][SubNodeType.AS.ordinal()];
					lcrfNetwork.addEdge(from, new long[] { to });
				}

				from = node_array[size - 1][polar.ordinal()][SubNodeType.AS.ordinal()];
				to = end;
				lcrfNetwork.addEdge(from, new long[] { to });
			}
		} else {
			
			if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
				for (PolarityType polar : PolarityType.values()) {
					lcrfNetwork.addEdge(start, new long[] {node_array[0][polar.ordinal()][SubNodeType.BS.ordinal()], NULL[polar.ordinal()] });
					
					for (int pos = 1; pos < size; pos++) {
						from = node_array[pos - 1][polar.ordinal()][SubNodeType.BS.ordinal()];
						to = node_array[pos][polar.ordinal()][SubNodeType.BS.ordinal()];
						lcrfNetwork.addEdge(from, new long[] { to });
					}

					from = node_array[size - 1][polar.ordinal()][SubNodeType.BS.ordinal()];
					to = end;
					lcrfNetwork.addEdge(from, new long[] { to });
					
				}
			} 
				
			
			
			
		}
		// } else {
		//System.out.println("No Entity found in this Instance, Discard!");
		// }

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
			to = node_array[0][j][SubNodeType.BS.ordinal()];
			
			if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
				for(int i = 0; i < NULL.length; i++) {
					lcrfNetwork.addNode(NULL[i]);
					lcrfNetwork.addEdge(from, new long[] { to, NULL[i] });
				}
			} else {	
				lcrfNetwork.addEdge(from, new long[] { to });
			}
		}

		for (int pos = 0; pos < size; pos++) {
			for (int j = 0; j < PolarityTypeSize; j++) {
				// before to next before
				if (pos < size - 1) {
					from = node_array[pos][j][SubNodeType.BS.ordinal()];
					to = node_array[pos + 1][j][SubNodeType.BS.ordinal()];
					lcrfNetwork.addEdge(from, new long[] { to });
				}

				// before to next before
				if (pos < size - 1 && pos > 0) {
					from = node_array[pos][j][SubNodeType.BD.ordinal()];
					to = node_array[pos + 1][j][SubNodeType.BD.ordinal()];
					lcrfNetwork.addEdge(from, new long[] { to });
				}

				// before to current entity
				from = node_array[pos][j][SubNodeType.BS.ordinal()];
				to = node_array[pos][j][SubNodeType.e.ordinal()];
				lcrfNetwork.addEdge(from, new long[] { to });

				// before to current entity
				from = node_array[pos][j][SubNodeType.BD.ordinal()];
				to = node_array[pos][j][SubNodeType.e.ordinal()];
				lcrfNetwork.addEdge(from, new long[] { to });

				// entity to after (same)
				from = node_array[pos][j][SubNodeType.e.ordinal()];
				to = node_array[pos][j][SubNodeType.AS.ordinal()];
				lcrfNetwork.addEdge(from, new long[] { to });

				// entity to after (diff)
				if (pos < size - 1) {
					from = node_array[pos][j][SubNodeType.e.ordinal()];
					to = node_array[pos][j][SubNodeType.AD.ordinal()];
					lcrfNetwork.addEdge(from, new long[] { to });
				}

				// entity to next entity
				if (pos < size - 1) {
					from = node_array[pos][j][SubNodeType.e.ordinal()];
					to = node_array[pos + 1][j][SubNodeType.e.ordinal()];
					lcrfNetwork.addEdge(from, new long[] { to });
				}

				// after to next after
				if (pos < size - 1) {
					from = node_array[pos][j][SubNodeType.AS.ordinal()];
					to = node_array[pos + 1][j][SubNodeType.AS.ordinal()];
					lcrfNetwork.addEdge(from, new long[] { to });
					if (pos < size - 2) {
						from = node_array[pos][j][SubNodeType.AD.ordinal()];
						to = node_array[pos + 1][j][SubNodeType.AD.ordinal()];
						lcrfNetwork.addEdge(from, new long[] { to });
					}
				}

				// after to next before
				if (pos < size - 1) {

					for (int k = 0; k < PolarityTypeSize; k++) {
						if (j == k) {
							from = node_array[pos][j][SubNodeType.AS.ordinal()];
							to = node_array[pos + 1][k][SubNodeType.BS.ordinal()];
						} else {
							from = node_array[pos][j][SubNodeType.AD.ordinal()];
							to = node_array[pos + 1][k][SubNodeType.BD.ordinal()];
						}
						lcrfNetwork.addEdge(from, new long[] { to });

					}
				}

			}

		}

		// add last column of span node to end
		for (int j = 0; j < PolarityTypeSize; j++) {
			from = node_array[size - 1][j][SubNodeType.AS.ordinal()];
			to = end;
			lcrfNetwork.addEdge(from, new long[] { to });
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
			for(int i = 0; i < NULL.length; i++)
				lcrfNetwork.addNode(NULL[i]);
		}
		

		// //////////////////////////////////////////

		int last_entity_pos = -1;
		PolarityType last_polar = null;
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

				if (polar == last_polar || last_polar == null) {
					from = node_array[pos][polar.ordinal()][SubNodeType.BS.ordinal()];
				} else {
					from = node_array[pos][polar.ordinal()][SubNodeType.BD.ordinal()];
				}
				to = node_array[pos][polar.ordinal()][SubNodeType.e.ordinal()];
				lcrfNetwork.addEdge(from, new long[] { to });

				if (last_entity_pos == -1) {
					from = start;
					to = node_array[0][polar.ordinal()][SubNodeType.BS.ordinal()];
					
					if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
						
						PolarityType NullTargetPolar = PolarityType.valueOf(inst.NULLTarget);
						lcrfNetwork.addEdge(start, new long[] {to, NULL[NullTargetPolar.ordinal()] });
						
					} else {
					
						lcrfNetwork.addEdge(from, new long[] { to });
					}

					// / directly from left to right
					for (int i = 0; i < pos; i++) {
						// from before node[pos] to before node at [pos+1]
						from = node_array[i][polar.ordinal()][SubNodeType.BS.ordinal()];
						to = node_array[i + 1][polar.ordinal()][SubNodeType.BS.ordinal()];
						lcrfNetwork.addEdge(from, new long[] { to });

					}

				} else {

					// latent path
					for (int i = last_entity_pos + 1; i < pos; i++) {
						if (last_polar == polar) {
							// add AS->AS
							from = node_array[i - 1][last_polar.ordinal()][SubNodeType.AS.ordinal()];
							to = node_array[i][last_polar.ordinal()][SubNodeType.AS.ordinal()];
							lcrfNetwork.addEdge(from, new long[] { to });
						} else {
							// add AD->AD
							from = node_array[i - 1][last_polar.ordinal()][SubNodeType.AD.ordinal()];
							to = node_array[i][last_polar.ordinal()][SubNodeType.AD.ordinal()];
							lcrfNetwork.addEdge(from, new long[] { to });
						}

						if (last_polar == polar) {
							// add BS->BS
							from = node_array[i][polar.ordinal()][SubNodeType.BS.ordinal()];
							to = node_array[i + 1][polar.ordinal()][SubNodeType.BS.ordinal()];
							lcrfNetwork.addEdge(from, new long[] { to });
						} else {
							// add BD->BD
							from = node_array[i][polar.ordinal()][SubNodeType.BD.ordinal()];
							to = node_array[i + 1][polar.ordinal()][SubNodeType.BD.ordinal()];
							lcrfNetwork.addEdge(from, new long[] { to });
						}

					}

					for (int i = last_entity_pos; i < pos; i++) {
						// add AS/AD->BS/BD
						if (last_polar == polar) {
							from = node_array[i][last_polar.ordinal()][SubNodeType.AS.ordinal()];
							to = node_array[i + 1][polar.ordinal()][SubNodeType.BS.ordinal()];
						} else {
							from = node_array[i][last_polar.ordinal()][SubNodeType.AD.ordinal()];
							to = node_array[i + 1][polar.ordinal()][SubNodeType.BD.ordinal()];
						}
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

				boolean nextSame = true;
				for (int i = pos + 1; i < size; i++) {
					if (this.startOfEntity(i, size, outputs)) {
						nextSame = PolarityType.valueOf(outputs.get(i).getForm().substring(2)) == polar;
						break;
					}
				}

				// add link from entity to After
				from = node_array[pos][polar.ordinal()][SubNodeType.e.ordinal()];
				if (nextSame) {
					to = node_array[pos][polar.ordinal()][SubNodeType.AS.ordinal()];
				} else {
					to = node_array[pos][polar.ordinal()][SubNodeType.AD.ordinal()];
				}
				lcrfNetwork.addEdge(from, new long[] { to });

				last_entity_pos = pos;
				last_polar = polar;

			}

		}

		// add the last column node to end
		if (polar != null) {
			for (int pos = last_entity_pos + 1; pos < size; pos++) {
				from = node_array[pos - 1][polar.ordinal()][SubNodeType.AS.ordinal()];
				to = node_array[pos][polar.ordinal()][SubNodeType.AS.ordinal()];
				lcrfNetwork.addEdge(from, new long[] { to });
			}

			from = node_array[size - 1][polar.ordinal()][SubNodeType.AS.ordinal()];
			to = end;
			lcrfNetwork.addEdge(from, new long[] { to });
		} else {
	

			//System.out.println("No Entity found in this Instance, Discard!");
			if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
				PolarityType polar1 = PolarityType.valueOf(inst.NULLTarget);
				
					lcrfNetwork.addEdge(start, new long[] {node_array[0][polar1.ordinal()][SubNodeType.BS.ordinal()] , NULL[polar1.ordinal()]});
					
					for (int pos = 1; pos < size; pos++) {
						from = node_array[pos - 1][polar1.ordinal()][SubNodeType.BS.ordinal()];
						to = node_array[pos][polar1.ordinal()][SubNodeType.BS.ordinal()];
						lcrfNetwork.addEdge(from, new long[] { to });
					}

					from = node_array[size - 1][polar1.ordinal()][SubNodeType.BS.ordinal()];
					to = end;
					lcrfNetwork.addEdge(from, new long[] { to });
					
				
			} 
		

		}

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);

		// System.err.println(lcrfNetwork.countNodes()+" nodes.");
		// System.exit(1);

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
		for(int i = 0; i < PolarityTypeSize; i++)
			NULLNodes[i] = this.toNode_NULL(size, i);
		return NULLNodes;
	}
	
	private long toNode_NULL(int size, int polar) {
		return NetworkIDMapper.toHybridNodeID(new int[] {size, PolarityTypeSize - polar, SubNodeTypeSize - SubNodeType.NULL.ordinal(), 0, NodeType.Span.ordinal() });
	}


}
