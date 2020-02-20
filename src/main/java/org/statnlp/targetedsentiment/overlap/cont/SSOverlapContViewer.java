package org.statnlp.targetedsentiment.overlap.cont;

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
import org.statnlp.targetedsentiment.overlap.cont.SSOverlapContCompiler.*;
import org.statnlp.ui.visualize.type.VNode;
import org.statnlp.ui.visualize.type.VisualizationViewerEngine;
import org.statnlp.ui.visualize.type.VisualizeGraph;
import org.statnlp.util.instance_parser.InstanceParser;




public class SSOverlapContViewer extends VisualizationViewerEngine {
	
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
	
	public SSOverlapContViewer(Map<Integer, Label> labels){
		super(null);
		this.labels = labels;
	}
	
	
	public SSOverlapContViewer(InstanceParser instanceParser) {
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
		
		int pos = 2 * size - ids[0]; // position
		int nodeType = ids[1];
		int polar = ids[2];
		
		if (nodeType == NodeType.B0.ordinal()) {
			pos = ids[0] - 1;
		}
		//System.out.println("ids:" + Arrays.toString(ids));
		String label =  Arrays.toString(ids);
		
		if (nodeType == NodeType.Root.ordinal())
		{
			label = "<Root>";
		}
		else if  (nodeType == NodeType.X.ordinal())
		{
			label = "X";
		}
		else if (nodeType == NodeType.E.ordinal()) {
			label = inputs.get(pos)[0];
		}
		else
		{
			label = NodeType.values()[nodeType].name() + "-" + new String[]{"+", "0", "-"}[polar];//Arrays.toString(ids);
			label += Arrays.toString(ids);
			
		}
		
		
		return  label;
	}
	
	protected void initNodeColor(VisualizeGraph vg)
	{
		if (colorMap != null){
			for(VNode node : vg.getNodes())
			{
				int[] ids = node.ids;
				int pos = 2 * size - ids[0];
				int nodeType = ids[1];
				int polar = ids[2];
				
				if (nodeType == NodeType.B0.ordinal()) {
					pos = ids[0] - 1;
				}
				
				
				if(nodeType == NodeType.Root.ordinal() || nodeType == NodeType.X.ordinal()){
					
					node.color = colorMap[8];
					
				} else  {
					
					
					node.color = colorMap[3];
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
			int pos = 2 * size - ids[0];
			int nodeType = ids[1];
			int polar = ids[2];
			
			if (nodeType == NodeType.B0.ordinal()) {
				pos = ids[0] - 1;
			}
			
			double x = pos * span_width;
			
			double y = nodeType * span_height + offset_height + polar * NodeType.values().length * span_height;
			
			if(nodeType == NodeType.Root.ordinal()){
				x = (- 2) * span_width;
				y = 3 * span_height + offset_height;
			}
			else if (nodeType == NodeType.X.ordinal()){
				x = (size + 1) * span_width;
				y = 3 * span_height + offset_height;
			} else {
				
				if (nodeType == NodeType.B.ordinal()) {
					x -= 5;
					
					y = NodeType.A.ordinal() * span_height + offset_height + polar * NodeType.values().length * span_height;
				}
				
				
				if (nodeType == NodeType.A.ordinal()) {
					x += 5;
					
					y = NodeType.B.ordinal() * span_height + offset_height + polar * NodeType.values().length * span_height;
				}
				
				if (nodeType == NodeType.B0.ordinal()) {
					x -= 5;
				}
				
				if (nodeType == NodeType.A0.ordinal()) {
					x += 5;
				}
			}
			
			
			node.point = new Point2D.Double(x,y);
			layout.setLocation(node, node.point);
			layout.lock(node, true);
		}
	}

}
