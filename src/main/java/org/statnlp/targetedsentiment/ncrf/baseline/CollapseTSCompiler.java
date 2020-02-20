package org.statnlp.targetedsentiment.ncrf.baseline;

import java.util.ArrayList;
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
import org.statnlp.targetedsentiment.common.TSInstance.TSInstanceType;

public class CollapseTSCompiler extends NetworkCompiler {

	int NEMaxLength = 3;
	int SpanMaxLength = 10;
	boolean full_connected = true;

	public static BaseNetwork[] labelNetwork = new BaseNetwork[100];
	public static BaseNetwork[] unlabelNetwork = new BaseNetwork[100];
	/*
	 * TargetSentimentViewer viewer = new TargetSentimentViewer(this, null, 8);
	 * public static boolean visual = ;
	 */

	public CollapseTSCompiler() {
		super(null);
		// TODO Auto-generated constructor stub
	}

	public CollapseTSCompiler(int NEMaxLength, int SpanMaxLength) {
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
		O, B_positive, B_negative, B_neutral, B_unknown, I_positive, I_negative, I_neutral, I_unknown
	}

	// public enum SentType {positive, negative, neutral}

	int SubNodeTypeSize = SubNodeType.values().length;

	
	private long toNode_start(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size + 1, 0, 0, 0, NodeType.Start.ordinal() });
	}

	// private long toNode_hiddenState(int size, int bIndex, OutputToken
	// hiddenState){
	// //System.out.println("bIndex=" + bIndex);
	// return NetworkIDMapper.toHybridNodeID(new int[]{size - bIndex, 2, 0,
	// hiddenState.getId(), nodeType.HiddenState.ordinal()});
	// }

	// private long toNode_Entity(int size, int bIndex, int row, EntityNodeType
	// type){
	// System.out.println("bIndex=" + bIndex);
	// return NetworkIDMapper.toHybridNodeID(new int[]{size - bIndex,
	// NodeTypeSize - row, 0, 0, nodeType.Entity.ordinal()});
	// }

	private long toNode_Span(int size, int bIndex, int subnode) {
		// System.out.println("bIndex=" + bIndex);
		return NetworkIDMapper.toHybridNodeID(new int[] { size - bIndex,  subnode, 0, 0, NodeType.Span.ordinal() });
	}

	// private long toNode_observation(int size, int bIndex, InputToken
	// observation){
	// return NetworkIDMapper.toHybridNodeID(new int[]{size - bIndex, 1, 0,
	// observation.getId(), nodeType.Observation.ordinal()});
	// }
	//
	//

	/**/
	private long toNode_end(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 0, 0, 0, 0, NodeType.End.ordinal() });
	}
	
	
	
	


	@Override
	public Network compileLabeled(int networkId, Instance instance, LocalNetworkParam param) {
		TSInstance inst = (TSInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();
		int size = inst.size();
		
		if (inst.type == TSInstanceType.Sentence_level)
		{
			long start = this.toNode_start(size);
			lcrfNetwork.addNode(start);
			
			long end = this.toNode_end(inst.size());
			lcrfNetwork.addNode(end);

			return lcrfNetwork.build(networkId, inst, param, this);
		}

		List<String[]> inputs = (List<String[]>) inst.getInput();
		ArrayList<Label> outputs = (ArrayList<Label>) inst.getOutput();

		

		long start = this.toNode_start(size);
		lcrfNetwork.addNode(start);

		long[][] node_array = new long[size][SubNodeTypeSize];

		// build node array
		for (int pos = 0; pos < size; pos++) {
			for (int subtype = 0; subtype < SubNodeTypeSize; subtype++) {
				long node = this.toNode_Span(size, pos, subtype);
				lcrfNetwork.addNode(node);
				node_array[pos][subtype] = node;

			}
		}

		long end = this.toNode_end(inst.size());
		lcrfNetwork.addNode(end);

		// //////////////////////////////////////////

		int last_entity_pos = -1;
		SubNodeType last_polar = null;
		SubNodeType sent = null;
		long from = -1;
		long to = -1;
		int entity_begin = -1;

		long last_one = start;

		for (int pos = 0; pos < size; pos++) {
			String word = inputs.get(pos)[0];
			String label = outputs.get(pos).getForm();
			
			/*
			if (inst.useAdditionalData && !label.equals("O"))
			{
				String suffix = label.substring(2);
				if (TargetSentimentGlobal.NETHashSet.contains(suffix))
					label = label.charAt(0) + "-unknown";
				else
					label = "O";
			}*/

			label = label.replace('-', '_');
			sent = SubNodeType.valueOf(label);

			from = last_one;
			to = node_array[pos][sent.ordinal()];
			lcrfNetwork.addEdge(from, new long[] { to });

			last_one = to;
		}

		// add the last column node to end
		if (sent != null) {
			from = last_one;
			to = end;
			lcrfNetwork.addEdge(from, new long[] { to });
		} else {
			// polar = SentType._;
			// from = start;
			// to = node_array[0][polar.ordinal()][SubNodeType.B.ordinal()];
			// network.addEdge(from, new long[]{to});

			System.out.println("No Entity found in this Instance, Discard!");
			/*
			 * for(int pos = 0; pos < size; pos++) {
			 * 
			 * }
			 */

		}

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		/*
		 * if (visual) viewer.visualizeNetwork(network, null,
		 * "Sentiment Model:labeled[" + networkId + "]");
		 */
		// System.err.println(network.countNodes()+" nodes.");
		// System.exit(1);
		/*
		int instid = inst.getInstanceId();
		if (instid < 0) instid = -instid;
		labelNetwork[instid] = network;
		
		if (unlabelNetwork[instid] != null)
		{
		boolean contain = unlabelNetwork[instid].contains(labelNetwork[instid]);
		System.out.println("===================");
		System.out.println("contain=" + contain);
		System.out.println("===================");
		}*/
		
		return network;
	}

	@Override
	public Network compileUnlabeled(int networkId, Instance instance, LocalNetworkParam param) {
		TSInstance inst = (TSInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		// ArrayList<Label> outputs = (ArrayList<Label>)inst.getOutput();

		int size = inst.size();
		
		if (inst.type == TSInstanceType.Sentence_level)
		{
			long start = this.toNode_start(size);
			lcrfNetwork.addNode(start);
			
			long end = this.toNode_end(inst.size());
			lcrfNetwork.addNode(end);

			return lcrfNetwork.build(networkId, inst, param, this);
		}
		
		
		if (TargetSentimentGlobal.FIXNE)
			return this.compileUnlabeledFixNE(networkId, instance, param);
		

		long start = this.toNode_start(size);
		lcrfNetwork.addNode(start);

		long[][] node_array = new long[size][SubNodeTypeSize];

		// build node array
		for (int pos = 0; pos < size; pos++) {
			for (int subtype = 0; subtype < SubNodeTypeSize; subtype++) {

				{
					long node = this.toNode_Span(size, pos, subtype);
					lcrfNetwork.addNode(node);
					node_array[pos][subtype] = node;
				}
			}
		}

		long end = this.toNode_end(inst.size());
		lcrfNetwork.addNode(end);

		long from = -1, to = -1;
		int BSize = SubNodeType.I_positive.ordinal() - 1;

		if (full_connected) {
			// Start to B & O
			for (int i = 0; i < SubNodeTypeSize; i++) {
				from = start;
				to = node_array[0][i];
				lcrfNetwork.addEdge(from, new long[] { to });
			}

			// sentence
			for (int pos = 0; pos < size - 1; pos++) {
				for (int i = 0; i < SubNodeTypeSize; i++) {
					for (int j = 0; j < SubNodeTypeSize; j++) {
						from = node_array[pos][i];
						to = node_array[pos + 1][j];
						lcrfNetwork.addEdge(from, new long[] { to });
					}
				}

			}

			// add last column of span node to end
			for (int i = 0; i < SubNodeTypeSize; i++) {
				from = node_array[size - 1][i];
				to = end;
				lcrfNetwork.addEdge(from, new long[] { to });

			}

			BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
			
			/*
			int instid = inst.getInstanceId();
			if (instid < 0) instid = -instid;
			
			unlabelNetwork[instid] = network;
			
			if (labelNetwork[instid] != null)
			{
			boolean contain = network.contains(labelNetwork[instid]);
			System.out.println("===================");
			System.out.println("contain=" + contain);
			System.out.println("===================");
			}*/
			return network;

		}

		// Start to B & O
		for (int i = 0; i <= BSize; i++) {
			from = start;
			to = node_array[0][i];
			lcrfNetwork.addEdge(from, new long[] { to });
		}

		// sentence
		for (int pos = 0; pos < size - 1; pos++) {
			// current O
			for (int j = 0; j <= BSize; j++) {
				from = node_array[pos][0];
				to = node_array[pos + 1][j];
				lcrfNetwork.addEdge(from, new long[] { to });
			}

			// current B
			for (int i = 1; i <= BSize; i++) {
				// next is O & B
				for (int j = 0; j <= BSize; j++) {
					from = node_array[pos][i];
					to = node_array[pos + 1][j];
					lcrfNetwork.addEdge(from, new long[] { to });

				}

				// next is I
				from = node_array[pos][i];
				to = node_array[pos + 1][i + BSize];
				lcrfNetwork.addEdge(from, new long[] { to });

			}

			// current I
			for (int i = BSize + 1; i < SubNodeTypeSize; i++) {
				// next is O or B
				for (int j = 0; j <= BSize; j++) {
					from = node_array[pos][i];
					to = node_array[pos + 1][j];
					lcrfNetwork.addEdge(from, new long[] { to });

				}

				// next is I
				from = node_array[pos][i];
				to = node_array[pos + 1][i];
				lcrfNetwork.addEdge(from, new long[] { to });

			}

		}

		// add last column of span node to end
		for (int i = 0; i < SubNodeTypeSize; i++) {
			from = node_array[size - 1][i];
			to = end;
			lcrfNetwork.addEdge(from, new long[] { to });

		}

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		/*
		 * if (visual) viewer.visualizeNetwork(network, null,
		 * "Sentiment Model:unlabeled[" + networkId + "]");
		 */

		// System.err.println(network.countNodes()+" nodes.");
		// System.exit(1);

		return network;
	}
	
	
	public Network compileUnlabeledFixNE(int networkId, Instance instance, LocalNetworkParam param) {
		TSInstance inst = (TSInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		ArrayList<Label> outputs = (ArrayList<Label>)inst.getOutput();

		int size = inst.size();
		
		if (inst.type == TSInstanceType.Sentence_level)
		{
			long start = this.toNode_start(size);
			lcrfNetwork.addNode(start);
			
			long end = this.toNode_end(inst.size());
			lcrfNetwork.addNode(end);

			return lcrfNetwork.build(networkId, inst, param, this);
		}

		long start = this.toNode_start(size);
		lcrfNetwork.addNode(start);

		long[][] node_array = new long[size][SubNodeTypeSize];

		// build node array
		for (int pos = 0; pos < size; pos++) {
			for (int subtype = 0; subtype < SubNodeTypeSize; subtype++) {

				{
					long node = this.toNode_Span(size, pos, subtype);
					lcrfNetwork.addNode(node);
					node_array[pos][subtype] = node;
				}
			}
		}

		long end = this.toNode_end(inst.size());
		lcrfNetwork.addNode(end);

		long from = -1, to = -1;
		int BSize = SubNodeType.I_positive.ordinal() - 1;

		if (full_connected) {
			// Start to B & O
			if (outputs.get(0).getForm().equals("O")) {
				from = start;
				to = node_array[0][0];
				lcrfNetwork.addEdge(from, new long[] { to });
			} else {
				for (int i = 1; i <= SubNodeType.B_unknown.ordinal(); i++) {
					from = start;
					to = node_array[0][i];
					lcrfNetwork.addEdge(from, new long[] { to });
				}
			}

			// sentence
			for (int pos = 0; pos < size - 1; pos++) {
				
				char current = outputs.get(pos).getForm().charAt(0);
				char next = outputs.get(pos + 1).getForm().charAt(0);
				
				
				if (current == 'O')
				{
					if (next == 'O') {
						from = node_array[pos][0];
						to = node_array[pos + 1][0];
						lcrfNetwork.addEdge(from, new long[] { to });
					} else { // next == 'B'
						for (int j = 1; j <= SubNodeType.B_unknown.ordinal(); j++) {
							from = node_array[pos][0];
							to = node_array[pos + 1][j];
							lcrfNetwork.addEdge(from, new long[] { to });
						}
						
					}
					
				}
				else if (current == 'B')
				{
					if (next == 'O') {
						for (int i = 1; i <= SubNodeType.B_unknown.ordinal(); i++) {
							from = node_array[pos][i];
							to = node_array[pos + 1][0];
							lcrfNetwork.addEdge(from, new long[] { to });
						
						}
					}
					else if (next == 'I') {
						for (int i = 1; i <= SubNodeType.B_unknown.ordinal(); i++) {
							from = node_array[pos][i];
							to = node_array[pos + 1][i + 4];
							lcrfNetwork.addEdge(from, new long[] { to });
						}
						
					} else { // next == 'B'
						for (int i = 1; i <= SubNodeType.B_unknown.ordinal(); i++) {
							for (int j = 1; j <= SubNodeType.B_unknown.ordinal(); j++) {
								from = node_array[pos][i];
								to = node_array[pos + 1][j];
								lcrfNetwork.addEdge(from, new long[] { to });
							}
						}
						
					}
					
				}
				else //current == 'I'
				{
					if (next == 'O') {
						for (int i = 1; i <= SubNodeType.B_unknown.ordinal(); i++) {
							from = node_array[pos][i + 4];
							to = node_array[pos + 1][0];
							lcrfNetwork.addEdge(from, new long[] { to });
						}
					} else if (next == 'I') {
						for (int i = 1; i <= SubNodeType.B_unknown.ordinal(); i++) {
							from = node_array[pos][i + 4];
							to = node_array[pos + 1][i + 4];
							lcrfNetwork.addEdge(from, new long[] { to });
						}
					} else { //next == 'B'
						for (int i = 1; i <= SubNodeType.B_unknown.ordinal(); i++) {
							for (int j = 1; j <= SubNodeType.B_unknown.ordinal(); j++) {
								from = node_array[pos][i + 4];
								to = node_array[pos + 1][j];
								lcrfNetwork.addEdge(from, new long[] { to });
							}
						}
					}
					
				}
				
				
			}

			// add last column of span node to end
			char last = outputs.get(size - 1).getForm().charAt(0);
			
			if (last == 'O') {
				from = node_array[size - 1][0];
				to = end;
				lcrfNetwork.addEdge(from, new long[] { to });
			} else if (last == 'B') {
				for (int i = 1; i <= SubNodeType.B_unknown.ordinal(); i++) {
					from = node_array[size - 1][i];
					to = end;
					lcrfNetwork.addEdge(from, new long[] { to });
				}
			} else { // last == 'I'
				for (int i = 1; i <= SubNodeType.B_unknown.ordinal(); i++) {
					from = node_array[size - 1][i + 4];
					to = end;
					lcrfNetwork.addEdge(from, new long[] { to });
				}
			}
			
			

			BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
			
		
			return network;

		}

		// Start to B & O
		for (int i = 0; i <= BSize; i++) {
			from = start;
			to = node_array[0][i];
			lcrfNetwork.addEdge(from, new long[] { to });
		}

		// sentence
		for (int pos = 0; pos < size - 1; pos++) {
			// current O
			for (int j = 0; j <= BSize; j++) {
				from = node_array[pos][0];
				to = node_array[pos + 1][j];
				lcrfNetwork.addEdge(from, new long[] { to });
			}

			// current B
			for (int i = 1; i <= BSize; i++) {
				// next is O & B
				for (int j = 0; j <= BSize; j++) {
					from = node_array[pos][i];
					to = node_array[pos + 1][j];
					lcrfNetwork.addEdge(from, new long[] { to });

				}

				// next is I
				from = node_array[pos][i];
				to = node_array[pos + 1][i + BSize];
				lcrfNetwork.addEdge(from, new long[] { to });

			}

			// current I
			for (int i = BSize + 1; i < SubNodeTypeSize; i++) {
				// next is O or B
				for (int j = 0; j <= BSize; j++) {
					from = node_array[pos][i];
					to = node_array[pos + 1][j];
					lcrfNetwork.addEdge(from, new long[] { to });

				}

				// next is I
				from = node_array[pos][i];
				to = node_array[pos + 1][i];
				lcrfNetwork.addEdge(from, new long[] { to });

			}

		}

		// add last column of span node to end
		for (int i = 0; i < SubNodeTypeSize; i++) {
			from = node_array[size - 1][i];
			to = end;
			lcrfNetwork.addEdge(from, new long[] { to });

		}

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		/*
		 * if (visual) viewer.visualizeNetwork(network, null,
		 * "Sentiment Model:unlabeled[" + networkId + "]");
		 */

		// System.err.println(network.countNodes()+" nodes.");
		// System.exit(1);

		return network;
	}


	@Override
	public Instance decompile(Network network) {
		TSInstance inst = (TSInstance) network.getInstance();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		ArrayList<String> outputs = (ArrayList<String>) inst.getOutput();

		int size = inst.size();
		

		ArrayList<int[]> preds = new ArrayList<int[]>();
		ArrayList<int[]> preds_refine = new ArrayList<int[]>();

		int node_k = network.countNodes() - 1;
		// node_k = 0;
		while (true) {
			// System.out.print(node_k + " ");
			node_k = network.getMaxPath(node_k)[0];

			int[] ids = NetworkIDMapper.toHybridNodeArray(network.getNode(node_k));
			// System.out.println("ids:" + Arrays.toString(ids));

			if (ids[4] == NodeType.End.ordinal()) {
				break;
			}

			int pos = size - ids[0];
			int subnode = ids[1];
			int node_type = ids[4];

			if (ids[4] == NodeType.Span.ordinal()) {

				preds.add(new int[] { pos, subnode });

			}

		}

		ArrayList<String> predication_array = new ArrayList<String>();
		String subnode = "";

		for (int i = 0; i < preds.size(); i++) {
			int[] ids = preds.get(i);
			int pos = ids[0];
			int subnode_index = ids[1];

			subnode = SubNodeType.values()[subnode_index].name();
			subnode = subnode.replace('_', '-');

			predication_array.add(new String(subnode));

		}

		// System.out.println();

		String[] prediction = new String[size];

		// System.out.println("\n~~\n");
		for (int k = 0; k < prediction.length; k++) {
			prediction[k] = predication_array.get(k);
			// System.out.print(prediction[k].getName() + " ");
		}
		// System.out.println();

		inst.setPrediction(prediction);

		return inst;

	}

}
