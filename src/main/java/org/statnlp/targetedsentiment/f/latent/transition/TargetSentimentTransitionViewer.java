package org.statnlp.targetedsentiment.f.latent.transition;

import java.awt.Color;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;

import org.statnlp.commons.types.Label;
import org.statnlp.targetedsentiment.common.TSInstance;
import org.statnlp.targetedsentiment.f.latent.transition.TargetSentimentTransitionCompiler.*;
import org.statnlp.ui.visualize.type.VNode;
import org.statnlp.ui.visualize.type.VisualizationViewerEngine;
import org.statnlp.ui.visualize.type.VisualizeGraph;
import org.statnlp.util.instance_parser.InstanceParser;

public class TargetSentimentTransitionViewer extends VisualizationViewerEngine {

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

	int PolarityTypeSize = PolarityType.values().length;
	int SubNodeTypeSize = SubNodeType.values().length;
	
	Color[] polarColor = new Color[]{Color.RED, Color.LIGHT_GRAY, Color.GREEN};

	/**
	 * The list of labels to be used in the visualization.<br>
	 * This member is used to support visualization on those codes that do not
	 * utilize the generic pipeline. In the pipeline, use
	 * {@link #instanceParser} instead.
	 */
	protected Map<Integer, Label> labels;

	public TargetSentimentTransitionViewer(Map<Integer, Label> labels) {
		super(null);
		this.labels = labels;

	}

	public TargetSentimentTransitionViewer(InstanceParser instanceParser) {
		super(instanceParser);
		// potentialSentWord = instance.potentialSentWord;
	}

	@SuppressWarnings("unchecked")
	protected void initData() {
		this.instance = (TSInstance<Label>) super.instance;
		this.inputs = (ArrayList<String[]>) super.inputs;
		this.outputs = (ArrayList<Label>) super.outputs;
		this.size = instance.size();
		// WIDTH = instance.Length * span_width;
		potentialSentWord = instance.potentialSentWord;
	}

	@Override
	protected String label_mapping(VNode node) {
		int[] ids = node.ids;

		int pos = size - ids[0];
		int polar = PolarityTypeSize - ids[1];
		int subnode = SubNodeTypeSize - ids[2];
		int nodeType = ids[4];

		String label = Arrays.toString(ids);

		if (nodeType == NodeType.Start.ordinal()) {
			label = "<Root>";
		} else if (nodeType == NodeType.End.ordinal()) {
			label = "X";
		} else { //Span
			if (subnode == SubNodeType.NULL.ordinal()) {
				label = "NULL-" + new String[] { "+", "0", "-" }[polar];
			} else if (subnode == SubNodeType.e.ordinal()) {
				label = inputs.get(pos)[0];
			} else {
				label = SubNodeType.values()[subnode].name();// + "-" + new String[] { "+", "0", "-" }[polar];
			}
		}

		return label;
	}

	protected void initNodeColor(VisualizeGraph vg) {
		if (colorMap != null) {
			for (VNode node : vg.getNodes()) {
				int[] ids = node.ids;
				int pos = size - ids[0];
				int polar = PolarityTypeSize - ids[1];
				int subnode = SubNodeTypeSize - ids[2];
				int nodeType = ids[4];


				if (nodeType == NodeType.Start.ordinal() || nodeType == NodeType.End.ordinal()) {

					node.color = colorMap[8];
				} else {
					if (subnode == SubNodeType.NULL.ordinal()) {
						node.color = Color.PINK;
					} else {
						node.color = polarColor[polar];
						
						
						if (subnode == SubNodeType.e.ordinal()) {
							node.color = Color.WHITE;
						}
					}
					
					
					/*else if (SubNodeType.values()[subnode].name().startsWith("B")) {
						node.color = colorMap[3];
					} else if (SubNodeType.values()[subnode].name().startsWith("A")) {
						node.color = colorMap[4];
					} else if (SubNodeType.values()[subnode].name().startsWith("e")) {
						node.color = colorMap[5];
					}*/
					
				}

			}
		}

	}

	protected void initNodeCoordinate(VisualizeGraph vg) {
		for (VNode node : vg.getNodes()) {
			int[] ids = node.ids;
			int pos = size - ids[0];
			int polar = PolarityTypeSize - ids[1];
			int subnode = SubNodeTypeSize - ids[2];
			int nodeType = ids[4];
			
			

			double x = pos * span_width;

			double y = subnode * (span_height * 0.5) + polar * (span_height * 10);

			if (nodeType == NodeType.Start.ordinal()) {
				x = (-2) * span_width;
				y = 3 * span_height + offset_height;
			} else if (nodeType == NodeType.End.ordinal()) {
				x = (size + 1) * span_width;
				y = 3 * span_height + offset_height;
			} else {
				 if (subnode == SubNodeType.NULL.ordinal()) {
					x = (-1) * span_width;
					y = 6 * span_height + polar * offset_height;
				} else if (SubNodeType.values()[subnode].name().startsWith("B")) {
					x -= 5;
				} else if (SubNodeType.values()[subnode].name().startsWith("A")) {
					x += 5;
				}
			}
			

			node.point = new Point2D.Double(x, y);
			layout.setLocation(node, node.point);
			layout.lock(node, true);
		}
	}

}
