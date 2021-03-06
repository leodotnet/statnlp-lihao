package org.statnlp.targetedsentiment.ncrf.baseline;

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
import org.statnlp.targetedsentiment.ncrf.semi.SentimentParsingSemiCompiler.NodeType;
import org.statnlp.targetedsentiment.ncrf.semi.SentimentParsingSemiCompiler.PolarityType;

/*******
 * 
 * @author Li Hao
 * @assumption: Only neighbor entities may have overlapping scopes
 *
 */

public class SentimentParsingSemiNERCompiler extends NetworkCompiler {

	public SentimentParsingSemiNERCompiler() {
		super(null);
		setCapacity();
	}

	public void setCapacity() {
		int MAX_SENTENCE_LENGTH = TargetSentimentGlobal.MAX_LENGTH_LENGTH;
		NetworkIDMapper.setCapacity(new int[] { MAX_SENTENCE_LENGTH, PolarityTypeSize + 1,NodeTypeSize + 1 });

	}

	private static final long serialVersionUID = 2100499563741744475L;

	public enum NodeType {
		X, O, B, Root
	};

	public enum PolarityType {
		positive, neutral, negative
	}

	int NodeTypeSize = NodeType.values().length;

	int PolarityTypeSize = PolarityType.values().length;

	protected long toNode_root(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size + 1, 0,  NodeType.Root.ordinal() });
	}

	protected long toNode_X() {
		return NetworkIDMapper.toHybridNodeID(new int[] { 0, 0, 0 });
	}

	// pos: position of word in the sentence starting from 0 to size - 1
	protected long toNode_Span(int size, int pos, int node_type) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - pos, 0, node_type });
	}

	/*
	 * private long toNode_B(int size, int pos) { return
	 * NetworkIDMapper.toHybridNodeID(new int[] { size - pos,
	 * NodeType.B.ordinal(), 0 }); }
	 * 
	 * private long toNode_E(int size, int pos) { return
	 * NetworkIDMapper.toHybridNodeID(new int[] { size - pos,
	 * NodeType.E.ordinal(), 0 }); }
	 * 
	 * private long toNode_A(int size, int pos, int type) { return
	 * NetworkIDMapper.toHybridNodeID(new int[] { size - pos,
	 * NodeType.A.ordinal(), type }); }
	 * 
	 * private long toNode_B0(int size, int pos, int type) { return
	 * NetworkIDMapper.toHybridNodeID(new int[] { size - pos,
	 * NodeType.B0.ordinal(), type }); }
	 * 
	 * private long toNode_A0(int size, int pos, int type) { return
	 * NetworkIDMapper.toHybridNodeID(new int[] { size - pos,
	 * NodeType.A0.ordinal(), type }); }
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

		long[][] node_arr = new long[NodeTypeSize][size];

		/****** build node array ******/
		long node = -1;

		for (int node_type = NodeType.X.ordinal() + 1; node_type < NodeType.Root.ordinal(); node_type++) {

			for (int pos = 0; pos < size; pos++) {
				node = this.toNode_Span(size, pos, node_type);
				node_arr[node_type][pos] = node;
				lcrfNetwork.addNode(node);
			}

		}

		long X = this.toNode_X();
		lcrfNetwork.addNode(X);
		/****** build node array ******/

		long[] B = node_arr[NodeType.B.ordinal()];
		long[] O = node_arr[NodeType.O.ordinal()];

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
					to = B[pos];
					lcrfNetwork.addEdge(from, new long[] { to });
					from = to;
				}

				entity_begin = pos;

			}

			if (end_entity) {

				String nextTag = (pos + 1 >= size) ? "X" : outputs.get(pos + 1).getForm();

				if (nextTag.startsWith("O")) {
					to = O[pos + 1];
				} else if (nextTag.startsWith("X")) {
					to = X;
				} else if (nextTag.startsWith("B")) {
					PolarityType nextPolar = PolarityType.valueOf(nextTag.substring(2));
					to = B[pos + 1];
				}

				lcrfNetwork.addEdge(from, new long[] { to });

				from = to;
				last_entity_pos = pos;
				last_polar = polar;

			}

		}

		// add the last column node to end
		if (polar != null) {

			if (from != X) {

				for (int i = last_entity_pos + 2; i < size; i++) {
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

		// labelNetworks[networkId] = network;
		// visualize(network, "Sentiment Model:labeled", networkId);
		return network;
	}

	@Override
	public Network compileUnlabeled(int networkId, Instance instance, LocalNetworkParam param) {

		TSInstance inst = (TSInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		ArrayList<String[]> inputs = (ArrayList<String[]>) inst.getInput();
		// ArrayList<String> outputs = (ArrayList<String>) inst.getOutput();

		int size = inst.size();

		long root = this.toNode_root(size);
		lcrfNetwork.addNode(root);

		long[][] node_arr = new long[NodeTypeSize][size];

		/****** build node array ******/
		long node = -1;

		for (int node_type = NodeType.X.ordinal() + 1; node_type < NodeType.Root.ordinal(); node_type++) {

			for (int pos = 0; pos < size; pos++) {
				node = this.toNode_Span(size, pos, node_type);
				node_arr[node_type][pos] = node;
				lcrfNetwork.addNode(node);
			}

		}

		long X = this.toNode_X();
		lcrfNetwork.addNode(X);
		/****** build node array ******/

		long[] B = node_arr[NodeType.B.ordinal()];
		long[] O = node_arr[NodeType.O.ordinal()];

		// //////////////////////////////////////////

		long from = -1, to = -1, to1 = -1;

		// add first column of span node from start
		from = root;
		to = B[0];
		lcrfNetwork.addEdge(from, new long[] { to });

		to = O[0];
		lcrfNetwork.addEdge(from, new long[] { to });

		int LNER = TargetSentimentGlobal.NER_SPAN_MAX;
		int LO = TargetSentimentGlobal.O_SPAN_MAX;

		for (int pos = 0; pos < size; pos++) {

			// B

			from = B[pos];

			for (int L = 1; L <= LNER; L++) {

				if (pos + L < size) {
					// to B+
					to = B[pos + L];
					lcrfNetwork.addEdge(from, new long[] { to });

					// to B0
					to = B[pos + L];
					lcrfNetwork.addEdge(from, new long[] { to });

					// to B-
					to = B[pos + L];
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

			// O
			from = O[pos];
			if (pos + 1 < size) {
				to = O[pos + 1];
				lcrfNetwork.addEdge(from, new long[] { to });

				to = B[pos + 1];
				lcrfNetwork.addEdge(from, new long[] { to });

				// to B0
				to = B[pos + 1];
				lcrfNetwork.addEdge(from, new long[] { to });

				// to B-
				to = B[pos + 1];
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

		while (true) {

			child = network.getMaxPath(parent);

			int[] parent_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(parent));
			int[] child_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(child[0]));
			// System.out.println("ids:" + Arrays.toString(ids));

			int parent_pos = 2 * size - parent_ids[0];
			int parent_node_type = parent_ids[2];
			int parent_polar = parent_ids[1];

			int child_pos = 2 * size - child_ids[0];
			int child_node_type = child_ids[2];
			int child_polar = child_ids[1];

			if (child_node_type == NodeType.X.ordinal()) {
				child_pos = size;
			}

			if (parent_node_type == NodeType.O.ordinal() && (child_node_type == NodeType.B.ordinal() || child_node_type == NodeType.O.ordinal() || child_node_type == NodeType.X.ordinal())) {

				for (int i = parent_pos; i < child_pos; i++) {
					prediction.add(new Label("O", 0));
				}
			}

			else if (parent_node_type == NodeType.B.ordinal() && (child_node_type == NodeType.B.ordinal() || child_node_type == NodeType.O.ordinal() || child_node_type == NodeType.X.ordinal())) {

				String polar = PolarityType.values()[parent_polar].name();

				prediction.add(new Label("B-neutral", 0));
				for (int i = parent_pos + 1; i < child_pos; i++) {
					prediction.add(new Label("I-neutral", 0));
				}

			}

			if (child_node_type == NodeType.X.ordinal()) {
				break;
			}

			parent = child[0];

		}

		/*
		 * String[] pred_arr = new String[prediction.size()]; for(int k = 0; k <
		 * prediction.size(); k++) { pred_arr[k] = prediction.get(k).getForm();
		 * }
		 */

		inst.setPrediction(prediction);
		inst.setScopes(scopes);
		return inst;
	}

}
