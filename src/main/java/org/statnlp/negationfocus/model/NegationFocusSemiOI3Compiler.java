package org.statnlp.negationfocus.model;

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
import org.statnlp.negationfocus.common.NegationCompiler;
import org.statnlp.negationfocus.common.NegationGlobal;
import org.statnlp.negationfocus.common.NegationInstance;
import org.statnlp.negationfocus.common.SemanticRole;
import org.statnlp.negationfocus.common.Utils;

public class NegationFocusSemiOI3Compiler extends NegationCompiler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2899070269878131792L;

	public NegationFocusSemiOI3Compiler() {
		super();
	}

	@Override
	public NegationInstance decompile(Network network) {
		BaseNetwork lcrfNetwork = (BaseNetwork) network;
		NegationInstance inst = (NegationInstance) network.getInstance();
		int size = inst.size();
		// List<String[]> inputs = (List<String[]>) inst.getInput();

		ArrayList<Label> predication_array = new ArrayList<Label>();

		long rootNode = toNode_Root(size);
		int node_k = Arrays.binarySearch(lcrfNetwork.getAllNodes(), rootNode);

		int last_pos = -1;
		int last_tag_id = -1;
		int last_node_type = -1;

		// node_k = 0;
		while (true) {
			// System.out.print(node_k + " ");
			int[] parent_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(node_k));
			int parent_pos = size - parent_ids[0];
			int parent_tag_id = parent_ids[1];
			int parent_node_type = parent_ids[2];

			if (parent_node_type == NodeType.X.ordinal()) {
				if (last_pos >= 0) {
					Label label = this._labelsMap.get(last_tag_id);
					for (int i = last_pos; i < parent_pos; i++)
						predication_array.add(new Label(label));
				}
				break;
			}

			int[] childs = network.getMaxPath(node_k);

			int[] child_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(childs[0]));
			;

			if (parent_node_type == NodeType.Root.ordinal()) {

			} else if (parent_node_type == NodeType.Node.ordinal()) {

				if (last_pos >= 0) {
					Label label = this._labelsMap.get(last_tag_id);
					for (int i = last_pos; i < parent_pos; i++)
						predication_array.add(new Label(label));
				}

			}

			last_pos = parent_pos;
			last_tag_id = parent_tag_id;
			last_node_type = parent_node_type;

			node_k = childs[0];

		}

		inst.setPrediction(predication_array);

		return inst;
	}

	@Override
	public Network compileUnlabeled(int networkId, Instance instance, LocalNetworkParam param) {
		
		if (NegationGlobal.SEMI_UNLABEL_PRUNING) {
			return this.compileUnlabeledPruning(networkId, instance, param);
		}

		NegationInstance inst = (NegationInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		ArrayList<Label> outputs = this.convert2Output(inst);

		int size = inst.size();

		long start = this.toNode_Root(size);
		lcrfNetwork.addNode(start);

		long[][] node_array = new long[size][this._labels.length];

		// build node array
		for (int pos = 0; pos < size; pos++) {

			for (int tag_id = 0; tag_id < this._labels.length; tag_id++) {
				long node = this.toNode_Node(size, pos, tag_id);
				lcrfNetwork.addNode(node);
				node_array[pos][tag_id] = node;
			}

		}

		long X = this.toNode_X(inst.size());
		lcrfNetwork.addNode(X);

		/////////
		long from = start;
		for (int tag_id = 0; tag_id < this._labels.length; tag_id++) {
			long to = node_array[0][tag_id];
			lcrfNetwork.addEdge(from, new long[] { to });
		}

		for (int pos = 0; pos < size; pos++)
			for (int tag_id = 0; tag_id < this._labels.length; tag_id++)
				for (int L = 1; L <= NegationGlobal.MAX_SPAN_LENGTH; L++)
					if (pos + L >= size) {
						from = node_array[pos][tag_id];
						long to = X;
						lcrfNetwork.addEdge(from, new long[] { to });
						break;
					} else {
						for (int next_tag_id = 0; next_tag_id < this._labels.length; next_tag_id++) {
							from = node_array[pos][tag_id];
							long to = node_array[pos + L][next_tag_id];
							lcrfNetwork.addEdge(from, new long[] { to });
						}
					}

		/*
		 * for (int last_tag_id = 0; last_tag_id < this._labels.length;
		 * last_tag_id++) { from = node_array[size - 1][last_tag_id];
		 * lcrfNetwork.addEdge(from, new long[]{X}); }
		 */

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		return network;
	}

	public Network compileUnlabeledPruning(int networkId, Instance instance, LocalNetworkParam param) {

		NegationInstance inst = (NegationInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		ArrayList<Label> outputs = this.convert2Output(inst);

		int size = inst.size();

		long start = this.toNode_Root(size);
		lcrfNetwork.addNode(start);

		long[][] node_array = new long[size][this._labels.length];

		// build node array
		for (int pos = 0; pos < size; pos++) {

			for (int tag_id = 0; tag_id < this._labels.length; tag_id++) {
				long node = this.toNode_Node(size, pos, tag_id);
				lcrfNetwork.addNode(node);
				node_array[pos][tag_id] = node;
			}

		}

		long X = this.toNode_X(inst.size());
		lcrfNetwork.addNode(X);

		SemanticRole sr = inst.sr;

		int[] tag_id_set = new int[] { 0, 1, 2 };
		int[] next_tag_id_set = new int[] { 0, 1, 2 };

		int[] idx = (int[]) inst.outputPosition.get(0);
		int startIdx = idx[0];
		int endIdx = idx[1];
		int roleIdx = sr.roletype[startIdx];
		if (roleIdx < 0)
			next_tag_id_set = new int[] { 0 };
		else
			next_tag_id_set = new int[] { 1, 2 };
		/////////
		long from = start;
		for (int next_tag_id : next_tag_id_set) {
			long to = node_array[0][next_tag_id];
			lcrfNetwork.addEdge(from, new long[] { to });
		}

		// tag_id_set = next_tag_id_set;

		for (int i = 0; i < inst.outputSemi.size(); i++) {

			idx = (int[]) inst.outputPosition.get(i);
			startIdx = idx[0];
			endIdx = idx[1];
			int currRoleIdx = sr.roletype[startIdx];
			int nextRoleIdx = endIdx < size ? sr.roletype[endIdx] : -2;

			if (currRoleIdx < 0) {
				tag_id_set = new int[] { 0 };
			} else {
				tag_id_set = new int[] { 1, 2 };
			}

			if (nextRoleIdx < 0) {
				next_tag_id_set = new int[] { 0 };
				if (nextRoleIdx == -2)
					next_tag_id_set = new int[] {};
			} else {
				next_tag_id_set = new int[] { 1, 2 };
			}

			for (int tag_id : tag_id_set) {
				if (nextRoleIdx == -2) {
					from = node_array[startIdx][tag_id];
					long to = X;
					lcrfNetwork.addEdge(from, new long[] { to });
				} else {

					for (int next_tag_id : next_tag_id_set) {
						from = node_array[startIdx][tag_id];
						long to = node_array[endIdx][next_tag_id];
						lcrfNetwork.addEdge(from, new long[] { to });
					}
				}
			}

		}

		/*
		 * for (int last_tag_id = 0; last_tag_id < this._labels.length;
		 * last_tag_id++) { from = node_array[size - 1][last_tag_id];
		 * lcrfNetwork.addEdge(from, new long[]{X}); }
		 */

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		return network;
	}

	@Override
	public Network compileLabeled(int networkId, Instance instance, LocalNetworkParam param) {

		NegationInstance inst = (NegationInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		ArrayList<Label> outputs = this.convert2Output(inst);

		int size = inst.size();

		long start = this.toNode_Root(size);
		lcrfNetwork.addNode(start);

		long[][] node_array = new long[size][this._labels.length];

		// build node array
		for (int pos = 0; pos < size; pos++) {

			for (int tag_id = 0; tag_id < this._labels.length; tag_id++) {
				long node = this.toNode_Node(size, pos, tag_id);
				lcrfNetwork.addNode(node);
				node_array[pos][tag_id] = node;
			}

		}

		long X = this.toNode_X(size);
		lcrfNetwork.addNode(X);

		/////////
		long from = start;

		for (int i = 0; i < inst.outputSemi.size(); i++) {

			int tag_id = ((Label) (inst.outputSemi).get(i)).getId();
			int[] idx = (int[]) inst.outputPosition.get(i);
			int startIdx = idx[0];
			int endIdx = idx[1];

			long to = node_array[startIdx][tag_id];
			lcrfNetwork.addEdge(from, new long[] { to });
			from = to;

			if (endIdx >= size)
				break;
		}

		lcrfNetwork.addEdge(from, new long[] { X });

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		return network;

	}

	public Label[] getLabels() {
		String[] labelForms = new String[] { "O", "OR", "IR" };

		Label[] labels = new Label[labelForms.length];
		for (int i = 0; i < labels.length; i++)
			labels[i] = new Label(labelForms[i], i);

		return labels;
	}

	public ArrayList<Label> convert2Output(NegationInstance inst) {

		inst.outputPosition = new ArrayList<int[]>();
		inst.outputSemi = new ArrayList<Label>();

		int startIdx = 0;
		SemanticRole sr = inst.sr;

		while (true) {

			int endIdx = Utils.getSeqEndPos(startIdx, sr.roletype, sr.roletypeBegin); // [startIdx,
																	// endIdx)
			int roleIdx = sr.roletype[startIdx];

			if (roleIdx == -1) {
				for (int i = startIdx; i < endIdx; i++) {
					inst.outputPosition.add(new int[] { i, i + 1 });
					inst.outputSemi.add(this._labelsMap.get(0));
				}
			} else {
				inst.outputPosition.add(new int[] { startIdx, endIdx });
				inst.outputSemi.add(this._labelsMap.get(inst.focusScope[startIdx] + 1));

			}

			if (endIdx >= inst.size())
				break;

			startIdx = endIdx;
		}

		ArrayList<Label> outputs = new ArrayList<Label>();
		for (int i = 0; i < inst.size(); i++) {
			int tag_id = inst.focusScope[i];
			if (tag_id == 1)
				tag_id = 2;
			outputs.add(this._labelsMap.get(tag_id));
		}

		inst.setOutput(outputs);

		return outputs;
	}

}
