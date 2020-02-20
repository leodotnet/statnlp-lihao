package org.statnlp.example.descriptor.semeval;

import java.util.Arrays;
import java.util.List;

import org.statnlp.commons.types.Instance;
import org.statnlp.example.base.BaseNetwork;
import org.statnlp.example.base.BaseNetwork.NetworkBuilder;
import org.statnlp.example.descriptor.CandidatePair;
import org.statnlp.example.descriptor.RelInstance;
import org.statnlp.example.descriptor.RelationDescriptor;
import org.statnlp.example.descriptor.RelationType;
import org.statnlp.example.descriptor.Span;
import org.statnlp.hypergraph.LocalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkCompiler;
import org.statnlp.hypergraph.NetworkIDMapper;
import org.statnlp.util.Pipeline;

public class SemEvalLRNetworkCompiler extends NetworkCompiler {

	private static final long serialVersionUID = -7882408904222741413L;

	public enum NodeType {LEAF, SPAN, NODE, ROOT};
	private boolean DEBUG = false;
	private boolean enableDescriptorArgRel = true; //set this to false to use extend variant
	private boolean descriptorBetweenArgs = false; //means can contain or not contain.
	private boolean descriptorCoverArgs = true; //two choices, either cover or not.
	private int leftRightBound = -1;
	private int maxSpanLen = -1;
	
	static {
		//nodeType, leftIndex, rightIndex, relation type.
		NetworkIDMapper.setCapacity(new int[]{4, 200, 200, 120});
	}
	
	public SemEvalLRNetworkCompiler(boolean enableDescriptorArgRel, boolean descriptorBetweenArgs, boolean descriptorCoverArgs, int leftRightBound, int maxSpanLen) {
		this.enableDescriptorArgRel = enableDescriptorArgRel;
		this.descriptorBetweenArgs = descriptorBetweenArgs;
		this.descriptorCoverArgs = descriptorCoverArgs;
		this.leftRightBound = leftRightBound;
		this.maxSpanLen = maxSpanLen;
	}

	private long toNode_leaf() {
		return toNode(NodeType.LEAF, 0, 0, 0); 
	}
	
	private long toNode_Span(int leftIndex, int rightIndex){
		return toNode(NodeType.SPAN, leftIndex, rightIndex, 0);
	}
	
	private long toNode_Rel(int label) {
		return toNode(NodeType.NODE, 0, 0, label);
	}
	
	private long toNode_root() {
		return toNode(NodeType.ROOT, 0, 0, 0);
	}
	
	private long toNode(NodeType nodeType, int leftIndex, int rightIndex, int labelId) {
		return NetworkIDMapper.toHybridNodeID(new int[]{nodeType.ordinal(), leftIndex, rightIndex, labelId});
	}
	
	public SemEvalLRNetworkCompiler(Pipeline<?> pipeline) {
		super(pipeline);
	}

	@Override
	public Network compileLabeled(int networkId, Instance inst, LocalNetworkParam param) {
		NetworkBuilder<BaseNetwork> builder = NetworkBuilder.builder(BaseNetwork.class);
		RelInstance lgInst = (RelInstance)inst;
		CandidatePair input = lgInst.getInput();
		long leaf = toNode_leaf();
		builder.addNode(leaf);
		long node = toNode_Rel(lgInst.getOutput().getType().id);
		builder.addNode(node);
		List<Span> spans = input.spans;
		int leftIdx = input.leftSpanIdx;
		Span leftSpan = spans.get(leftIdx);
		int rightIdx = input.rightSpanIdx;
		Span rightSpan = spans.get(rightIdx); 	
		for (int pos = 0; pos < lgInst.size(); pos++) {
			int end = lgInst.size();
			if (maxSpanLen > 0) {
				end = Math.min(pos + maxSpanLen -1 , end);
			}
			for (int right = pos; right < end; right++) {
				boolean overlapLeft = this.overlap(leftSpan, pos, right);
				boolean overlapRight = this.overlap(rightSpan, pos, right);
				if (this.enableDescriptorArgRel) {
					if (!descriptorCoverArgs) {
						if (overlapLeft || overlapRight) 
							continue;
					} else {
						//this means have to cover the two
						if (!overlapLeft) continue;
						if (!overlapRight) continue;
					}
					if (descriptorBetweenArgs) {
						if (pos < leftSpan.start || right > rightSpan.end) continue;
					}
				} else {
					if (this.leftRightBound <= 0) throw new RuntimeException("enabling the extend model but no bound?");
					if (pos < leftSpan.start - this.leftRightBound) continue;
					if (right > rightSpan.end + this.leftRightBound) continue;
					if (pos == leftSpan.start && right == leftSpan.end) continue;
					if (pos == rightSpan.start && right == rightSpan.end) continue;
				}
				long spanNode = this.toNode_Span(pos, right);
				builder.addNode(spanNode);
				builder.addEdge(spanNode, new long[]{leaf});
				builder.addEdge(node, new long[]{spanNode});
			}
		}
		builder.addEdge(node, new long[]{leaf});
		long root = toNode_root();
		builder.addNode(root);
		builder.addEdge(root, new long[]{node});
		BaseNetwork network = builder.build(networkId, inst, param, this);
		if (DEBUG) {
			BaseNetwork unlabeled = this.compileUnlabeled(networkId, inst, param);
			if(!unlabeled.contains(network))
				System.err.println("not contains");
		}
		return network;
	}

	@Override
	public BaseNetwork compileUnlabeled(int networkId, Instance inst, LocalNetworkParam param) {
		NetworkBuilder<BaseNetwork> builder = NetworkBuilder.builder(BaseNetwork.class);
		RelInstance lgInst = (RelInstance)inst;
		CandidatePair input = lgInst.getInput();
		long leaf = toNode_leaf();
		builder.addNode(leaf);
		long root = toNode_root();
		builder.addNode(root);
		List<Span> spans = input.spans;
		int leftIdx = input.leftSpanIdx;
		Span leftSpan = spans.get(leftIdx);
		int rightIdx = input.rightSpanIdx;
		Span rightSpan = spans.get(rightIdx); 	
		for (int l = 0; l < RelationType.RELS.size(); l++) {
			long node = this.toNode_Rel(l);
			builder.addNode(node);
			for (int pos = 0; pos < inst.size(); pos++) {
				int end = lgInst.size();
				if (maxSpanLen > 0) {
					end = Math.min(pos + maxSpanLen -1 , end);
				}
				for (int right = pos; right < end; right++) {
					boolean overlapLeft = this.overlap(leftSpan, pos, right);
					boolean overlapRight = this.overlap(rightSpan, pos, right);
					if (this.enableDescriptorArgRel) {
						if (!descriptorCoverArgs) {
							if (overlapLeft || overlapRight) 
								continue;
						} else {
							//this means have to cover the two
							if (!overlapLeft) continue;
							if (!overlapRight) continue;
						}
						if (descriptorBetweenArgs) {
							if (pos < leftSpan.start || right > rightSpan.end) continue;
						}
					}else {
						if (this.leftRightBound <= 0) throw new RuntimeException("enabling the extend model but no bound?");
						if (pos < leftSpan.start - this.leftRightBound) continue;
						if (right > rightSpan.end + this.leftRightBound) continue;
						if (pos == leftSpan.start && right == leftSpan.end) continue;
						if (pos == rightSpan.start && right == rightSpan.end) continue;
					}
					long spanNode = this.toNode_Span(pos, right);
					builder.addNode(spanNode);
					builder.addEdge(spanNode, new long[]{leaf});
					builder.addEdge(node, new long[]{spanNode});
				}
			}
			builder.addEdge(node, new long[]{leaf});
			builder.addEdge(root, new long[]{node});
		}
		return builder.build(networkId, inst, param, this);
	}
	
	private boolean overlap(Span span, int dl, int dr) {
		if (dl > span.end) return false;
		if (dr < span.start) return false;
		return true;
	}

	@Override
	public Instance decompile(Network network) {
		BaseNetwork baseNetwork = (BaseNetwork)network;
		RelInstance inst = (RelInstance)network.getInstance();
		long node = this.toNode_root();
		int nodeIdx = Arrays.binarySearch(baseNetwork.getAllNodes(), node);
		int labeledNodeIdx = baseNetwork.getMaxPath(nodeIdx)[0];
		int[] arr = baseNetwork.getNodeArray(labeledNodeIdx);
		int labelId = arr[3];
		RelationType predType = RelationType.get(labelId);
		nodeIdx = baseNetwork.getMaxPath(labeledNodeIdx)[0];
		arr = baseNetwork.getNodeArray(nodeIdx);
		int left = arr[1];
		int right = arr[2];
		if (arr[0] == NodeType.LEAF.ordinal()) {
			left = -1;
			right = -1;
		}
		RelationDescriptor prediction = new RelationDescriptor(predType, left, right);
		inst.setPrediction(prediction);
		return inst;
	}

}
