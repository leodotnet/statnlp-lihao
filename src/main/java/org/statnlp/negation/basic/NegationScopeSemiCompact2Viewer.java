package org.statnlp.negation.basic;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.statnlp.commons.types.Label;
import org.statnlp.negation.basic.NegationScopeSemiCompact2Compiler;
import org.statnlp.negation.basic.NegationScopeSemiCompact2Compiler.*;
import org.statnlp.negation.common.NegationGlobal;
import org.statnlp.negation.common.NegationInstance;
import org.statnlp.ui.visualize.type.VNode;
import org.statnlp.ui.visualize.type.VisualizationViewerEngine;
import org.statnlp.ui.visualize.type.VisualizeGraph;
import org.statnlp.util.instance_parser.DelimiterBasedInstanceParser;
import org.statnlp.util.instance_parser.InstanceParser;



public class NegationScopeSemiCompact2Viewer extends VisualizationViewerEngine {
	
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
	
	public NegationScopeSemiCompact2Viewer(Map<Integer, Label> labels){
		super(null);
		this.labels = labels;
	}
	
	
	public NegationScopeSemiCompact2Viewer(InstanceParser instanceParser) {
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
		int pos = 1000 - ids[0]; // position
		//int tag_id = ids[1];
		int numSpan = 100 - ids[2];
		int nodeType = ids[3];

		String label =  Arrays.toString(ids);
		
		if (nodeType == NodeType.O.ordinal()) {
			label = pos < size ? this.inputs.get(pos)[NegationInstance.FEATURE_TYPES.word.ordinal()] : "<END>";
		} else if (nodeType == NodeType.Root.ordinal()) {
			label = "<Root>";
		} else if (nodeType == NodeType.X.ordinal()) {
			label = "<X>";
		} else if (nodeType == NodeType.Y.ordinal()) {
			label = "<Y>";
		} else {
			label = Arrays.toString(ids);//NodeType.values()[nodeType].name();//Arrays.toString(ids);//NegationCompiler.LABELS[tag_id].getForm();
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
				int pos = 1000 - ids[0]; // position
				//int tag_id = ids[1];
				int numSpan = 100 - ids[2];
				int nodeType = ids[3];
				
				
				if(nodeType == NodeType.Root.ordinal()){
					node.color = colorMap[8];
					
				} else if (nodeType == NodeType.X.ordinal()) {
					node.color = colorMap[3];
				} else if (nodeType == NodeType.Y.ordinal()) {
					node.color = colorMap[3];
				} else if (nodeType == NodeType.O.ordinal()) {
					node.color = colorMap[0];
					if (pos < size && instance.negation != null && instance.negation.cue[pos] == 1)
						node.color = colorMap[8];
				}
				else
				{
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
			int pos = 1000 - ids[0]; // position
			//int tag_id = ids[1];
			int numSpan = 100 - ids[2];
			int nodeType = ids[3];
			
			double x = pos * span_width;
			int mappedId = nodeType - NodeType.O.ordinal();
			
			double y = mappedId * span_height + offset_height;
			
			if(nodeType == NodeType.Root.ordinal()){
				x = (pos - 1) * span_width;
				y = 4 * span_height + offset_height;
			}
			else if (nodeType == NodeType.X.ordinal()){
				x = (size + 1) * span_width;
				y = 2 * span_height + offset_height;
			} else if (nodeType == NodeType.Y.ordinal()){
				x = (size + 1) * span_width;
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
