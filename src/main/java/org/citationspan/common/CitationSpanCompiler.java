package org.citationspan.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.example.base.BaseNetwork;
import org.statnlp.example.base.BaseNetwork.NetworkBuilder;
import org.statnlp.hypergraph.LocalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkCompiler;
import org.statnlp.hypergraph.NetworkIDMapper;



public abstract class CitationSpanCompiler extends NetworkCompiler {

	public Map<Integer, Label> _labelsMap;
	public Label[] _labels;
	
	public static Label[] LABELS;
	
	
	
	public CitationSpanCompiler() {
		super(null);
		NetworkIDMapper.setCapacity(new int[] { CitationSpanGlobal.MAX_SENTENCE_LENGTH, 20, NodeTypeSize});
		
		_labels = this.getLabels();
		LABELS = _labels;
		this._labelsMap = new HashMap<Integer, Label>();
		for(Label label: _labels){
			this._labelsMap.put(label.getId(), new Label(label));
		}
	}


	/**
	 * 
	 */
	private static final long serialVersionUID = 2100499563741744475L;

	public enum NodeType {
		X, Node, Root
	};


	int NodeTypeSize = NodeType.values().length;
	
	public Label[] getLabels() {
		String[] labelForms = new String[]{"O", "I"};
		
		Label[] labels = new Label[labelForms.length];
		for(int i = 0; i < labels.length; i++)
			labels[i] = new Label(labelForms[i], i);
		
		return labels;
	}
	
	
	public ArrayList<Label> convert2Output(CitationSpanInstance inst) {
		ArrayList<Label> output = new ArrayList<Label>();
		for(int i = 0; i < inst.size(); i++) {
			//output.add(this._labelsMap.get(inst.focusScope[i]));
		}
		
		return output;
	}
	

	protected long toNode_Root(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size + 1, 0, NodeType.Root.ordinal() });
	}

	protected long toNode_Node(int size, int pos, int tag_id) {
		return NetworkIDMapper.toHybridNodeID(new int[] { size - pos, tag_id, NodeType.Node.ordinal()});
	}

	protected long toNode_X(int size) {
		return NetworkIDMapper.toHybridNodeID(new int[] { 0, 0, NodeType.X.ordinal() });
	}


}
