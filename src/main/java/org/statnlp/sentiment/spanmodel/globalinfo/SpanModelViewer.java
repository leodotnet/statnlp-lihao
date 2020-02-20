package org.statnlp.sentiment.spanmodel.globalinfo;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.statnlp.commons.types.Label;
import org.statnlp.commons.types.LinearInstance;
import org.statnlp.sentiment.spanmodel.SpanModelCompiler.NodeType;
import org.statnlp.sentiment.spanmodel.SpanModelCompiler.OPNodeType;
import org.statnlp.sentiment.spanmodel.SpanModelCompiler.SentimentStateType;
import org.statnlp.sentiment.spanmodel.common.SentimentInstance;
import org.statnlp.ui.visualize.type.VNode;
import org.statnlp.ui.visualize.type.VisualizationViewerEngine;
import org.statnlp.ui.visualize.type.VisualizeGraph;
import org.statnlp.util.instance_parser.DelimiterBasedInstanceParser;
import org.statnlp.util.instance_parser.InstanceParser;



public class SpanModelViewer extends VisualizationViewerEngine {
	
	static double span_width = 100;

	static double span_height = 100;
	
	static double offset_width = 100;
	
	static double offset_height = 100;
	
	/**
	 * The list of instances to be visualized
	 */
	protected SentimentInstance<Integer> instance;
	
	protected ArrayList<String> inputs;
	
	protected Integer outputs;
	
	/**
	 * The list of labels to be used in the visualization.<br>
	 * This member is used to support visualization on those codes that do not utilize
	 * the generic pipeline. In the pipeline, use {@link #instanceParser} instead.
	 */
	protected Map<Integer, Label> labels;
	
	public SpanModelViewer(Map<Integer, Label> labels){
		super(null);
		this.labels = labels;
	}
	
	
	public SpanModelViewer(InstanceParser instanceParser) {
		super(instanceParser);
	}
	
	@SuppressWarnings("unchecked")
	protected void initData()
	{
		this.instance = (SentimentInstance<Integer>)super.instance;
		this.inputs = (ArrayList<String>)super.inputs;
		this.outputs = (Integer)super.outputs;
		//WIDTH = instance.Length * span_width;
	}
	
	@Override
	protected String label_mapping(VNode node) {
		int[] ids = node.ids;
//		int size = instance.size();
		int pos = ids[0]-1; // position
		int nodeId = ids[1];
		int sent = ids[4];
		int nodeType = ids[5];
		//System.out.println("ids:" + Arrays.toString(ids));
		String label =  Arrays.toString(ids);
		
		if (nodeType == NodeType.OP.ordinal() && pos == 0)
			label = label + OPNodeType.values()[nodeId].name();
		
		return  label;
	}
	
	protected void initNodeColor(VisualizeGraph vg)
	{
		if (colorMap != null){
			for(VNode node : vg.getNodes())
			{
				int[] ids = node.ids;
				int pos = ids[0];
				int nodeId = ids[1];
				int nodeType = ids[5];
				if(nodeType == NodeType.Sentiment.ordinal()){
					
					node.color = colorMap[8];
					
					
				} else if (nodeType == NodeType.OP.ordinal()) {
					node.color = colorMap[3];
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
			int size = this.inputs.size();
			int pos = ids[0]-1;
			int labelId = ids[1];
			int sent = ids[4];
			int nodeType = ids[5];
			
			double x = pos * span_width;
			int mappedId = labelId;
			if (nodeType == NodeType.OP.ordinal())
			{
				mappedId = labelId + SentimentStateType.values().length;
			} else if (nodeType == NodeType.Sentiment.ordinal())
			{
				mappedId = labelId;
			}
			
			double y = mappedId * span_height + offset_height + 1500 * sent;
			
			if(nodeType == NodeType.Root.ordinal()){
				x = (pos + 1) * span_width;
				y = 3 * span_height + offset_height;
			}
			else if (nodeType == NodeType.Leaf.ordinal()){
				x = (-1) * span_width;
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
