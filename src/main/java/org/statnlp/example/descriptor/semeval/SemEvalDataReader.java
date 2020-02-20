package org.statnlp.example.descriptor.semeval;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.statnlp.commons.io.RAWF;
import org.statnlp.commons.types.DependencyTree;
import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Sentence;
import org.statnlp.commons.types.WordToken;
import org.statnlp.example.descriptor.CandidatePair;
import org.statnlp.example.descriptor.RelInstance;
import org.statnlp.example.descriptor.RelationDescriptor;
import org.statnlp.example.descriptor.RelationType;
import org.statnlp.example.descriptor.Span;

public class SemEvalDataReader {

	public int maxSentLen;
	
	public RelInstance[] read(String file, boolean isTrainingInsts, int number, String specificRel) {
		return read(file, isTrainingInsts, number, false, specificRel, false);
	}
	
	public RelInstance[] read(String file, boolean isTrainingInsts, int number) {
		return read(file, isTrainingInsts, number, false, null, false);
	}
	
	public RelInstance[] read(String file, boolean isTrainingInsts, int number, boolean alwaysLastAsHead, String specificRel,
			boolean useGenericRelation) {
		List<RelInstance> insts = new ArrayList<RelInstance>();
		BufferedReader br;
		Set<String> sentSet = new HashSet<>();
		this.maxSentLen = 0;
		try {
			br = RAWF.reader(file);
			String line = null;
			int instId = 1;
			int maxSentSize = -1;
			int maxE1 = -1;
			int maxE2 = -1;
			while ((line = br.readLine()) != null) {
				String[] vals = line.split(" ");
				String tagSeq = br.readLine();
				String[] tagVals = tagSeq.split(" ");
				String headIdxs = br.readLine();
				String[] headIdxArr = headIdxs.split(" ");
				String headLabelSeq = br.readLine();
				String[] headLabels = headLabelSeq.split(" ");
				String hypernymSeq = br.readLine();
				String[] hypernyms = hypernymSeq.split(" ");
				String nerSeq = br.readLine();
				String[] ners = nerSeq.split(" ");
				String entSeq = br.readLine();
				String relation = br.readLine();
				if (useGenericRelation) {
					relation = "Generic";
//					if (relation.endsWith(LinearConfig.REV_SUFFIX)) 
//						relation += "Generic"+LinearConfig.REV_SUFFIX;
				}
				WordToken[] wts = new WordToken[vals.length];
				for (int p = 0; p < wts.length; p++) {
					wts[p] = new WordToken(vals[p], tagVals[p], Integer.valueOf(headIdxArr[p]), ners[p], headLabels[p], hypernyms[p]);
				}
				Sentence sent = new Sentence(wts);
				sent.depTree = new DependencyTree(sent);
				String[] entIdxs = entSeq.split(" ");
				String[] ent1Idxs = entIdxs[0].split(",");
				String[] ent2Idxs = entIdxs[1].split(",");
				int e1Start = Integer.valueOf(ent1Idxs[0]);
				int e1End = Integer.valueOf(ent1Idxs[1]);
				maxE1 = Math.max(maxE1, e1End - e1Start + 1);
				int e2Start = Integer.valueOf(ent2Idxs[0]);
				int e2End = Integer.valueOf(ent2Idxs[1]);
				maxE2 = Math.max(maxE2, e2End - e2Start + 1);
				Span leftSpan = new Span(e1Start, e1End, "entity");
				this.setHeadIdx(sent, leftSpan, alwaysLastAsHead);
				Span rightSpan = new Span(e2Start, e2End, "entity");
				this.setHeadIdx(sent, rightSpan, alwaysLastAsHead);
				List<Span> spans = new ArrayList<>(2);
				spans.add(leftSpan);
				spans.add(rightSpan);
				CandidatePair cp = new CandidatePair(sent, spans, 0, 1);
				sentSet.add(sent.toString());
				if (specificRel != null && !relation.startsWith(specificRel)) {
					br.readLine();//empty line
					continue;
				}
				RelationDescriptor descriptor = new RelationDescriptor(RelationType.get(relation));
				RelInstance inst = new RelInstance(instId, 1.0, cp, descriptor);
				maxSentSize = Math.max(maxSentSize, inst.size());
				if (isTrainingInsts) {
					inst.setLabeled();
				} else {
					inst.setUnlabeled();
				}
				insts.add(inst);
				if (insts.size() == number) {
					break;
				}
				instId++;
				br.readLine(); //empty line;
			}
			br.close();
			System.out.println("[Info] number of unique sentences: " + sentSet.size());
			sentSet.clear();
			sentSet = null;
			System.out.println("[Info] maximum size of sentence: " + maxSentSize);
			maxSentLen = maxSentSize;
			System.out.println("[Info] number of instances read: " + insts.size());
			System.out.println("[Info] maximum e1 length: " + maxE1);
			System.out.println("[Info] maximum e2 length: " + maxE2);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return insts.toArray(new RelInstance[insts.size()]);
	}
	
	private void setHeadIdx (Sentence sent, Span span, boolean lastWordAsHead) {
		//a mention is from the start to the head end according to Zhou 2005
		if (!lastWordAsHead) {
			for (int i = span.start; i <= span.end; i++) {
				if (i > span.start && sent.get(i).getTag().equals("IN")) {
					span.headIdx = i -1;
					return;
				}
			}
		}
		span.headIdx = span.end;
	}
	
	public void putInMaxLen(Instance[] insts) {
		for (Instance inst : insts) {
			RelInstance relInst = (RelInstance)inst;
			relInst.input.maxSentLen = this.maxSentLen;
		}
	}
	
}
