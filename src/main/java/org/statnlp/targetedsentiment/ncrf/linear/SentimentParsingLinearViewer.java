package org.statnlp.targetedsentiment.ncrf.linear;

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
import org.statnlp.targetedsentiment.ncrf.linear.SentimentParsingLinearCompiler.*;
import org.statnlp.ui.visualize.type.VNode;
import org.statnlp.ui.visualize.type.VisualizationViewerEngine;
import org.statnlp.ui.visualize.type.VisualizeGraph;
import org.statnlp.util.instance_parser.InstanceParser;




public class SentimentParsingLinearViewer extends VisualizationViewerEngine {
	
	static double span_width = 100;

	static double span_height = 100;
	
	static double offset_width = 100;
	
	static double offset_height = 40;
	
	/**
	 * The list of instances to be visualized
	 */
	protected TSInstance<Label> instance;
	
	protected ArrayList<String[]> inputs;
	
	protected ArrayList<Label> outputs;
	
	int size = -1;
	
	ArrayList<Integer> potentialSentWord = null;
	
	
	/**
	 * The list of labels to be used in the visualization.<br>
	 * This member is used to support visualization on those codes that do not utilize
	 * the generic pipeline. In the pipeline, use {@link #instanceParser} instead.
	 */
	protected Map<Integer, Label> labels;
	
	public SentimentParsingLinearViewer(Map<Integer, Label> labels){
		super(null);
		this.labels = labels;
		
	}
	
	
	public SentimentParsingLinearViewer(InstanceParser instanceParser) {
		super(instanceParser);
		//potentialSentWord = instance.potentialSentWord;
	}
	
	@SuppressWarnings("unchecked")
	protected void initData()
	{
		this.instance = (TSInstance<Label>)super.instance;
		this.inputs = (ArrayList<String[]>)super.inputs;
		this.outputs = (ArrayList<Label>)super.outputs;
		this.size = instance.size();
		//WIDTH = instance.Length * span_width;
		potentialSentWord = instance.potentialSentWord;
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
		else if (nodeType == NodeType.O.ordinal()) {
			label = inputs.get(pos)[0];
		}
		else if (nodeType == NodeType.B0.ordinal() || nodeType == NodeType.A0.ordinal()) {
			label = NodeType.values()[nodeType].name() + " " + Arrays.toString(ids);
		}
		else if (nodeType == NodeType.NULL.ordinal()) {
			label = NodeType.values()[nodeType].name() + "-" + new String[]{"+", "0", "-"}[polar];
		}
		else
		{
			label = NodeType.values()[nodeType].name() + "-" + new String[]{"+", "0", "-"}[polar];//Arrays.toString(ids);
			//label += Arrays.toString(ids);
			
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
					
					node.color = colorMap[6];
				}
				
				
				if (nodeType == NodeType.A0.ordinal()) {
					
					node.color = colorMap[5];
				}
				
				
				if(nodeType == NodeType.Root.ordinal() || nodeType == NodeType.X.ordinal()){
					
					node.color = colorMap[8];
					
				} 
				
				if (nodeType == NodeType.B.ordinal() ){
					
					node.color = colorMap[3];
				}
				
				if (nodeType == NodeType.I.ordinal() ){
					
					node.color = colorMap[4];
				}
				
				if (nodeType == NodeType.NULL.ordinal()) {
					node.color = Color.PINK;
				}
				
				if (nodeType == NodeType.O.ordinal()) {
					
					if (this.potentialSentWord != null && this.potentialSentWord.contains(pos)) {
						node.color = colorMap[2];
					} else {
						node.color = colorMap[0];
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
			int pos = 2 * size - ids[0];
			int nodeType = ids[1];
			int polar = ids[2];
			
			if (nodeType == NodeType.B0.ordinal()) {
				pos = ids[0] - 1;
			}
			
			if (nodeType != NodeType.B.ordinal() && nodeType != NodeType.I.ordinal() && nodeType != NodeType.NULL.ordinal()) {
				polar = 0;
			} else if (nodeType == NodeType.B.ordinal() || nodeType == NodeType.I.ordinal()) {
				polar--;
			}
			
			double x = pos * span_width;
			
			double y = nodeType * span_height +   polar  * (offset_height);
			
			if(nodeType == NodeType.Root.ordinal()){
				x = (- 2) * span_width;
				y = 3 * span_height + offset_height;
			} else if (nodeType == NodeType.X.ordinal()){
				x = (size + 1) * span_width;
				y = 3 * span_height + offset_height;
			} else if (nodeType == NodeType.NULL.ordinal()) { 
				x = (- 1) * span_width;
				y = 3 * span_height + offset_height + polar * offset_height;
			} else {
				/*
				if (nodeType == NodeType.B.ordinal()) {
					
					
					y = span_height + offset_height + polar * NodeType.values().length * span_height;
				}
				
				
				if (nodeType == NodeType.O.ordinal()) {
					
					
					y = polar * NodeType.values().length * span_height;
				}*/
				
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
