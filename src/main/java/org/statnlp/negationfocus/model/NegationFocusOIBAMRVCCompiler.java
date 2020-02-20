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
import org.statnlp.negationfocus.common.NegationInstance;
import org.statnlp.negationfocus.common.SemanticRole;
import org.statnlp.negationfocus.common.Utils;

public class NegationFocusOIBAMRVCCompiler extends NegationCompiler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2899070269878131792L;

	public NegationFocusOIBAMRVCCompiler() {
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
		// node_k = 0;
		while (true) {
			// System.out.print(node_k + " ");
			int[] parent_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(node_k));
			int parent_pos = size - parent_ids[0];
			int parent_tag_id = parent_ids[1];
			int parent_node_type = parent_ids[2];

			if (parent_node_type == NodeType.X.ordinal()) {
				break;
			}

			int[] childs = network.getMaxPath(node_k);

			int[] child_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(childs[0]));
			;

			if (parent_node_type == NodeType.Root.ordinal()) {

			} else if (parent_node_type == NodeType.Node.ordinal()) {
				Label label = this._labelsMap.get(parent_tag_id);
				predication_array.add(new Label(label));

			}

			node_k = childs[0];

		}

		inst.setPrediction(predication_array);

		return inst;
	}

	@Override
	public Network compileUnlabeled(int networkId, Instance instance, LocalNetworkParam param) {

		NegationInstance inst = (NegationInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		ArrayList<Label> outputs = this.convert2Output(inst);
		inst.setOutput(outputs);

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

		for (int pos = 1; pos < size; pos++) {
			for (int last_tag_id = 0; last_tag_id < this._labels.length; last_tag_id++) {
				for (int tag_id = 0; tag_id < this._labels.length; tag_id++) {
					
				
					if (last_tag_id == 1) {
						if (tag_id == 0 || tag_id >= 6) continue;
					}
					
					if (last_tag_id >= 6) {
						if (tag_id == 0) continue;
					}
					
					if (last_tag_id == 7) {
						if (tag_id == 6 || tag_id == 8) continue;
					}
					
					
					from = node_array[pos - 1][last_tag_id];
					long to = node_array[pos][tag_id];
					lcrfNetwork.addEdge(from, new long[] { to });
				}
			}
		}

		for (int last_tag_id = 0; last_tag_id < this._labels.length; last_tag_id++) {
			from = node_array[size - 1][last_tag_id];
			lcrfNetwork.addEdge(from, new long[] { X });
		}

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		return network;
	}

	@Override
	public Network compileLabeled(int networkId, Instance instance, LocalNetworkParam param) {

		NegationInstance inst = (NegationInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		ArrayList<Label> outputs = this.convert2Output(inst);
		inst.setOutput(outputs);

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

		for (int pos = 0; pos < size; pos++) {
			int tag_id = outputs.get(pos).getId();
			long to = node_array[pos][tag_id];
			lcrfNetwork.addEdge(from, new long[] { to });
			from = to;
		}

		lcrfNetwork.addEdge(from, new long[] { X });

		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		return network;

	}

	@Override
	public Label[] getLabels() {
		String[] labelForms = new String[] { "OB", "OA", "OM", "OR", "OV", "OCR", "IR", "ICR", "IV" };

		Label[] labels = new Label[labelForms.length];
		for (int i = 0; i < labels.length; i++)
			labels[i] = new Label(labelForms[i], i);

		return labels;
	}

	@Override
	public ArrayList<Label> convert2Output(NegationInstance inst) {

		SemanticRole sr = inst.sr;

		int l_focusVerb_pos = inst.size();
		int r_focusVerb_pos = 0;
		for (int i = 0; i < inst.size(); i++) {
			if (inst.focusScope[i] == 1 && i < l_focusVerb_pos) {
				l_focusVerb_pos = i;
			}

			if (inst.focusScope[i] == 1 && i > r_focusVerb_pos) {
				r_focusVerb_pos = i;
			}
		}

		ArrayList<Label> output = new ArrayList<Label>();
		int last_lable_id = 0;
		for (int i = 0; i < inst.size(); i++) {

			int roleIdx = sr != null ? sr.roletype[i] : -1;
			String roleName = (roleIdx >= 0) ? sr.roleNameList.get(roleIdx) : "*";

			int lable_id = inst.focusScope[i];

			if (lable_id == 0) {
				
				if (roleName.startsWith("*")) {

					if (i < l_focusVerb_pos)
						lable_id = 0;
					else if (i > r_focusVerb_pos)
						lable_id = 1;
					else
						lable_id = 2;
				} else {
					
					if (roleName.startsWith("V"))
						lable_id = 4;
					else if (roleName.startsWith("C"))
						lable_id = 5;
					else
						lable_id = 3;
				}
			} else {
				
				if (roleName.startsWith("V"))
					lable_id = 8;
				else if (roleName.startsWith("C"))
					lable_id = 7;
				else
					lable_id = 6;
				
				
			
			}
			output.add(this._labelsMap.get(lable_id));

			last_lable_id = lable_id;
		}

		return output;
	}

}
