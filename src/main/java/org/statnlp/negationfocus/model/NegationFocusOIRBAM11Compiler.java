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

public class NegationFocusOIRBAM11Compiler extends NegationCompiler {

	/**
	 * 
	 */
	private static final long serialVersionUID = 2899070269878131792L;

	public NegationFocusOIRBAM11Compiler() {
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
		
		SemanticRole sr = inst.sr;
		assert(sr != null);
		String focusRoleName = null;
		int focusTagID = -1;
		
		//String[] labelForms = new String[] { "OB", "OA", "OM", "OVB", "OVA", "OVM", "ORL", "ORR", "IV", "IRL", "IRR" };
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
				
				int roleIdx = sr != null ? sr.roletype[parent_pos] : -1;
				String roleName = (roleIdx >= 0) ? sr.roleNameList.get(roleIdx) : "*";
				
				String tagStr = this.LABELS[parent_tag_id].getForm();
				
				if (tagStr.startsWith("I")) {
					focusRoleName = roleName;
					focusTagID = parent_tag_id;
				} else {
					if (roleName.startsWith("C") && focusRoleName != null && roleName.contains(focusRoleName)) {
						parent_tag_id = focusTagID;
					}
				}
				
				
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
		SemanticRole sr = inst.sr;
		
		int[] tag_id_set = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
		int[] last_tag_id_set = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};

		long start = this.toNode_Root(size);
		lcrfNetwork.addNode(start);

		long[][] node_array = new long[size][this._labels.length];

		// build node array
		for (int pos = 0; pos < size; pos++) {
			
			tag_id_set = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
			
			if (inst.focusVerb[pos] == 0) {
				tag_id_set[3] = -1;
				tag_id_set[4] = -1;
				tag_id_set[5] = -1;
				tag_id_set[8] = -1;
				
				if (pos < sr.l_focusVerb_pos) {
					tag_id_set[7] = -1;
					tag_id_set[10] = -1;
				} else if (pos > sr.r_focusVerb_pos) {
					tag_id_set[6] = -1;
					tag_id_set[9] = -1;
					
				}
			} else {
				tag_id_set = new int[]{0, -1, -1, 3, 4, 5, -1, -1, 8, -1, -1};
			}

			for (int tag_id : tag_id_set) {
				
				if (tag_id == -1) continue;
				
				long node = this.toNode_Node(size, pos, tag_id);
				lcrfNetwork.addNode(node);
				node_array[pos][tag_id] = node;
			}

		}

		long X = this.toNode_X(inst.size());
		lcrfNetwork.addNode(X);

		
		/////////
		long from = start;
		
		
		//String[] labelForms = new String[] { "OB", "OA", "OM", "OVB", "OVA", "OVM", "ORL", "ORR", "IV", "IRL", "IRR" };
		tag_id_set = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
		if (inst.focusVerb[0] == 0) {
			tag_id_set[3] = -1;
			tag_id_set[4] = -1;
			tag_id_set[5] = -1;
			tag_id_set[8] = -1;
			
			if (0 < sr.l_focusVerb_pos) {
				tag_id_set[7] = -1;
				tag_id_set[10] = -1;
			}
		} else {
			tag_id_set = new int[]{-1, -1, -1, 3, 4, 5, -1, -1, 8, -1, -1};
		}
		
		
		
		for (int tag_id : tag_id_set) {
			if (tag_id == -1) continue;
			long to = node_array[0][tag_id];
			lcrfNetwork.addEdge(from, new long[] { to });
		}

		for (int pos = 1; pos < size; pos++) {
			
			tag_id_set = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
			last_tag_id_set = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
			
			if (inst.focusVerb[pos] == 0) {
				tag_id_set[3] = -1;
				tag_id_set[4] = -1;
				tag_id_set[5] = -1;
				tag_id_set[8] = -1;
				
				if (pos < sr.l_focusVerb_pos) {
					tag_id_set[7] = -1;
					tag_id_set[10] = -1;
				} else if (pos > sr.r_focusVerb_pos) {
					tag_id_set[6] = -1;
					tag_id_set[9] = -1;
					
				}
			} else {
				tag_id_set = new int[]{-1, -1, -1, 3, 4, 5, -1, -1, 8, -1, -1};
			}
			
			int last_pos = pos - 1;
			
			if (inst.focusVerb[last_pos] == 0) {
				last_tag_id_set[3] = -1;
				last_tag_id_set[4] = -1;
				last_tag_id_set[5] = -1;
				last_tag_id_set[8] = -1;
				
				if (last_pos < sr.l_focusVerb_pos) {
					last_tag_id_set[7] = -1;
					last_tag_id_set[10] = -1;
				} else if (last_pos > sr.r_focusVerb_pos) {
					last_tag_id_set[6] = -1;
					last_tag_id_set[9] = -1;
					
				}
			} else {
				last_tag_id_set = new int[]{-1, -1, -1, 3, 4, 5, -1, -1, 8, -1, -1};
			}
			
			
			for (int last_tag_id : last_tag_id_set) {
				
				if (last_tag_id == -1) continue;
				
				for (int tag_id : tag_id_set) {
					
					if (tag_id == -1) continue;
					
					/*
					if (last_tag_id == 1) {
						if (tag_id == 0 || tag_id >= 6) continue;
					}
					
					if (last_tag_id >= 6) {
						if (tag_id == 0) continue;
					}
					*/
					
					from = node_array[pos - 1][last_tag_id];
					long to = node_array[pos][tag_id];
					lcrfNetwork.addEdge(from, new long[] { to });
				}
			}
		}

		last_tag_id_set = new int[]{0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
		
		int last_pos = size - 1;
		
		if (inst.focusVerb[last_pos] == 0) {
			last_tag_id_set[3] = -1;
			last_tag_id_set[4] = -1;
			last_tag_id_set[5] = -1;
			last_tag_id_set[8] = -1;
			
			if (last_pos < sr.l_focusVerb_pos) {
				last_tag_id_set[7] = -1;
				last_tag_id_set[10] = -1;
			} else if (last_pos > sr.r_focusVerb_pos) {
				last_tag_id_set[6] = -1;
				last_tag_id_set[9] = -1;
				
			}
		} else {
			last_tag_id_set = new int[]{-1, -1, -1, 3, 4, 5, -1, -1, 8, -1, -1};
		}
		
		for (int last_tag_id : last_tag_id_set) {
			if (last_tag_id == -1) continue;
			
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
		String[] labelForms = new String[] { "OB", "OA", "OM", "OVB", "OVA", "OVM", "ORL", "ORR", "IV", "IRL", "IRR" };

		Label[] labels = new Label[labelForms.length];
		for (int i = 0; i < labels.length; i++)
			labels[i] = new Label(labelForms[i], i);

		return labels;
	}

	@Override
	public ArrayList<Label> convert2Output(NegationInstance inst) {

		SemanticRole sr = inst.sr;

		int l_focusScope_pos = inst.size();
		int r_focusScope_pos = 0;
		
		int l_focusVerb_pos = inst.size();
		int r_focusVerb_pos = 0;
		
		for (int i = 0; i < inst.size(); i++) {
			if (inst.focusScope[i] == 1 && i < l_focusScope_pos) {
				l_focusScope_pos = i;
			}

			if (inst.focusScope[i] == 1 && i > r_focusScope_pos) {
				r_focusScope_pos = i;
			}
			
			
			if (inst.focusVerb[i] == 1 && i < l_focusVerb_pos) {
				l_focusVerb_pos = i;
			}

			if (inst.focusVerb[i] == 1 && i > r_focusVerb_pos) {
				r_focusVerb_pos = i;
			}
		}
		
		sr.l_focusScope_pos = l_focusScope_pos;
		sr.r_focusScope_pos = r_focusScope_pos;
		sr.l_focusVerb_pos = l_focusVerb_pos;
		sr.r_focusVerb_pos = r_focusVerb_pos;
		

		ArrayList<Label> output = new ArrayList<Label>();
		int last_lable_id = 0;
		for (int i = 0; i < inst.size(); i++) {

			int roleIdx = sr != null ? sr.roletype[i] : -1;
			String roleName = (roleIdx >= 0) ? sr.roleNameList.get(roleIdx) : "*";

			int lable_id = inst.focusScope[i];

			if (lable_id == 0) {
				
				if (roleName.startsWith("*")) {

					if (i < l_focusScope_pos)
						lable_id = 0;
					else if (i > r_focusScope_pos)
						lable_id = 1;
					else
						lable_id = 2;
				} else {
					
					if (roleName.startsWith("V") || roleName.startsWith("C-V")) {
						
						if (i < l_focusScope_pos)
							lable_id = 3;
						else if (i > r_focusScope_pos)
							lable_id = 4;
						else
							lable_id = 5;
						
					} else {
						if (i < l_focusVerb_pos)
							lable_id = 6;
						else
							lable_id = 7;
					}
				}
			} else {
				
				if (roleName.startsWith("V") || roleName.startsWith("C-V"))
					lable_id = 8;
				else
					if (i < l_focusVerb_pos)
						lable_id = 9;
					else
						lable_id = 10;
				
				
			
			}
			output.add(this._labelsMap.get(lable_id));

			last_lable_id = lable_id;
		}

		return output;
	}

}
