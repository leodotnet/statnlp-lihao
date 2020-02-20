package org.statnlp.targetedsentiment.ncrf.baseline;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.statnlp.commons.types.Label;
import org.statnlp.commons.types.LinearInstance;
import org.statnlp.targetedsentiment.common.TSInstance;
import org.statnlp.targetedsentiment.f.baseline.CollapseTSCompiler.*;
import org.statnlp.ui.visualize.type.VNode;
import org.statnlp.ui.visualize.type.VisualizationViewerEngine;
import org.statnlp.ui.visualize.type.VisualizeGraph;
import org.statnlp.util.instance_parser.DelimiterBasedInstanceParser;
import org.statnlp.util.instance_parser.InstanceParser;



public class CollapseViewer extends VisualizationViewerEngine {
	
	static double span_width = 100;

	static double span_height = 100;
	
	static double offset_width = 100;
	
	static double offset_height = 100;
	
	/**
	 * The list of instances to be visualized
	 */
	protected TSInstance<Label> instance;
	
	protected ArrayList<String[]> inputs;
	
	protected ArrayList<Label> outputs;
	
	int size = -1;
	
	/**
	 * The list of labels to be used in the visualization.<br>
	 * This member is used to support visualization on those codes that do not utilize
	 * the generic pipeline. In the pipeline, use {@link #instanceParser} instead.
	 */
	protected Map<Integer, Label> labels;
	
	public CollapseViewer(Map<Integer, Label> labels){
		super(null);
		this.labels = labels;
	}
	
	
	public CollapseViewer(InstanceParser instanceParser) {
		super(instanceParser);
	}
	
	@SuppressWarnings("unchecked")
	protected void initData()
	{
		this.instance = (TSInstance<Label>)super.instance;
		this.inputs = (ArrayList<String[]>)super.inputs;
		this.outputs = (ArrayList<Label>)super.outputs;
		this.size = instance.size();
		//WIDTH = instance.Length * span_width;
	}
	
	@Override
	protected String label_mapping(VNode node) {
		int[] ids = node.ids;
		
		int pos = size - ids[0]; // position
		int nodeId = ids[1];
		int nodeType = ids[4];
		//System.out.println("ids:" + Arrays.toString(ids));
		String label =  Arrays.toString(ids);
		
		if (nodeType == NodeType.Span.ordinal())
		{
			label = SubNodeType.values()[nodeId].name();//Arrays.toString(ids);
			
			if (nodeId == SubNodeType.O.ordinal())
			{
				label = inputs.get(pos)[0];
			}
			
		}
		else if (nodeType == NodeType.Start.ordinal())
		{
			label = "<Start>";
		}
		else
		{
			label = "<End>";
		}
		
		//label = Arrays.toString(ids);//SubNodeType.values()[nodeId].name();
		
		return  label;
	}
	
	protected void initNodeColor(VisualizeGraph vg)
	{
		if (colorMap != null){
			for(VNode node : vg.getNodes())
			{
				int[] ids = node.ids;
				int pos = size - ids[0];
				int nodeId = ids[1];
				int nodeType = ids[4];
				if(nodeType == NodeType.Start.ordinal() || nodeType == NodeType.End.ordinal()){
					
					node.color = colorMap[8];
					
				} else if (nodeType == NodeType.Span.ordinal()) {
					if (nodeId == 0)
					{
						node.color = colorMap[0];
					}
					else
					{
						node.color = colorMap[(nodeId - 1) % 4];
					}
				}
				
			}
		}
		
	}
	
	protected void initNodeCoordinate(VisualizeGraph vg)
	{
		for(VNode node : vg.getNodes())
		{
			int[] ids = node.ids;
			int size = this.inputs.size();
			int pos = size - ids[0];
			int nodeId = ids[1];
			int nodeType = ids[4];
			
			double x = pos * span_width;
			int mappedId = nodeId;
			if (nodeType == NodeType.Span.ordinal())
			{
			}
			
			
			double y = mappedId * span_height + offset_height;
			
			if(nodeType == NodeType.Start.ordinal()){
				x = (pos - 2) * span_width;
				y = 3 * span_height + offset_height;
			}
			else if (nodeType == NodeType.End.ordinal()){
				x = (size + 1) * span_width;
				y = 3 * span_height + offset_height;
			}
			
			
			node.point = new Point2D.Double(x,y);
			layout.setLocation(node, node.point);
			layout.lock(node, true);
		}
	}

}
