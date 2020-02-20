package org.statnlp.example.descriptor;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.statnlp.commons.types.Sentence;

public class CandidatePair {

	public Sentence sent;
	
	/**
	 * Only consider the entity span.
	 */
	public List<Span> spans;
	
	/**
	 * The index is the index in the span.
	 */
	public int leftSpanIdx;
	public int rightSpanIdx;
	
	public int a1HeadIdx;
	public int a2HeadIdx;
	
	/**
	 * The maximum sentence length for the current full set of data.
	 */
	public int maxSentLen;
	
	public int[] sdps;
	public boolean isContinuous = true;
	public boolean hasDes = false;
	
	/**
	 * Input Class to logistic regression model.
	 * @param sent
	 * @param spans
	 * @param leftSpanIdx: may not be the arg1Idx, depends on the label
	 * @param rightSpanIdx: may not be the arg2idx, depends on the label
	 */
	public CandidatePair(Sentence sent, List<Span> spans, int leftSpanIdx, int rightSpanIdx) {
		this.sent = sent;
		this.spans = spans;
		this.leftSpanIdx = leftSpanIdx;
		this.rightSpanIdx = rightSpanIdx;
		for (Span span : this.spans) {
			span.headIdx = this.findHeadIdx(sent, span);
		}
		this.getShortestDependencyPath();
	}
	
	private int findHeadIdx (Sentence sent, Span span) {
		Set<Integer> heads = new HashSet<>();
		int currHeadIdx = -1;
		//determine by dependencies
		for (int i = span.start; i <= span.end; i++) {
			int headIdx = sent.get(i).getHeadIndex();
			if (headIdx >= span.start && headIdx <= span.end) {
				currHeadIdx = headIdx;
				heads.add(headIdx);
			}
		}
		if (heads.size() == 1) {
			return currHeadIdx;
		} else {
			//a mention is from the start to the head end according to Zhou 2005
			for (int i = span.start; i <= span.end; i++) {
				if (i > span.start && sent.get(i).getTag().equals("IN")) {
					return i - 1;
				}
			}
			return span.end;
		}
	}
	
	private void getShortestDependencyPath() {
		Graph<Integer, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);
		int[] des = new int[sent.length()];
		for (int i = 0; i < sent.length(); i++) {
			g.addVertex(i);
		}
		for (int i = 0; i < sent.length(); i++) {
			int head = sent.get(i).getHeadIndex();
			if (head != -1) {
				g.addEdge(i, head);
			}
		}
		Span arg1Span = this.spans.get(leftSpanIdx);
		Span arg2Span = this.spans.get(rightSpanIdx);
		DijkstraShortestPath<Integer, DefaultEdge> dg = new DijkstraShortestPath<>(g);
		GraphPath<Integer, DefaultEdge>  results = dg.getPath(arg1Span.headIdx, arg2Span.headIdx);
		List<Integer> list =  results.getVertexList();
		StringBuilder sb = new StringBuilder();
		for (int v : list) {
			if (v >= arg1Span.start && v <= arg1Span.end) continue;
			if (v >= arg2Span.start && v <= arg2Span.end) continue;
			if (v == arg1Span.headIdx || v == arg2Span.headIdx) continue;
			des[v] = 1;
			sb.append(sent.get(v).getForm() + " ");
		}
		this.sdps = des;
		int lb = -1;
		int rb = -1;
		int sum = 0;
		for (int i = 0; i < this.sdps.length; i++) {
			if (sdps[i] == 1) {lb = i; break;}
		}
		for (int i = this.sdps.length - 1; i >= 0; i--) {
			if (sdps[i] == 1) {rb = i; break;}
		}
		for (int i = 0; i < this.sdps.length; i++) {
			sum += sdps[i];
		}
		if (lb >= 0) this.hasDes = true;
		if (rb - lb  + 1 == sum) {
			this.isContinuous = true;
		} else {
			this.isContinuous = false;
		}
	}
	
}
