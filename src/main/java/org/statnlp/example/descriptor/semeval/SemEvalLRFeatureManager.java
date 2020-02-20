package org.statnlp.example.descriptor.semeval;

import java.util.ArrayList;
import java.util.List;

import org.statnlp.commons.types.Sentence;
import org.statnlp.commons.types.WordToken;
import org.statnlp.example.descriptor.CandidatePair;
import org.statnlp.example.descriptor.Config;
import org.statnlp.example.descriptor.RelInstance;
import org.statnlp.example.descriptor.RelationType;
import org.statnlp.example.descriptor.Span;
import org.statnlp.example.descriptor.emb.WordEmbedding;
import org.statnlp.example.descriptor.semeval.SemEvalLRMain.NeuralType;
import org.statnlp.example.descriptor.semeval.SemEvalLRNetworkCompiler.NodeType;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.FeatureManager;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkConfig;

import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;

public class SemEvalLRFeatureManager extends FeatureManager {

	private static final long serialVersionUID = -4782588008491511674L;

	private enum FeaType {
		word,
		tag,
		bigram_word,
		bigram_tag,
		fo_word,
		fo_tag,
		fo_phrase,
		fo_bigram_word,
		fo_bigram_tag,
		transition,
		contextual,
		path,
		bow,
		a1, a1w, a2, a2w, a1a2w, betw, a1t, a2t, a1a2t, a1left, a1right, a2left, a2right,
		dist,bettseq, loutside, routside, pref5bet,
		depWord, depPath, depLabel,desugs,desbgs,destgs,
		a1hypernym, a2hypernym, bethypernym, a1a2hypernym, a1ner, a2ner, betner, emb, spanLen}
	
	private boolean useGeneralTags = true; //true gives us higher performance
	private boolean dependencyFeatures = true;
	private boolean useRelDiscreteFeatures = true;
	private boolean useSimpleRelFeatures = true;
	private NeuralType nnType = null;
	private boolean usePositionEmbedding = true;
	private boolean toLowercase = true;
	private boolean zero_digit = true;
	private boolean descritorFeature = false;
	private boolean nnDescriptorFeature = false;
	//surrounding words for the neural/continuous features
	private boolean surroundingWords = false; 
	private boolean hypernymFeat = false;
	private boolean unkDescriptorAsSent = false;
	private boolean useHeadWord = false;
	private boolean useBetSentOnly = false;
	private boolean positionIndicator = false;
	private boolean extentModel = false;
	protected transient WordEmbedding emb;
	
	public SemEvalLRFeatureManager(GlobalNetworkParam param_g, boolean useRelDiscreteFeatures, boolean useGeneralTags, boolean depf,
			NeuralType nnType, boolean usePositionEmbedding, boolean toLowercase, boolean zero_digit, 
			boolean descriptorFeature, boolean nnDescriptorFeature, boolean surroundingWords, boolean useSimpleRelFeatures,
			boolean hypernymFeat, boolean unkDescriptorAsSent, boolean useHeadWord, boolean useBetSentOnly, 
			boolean positionIndicator, boolean extentModel, WordEmbedding emb) {
		super(param_g);
		this.useRelDiscreteFeatures = useRelDiscreteFeatures;
		this.useGeneralTags = useGeneralTags;
		this.dependencyFeatures = depf;
		this.nnType = nnType;
		this.usePositionEmbedding = usePositionEmbedding;
		this.toLowercase = toLowercase;
		this.zero_digit = zero_digit;
		this.descritorFeature = descriptorFeature;
		this.nnDescriptorFeature = nnDescriptorFeature;
		this.surroundingWords = surroundingWords;
		this.useSimpleRelFeatures = useSimpleRelFeatures;
		this.hypernymFeat = hypernymFeat;
		this.unkDescriptorAsSent = unkDescriptorAsSent;
		this.useHeadWord = useHeadWord;
		this.useBetSentOnly = useBetSentOnly;
		this.positionIndicator = positionIndicator;
		this.extentModel = extentModel;
		this.emb = emb;
	}
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k, int children_k_index) {
		int[] paArr = network.getNodeArray(parent_k);
		int nodeType = paArr[0];
		if (nodeType != NodeType.NODE.ordinal())
			return FeatureArray.EMPTY;
		RelInstance inst = (RelInstance)network.getInstance();
		CandidatePair input = inst.getInput();
		Sentence sent = input.sent;

		int leftSpanIdx = input.leftSpanIdx;
		int rightSpanIdx = input.rightSpanIdx;
		int arg1SpanIdx = leftSpanIdx;
		int arg2SpanIdx = rightSpanIdx;
		Span arg1Span = inst.getInput().spans.get(arg1SpanIdx);
		Span arg2Span = inst.getInput().spans.get(arg2SpanIdx);
		FeatureArray startFa = this.createFeatureArray(network, new int[0]);
		FeatureArray currFa = startFa;
		int relId = paArr[3];
		String relForm = RelationType.get(relId).form;
		int[] child = network.getNodeArray(children_k[0]);
		int dstart = child[1];
		int dend = child[2];
		if (child[0] == NodeType.LEAF.ordinal()) {
			dstart = -1;
			dend = -1;
		}
		if (this.useRelDiscreteFeatures) {
			if (this.useSimpleRelFeatures) {
				currFa = this.addSimpleRelFeatures(currFa, network, sent, arg1Span, arg2Span, relForm, parent_k, children_k_index,
						dstart, dend);
			} else {
				currFa = this.addRelFeatures(currFa, network, sent, arg1Span, arg2Span, relForm, parent_k, children_k_index,
						dstart, dend);
			}
			if (this.hypernymFeat) {
				currFa = this.addHypernymFeatures(currFa, network, sent, arg1Span, arg2Span, relForm, parent_k, children_k_index,
						dstart, dend);
			}
			currFa = this.addNERFeatures(currFa, network, sent, arg1Span, arg2Span, relForm, parent_k, children_k_index,
					dstart, dend);
		}
		if (this.descritorFeature) {
			currFa = extentModel ? this.addDescriptorFeatureForExtent(currFa, network, sent, dstart, dend, relForm, arg1Span, arg2Span) :
					this.addDescriptorFeature(currFa, network, sent, dstart, dend, relForm, arg1Span, arg2Span);
		}
			
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			this.addFirstNeuralFeatures(network, parent_k, children_k_index, relId, sent, arg1Span, arg2Span, dstart, dend, input);
			if (this._param_g.getNNParamG().getAllNets().size() > 1) {
				this.addSecondNeuralFeatures(network, parent_k, children_k_index, relId, sent, arg1Span, arg2Span, dstart, dend, input);
			}
		}
		return startFa;
	}
	
	private void addSecondNeuralFeatures(Network network, int parent_k, int children_k_index, 
			int relId, Sentence sent, Span arg1Span, Span arg2Span, int dstart, int dend, CandidatePair input) {
		
	}
	
	private void addFirstNeuralFeatures(Network network, int parent_k, int children_k_index, 
			int relId, Sentence sent, Span arg1Span, Span arg2Span, int dstart, int dend, CandidatePair input) {
		if (nnType == NeuralType.continuous || nnType == NeuralType.mlp) {
			StringBuilder edgeInputBuilder = new StringBuilder();
			if (surroundingWords && nnType != NeuralType.mlp) {
				String a1LeftWord = arg1Span.start - 1 >= 0 ? sent.get(arg1Span.start - 1).getForm() : "<START>";
				edgeInputBuilder.append(a1LeftWord + Config.NEURAL_SEP);
			}
			StringBuilder a1Seq = new StringBuilder("");
			if (this.useHeadWord) {
				String a1Head = sent.get(arg1Span.headIdx).getForm();
				a1Seq.append(a1Head);
			} else {
				for (int i = arg1Span.start; i <= arg1Span.end; i++) {
					String curr_word = sent.get(i).getForm();
					a1Seq.append(i == arg1Span.start? curr_word : " " + curr_word);
				}
			}
			edgeInputBuilder.append(a1Seq.toString() + Config.NEURAL_SEP);
			if (surroundingWords && nnType != NeuralType.mlp) {
				String a1RightWord = sent.get(arg1Span.end + 1).getForm();
				edgeInputBuilder.append(a1RightWord + Config.NEURAL_SEP);
			}
			if (this.nnDescriptorFeature) {
				StringBuilder des = new StringBuilder("");
				if (dstart == -1) des.append("UNKNOWN_DESCRIPTOR");
				else {
					for (int i = dstart; i <= dend; i++) {
						String curr_word = sent.get(i).getForm();
						des.append(i == dstart ? curr_word : " " + curr_word);
					}
				}
				edgeInputBuilder.append(des.toString() + Config.NEURAL_SEP);
			}
			if(surroundingWords && nnType != NeuralType.mlp) {
				String a2LeftWord = sent.get(arg2Span.start - 1).getForm();
				edgeInputBuilder.append(a2LeftWord + Config.NEURAL_SEP);
			}
			StringBuilder a2Seq = new StringBuilder("");
			if (this.useHeadWord) {
				String a2Head = sent.get(arg2Span.headIdx).getForm();
				a2Seq.append(a2Head);
			} else {
				for (int i = arg2Span.start; i <= arg2Span.end; i++) {
					String curr_word = sent.get(i).getForm();
					a2Seq.append(i == arg2Span.start ? curr_word : " " + curr_word);
				}
			}
			edgeInputBuilder.append(a2Seq.toString());
			if (surroundingWords && nnType != NeuralType.mlp) {
				String a2RightWord = arg2Span.end + 1 < sent.length() ?  sent.get(arg2Span.end + 1).getForm() : "<END>";
				edgeInputBuilder.append(Config.NEURAL_SEP + a2RightWord);
			}
			this.addNeural(network, 0, parent_k, children_k_index, edgeInputBuilder.toString(), relId);
		} else if (nnType == NeuralType.cnn || nnType == NeuralType.cnn_batch_one) {
			String edgeInput = null;
			StringBuilder edgeInputBuilder = new StringBuilder();
			if (usePositionEmbedding) {
				int p1 = -1 - arg1Span.headIdx;
				int p2 = -1 - arg2Span.headIdx;
				edgeInputBuilder.append("<START>"+ Config.NEURAL_SEP + p1 + Config.NEURAL_SEP + p2); 
			}else {
				edgeInputBuilder.append("<START>"); //PADDING is the special token in senna embeddings.
			}
			for (int i = 0; i < sent.length(); i++) {
				String word = sent.get(i).getForm();
				if (toLowercase) word = word.toLowerCase();
				if (zero_digit) word = word.replaceAll("\\d", "0");
				if (usePositionEmbedding) {
					int p1 = i - arg1Span.headIdx;
					int p2 = i - arg2Span.headIdx;
					word = word + Config.NEURAL_SEP + p1 + Config.NEURAL_SEP + p2;
				}
				edgeInputBuilder.append(" " + word);
			}
			if (usePositionEmbedding) {
				int p1 = sent.length() - arg1Span.headIdx;
				int p2 = sent.length() - arg2Span.headIdx;
				edgeInputBuilder.append(" <END>"+ Config.NEURAL_SEP + p1 + Config.NEURAL_SEP + p2); 
			}else {
				edgeInputBuilder.append(" <END>");
			}
			if (nnDescriptorFeature) {
				if (dstart == -1) { //dend is also -1
					dstart = 0;
					dend = sent.length() - 1;
					//if not then return the sentence representation.
				}
				edgeInputBuilder.append(Config.NEURAL_DESC_SEP + (dstart + 1) + Config.NEURAL_DESC_SEP + (dend + 1));
			}
			edgeInput = edgeInputBuilder.toString();
			this.addNeural(network, 0, parent_k, children_k_index, edgeInput, relId);
		} else if (nnType == NeuralType.gru || nnType == NeuralType.lstm) {
			StringBuilder edgeInputBuilder = new StringBuilder();
			int a1Start = arg1Span.start; int a1End = arg1Span.end;
			int a2Start = arg2Span.start; int a2End = arg2Span.end;
			int a1Head = arg1Span.headIdx;
			int a2Head = arg2Span.headIdx;
			if (this.useBetSentOnly) {
				for (int i = arg1Span.start; i <= arg2Span.end; i++) {
					edgeInputBuilder.append(i == arg1Span.start ? sent.get(i).getForm() : " " + sent.get(i).getForm());
				}
				edgeInputBuilder.append(Config.NEURAL_SEP);
				a1Start -= arg1Span.start;  a1End -= arg1Span.start;
				a2Start -= arg1Span.start;  a2End -= arg1Span.start;
				a1Head -= arg1Span.start;   a2Head -= arg1Span.start;
			} else {
				if (this.positionIndicator) {
					for (int i = 0; i < sent.length(); i++) {
						if (arg1Span.start == i) {
							edgeInputBuilder.append(i == 0 ? "<e1>" : " <e1>");
							edgeInputBuilder.append(" " + sent.get(i).getForm());
						}
						if (arg1Span.end == i) {
							if (arg1Span.start != arg1Span.end)
								edgeInputBuilder.append(" " + sent.get(i).getForm());
							edgeInputBuilder.append(" </e1>");
						}
						if (i == arg2Span.start) {
							edgeInputBuilder.append(" <e2>");
							edgeInputBuilder.append(" " + sent.get(i).getForm());
						}
						if (i == arg2Span.end) {
							if (arg2Span.start != arg2Span.end)
								edgeInputBuilder.append(" " + sent.get(i).getForm());
							edgeInputBuilder.append(" </e2>");
						}
						if (i != arg1Span.start && i != arg1Span.end && i != arg2Span.start && i != arg2Span.end) {
							edgeInputBuilder.append(i == 0 ? sent.get(i).getForm() : " " + sent.get(i).getForm());
						}
					}
					edgeInputBuilder.append(Config.NEURAL_SEP);
					a1Start += 1; a1End += 1;
					a2Start += 3; a2End += 3;
					a1Head += 1; a2Head += 3;
				} else {
					edgeInputBuilder.append(sent.toString() + Config.NEURAL_SEP);
				}
			}
			if (this.useHeadWord) {
				edgeInputBuilder.append(a1Head + " ");
			} else {
				edgeInputBuilder.append(a1Start + " " + a1End + " ");
			}
			if (this.nnDescriptorFeature && !this.useHeadWord) {
				if (dstart == -1) {
					if (this.unkDescriptorAsSent) {
						dstart = 0;
						dend = sent.length() - 1;
						//if not then return the sentence representation.
					} else{
						dstart = input.maxSentLen;
						dend = input.maxSentLen + 1;
					}
				}
				edgeInputBuilder.append(dstart + " " + dend + " ");
			}
			if (this.useHeadWord) {
				edgeInputBuilder.append(a2Head);
			} else {
				edgeInputBuilder.append(a2Start + " " + a2End);
			}
			this.addNeural(network, 0, parent_k, children_k_index, edgeInputBuilder.toString(), relId);
		} else if (nnType == NeuralType.rnnpool) {
			StringBuilder edgeInputBuilder = new StringBuilder();
			if (this.positionIndicator) {
				for (int i = 0; i < sent.length(); i++) {
					if (arg1Span.start == i) {
						edgeInputBuilder.append(i == 0 ? "<e1>" : " <e1>");
						edgeInputBuilder.append(" " + sent.get(i).getForm());
					}
					if (arg1Span.end == i) {
						if (arg1Span.start != arg1Span.end)
							edgeInputBuilder.append(" " + sent.get(i).getForm());
						edgeInputBuilder.append(" </e1>");
					}
					if (i == arg2Span.start) {
						edgeInputBuilder.append(" <e2> " + sent.get(i).getForm());
					}
					if (i == arg2Span.end) {
						if (arg2Span.start != arg2Span.end)
							edgeInputBuilder.append(" " + sent.get(i).getForm());
						edgeInputBuilder.append(" </e2>");
					}
					if (i != arg1Span.start && i != arg1Span.end && i != arg2Span.start && i != arg2Span.end) {
						edgeInputBuilder.append(i == 0 ? sent.get(i).getForm() : " " + sent.get(i).getForm());
					}
				}
			} else {
				edgeInputBuilder.append(sent.toString());
			}
			this.addNeural(network, 0, parent_k, children_k_index, edgeInputBuilder.toString(), relId);
		}
	}
	
	private FeatureArray addDescriptorFeatureForExtent(FeatureArray currFa, Network network, Sentence sent, int dstart, int dend, String relType, Span arg1Span, Span arg2Span) {
		List<WordToken> list = new ArrayList<>();
		if (dstart != -1) {
			for (int i = dstart; i <= dend; i++) {
				if (i >= arg1Span.start && i <= arg1Span.end) continue;
				if (i >= arg2Span.start && i <= arg2Span.end) continue;
				list.add(sent.get(i));
			}
			List<Integer> ugs = new ArrayList<Integer>();
			for(WordToken wt : list) {
				String w = wt.getForm();
				String t = wt.getTag();
				ugs.add(this._param_g.toFeature(network, FeaType.desugs.name(), relType, w));
				ugs.add(this._param_g.toFeature(network, FeaType.desugs.name() + "t", relType, t));
			}
			if (ugs.size() > 0) {
				currFa = currFa.addNext(this.createFeatureArray(network, ugs));
			}
			List<Integer> bgs = new ArrayList<Integer>();
			for (int i = 0; i < list.size()- 1; i++) {
				String w = list.get(i).getForm();
				String t = list.get(i).getTag();
				String rw = list.get(i + 1).getForm();
				String rt = list.get(i + 1).getTag();
				bgs.add(this._param_g.toFeature(network, FeaType.desbgs.name(), relType, w + " " + rw));
				bgs.add(this._param_g.toFeature(network, FeaType.desbgs.name() + "t", relType, t + " " + rt));
			}
			if (bgs.size() > 0) {
				currFa = currFa.addNext(this.createFeatureArray(network, bgs));
			}
			List<Integer> tgs = new ArrayList<Integer>();
			for (int i = 0; i < list.size() - 2; i++) {
				String w = list.get(i).getForm();
				String t = list.get(i).getTag();
				String rw = list.get(i + 1).getForm();
				String rt = list.get(i + 1).getTag();
				String rrw = list.get(i + 2).getForm();
				String rrt = list.get(i + 2).getTag();
				tgs.add(this._param_g.toFeature(network, FeaType.destgs.name(), relType, w + " " + rw + " " + rrw));
				tgs.add(this._param_g.toFeature(network, FeaType.destgs.name() + "t", relType, t + " " + rt + " " + rrt));
			}
			if (tgs.size() > 0) {
				currFa = currFa.addNext(this.createFeatureArray(network, tgs));
			}
		}
		if (this.emb != null) {
			List<Integer> embFeats = new ArrayList<>(this.emb.getDimension());
			List<Double> embFeatVal = new ArrayList<>(this.emb.getDimension());
			double[] avg = new double[this.emb.getDimension()];
			if (list.size() > 0) {
				for (int i = 0; i < list.size(); i++) {
					double[] curr = this.emb.getEmbedding(list.get(i).getForm());
					for (int d = 0; d< this.emb.getDimension(); d++) {
						avg[d] += curr[d];
					}
				}
				for (int d = 0; d < this.emb.getDimension(); d++) {
					avg[d] /= (list.size());
					embFeats.add(this._param_g.toFeature(network, FeaType.emb.name() + "-" + (d+1), relType, ""));
					embFeatVal.add(avg[d]);
				}
			} else {
				double[] curr = this.emb.getEmbedding("***UNKNOWN***");
				for (int d = 0; d< this.emb.getDimension(); d++) {
					avg[d] += curr[d];
					embFeats.add(this._param_g.toFeature(network, FeaType.emb.name() + "-" + (d+1), relType, ""));
					embFeatVal.add(avg[d]);
				}
			}
			currFa = currFa.addNext(this.createFeatureArray(network, embFeats, embFeatVal));
		}
		int[] spanLenFs = new int[1];
		double[] val = new double[1];
		int spanLen = dstart == -1? 1 : dend - dstart + 1;
		spanLenFs[0] = this._param_g.toFeature(network, FeaType.spanLen.name(), "",  "");
		val[0] = 1.0 / (spanLen + 1);
		currFa = currFa.addNext(this.createFeatureArray(network, spanLenFs, val));
		return currFa;
	}
	
	private FeatureArray addDescriptorFeature(FeatureArray currFa, Network network, Sentence sent, int dstart, int dend, String relType, Span arg1Span, Span arg2Span) {
		if (dstart != -1) {
			
			List<Integer> ugs = new ArrayList<Integer>();
			for (int i = dstart; i <= dend; i++) {
				String w = sent.get(i).getForm();
				String t = sent.get(i).getTag();
				ugs.add(this._param_g.toFeature(network, FeaType.desugs.name(), relType, w));
				ugs.add(this._param_g.toFeature(network, FeaType.desugs.name() + "t", relType, t));
			}
			if (ugs.size() > 0) {
				currFa = currFa.addNext(this.createFeatureArray(network, ugs));
			}
			List<Integer> bgs = new ArrayList<Integer>();
			for (int i = dstart; i < dend; i++) {
				String w = sent.get(i).getForm();
				String t = sent.get(i).getTag();
				String rw = sent.get(i + 1).getForm();
				String rt = sent.get(i + 1).getTag();
				bgs.add(this._param_g.toFeature(network, FeaType.desbgs.name(), relType, w + " " + rw));
				bgs.add(this._param_g.toFeature(network, FeaType.desbgs.name() + "t", relType, t + " " + rt));
			}
			if (bgs.size() > 0) {
				currFa = currFa.addNext(this.createFeatureArray(network, bgs));
			}
			List<Integer> tgs = new ArrayList<Integer>();
			for (int i = dstart; i < dend - 1; i++) {
				String w = sent.get(i).getForm();
				String rw = sent.get(i + 1).getForm();
				String rrw = sent.get(i + 2).getForm();
				String t = sent.get(i).getTag();
				String rt = sent.get(i + 1).getTag();
				String rrt = sent.get(i + 2).getTag();
				tgs.add(this._param_g.toFeature(network, FeaType.destgs.name(), relType, w + " " + rw + " " + rrw));
				tgs.add(this._param_g.toFeature(network, FeaType.destgs.name() + "t", relType, t + " " + rt + " " + rrt));
			}
			if (tgs.size() > 0) {
				currFa = currFa.addNext(this.createFeatureArray(network, tgs));
			}
		}
		if (this.emb != null) {
			List<Integer> embFeats = new ArrayList<>(this.emb.getDimension());
			List<Double> embFeatVal = new ArrayList<>(this.emb.getDimension());
			double[] avg = new double[this.emb.getDimension()];
			if (dstart != -1) {
				for (int i = dstart; i <= dend; i++) {
					double[] curr = this.emb.getEmbedding(sent.get(i).getForm());
					for (int d = 0; d< this.emb.getDimension(); d++) {
						avg[d] += curr[d];
					}
				}
				for (int d = 0; d < this.emb.getDimension(); d++) {
					avg[d] /= (dend - dstart + 1);
					embFeats.add(this._param_g.toFeature(network, FeaType.emb.name() + "-" + (d+1), relType, ""));
					embFeatVal.add(avg[d]);
				}
			} else {
				double[] curr = this.emb.getEmbedding("***UNKNOWN***");
				for (int d = 0; d< this.emb.getDimension(); d++) {
					avg[d] += curr[d];
					embFeats.add(this._param_g.toFeature(network, FeaType.emb.name() + "-" + (d+1), relType, ""));
					embFeatVal.add(avg[d]);
				}
			}
			currFa = currFa.addNext(this.createFeatureArray(network, embFeats, embFeatVal));
		}
		int[] spanLenFs = new int[1];
		double[] val = new double[1];
		int spanLen = dstart == -1? 1 : dend - dstart + 1;
		spanLenFs[0] = this._param_g.toFeature(network, FeaType.spanLen.name(), "", "");
		val[0] = 1.0 / (spanLen + 1);
		currFa = currFa.addNext(this.createFeatureArray(network, spanLenFs, val));
		return currFa;
	}
	
	@SuppressWarnings("unused")
	private String getLabel(Sentence sent, int i, int dstart, int dend, Span arg1Span, Span arg2Span, String relType) {
		if (i < 0) {
			return "START";
		}
		if (i == sent.length()) {
			return "END";
		}
		String label = null;
		if (i == dstart) {
			label = "B-" + relType;
		} else if (i > dstart && i <= dend) {
			label = "I-" + relType;
		} else {
			label = "O";
		}
		if (i >= arg1Span.start && i <= arg1Span.end) {
			label += "-ARG1";
		}
		if (i >= arg2Span.start && i <= arg2Span.end) {
			label += "-ARG2";
		}
		return label;
	}
	
	private FeatureArray addSimpleRelFeatures(FeatureArray currFa, Network network, Sentence sent, 
			Span arg1Span, Span arg2Span, String relType, int parent_k, int children_k_index,
			int dstart, int dend) {
		String relationLabel = relType;
		List<Integer> a1s = new ArrayList<>(arg1Span.end - arg1Span.start + 2);
		StringBuilder a1Seq = new StringBuilder("");
		for (int i = arg1Span.start; i <= arg1Span.end; i++) {
			String curr_word = sent.get(i).getForm();
			if (i == arg1Span.start)
				a1Seq.append(curr_word);
			else a1Seq.append(" " + curr_word);
			a1s.add(this._param_g.toFeature(network, FeaType.a1w.name(), relationLabel, curr_word));
		}
		a1s.add(this._param_g.toFeature(network, FeaType.a1.name(), relationLabel, a1Seq.toString()));
		currFa = currFa.addNext(this.createFeatureArray(network, a1s));
		
		List<Integer> a1lr = new ArrayList<>(2);
		String a1leftWord = arg1Span.start - 1 >= 0 ? sent.get(arg1Span.start - 1).getForm() : "START";
		String a1RightWord = sent.get(arg1Span.end + 1).getForm();
		a1lr.add(this._param_g.toFeature(network, FeaType.a1left.name(), relationLabel, a1leftWord));
		a1lr.add(this._param_g.toFeature(network, FeaType.a1right.name(), relationLabel, a1RightWord));
		currFa = currFa.addNext(this.createFeatureArray(network, a1lr));
		
		List<Integer> a2s = new ArrayList<>(arg2Span.end - arg2Span.start + 2);
		StringBuilder a2Seq = new StringBuilder("");
		for (int i = arg2Span.start; i <= arg2Span.end; i++) {
			String curr_word = sent.get(i).getForm();
			if (i == arg2Span.start)
				a2Seq.append(curr_word);
			else a2Seq.append(" " + curr_word);
			a2s.add(this._param_g.toFeature(network, FeaType.a2w.name(), relationLabel, curr_word));
		}
		a2s.add(this._param_g.toFeature(network, FeaType.a2.name(), relationLabel, a2Seq.toString()));
		currFa = currFa.addNext(this.createFeatureArray(network, a2s));
		List<Integer> a2lr = new ArrayList<>(2);
		String a2leftWord = sent.get(arg2Span.start - 1).getForm();
		String a2RightWord = arg2Span.end + 1 < sent.length() ? sent.get(arg2Span.end + 1).getForm() : "END";
		a2lr.add(this._param_g.toFeature(network, FeaType.a2left.name(), relationLabel, a2leftWord));
		a2lr.add(this._param_g.toFeature(network, FeaType.a2right.name(), relationLabel, a2RightWord));
		currFa = currFa.addNext(this.createFeatureArray(network, a2lr));
		return currFa;
	}
	
	private FeatureArray addRelFeatures (FeatureArray currFa, Network network, Sentence sent, 
			Span arg1Span, Span arg2Span, String relType, int parent_k, int children_k_index,
			int dstart, int dend) {
		String relationLabel = relType;
		//arg1 features
		List<Integer> a1a2 = new ArrayList<>(arg1Span.end - arg1Span.start + arg2Span.end - arg2Span.start  + 2);
		List<Integer> a1s = new ArrayList<>(arg1Span.end - arg1Span.start + 2);
		StringBuilder a1Seq = new StringBuilder("");
		for (int i = arg1Span.start; i <= arg1Span.end; i++) {
			String curr_word = sent.get(i).getForm();
			String curr_tag = sent.get(i).getTag();
			if (this.useGeneralTags) {
				curr_tag = this.getGeneralTag(curr_tag);
			}
			if (i == arg1Span.start)
				a1Seq.append(curr_word);
			else a1Seq.append(" " + curr_word);
			a1s.add(this._param_g.toFeature(network, FeaType.a1w.name(), relationLabel, curr_word));
			a1s.add(this._param_g.toFeature(network, FeaType.a1t.name(), relationLabel, curr_tag));
			a1a2.add(this._param_g.toFeature(network, FeaType.a1a2w.name(), relationLabel, curr_word));
			a1a2.add(this._param_g.toFeature(network, FeaType.a1a2t.name(), relationLabel, curr_tag));
		}
		a1s.add(this._param_g.toFeature(network, FeaType.a1.name(), relationLabel, a1Seq.toString()));
		currFa = currFa.addNext(this.createFeatureArray(network, a1s));

		
		//arg2 features
		List<Integer> a2s = new ArrayList<>(arg2Span.end - arg2Span.start + 2);
		StringBuilder a2Seq = new StringBuilder("");
		for (int i = arg2Span.start; i <= arg2Span.end; i++) {
			String curr_word = sent.get(i).getForm();
			String curr_tag = sent.get(i).getTag();
			if (this.useGeneralTags) {
				curr_tag = this.getGeneralTag(curr_tag);
			}
			if (i == arg2Span.start)
				a2Seq.append(curr_word);
			else a2Seq.append(" " + curr_word);
			a2s.add(this._param_g.toFeature(network, FeaType.a2w.name(), relationLabel, curr_word));
			a2s.add(this._param_g.toFeature(network, FeaType.a2t.name(), relationLabel, curr_tag));
			a1a2.add(this._param_g.toFeature(network, FeaType.a1a2w.name(), relationLabel, curr_word));
			a1a2.add(this._param_g.toFeature(network, FeaType.a1a2t.name(), relationLabel, curr_tag));
		}
		a2s.add(this._param_g.toFeature(network, FeaType.a2.name(), relationLabel, a2Seq.toString()));
		currFa = currFa.addNext(this.createFeatureArray(network, a2s));
		currFa = currFa.addNext(this.createFeatureArray(network, a1a2));
		
		//words between
		List<Integer> bet = new ArrayList<>();
		StringBuilder tagSeq = new StringBuilder();
		for (int i = arg1Span.end + 1; i < arg2Span.start; i++) {
			String curr_word = sent.get(i).getForm();
			String curr_tag_f = sent.get(i).getTag().substring(0, 1);
			bet.add(this._param_g.toFeature(network, FeaType.betw.name(), relationLabel, curr_word));
			if (i == arg1Span.end + 1) {
				tagSeq.append(curr_tag_f);
			} else {
				tagSeq.append("_" + curr_tag_f);
			}
			String pref = curr_word.length() >= 5 ? curr_word.substring(0, 5) : curr_word;
			bet.add(this._param_g.toFeature(network, FeaType.pref5bet.name(), relationLabel, pref));
		}
		bet.add(this._param_g.toFeature(network, FeaType.bettseq.name(), relationLabel, tagSeq.toString()));
		currFa = currFa.addNext(this.createFeatureArray(network, bet));
		
		int[] dis = new int[1];
		dis[0] = this._param_g.toFeature(network, FeaType.dist.name(), relationLabel, (arg2Span.start - arg1Span.end) + "");
		currFa = currFa.addNext(this.createFeatureArray(network, dis));
		
		//word outside
		String arg1lw = arg1Span.start - 1 >= 0 ? sent.get(arg1Span.start - 1).getForm() : "START";
		String arg2rw = arg2Span.end + 1 < sent.length()? sent.get(arg2Span.end + 1).getForm() : "END";
		List<Integer> outside = new ArrayList<>();
		outside.add(this._param_g.toFeature(network, FeaType.loutside.name(), relationLabel, arg1lw));
		outside.add(this._param_g.toFeature(network, FeaType.routside.name(), relationLabel, arg2rw));
		currFa = currFa.addNext(this.createFeatureArray(network, outside));
		
		if (this.dependencyFeatures) {
			//dependency features.
			//later change it to span head word.
			List<Integer> deps = new ArrayList<>();
			int hm1Idx = arg1Span.headIdx;
			int hm2Idx = arg2Span.headIdx;
			String hm1 = sent.get(hm1Idx).getForm();
			String hm2 = sent.get(hm2Idx).getForm();
			int hm1HeadIdx = sent.get(arg1Span.headIdx).getHeadIndex();
			String hm1Head = hm1HeadIdx == -1 ? "<ROOT>" : sent.get(hm1HeadIdx).getForm();
			deps.add(this._param_g.toFeature(network, FeaType.depWord.name() + "-arg1", relationLabel, hm1 + " " + hm1Head));
			int hm2HeadIdx = sent.get(arg2Span.headIdx).getHeadIndex();
			String hm2Head = hm2HeadIdx == -1 ? "<ROOT>" : sent.get(hm2HeadIdx).getForm();
			deps.add(this._param_g.toFeature(network, FeaType.depWord.name()+ "-arg2", relationLabel, hm2 + " " + hm2Head));
			this.findPath(sent, arg1Span.headIdx, arg2Span.headIdx, network, relationLabel, deps);
			currFa = currFa.addNext(this.createFeatureArray(network, deps));
		}
		
		return currFa;
	}
	
	private FeatureArray addHypernymFeatures (FeatureArray currFa, Network network, Sentence sent, 
			Span arg1Span, Span arg2Span, String relType, int parent_k, int children_k_index,
			int dstart, int dend) {
		String relationLabel = relType;
		List<Integer> a1a2 = new ArrayList<>(arg1Span.end - arg1Span.start + arg2Span.end - arg2Span.start  + 2);
		List<Integer> a1s = new ArrayList<>(arg1Span.end - arg1Span.start + 2);
		for (int i = arg1Span.start; i <= arg1Span.end; i++) {
			String currHypernym = sent.get(i).getHypernym();
			if(!currHypernym.equals("0")) {
				currHypernym = currHypernym.split("\\.")[1];
				a1s.add(this._param_g.toFeature(network, FeaType.a1hypernym.name(), relationLabel, currHypernym));
				a1a2.add(this._param_g.toFeature(network, FeaType.a1a2hypernym.name(), relationLabel, currHypernym));
			}
		}
		currFa = currFa.addNext(this.createFeatureArray(network, a1s));
		List<Integer> a2s = new ArrayList<>(arg2Span.end - arg2Span.start + 2);
		for (int i = arg2Span.start; i <= arg2Span.end; i++) {
			String currHypernym = sent.get(i).getHypernym();
			if(!currHypernym.equals("0")) {
				currHypernym = currHypernym.split("\\.")[1];
				a2s.add(this._param_g.toFeature(network, FeaType.a2hypernym.name(), relationLabel, currHypernym));
				a1a2.add(this._param_g.toFeature(network, FeaType.a1a2hypernym.name(), relationLabel, currHypernym));
			}
		}
		currFa = currFa.addNext(this.createFeatureArray(network, a2s));
		currFa = currFa.addNext(this.createFeatureArray(network, a1a2));
		List<Integer> bet = new ArrayList<>();
		for (int i = arg1Span.end + 1; i < arg2Span.start; i++) {
			String currHypernym = sent.get(i).getHypernym();
			if(!currHypernym.equals("0")) {
				currHypernym = currHypernym.split("\\.")[1];
				bet.add(this._param_g.toFeature(network, FeaType.bethypernym.name(), relationLabel, currHypernym));
			}
		}
		currFa = currFa.addNext(this.createFeatureArray(network, bet));
		return currFa;
	}
	
	private FeatureArray addNERFeatures (FeatureArray currFa, Network network, Sentence sent, 
			Span arg1Span, Span arg2Span, String relType, int parent_k, int children_k_index,
			int dstart, int dend) {
		String relationLabel = relType;
		List<Integer> a1s = new ArrayList<>(arg1Span.end - arg1Span.start + 2);
		for (int i = arg1Span.start; i <= arg1Span.end; i++) {
			String currNER = sent.get(i).getEntity();
			if(!currNER.equals("0")) {
				String[] vals = currNER.split(":");
				if (vals.length == 2) currNER = vals[1];
				else if (vals.length == 3) currNER = vals[2];
				else throw new RuntimeException("length not 2 or 3: " + currNER);
				a1s.add(this._param_g.toFeature(network, FeaType.a1ner.name(), relationLabel, currNER));
			}
		}
		currFa = currFa.addNext(this.createFeatureArray(network, a1s));
		List<Integer> a2s = new ArrayList<>(arg2Span.end - arg2Span.start + 2);
		for (int i = arg2Span.start; i <= arg2Span.end; i++) {
			String currNER = sent.get(i).getEntity();
			if(!currNER.equals("0")) {
				String[] vals = currNER.split(":");
				if (vals.length == 2) currNER = vals[1];
				else if (vals.length == 3) currNER = vals[2];
				else throw new RuntimeException("length not 2 or 3: " + currNER);
				a2s.add(this._param_g.toFeature(network, FeaType.a2ner.name(), relationLabel, currNER));
			}
		}
		currFa = currFa.addNext(this.createFeatureArray(network, a2s));
		return currFa;
	}
	
	private String getGeneralTag(String posTag) {
		if (posTag.equals("CD") || posTag.equals("JJ") || posTag.equals("JJR") || posTag.equals("JJS"))
			return "ADJ";
		else if (posTag.equals("VB") || posTag.equals("VBD") || posTag.equals("VBG") || posTag.equals("VBN") || posTag.equals("VBP") || posTag.equals("VBZ")|| posTag.equals("MD"))
			return "V";
		else if (posTag.equals("NN") || posTag.equals("NNS") || posTag.equals("NNP") || posTag.equals("NNPS"))
			return "N";
		else if (posTag.equals("RB") || posTag.equals("RBR") || posTag.equals("RBS") || posTag.equals("RP") || posTag.equals("WRB"))
			return "ADV";
		else if (posTag.equals("DET") || posTag.equals("PDT") || posTag.equals("WDT") || posTag.equals("POS"))
			return "DET";
		else if (posTag.equals("PRP") || posTag.equals("WP")) 
			return "PRP";
		else if (posTag.equals("PRP$") || posTag.equals("WP$"))
			return "PRP$";
		else if (posTag.equals("TO") || posTag.equals("IN"))
			return "PREP";
		else if (posTag.equals("CC"))
			return "CONJ";
		else if (posTag.equals("EX") || posTag.equals("FW") || posTag.equals("SYM") || posTag.equals("UH") || posTag.equals("LS"))
			return "OTHER";
		else return posTag;
	}

	/**
	 * Check if hm1Idx and hm2Idx are equal to each other.
	 * @param sent
	 * @param hm1Idx
	 * @param hm2Idx
	 */
	private void findPath(Sentence sent, int hm1Idx, int hm2Idx, Network network, String rel, List<Integer> fs) {
		StringBuilder path = new StringBuilder("path:");
		TIntStack stack1 = new TIntArrayStack();
		TIntStack stack2 = new TIntArrayStack();
		boolean found1 =  sent.depTree.dfsFind(hm1Idx, stack1);
		boolean found2 = sent.depTree.dfsFind(hm2Idx, stack2);
		if (!(found1 && found2)) throw new RuntimeException("Not found in the dependency tree?");
		int[] trace1 = stack1.toArray();
		int[] trace2 = stack2.toArray();
		int contIdx1 = containsElement(trace1, hm2Idx);
		int contIdx2 = containsElement(trace2, hm1Idx);
		if (contIdx1 >= 0 || contIdx2 >= 0) {
			if (contIdx1 >= 0 && contIdx2 >= 0 && hm1Idx != hm2Idx) throw new RuntimeException("impossible");
			if (contIdx1 >= 0) {
				for (int k = 0; k < contIdx1 ; k++) { // because the trace is backtracked.
					String depLabel = sent.get(trace1[k]).getDepLabel();
					fs.add(this._param_g.toFeature(network, FeaType.depLabel.name(), rel, depLabel));
					if (k==0) path.append(depLabel);
					else path.append(" " + depLabel);
				}
			} else {
				for (int k = contIdx2 - 1; k >=0 ; k--) { // because the trace is backtracked.
					String depLabel = sent.get(trace2[k]).getDepLabel();
					fs.add(this._param_g.toFeature(network, FeaType.depLabel.name(), rel, depLabel));
					if (k==contIdx2 - 1) path.append(depLabel);
					else path.append(" " + depLabel);
				}
			}
		} else {
			int stamp1 = trace1.length - 1;
			int stamp2 = trace2.length - 1;
			while (stamp1 >= 0 && stamp2 >= 0) {
				if (trace1[stamp1] != trace2[stamp2]) {
					stamp1++;
					stamp2++;
					break;
				} else {
					stamp1--;
					stamp2--;
				}
			}
			//trace1[stamp1] should be equal to trace2[stamp2]
			for (int k = 0; k < stamp1; k++) {
				String depLabel = sent.get(trace1[k]).getDepLabel();
				fs.add(this._param_g.toFeature(network, FeaType.depLabel.name(), rel, depLabel));
				if (k==0) path.append(depLabel);
				else path.append(" " + depLabel);
			}
			for (int k = stamp2 - 1; k >=0; k--) {
				String depLabel = sent.get(trace2[k]).getDepLabel();
				fs.add(this._param_g.toFeature(network, FeaType.depLabel.name(), rel, depLabel));
				path.append(" " + depLabel);
			}
		}
		//Note: the path is label path
		fs.add(this._param_g.toFeature(network, FeaType.depPath.name(), rel, path.toString()));
	}
	
	private int containsElement(int[] arr, int val) {
		for (int i = 0; i < arr.length; i++) {
			if (arr[i] == val)
				return i;
		}
		return -1;
	}
}
