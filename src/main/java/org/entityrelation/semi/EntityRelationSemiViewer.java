package org.entityrelation.semi;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.entityrelation.common.EntityRelationCompiler;
import org.entityrelation.common.EntityRelationCompiler.NodeType;
import org.entityrelation.common.EntityRelationGlobal;
import org.entityrelation.common.EntityRelationInstance;
import org.entityrelation.common.EntityRelationOutput;
import org.statnlp.commons.types.Label;
import org.statnlp.hypergraph.NetworkIDMapper;
import org.statnlp.ui.visualize.type.VNode;
import org.statnlp.ui.visualize.type.VisualizationViewerEngine;
import org.statnlp.ui.visualize.type.VisualizeGraph;
import org.statnlp.util.instance_parser.DelimiterBasedInstanceParser;
import org.statnlp.util.instance_parser.InstanceParser;



public class EntityRelationSemiViewer extends VisualizationViewerEngine {
	
	static double span_width = 100;

	static double span_height = 100;
	
	static double offset_width = 100;
	
	static double offset_height = 100;
	
	/**
	 * The list of instances to be visualized
	 */
	protected EntityRelationInstance<Integer> instance;
	
	protected ArrayList<String[]> inputs;
	
	protected EntityRelationOutput outputs;
	
	/**
	 * The list of labels to be used in the visualization.<br>
	 * This member is used to support visualization on those codes that do not utilize
	 * the generic pipeline. In the pipeline, use {@link #instanceParser} instead.
	 */
	protected Map<Integer, Label> labels;
	
	public EntityRelationSemiViewer(Map<Integer, Label> labels){
		super(null);
		this.labels = labels;
	}
	
	
	public EntityRelationSemiViewer(InstanceParser instanceParser) {
		super(instanceParser);
	}
	
	@SuppressWarnings("unchecked")
	protected void initData()
	{
		this.instance = (EntityRelationInstance<Integer>)super.instance;
		this.inputs = (ArrayList<String[]>)super.inputs;
		this.outputs = (EntityRelationOutput)super.outputs;
		this.Root = EntityRelationCompiler.getNode_Root(inputs.size());
		//WIDTH = instance.Length * span_width;
	}
	
	@Override
	protected String label_mapping(VNode node) {
		int[] ids = node.ids;
		int size = instance.size();
		int i = size - ids[0];
		int k = size - ids[1];
		int nodeType = ids[2];
		int iFrom = size - ids[3];
		
		int r = ids[5];
		int t1 = ids[6];
		int t2 = ids[7];

		String label = i + "," + k ;//Arrays.toString(ids);
		
		
		
		
		if (nodeType == NodeType.T.ordinal()) {
			label = EntityRelationOutput.getRELATIONS(r).form + "-" + EntityRelationOutput.getENTITY(t1).form + "-" + EntityRelationOutput.getENTITY(t2).form;
			label = Arrays.toString(ids);
		} else if (nodeType == NodeType.Root.ordinal()) {
			label = "<Root>";
		} else if (nodeType == NodeType.X.ordinal()) {
			label = "<X>";
		} else if (nodeType == NodeType.E.ordinal()) {
			
		} else if (nodeType == NodeType.I1.ordinal()) {
			label = this.inputs.get(i)[0] + " " + Arrays.toString(ids);
		} else if (nodeType == NodeType.I2.ordinal()) {
			label = this.inputs.get(k)[0] + " " + Arrays.toString(ids);;
		} else if (nodeType == NodeType.A.ordinal()) {
			if (i == 0) {
				label = this.inputs.get(k)[0];
			} else if (k == 0){
				label = this.inputs.get(i)[0];
			}
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
				int i = size - ids[0];
				int k = size - ids[1];
				int nodeType = ids[2];
				int r = ids[5];
				int t1 = ids[6];
				int t2 = ids[7];
				
				
				if(nodeType == NodeType.Root.ordinal() || nodeType == NodeType.X.ordinal()){
					node.color = colorMap[8];
					
				} else if (nodeType == NodeType.A.ordinal()) {
					node.color = Color.white;
				} else if (nodeType == NodeType.E.ordinal()) {
					node.color = Color.cyan;
				} else if (nodeType == NodeType.T.ordinal()) {
					node.color = Color.green;
				} else if (nodeType == NodeType.I1.ordinal()) {
					node.color = Color.orange;
				}else if (nodeType == NodeType.I2.ordinal()) {
					node.color = Color.magenta;
				} else
				{
					node.color = Color.gray;
					
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
			int i = size - ids[0];
			int k = size - ids[1];
			int nodeType = ids[2];
			int r = ids[5];
			int t1 = ids[6];
			int t2 = ids[7];
			int iFrom = size -ids[3];
			int kFrom = size - ids[4];
			
			int tripleIdx = EntityRelationGlobal.getTripleIdx(new int[] {r, t1, t2});
			
			double x = i * span_width;
			double y = k * span_height;
			
			if(nodeType == NodeType.Root.ordinal()){
				x = -1 * span_width;
				y = -1 * span_height + offset_height;
			} else if (nodeType == NodeType.X.ordinal()){
				x = (size + 20) * span_width;
				y = (size + 20) * span_height;
			} else if (nodeType == NodeType.A.ordinal()) {
			
			} else if (nodeType == NodeType.E.ordinal()) {
				x += (size ) * span_width;
				y += (size + 2) * span_height;
			} else if (nodeType == NodeType.T.ordinal()){
				x += (tripleIdx * 2 + size) * span_width + 10 * r + 100;
				y += (size * 2) * span_height + 10 * r ;
			} else if (nodeType == NodeType.I1.ordinal()) {
				x += tripleIdx * 2 * span_width + span_width + span_width + 10 * r + 10 * i + 10 * iFrom;
				y += (size * 3) * span_height + span_height + 10 * r + 10 * iFrom;
			} else if (nodeType == NodeType.I2.ordinal()) {
				x += tripleIdx * 2 * span_width - 50 + span_width + span_height - 10 * r - 10 * i + 10 * kFrom;
				y += (size * 3) * span_height + 50 + span_height - 10 * r + 10 * kFrom;
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
