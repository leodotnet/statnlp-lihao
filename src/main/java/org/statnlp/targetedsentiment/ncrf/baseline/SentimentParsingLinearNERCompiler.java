package org.statnlp.targetedsentiment.ncrf.baseline;

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
import org.statnlp.targetedsentiment.common.TSInstance.TSInstanceType;

public class SentimentParsingLinearNERCompiler extends NetworkCompiler {

	int NEMaxLength = 3;
	int SpanMaxLength = 10;
	boolean full_connected = true;

	public static BaseNetwork[] labelNetwork = new BaseNetwork[100];
	public static BaseNetwork[] unlabelNetwork = new BaseNetwork[100];

	/*
	 * TargetSentimentViewer viewer = new TargetSentimentViewer(this, null, 8);
	 * public static boolean visual = ;
	 */

	public SentimentParsingLinearNERCompiler() {
		super(null);
		setCapacity();
		// TODO Auto-generated constructor stub
	}

	public void setCapacity() {
		int MAX_SENTENCE_LENGTH = TargetSentimentGlobal.MAX_LENGTH_LENGTH;
		NetworkIDMapper.setCapacity(new int[] { MAX_SENTENCE_LENGTH, 4, NodeTypeSize + 1 });

	}

	private static final long serialVersionUID = 2100499563741744475L;

	public enum NodeType {
		X, O, I, B, Root
	};

	public enum PolarityType {
		positive, neutral, negative
	}

	int NodeTypeSize = NodeType.values().length;

	private long toNode_root(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size + 1, 0, NodeType.Root.ordinal() });
	}

	private long toNode_Span(int size, int pos, int nodetype) {
		// System.out.println("bIndex=" + bIndex);
		return NetworkIDMapper.toHybridNodeID(new int[] { size - pos, 0, nodetype });
	}

	/**/
	private long toNode_X(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 0, 0, NodeType.X.ordinal() });
	}

	@Override
	public Network compileLabeled(int networkId, Instance instance, LocalNetworkParam param) {
		TSInstance inst = (TSInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();
		int size = inst.size();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		ArrayList<Label> outputs = (ArrayList<Label>) inst.getOutput();

		long root = this.toNode_root(size);
		lcrfNetwork.addNode(root);

		long[][] node_array = new long[NodeTypeSize][size];

		// build node array
		for (int pos = 0; pos < size; pos++) {
			for (int subtype = NodeType.X.ordinal() + 1; subtype < NodeType.Root.ordinal(); subtype++) {
				long node = this.toNode_Span(size, pos, subtype);
				lcrfNetwork.addNode(node);
				node_array[subtype][pos] = node;

			}
		}

		long X = this.toNode_X(inst.size());
		lcrfNetwork.addNode(X);

		// //////////////////////////////////////////

		int last_entity_pos = -1;
		PolarityType last_polar = null;
		PolarityType sent = null;
		long from = -1;
		long to = -1;
		int entity_begin = -1;

		long last_one = root;

		for (int pos = 0; pos < size; pos++) {
			String word = inputs.get(pos)[0];
			String label = outputs.get(pos).getForm();

			NodeType labelType = NodeType.valueOf(label.substring(0, 1));

			from = last_one;
			to = node_array[labelType.ordinal()][pos];
			lcrfNetwork.addEdge(from, new long[] { to });

			last_one = to;
		}

		// add the last column node to end
		if (last_one != root) {
			from = last_one;
			to = X;
			lcrfNetwork.addEdge(from, new long[] { to });
		} else {

			System.out.println("No Entity found in this Instance, Discard!");

		}

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);

		return network;
	}

	@Override
	public Network compileUnlabeled(int networkId, Instance instance, LocalNetworkParam param) {
		TSInstance inst = (TSInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		// ArrayList<Label> outputs = (ArrayList<Label>)inst.getOutput();

		int size = inst.size();

		long root = this.toNode_root(size);
		lcrfNetwork.addNode(root);

		long[][] node_array = new long[NodeTypeSize][size];

		// build node array
		for (int pos = 0; pos < size; pos++) {
			for (int subtype = NodeType.X.ordinal() + 1; subtype < NodeType.Root.ordinal(); subtype++) {
				long node = this.toNode_Span(size, pos, subtype);
				lcrfNetwork.addNode(node);
				node_array[subtype][pos] = node;

			}
		}

		long X = this.toNode_X(inst.size());
		lcrfNetwork.addNode(X);

		long from = -1, to = -1;

		// Start to B & O
		for (int i = NodeType.X.ordinal() + 1; i < NodeType.Root.ordinal(); i++) {
			from = root;
			to = node_array[i][0];
			lcrfNetwork.addEdge(from, new long[] { to });
		}

		// sentence
		for (int pos = 0; pos < size - 1; pos++) {
			for (int i = NodeType.X.ordinal() + 1; i < NodeType.Root.ordinal(); i++) {
				for (int j = NodeType.X.ordinal() + 1; j < NodeType.Root.ordinal(); j++) {
					from = node_array[i][pos];
					to = node_array[j][pos + 1];
					lcrfNetwork.addEdge(from, new long[] { to });
				}
			}

		}

		// add last column of span node to end
		for (int i = NodeType.X.ordinal() + 1; i < NodeType.Root.ordinal(); i++) {
			from = node_array[i][size - 1];
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
		ArrayList<String> outputs = (ArrayList<String>) inst.getOutput();
		ArrayList<Label> prediction = new ArrayList<Label>();
		ArrayList<int[]> scopes = new ArrayList<int[]>();

		int size = inst.size();

		long rootNode = toNode_root(size);
		int root = Arrays.binarySearch(lcrfNetwork.getAllNodes(), rootNode);

		int[] root_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(root));

		int node_k = root;

		while (true) {
			node_k = network.getMaxPath(node_k)[0];

			int[] ids = NetworkIDMapper.toHybridNodeArray(network.getNode(node_k));

			int pos = size - ids[0];
			int subnode = ids[1];
			int node_type = ids[2];

			if (node_type == NodeType.X.ordinal()) {
				break;
			} else {

				String labelStr = NodeType.values()[node_type].name();

				if (node_type == NodeType.O.ordinal()) {

				} else {
					labelStr += "-neutral";
					
					if (node_type == NodeType.B.ordinal()) {
						scopes.add(new int[]{pos, pos + 1});
					}
					
				}

				prediction.add(new Label(labelStr, 0));

			}

		}

		inst.setPrediction(prediction);
		inst.setScopes(scopes);
		return inst;

	}

}
