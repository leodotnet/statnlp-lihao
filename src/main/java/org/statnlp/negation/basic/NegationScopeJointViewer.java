package org.statnlp.negation.basic;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.statnlp.commons.types.Label;
import org.statnlp.negation.basic.NegationScopeJointCompiler.*;
import org.statnlp.negation.common.NegationCompiler;
import org.statnlp.negation.common.NegationInstance;
import org.statnlp.ui.visualize.type.VNode;
import org.statnlp.ui.visualize.type.VisualizationViewerEngine;
import org.statnlp.ui.visualize.type.VisualizeGraph;
import org.statnlp.util.instance_parser.DelimiterBasedInstanceParser;
import org.statnlp.util.instance_parser.InstanceParser;



public class NegationScopeJointViewer extends VisualizationViewerEngine {
	
	static double span_width = 100;

	static double span_height = 100;
	
	static double offset_width = 100;
	
	static double offset_height = 100;
	
	/**
	 * The list of instances to be visualized
	 */
	protected NegationInstance<Integer> instance;
	
	protected ArrayList<String[]> inputs;
	
	protected ArrayList<Label> outputs;
	
	/**
	 * The list of labels to be used in the visualization.<br>
	 * This member is used to support visualization on those codes that do not utilize
	 * the generic pipeline. In the pipeline, use {@link #instanceParser} instead.
	 */
	protected Map<Integer, Label> labels;
	
	public NegationScopeJointViewer(Map<Integer, Label> labels){
		super(null);
		this.labels = labels;
	}
	
	
	public NegationScopeJointViewer(InstanceParser instanceParser) {
		super(instanceParser);
	}
	
	@SuppressWarnings("unchecked")
	protected void initData()
	{
		this.instance = (NegationInstance<Integer>)super.instance;
		this.inputs = (ArrayList<String[]>)super.inputs;
		this.outputs = (ArrayList<Label>)super.outputs;
		//WIDTH = instance.Length * span_width;
	}
	
	@Override
	protected String label_mapping(VNode node) {
		int[] ids = node.ids;
		int size = instance.size();
		int pos = size - ids[1]; // position
		int tag_id = ids[4];
		int nodeType = ids[0];

		String label =  Arrays.toString(ids);
		
		if (nodeType == NodeType.CueNode.ordinal()) {
			if (tag_id == 0) {
				label = this.inputs.get(pos)[NegationInstance.FEATURE_TYPES.word.ordinal()];
			} else
				label = "B";
			
		} else if (nodeType == NodeType.ScopeStart.ordinal()) {
			label = "<SStart>";
		} else if (nodeType == NodeType.ScopeNode.ordinal()) {
			
				if (tag_id == 0) {
					label = this.inputs.get(pos)[NegationInstance.FEATURE_TYPES.word.ordinal()];
				} else
					label = NegationCompiler.LABELS[2 + tag_id].getForm(); 
			
			
		} else if (nodeType == NodeType.Root.ordinal()) {
			label = "<Root>";
		} else {
			label = "<X>";
		}
		
		return  label;
	}
	
	protected void initNodeColor(VisualizeGraph vg)
	{
		if (colorMap != null){
			for(VNode node : vg.getNodes())
			{
				int[] ids = node.ids;
				int size = instance.size();
				int pos = size - ids[1]; // position
				int tag_id = ids[4];
				int nodeType = ids[0];
				
				
				if(nodeType == NodeType.Root.ordinal()){
					node.color = colorMap[8];
					
				} else if (nodeType == NodeType.X.ordinal()) {
					node.color = colorMap[3];
				} else if (nodeType == NodeType.CueNode.ordinal()) {
					node.color = colorMap[4];
				}
				else  if (nodeType == NodeType.ScopeStart.ordinal()){
					node.color = colorMap[8];
				} else {   //scope node
					String label = NegationCompiler.LABELS[2 + tag_id].getForm(); 

						if (label.startsWith("O")) {
							node.color = colorMap[0];
						} else
							node.color = colorMap[2];
					
				}
			}
		}
		
	}
	
	protected void initNodeCoordinate(VisualizeGraph vg)
	{
		for(VNode node : vg.getNodes())
		{
			int[] ids = node.ids;
			int size = instance.size();
			int pos = size - ids[1]; // position
			int tag_id = ids[4];
			int nodeType = ids[0];
			int cueLeftBoundary = size -  ids[2];
			int cueRightBoundary = size - ids[3];
			
			double x = pos * span_width;
			int mappedId = tag_id;
			if (nodeType == NodeType.ScopeNode.ordinal())
			{
				mappedId = tag_id + 3 + cueLeftBoundary * 3 + (cueRightBoundary - cueLeftBoundary);
				x = (cueLeftBoundary + pos) * span_width;
				
			} else if (nodeType == NodeType.ScopeStart.ordinal()) {
				x = ( cueLeftBoundary-1) * span_width;
			}
			
			double y = mappedId * span_height + offset_height;
			
			if (nodeType == NodeType.ScopeNode.ordinal())
			{
				
			} else if (nodeType == NodeType.ScopeStart.ordinal()) {
				y = (tag_id + 3 + cueLeftBoundary * 3 + (cueRightBoundary - cueLeftBoundary) - 1) * span_height + offset_height;
			}
			
			if(nodeType == NodeType.Root.ordinal()){
				x = (pos - 1) * span_width;
				y = 3 * span_height + offset_height;
			}
			else if (nodeType == NodeType.X.ordinal()){
				x = (size + size) * span_width;
				y = 3 * span_height + offset_height;
			}
			
			
			node.point = new Point2D.Double(x,y);
			layout.setLocation(node, node.point);
			layout.lock(node, true);
		}
	}
	
	private Label getLabel(int labelId){
		if(this.instanceParser != null){
			return ((DelimiterBasedInstanceParser)this.instanceParser).getLabel(labelId);
		} else {
			return this.labels.get(labelId);
		}
	}

}
