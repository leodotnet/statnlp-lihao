package org.statnlp.negation.basic;

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
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.NetworkIDMapper;
import org.statnlp.negation.common.NegationCompiler;
import org.statnlp.negation.common.NegationGlobal;
import org.statnlp.negation.common.NegationInstance;
import org.statnlp.negation.common.Utils;

public class NegationSpanSemiOI2Compiler extends NegationCompiler {

	/**
	 * 
	 */
	private static final long serialVersionUID = -5689984204595365160L;

	public NegationSpanSemiOI2Compiler() {
		super();
	}

	@Override
	public NegationInstance decompile(Network network) {
		BaseNetwork nsnetwork = (BaseNetwork) network;
		NegationInstance inst = (NegationInstance) network.getInstance();
		int size = inst.size();
		// List<String[]> inputs = (List<String[]>) inst.getInput();

		ArrayList<Label> predication_array = new ArrayList<Label>();

		long rootNode = toNode_Root(size);
		int node_k = Arrays.binarySearch(nsnetwork.getAllNodes(), rootNode);
		// node_k = 0;
		while (true) {
			// System.out.print(node_k + " ");
			int[] parent_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(node_k));
			int parent_pos = size - parent_ids[0];
			int parent_tag_id = parent_ids[1];
			int parent_node_type = parent_ids[2];
			
			String tagParent = this._labels[parent_tag_id].getForm();

			if (parent_node_type == NodeType.X.ordinal()) {
				break;
			}

			int[] childs = network.getMaxPath(node_k);

			int[] child_ids = NetworkIDMapper.toHybridNodeArray(network.getNode(childs[0]));
			int child_pos = size - child_ids[0];

			if (parent_node_type == NodeType.Root.ordinal()) {

			} else if (parent_node_type == NodeType.Node.ordinal()) {
				
				if (tagParent.startsWith("O")) {
					if (parent_pos < size) {
						Label label = this._labelsMap.get(parent_tag_id);
						predication_array.add(new Label(label));
					}
				} else {
				
					for(int pos = parent_pos; pos < child_pos; pos++) {

						Label label = this._labelsMap.get(parent_tag_id);

						predication_array.add(new Label(label));
					}
				}

			}

			node_k = childs[0];

		}

		inst.setPrediction(predication_array);
		
		return inst;
	}

	@Override
	public Network compileUnlabeled(int networkId, Instance instance, LocalNetworkParam param) {

		NegationInstance inst = (NegationInstance) instance;
		NetworkBuilder<BaseNetwork> nsnetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		
		if (inst.getOutput() == null) {
			ArrayList<Label> outputs = this.convert2Output(inst);
			inst.setOutput(outputs); // the following code will not use the outputs
		}
		

		
		int size = inst.size();

		long start = this.toNode_Root(size);
		nsnetwork.addNode(start);

		long[][] node_array = new long[size + 1][this._labels.length];

		// build node array
		for (int pos = 0; pos < size + 1; pos++) {

			for (int tag_id = 0; tag_id < this._labels.length; tag_id++) {
				long node = this.toNode_Node(size, pos, tag_id);
				nsnetwork.addNode(node);
				node_array[pos][tag_id] = node;
			}

		}

		long X = this.toNode_X(inst.size());
		nsnetwork.addNode(X);

		// ///////
		
		int[] tag_id_set = new int[]{0, 1};
		int[] last_tag_id_set = new int[]{0, 1};
		
		
		
		long from = start;
		long to = -1;
		
		for (int tag_id : tag_id_set) {
			nsnetwork.addEdge(from, new long[] { node_array[0][tag_id] });
		}

		for (int pos = 0; pos < size; pos++) {
			
			//node O
			from = node_array[pos][0];
			{
				long node_O = node_array[pos + 1][0];
				nsnetwork.addEdge(from, new long[] { node_O });
				
				if (pos < size - 1) {
					long node_I = node_array[pos + 1][1];
					nsnetwork.addEdge(from, new long[] { node_I });
				}
			}
			
			
			//node I
			from = node_array[pos][1];
			
			for(int L = 1; L < NegationGlobal.L_MAX && pos + L <= size; L++ ) {
				
				long node_O = node_array[pos + L][0];
				nsnetwork.addEdge(from, new long[] { node_O });
				
			}
			
		}
		
		nsnetwork.addEdge(node_array[size][0], new long[] { X });


		BaseNetwork network = nsnetwork.build(networkId, inst, param, this);
		return network;
	}

	@Override
	public Network compileLabeled(int networkId, Instance instance, LocalNetworkParam param) {

		NegationInstance inst = (NegationInstance) instance;
		NetworkBuilder<BaseNetwork> nsnetwork = NetworkBuilder.builder();

		List<String[]> inputs = (List<String[]>) inst.getInput();
		ArrayList<Label> outputs = this.convert2Output(inst);
		if (inst.getOutput() == null) {
			inst.setOutput(outputs);
		}

		int size = inst.size();

		long start = this.toNode_Root(size);
		nsnetwork.addNode(start);

		long[][] node_array = new long[size + 1][this._labels.length];

		// build node array
		for (int pos = 0; pos < size + 1; pos++) {

			for (int tag_id = 0; tag_id < this._labels.length; tag_id++) {
				long node = this.toNode_Node(size, pos, tag_id);
				nsnetwork.addNode(node);
				node_array[pos][tag_id] = node;
			}

		}

		long X = this.toNode_X(inst.size());
		nsnetwork.addNode(X);
		

		// ///////
		long from = start;
		int lastTagId = 0;
		
		for (int pos = 0; pos < size; pos++) {
			int tagId = outputs.get(pos).getId();
			
			if (lastTagId == 0) {
				long to = node_array[pos][tagId];
				nsnetwork.addEdge(from, new long[] { to });
				from = to;
			} else {
				if (lastTagId == 1 && tagId == 0) {
					long to = node_array[pos][tagId];
					nsnetwork.addEdge(from, new long[] { to });
					from = to;
				}
			}
			
			lastTagId = tagId;
			
		}

		nsnetwork.addEdge(from, new long[] { node_array[size][0] });
		
		nsnetwork.addEdge(node_array[size][0], new long[] { X});

		BaseNetwork network = nsnetwork.build(networkId, inst, param, this);
		return network;

	}

	@Override
	public Label[] getLabels() {
		String[] labelForms = new String[] { "O", "I" };

		Label[] labels = new Label[labelForms.length];
		for (int i = 0; i < labels.length; i++)
			labels[i] = new Label(labelForms[i], i);

		return labels;
	}

	
	@Override
	public ArrayList<Label> convert2Output(NegationInstance inst) {
		
		ArrayList<Label> output = new ArrayList<Label>();
		
		if (inst.hasNegation == false) {
			for (int i = 0; i < inst.size(); i++) {
				output.add(this._labelsMap.get(0));
			}
			
			return output;
		}
		
		int[] cues = inst.negation.cue;
		
		int l_cue_pos = inst.size();
		int r_cue_pos = 0;
		for (int i = 0; i < inst.size(); i++) {
			if (cues[i] == 1 && i < l_cue_pos) {
				l_cue_pos = i;
			}

			if (cues[i] == 1 && i > r_cue_pos) {
				r_cue_pos = i;
			}
		}
		
		inst.negation.leftmost_cue_pos = l_cue_pos;
		inst.negation.rightmost_cue_pos = r_cue_pos;

		

		for (int i = 0; i < inst.size(); i++) {
			int lable_id = inst.negation.span[i];

			output.add(this._labelsMap.get(lable_id));

		}

		return output;
	}

}
