package org.statnlp.negation.basic;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.statnlp.commons.types.Label;
import org.statnlp.negation.common.NegationCompiler;
import org.statnlp.negation.common.NegationCompiler.NodeType;
import org.statnlp.negation.common.NegationInstance;
import org.statnlp.ui.visualize.type.VNode;
import org.statnlp.ui.visualize.type.VisualizationViewerEngine;
import org.statnlp.ui.visualize.type.VisualizeGraph;
import org.statnlp.util.instance_parser.DelimiterBasedInstanceParser;
import org.statnlp.util.instance_parser.InstanceParser;



public class NegationViewer extends VisualizationViewerEngine {
	
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
	
	public NegationViewer(Map<Integer, Label> labels){
		super(null);
		this.labels = labels;
	}
	
	
	public NegationViewer(InstanceParser instanceParser) {
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
		int pos = size - ids[0]; // position
		int tag_id = ids[1];
		int nodeType = ids[2];
		
		String latent = "";
		if (ids.length > 3) {
			latent = ids[3] + "";
		}

		String label =  Arrays.toString(ids);
		
		if (nodeType == NodeType.Node.ordinal()) {
			if (tag_id == 0) {
				label = (pos < size) ? this.inputs.get(pos)[NegationInstance.FEATURE_TYPES.word.ordinal()] : "<END>";
			} else
				label = NegationCompiler.LABELS[tag_id].getForm();
			
			label += latent;
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
				int pos = size - ids[0]; // position
				int tag_id = ids[1];
				int nodeType = ids[2];
				
				
				if(nodeType == NodeType.Root.ordinal()){
					node.color = colorMap[8];
					
				} else if (nodeType == NodeType.X.ordinal()) {
					node.color = colorMap[3];
				}
				else
				{
					String tag = NegationCompiler.LABELS[tag_id].getForm();
					if (tag.startsWith("O")) {
						node.color = colorMap[0];
					} else {
						node.color = colorMap[2];
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
			int size = instance.size();
			int pos = size - ids[0]; // position
			int tag_id = ids[1];
			int nodeType = ids[2];
			
			
			double x = pos * span_width;
			int mappedId = tag_id;
			if (nodeType == NodeType.Node.ordinal())
			{
				mappedId = tag_id;
			}
			
			double y = mappedId * span_height + offset_height +( (ids.length > 3) ? -30 + 10 * ids[3] : 0);
			
			if(nodeType == NodeType.Root.ordinal()){
				x = (pos - 1) * span_width;
				y = 3 * span_height + offset_height;
			}
			else if (nodeType == NodeType.X.ordinal()){
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
