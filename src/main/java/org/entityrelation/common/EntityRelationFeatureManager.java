package org.entityrelation.common;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.entityrelation.common.EntityRelationCompiler.NodeType;
import org.entityrelation.common.EntityRelationFeatureManager.FeatureType;
import org.entityrelation.common.EntityRelationFeatureManager.RelFeaType;
import org.statnlp.example.descriptor.Config;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.FeatureManager;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.neural.MLP;
import org.statnlp.hypergraph.neural.NeuralNetworkCore;

public abstract class EntityRelationFeatureManager extends FeatureManager {

	/**
	 * 
	 */
	private static final long serialVersionUID = 592253662868854534L;
	protected NeuralNetworkCore net;
	public String neuralType;
	public boolean moreBinaryFeatures = false;
	
	protected int wordWindowSize = 3;
	protected int wordHalfWindowSize = 1;
	protected int postagWindowSize = 3;
	protected int postagHalfWindowSize = 1;
	
	protected int wordNgramMinSize = 2;
	protected int wordNgramMaxSize = 4;
	protected int postagNgramMinSize = 2;
	protected int postagNgramMaxSize = 4;
	
	protected int bowWindowSize = 5;
	protected int bowHalfWindowSize = 2;
	
	protected boolean lastWordAsHead = true;

	public enum FeatureType {
		WORD,
		WORD_NGRAM,
		
		POS_TAG,
		POS_TAG_NGRAM,
		
		BOW,
		
		ALL_CAPS,
		ALL_DIGITS,
		ALL_ALPHANUMERIC,
		ALL_LOWERCASE,
		CONTAINS_DIGITS,
		CONTAINS_DOTS,
		CONTAINS_HYPHEN,
		INITIAL_CAPS,
		LONELY_INITIAL,
		PUNCTUATION_MARK,
		ROMAN_NUMBER,
		SINGLE_CHARACTER,
		URL,
		
		MENTION_PENALTY,
		
		RELATION_ENTITYTYPE,
		RELATION,
		ENTITYTYPE,
		DEP,
		TRANSITION, REL,
	}
	
	public enum RelFeaType {wm1, hm1, wm2, hm2, hm12,
		wbnull, wbfl, wbf, wbl, wbo, lexbigram, bm1f, bm1l, am2f, am2l,
		et12, etsub12, ml12, ml12et12, ml12etsub12, nummb,numwb, m1cm2, m2cm1, et12m1cm2, et12m2cm1, etsub12m1cm2, etsub12m2cm1, hm12m1cm2, hm12m2cm1,
		cm1m1, cp1p1, cm2m1, cm1p1, cp1p2,
		cphbnull, cphbfl, cphbf, cphbl, cphbo, cphbm1f, cphbm1l,cpham2f, cpham2l, cpp, cpph,
		h1head, h2head, et12samenp, et12samepp, et12samevp,
		a1, a1w, a2, a2w, a1a2w, betw, a1t, a2t, a1a2t, a1left, a1right, a2left, a2right,
		dist,bettseq, loutside, routside, pref5bet,
		depWord,desugs,desbgs,destgs,
		a1hypernym, a2hypernym, bethypernym, a1a2hypernym, a1ner, a2ner, betner, emb, spanLen,
		depWordPath, depLabelPath
	}



	public String OUT_SEP = MLP.OUT_SEP;
	public String IN_SEP = MLP.IN_SEP;
	protected final String START = "STR";
	protected final String END = "END";
	
	protected GlobalNetworkParam param_g = this._param_g;

	public EntityRelationFeatureManager(GlobalNetworkParam param_g) {
		super(param_g, null);
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			this.net = (NeuralNetworkCore) param_g.getNNParamG().getNet(0);
		}
	}

	public EntityRelationFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
		super(param_g);
		if (NetworkConfig.USE_NEURAL_FEATURES && !EntityRelationGlobal.neuralType.equals("continuous0")) {
			this.net = (NeuralNetworkCore) param_g.getNNParamG().getNet(0);
		}
		this.neuralType = neuralType;
		this.moreBinaryFeatures = moreBinaryFeatures;
	}


	int NodeTypeSize = NodeType.values().length;
	int WordFeatureTypeSize = EntityRelationInstance.FEATURE_TYPES.values().length;

	protected EntityRelationCompiler compiler = null;
	
	public void setCompiler(EntityRelationCompiler compiler) {
		this.compiler = compiler;
	}
	
	public void addCNNFirstNeuralFeatures(Network network, int parent_k, int children_k_index, 
			int relId, EntityRelationInstance inst,Entity arg1Span, Entity arg2Span) {
		
			ArrayList<String[]> input = (ArrayList<String[]>)inst.input;
			String edgeInput = null;
			StringBuilder edgeInputBuilder = new StringBuilder();
			if (EntityRelationGlobal.usePositionEmbedding) {
				int p1 = -1 - arg1Span.headIdx;
				int p2 = -1 - arg2Span.headIdx;
				edgeInputBuilder.append("<START>"+ Config.NEURAL_SEP + p1 + Config.NEURAL_SEP + p2); 
			}else {
				edgeInputBuilder.append("<START>"); //PADDING is the special token in senna embeddings.
			}
			for (int j = 0; j < input.size(); j++) {
				String word = input.get(j)[0];
				if (EntityRelationGlobal.EMBEDDING_WORD_LOWERCASE) word = word.toLowerCase();
				if (EntityRelationGlobal.zero_digit) word = word.replaceAll("\\d", "0");
				if (EntityRelationGlobal.usePositionEmbedding) {
					int p1 = j - arg1Span.headIdx;
					int p2 = j - arg2Span.headIdx;
					word = word + Config.NEURAL_SEP + p1 + Config.NEURAL_SEP + p2;
				}
				edgeInputBuilder.append(" " + word);
			}
			if (EntityRelationGlobal.usePositionEmbedding) {
				int p1 = input.size() - arg1Span.headIdx;
				int p2 = input.size() - arg2Span.headIdx;
				edgeInputBuilder.append(" <END>"+ Config.NEURAL_SEP + p1 + Config.NEURAL_SEP + p2); 
			}else {
				edgeInputBuilder.append(" <END>");
			}
			/*
			if (nnDescriptorFeature) {
				if (dstart == -1) { //dend is also -1
					dstart = 0;
					dend = sent.length() - 1;
					//if not then return the sentence representation.
				}
				edgeInputBuilder.append(Config.NEURAL_DESC_SEP + (dstart + 1) + Config.NEURAL_DESC_SEP + (dend + 1));
			}*/
			edgeInput = edgeInputBuilder.toString();
//			try {
			this.addNeural(network, 0, parent_k, children_k_index, edgeInput, relId);
//			} catch (Exception e) {
//				System.out.println(e);
//				System.exit(0);
//			}
			//System.out.println(edgeInput);
	}
	
	
	public ArrayList<Integer> extractSpanFeatures(EntityRelationInstance inst, Network network, ArrayList<String[]> inputs, 
			int pos, int size, NodeType nodetypeParent, NodeType nodetypeChild, String indicator, String entityType) {
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		ArrayList<String[]> features = new ArrayList<String[]>();
	
		AttributedWord currWord = inst.attriWords[pos];
		
		{
		
			for(int idx=pos-wordHalfWindowSize; idx<=pos+wordHalfWindowSize; idx++){
				String word = "";
				if(idx >= 0 && idx < size){
					word = inputs.get(idx)[0];
				}
				//featureList.add(param_g.toFeature(network, indicator+FeatureType.WORD.name()+(idx-pos), labelId, word));
				features.add(new String[] {FeatureType.WORD.name()+(idx-pos), entityType, word});
			}
			for(int idx=pos-postagHalfWindowSize; idx<=pos+postagHalfWindowSize; idx++){
				String postag = "";
				if(idx >= 0 && idx < size){
					postag = inputs.get(idx)[1];
				}
				//featureList.add(param_g.toFeature(network, indicator+FeatureType.POS_TAG.name()+(idx-pos), labelId, postag));
				features.add(new String[] {FeatureType.POS_TAG.name()+(idx-pos), entityType, postag});
			}
			for(int ngramSize=wordNgramMinSize; ngramSize<=wordNgramMaxSize; ngramSize++){
				for(int relPos=0; relPos<ngramSize; relPos++){
					String ngram = "";
					for(int idx=pos-ngramSize+relPos+1; idx<pos+relPos+1; idx++){
						if(ngram.length() > 0) ngram += " ";
						if(idx >= 0 && idx < size){
							ngram += inputs.get(idx)[0];
						}
					}
					//featureList.add(param_g.toFeature(network, indicator+FeatureType.WORD_NGRAM+" "+ngramSize+" "+relPos, labelId, ngram));
					features.add(new String[] {FeatureType.WORD_NGRAM+" "+ngramSize+" "+relPos, entityType, ngram});
				}
			}
			for(int ngramSize=postagNgramMinSize; ngramSize<=postagNgramMaxSize; ngramSize++){
				for(int relPos=0; relPos<ngramSize; relPos++){
					String ngram = "";
					for(int idx=pos-ngramSize+relPos+1; idx<pos+relPos+1; idx++){
						if(idx > pos-ngramSize+relPos+1) ngram += " ";
						if(idx >= 0 && idx < size){
							ngram += inputs.get(idx)[1];
						}
					}
					//featureList.add(param_g.toFeature(network, indicator+FeatureType.POS_TAG_NGRAM+" "+ngramSize+" "+relPos, labelId, ngram));
					features.add(new String[] {FeatureType.POS_TAG_NGRAM+" "+ngramSize+" "+relPos, entityType, ngram});
				}
			}
			List<String> bowList = new ArrayList<String>();
			for(int idx=pos-bowHalfWindowSize; idx<=pos+bowHalfWindowSize; idx++){
				if(idx >= 0 && idx < size){
					bowList.add(inputs.get(idx)[0]);
				}
			}
			Collections.sort(bowList);
			String bow = "";
			for(String word: bowList){
				if(bow.length() > 0) bow += " ";
				bow += word;
			}
			//featureList.add(param_g.toFeature(network, indicator+FeatureType.BOW.name(), labelId, bow));
			features.add(new String[] { FeatureType.BOW.name(), entityType, bow});
			
			
			
			
			for(FeatureType featureType: FeatureType.values()){
				switch(featureType){
				case ALL_CAPS:
				case ALL_DIGITS:
				case ALL_ALPHANUMERIC:
				case ALL_LOWERCASE:
				case CONTAINS_DIGITS:
				case CONTAINS_DOTS:
				case CONTAINS_HYPHEN:
				case INITIAL_CAPS:
				case LONELY_INITIAL:
				case PUNCTUATION_MARK:
					
				case ROMAN_NUMBER:
				case SINGLE_CHARACTER:
				case URL:
					
					features.add(new String[] { featureType.name(), entityType, currWord.getAttribute(featureType.name())});
					
				default:
					break;
				}
			}
			
			
			for(String indicatorStr : new String[] {indicator})
			for(String output : new String[] {entityType})//, "<Entity>"})
				for(String[] f : features) {
					featureList.add(param_g.toFeature(network, indicatorStr + f[0], output, f[2]));
				}
			
		}
		return featureList;
	}
	
	public FeatureArray addContinuousEmbeddingRelationFeatures(FeatureArray fp, Network network, EntityRelationInstance inst, int i, int k, String relationType) {
		
		ArrayList<String[]> inputs = (ArrayList<String[]>)inst.input;
		ArrayList<Integer> continuousFeatureList = new ArrayList<Integer>();
		ArrayList<Double> continuousFeatureValueList = new ArrayList<Double>();
		
		int[] path = inst.getDep(i, k);
		double[] sum = null;
		
		for(int j = 0; j < path.length; j++) {
			String w = inputs.get(path[j])[0].toLowerCase();
			double[] vec = EntityRelationGlobal.Word2Vec.getVector(w);
			
			if (vec != null)
				sum = Utils.vectorAdd(sum, vec);
		}
		
						
		if (sum != null) {
			sum = Utils.vectorScale(sum, 1.0 / path.length);
			
			for(int j = 0; j < EntityRelationGlobal.Word2Vec.ShapeSize; j++) {
				continuousFeatureList.add(this._param_g.toFeature(network, "continuous-emb:", /*indicator +*/ relationType, "dim" + j  + ":"));
				continuousFeatureValueList.add(sum[j]);							
			}
		}
		
		fp = fp.addNext(this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList));
		
		return fp;
	}
	
	public FeatureArray addContinuousEmbeddingSpanFeatures(FeatureArray fp, Network network, String word, String entityType) {
		
		ArrayList<Integer> continuousFeatureList = new ArrayList<Integer>();
		ArrayList<Double> continuousFeatureValueList = new ArrayList<Double>();
		
		double[] vec = EntityRelationGlobal.Word2Vec.getVector(word);
		if (vec == null) {
			vec = EntityRelationGlobal.Word2Vec.getVector(EntityRelationGlobal.UNK);
			
			if (vec != null) {
				for(int j = 0; j < EntityRelationGlobal.Word2Vec.ShapeSize; j++) {
					continuousFeatureList.add(this._param_g.toFeature(network, "continuous-emb:", /*indicator +*/ entityType, "dim" + j  + ":"));
					continuousFeatureValueList.add(vec[j]);
				}
			}
		}
		
		fp = fp.addNext(this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList));
		
		return fp;
	}
	
	public FeatureArray addPartialRelFeatures (FeatureArray fa, Network network, EntityRelationInstance inst, NodeType nodetypeParent, NodeType nodetypeChild, int i, int k, String relType, int parent_k, int children_k_index) {
		ArrayList<String[]> input = (ArrayList<String[]>)inst.input;
		String relationLabel = relType;
		
		if (nodetypeParent == NodeType.T) {
			
			if (EntityRelationGlobal.DEP_FEATURE) {
				//dependency features.
				//later change it to span head word.
				List<Integer> deps = new ArrayList<>();
				int hm1Idx = i;
				int hm2Idx = k;
				String hm1 = hm1Idx < 0 ? "<root>" : input.get(hm1Idx)[0];
				String hm2 = hm2Idx < 0 ? "<root>" : input.get(hm2Idx)[0];
				int hm1HeadIdx = inst.depIdx[i];//sent.get(arg1Span.headIdx).getHeadIndex();
				String hm1Head = hm1HeadIdx == -1 ? "<ROOT>" : input.get(hm1HeadIdx)[0];
				deps.add(this._param_g.toFeature(network, RelFeaType.depWord.name() + "-arg1", relationLabel, hm1 + " " + hm1Head));
				int hm2HeadIdx = inst.depIdx[k];//sent.get(arg2Span.headIdx).getHeadIndex();
				String hm2Head = hm2HeadIdx == -1 ? "<ROOT>" : input.get(hm2HeadIdx)[0];
				deps.add(this._param_g.toFeature(network, RelFeaType.depWord.name()+ "-arg2", relationLabel, hm2 + " " + hm2Head));
				//this.findPath(sent, arg1Span.headIdx, arg2Span.headIdx, network, relationLabel, deps);
				
				inst.getDep(hm1Idx, hm2Idx);
				int[] path = inst.depPath[hm1Idx][hm2Idx];
				String depPathToken = "";
				String depPathDepLabel = "";
				String depPathTokenBigram = "";
				for(int j = 0; j < path.length; j++) {
					String word = input.get(path[j])[0];
					String dep = input.get(path[j])[3];
					deps.add(param_g.toFeature(network,  RelFeaType.depWord.name() + "-arg1-depwordpath" , relationLabel, word));
					deps.add(param_g.toFeature(network,  RelFeaType.depWord.name() + "-arg1-deplabelpath", relationLabel, dep));
					
					depPathToken += "-" + word;
					depPathDepLabel += "-" + dep;
					depPathTokenBigram += "-" + word + "_" + (j < path.length - 1 ? input.get(path[j + 1]) : "<End>");
				}
				
				deps.add(param_g.toFeature(network, RelFeaType.depWordPath.name() , relationLabel, depPathToken));
				deps.add(param_g.toFeature(network, RelFeaType.depLabelPath.name() , relationLabel, depPathDepLabel));
				//deps.add(param_g.toFeature(network, RelFeaType.depWordPath.name() +"-bigram" , relationLabel, depPathTokenBigram));
				
				fa = fa.addNext(this.createFeatureArray(network, deps));
			}
			
			//words between
			List<Integer> bet = new ArrayList<>();
			StringBuilder tagSeq = new StringBuilder();
			for (int j = i + 1; j < k; j++) {
				String curr_word = input.get(j)[0];
				String curr_tag_f = input.get(j)[1].substring(0, 1);
				bet.add(this._param_g.toFeature(network, RelFeaType.betw.name(), relationLabel, curr_word));
				if (j == i + 1) {
					tagSeq.append(curr_tag_f);
				} else {
					tagSeq.append("_" + curr_tag_f);
				}
				String pref = curr_word.length() >= 5 ? curr_word.substring(0, 5) : curr_word;
				bet.add(this._param_g.toFeature(network, RelFeaType.pref5bet.name(), relationLabel, pref));
			}
			//bet.add(this._param_g.toFeature(network, RelFeaType.bettseq.name(), relationLabel, tagSeq.toString()));
			fa = fa.addNext(this.createFeatureArray(network, bet));
			
			int[] dis = new int[1];
			dis[0] = this._param_g.toFeature(network, RelFeaType.dist.name(), relationLabel, (k - i) + "");
			fa = fa.addNext(this.createFeatureArray(network, dis));
			
			//word outside
			String arg1lw = i - 1 >= 0 ? input.get(i - 1)[0] : "START";
			//String arg2rw = k + 1 < input.size()? input.get(k + 1)[0] : "END";
			List<Integer> outside = new ArrayList<>();
			outside.add(this._param_g.toFeature(network, RelFeaType.loutside.name(), relationLabel, arg1lw));
			//outside.add(this._param_g.toFeature(network, RelFeaType.routside.name(), relationLabel, arg2rw));
			fa = fa.addNext(this.createFeatureArray(network, outside));
			
			
			
			
		} else if (nodetypeParent == NodeType.I1 || nodetypeParent == NodeType.I2) {
			
			List<Integer> a1a2 = new ArrayList<>(2);
			
			if (nodetypeParent == NodeType.I1) {
				
				//arg1 features
				
				List<Integer> a1s = new ArrayList<>(2);
				String curr_word = input.get(i)[0];
				String curr_tag = input.get(k)[1];
				if (EntityRelationGlobal.USE_GENERAL_TAGS) {
					curr_tag = Utils.getGeneralTag(curr_tag);
				}
				
				a1s.add(this._param_g.toFeature(network, RelFeaType.a1w.name(), relationLabel, curr_word));
				a1s.add(this._param_g.toFeature(network, RelFeaType.a1t.name(), relationLabel, curr_tag));
				a1a2.add(this._param_g.toFeature(network, RelFeaType.a1a2w.name(), relationLabel, curr_word));
				a1a2.add(this._param_g.toFeature(network, RelFeaType.a1a2t.name(), relationLabel, curr_tag));
				//a1s.add(this._param_g.toFeature(network, RelFeaType.a1.name(), relationLabel, a1Seq.toString()));
				fa = fa.addNext(this.createFeatureArray(network, a1s));
				
			} else { //I2
				
				//arg2 features
				List<Integer> a2s = new ArrayList<>(2);
				String curr_word = input.get(i)[0];
				String curr_tag = input.get(i)[1];
				if (EntityRelationGlobal.USE_GENERAL_TAGS) {
					curr_tag = Utils.getGeneralTag(curr_tag);
				}
				
				a2s.add(this._param_g.toFeature(network, RelFeaType.a2w.name(), relationLabel, curr_word));
				a2s.add(this._param_g.toFeature(network, RelFeaType.a2t.name(), relationLabel, curr_tag));
				a1a2.add(this._param_g.toFeature(network, RelFeaType.a1a2w.name(), relationLabel, curr_word));
				a1a2.add(this._param_g.toFeature(network, RelFeaType.a1a2t.name(), relationLabel, curr_tag));
				//a2s.add(this._param_g.toFeature(network, RelFeaType.a2.name(), relationLabel, a2Seq.toString()));
				fa = fa.addNext(this.createFeatureArray(network, a2s));
				
				
			}
			
			fa = fa.addNext(this.createFeatureArray(network, a1a2));
			
		}
		
		return fa;
	}
	
	public FeatureArray addRelFeatures (FeatureArray fa, Network network, EntityRelationInstance inst,
			Entity arg1Span, Entity arg2Span, String relType, int parent_k, int children_k_index) {
		ArrayList<String[]> input = (ArrayList<String[]>)inst.input;
		String relationLabel = relType;
		//arg1 features
		List<Integer> a1a2 = new ArrayList<>(arg1Span.span[1] - arg1Span.span[0] + arg2Span.span[1] - arg2Span.span[0]  + 2);
		List<Integer> a1s = new ArrayList<>(arg1Span.span[1] - arg1Span.span[0] + 2);
		StringBuilder a1Seq = new StringBuilder("");
		for (int i = arg1Span.span[0]; i < arg1Span.span[1]; i++) {
			String curr_word = input.get(i)[0];
			String curr_tag = input.get(i)[1];
			if (EntityRelationGlobal.USE_GENERAL_TAGS) {
				curr_tag = Utils.getGeneralTag(curr_tag);
			}
			if (i == arg1Span.span[0])
				a1Seq.append(curr_word);
			else a1Seq.append(" " + curr_word);
			a1s.add(this._param_g.toFeature(network, RelFeaType.a1w.name(), relationLabel, curr_word));
			a1s.add(this._param_g.toFeature(network, RelFeaType.a1t.name(), relationLabel, curr_tag));
			a1a2.add(this._param_g.toFeature(network, RelFeaType.a1a2w.name(), relationLabel, curr_word));
			a1a2.add(this._param_g.toFeature(network, RelFeaType.a1a2t.name(), relationLabel, curr_tag));
		}
		a1s.add(this._param_g.toFeature(network, RelFeaType.a1.name(), relationLabel, a1Seq.toString()));
		fa = fa.addNext(this.createFeatureArray(network, a1s));

		
		//arg2 features
		List<Integer> a2s = new ArrayList<>(arg2Span.span[1] - arg2Span.span[0] + 2);
		StringBuilder a2Seq = new StringBuilder("");
		for (int i = arg2Span.span[0]; i < arg2Span.span[1]; i++) {
			String curr_word = input.get(i)[0];
			String curr_tag = input.get(i)[1];
			if (EntityRelationGlobal.USE_GENERAL_TAGS) {
				curr_tag = Utils.getGeneralTag(curr_tag);
			}
			if (i == arg2Span.span[0])
				a2Seq.append(curr_word);
			else a2Seq.append(" " + curr_word);
			a2s.add(this._param_g.toFeature(network, RelFeaType.a2w.name(), relationLabel, curr_word));
			a2s.add(this._param_g.toFeature(network, RelFeaType.a2t.name(), relationLabel, curr_tag));
			a1a2.add(this._param_g.toFeature(network, RelFeaType.a1a2w.name(), relationLabel, curr_word));
			a1a2.add(this._param_g.toFeature(network, RelFeaType.a1a2t.name(), relationLabel, curr_tag));
		}
		a2s.add(this._param_g.toFeature(network, RelFeaType.a2.name(), relationLabel, a2Seq.toString()));
		fa = fa.addNext(this.createFeatureArray(network, a2s));
		fa = fa.addNext(this.createFeatureArray(network, a1a2));
		
		//words between
		List<Integer> bet = new ArrayList<>();
		StringBuilder tagSeq = new StringBuilder();
		for (int i = arg1Span.span[1] + 1; i < arg2Span.span[0]; i++) {
			String curr_word = input.get(i)[0];
			String curr_tag_f = input.get(i)[1].substring(0, 1);
			bet.add(this._param_g.toFeature(network, RelFeaType.betw.name(), relationLabel, curr_word));
			if (i == arg1Span.span[1] + 1) {
				tagSeq.append(curr_tag_f);
			} else {
				tagSeq.append("_" + curr_tag_f);
			}
			String pref = curr_word.length() >= 5 ? curr_word.substring(0, 5) : curr_word;
			bet.add(this._param_g.toFeature(network, RelFeaType.pref5bet.name(), relationLabel, pref));
		}
		bet.add(this._param_g.toFeature(network, RelFeaType.bettseq.name(), relationLabel, tagSeq.toString()));
		fa = fa.addNext(this.createFeatureArray(network, bet));
		
		int[] dis = new int[1];
		dis[0] = this._param_g.toFeature(network, RelFeaType.dist.name(), relationLabel, (arg2Span.span[0] - arg1Span.span[1]) + "");
		fa = fa.addNext(this.createFeatureArray(network, dis));
		
		//word outside
		String arg1lw = arg1Span.span[0] - 1 >= 0 ? input.get(arg1Span.span[0] - 1)[0] : "START";
		String arg2rw = arg2Span.span[1] + 1 < input.size()? input.get(arg2Span.span[1] + 1)[0] : "END";
		List<Integer> outside = new ArrayList<>();
		outside.add(this._param_g.toFeature(network, RelFeaType.loutside.name(), relationLabel, arg1lw));
		outside.add(this._param_g.toFeature(network, RelFeaType.routside.name(), relationLabel, arg2rw));
		fa = fa.addNext(this.createFeatureArray(network, outside));
		
		if (EntityRelationGlobal.DEP_FEATURE) {
			//dependency features.
			//later change it to span head word.
			List<Integer> deps = new ArrayList<>();
			int hm1Idx = arg1Span.headIdx;
			int hm2Idx = arg2Span.headIdx;
			String hm1 = hm1Idx < 0 ? "<root>" : input.get(hm1Idx)[0];
			String hm2 = hm2Idx < 0 ? "<root>" : input.get(hm2Idx)[0];
			int hm1HeadIdx = inst.depIdx[arg1Span.headIdx];//sent.get(arg1Span.headIdx).getHeadIndex();
			String hm1Head = hm1HeadIdx == -1 ? "<ROOT>" : input.get(hm1HeadIdx)[0];
			deps.add(this._param_g.toFeature(network, RelFeaType.depWord.name() + "-arg1", relationLabel, hm1 + " " + hm1Head));
			int hm2HeadIdx = inst.depIdx[arg2Span.headIdx];//sent.get(arg2Span.headIdx).getHeadIndex();
			String hm2Head = hm2HeadIdx == -1 ? "<ROOT>" : input.get(hm2HeadIdx)[0];
			deps.add(this._param_g.toFeature(network, RelFeaType.depWord.name()+ "-arg2", relationLabel, hm2 + " " + hm2Head));
			//this.findPath(sent, arg1Span.headIdx, arg2Span.headIdx, network, relationLabel, deps);
			
			inst.getDep(hm1Idx, hm2Idx);
			int[] path = inst.depPath[hm1Idx][hm2Idx];
			String depPathToken = "";
			String depPathDepLabel = "";
			String depPathTokenBigram = "";
			for(int j = 0; j < path.length; j++) {
				String word = input.get(path[j])[0];
				String dep = input.get(path[j])[3];
				deps.add(param_g.toFeature(network,  RelFeaType.depWord.name() + "-arg1-depwordpath" , relationLabel, word));
				deps.add(param_g.toFeature(network,  RelFeaType.depWord.name() + "-arg1-deplabelpath", relationLabel, dep));
				
				depPathToken += "-" + word;
				depPathDepLabel += "-" + dep;
				depPathTokenBigram += "-" + word + "_" + (j < path.length - 1 ? input.get(path[j + 1]) : "<End>");
			}
			
			deps.add(param_g.toFeature(network, RelFeaType.depWordPath.name() , relationLabel, depPathToken));
			deps.add(param_g.toFeature(network, RelFeaType.depLabelPath.name() , relationLabel, depPathDepLabel));
			//deps.add(param_g.toFeature(network, RelFeaType.depWordPath.name() +"-bigram" , relationLabel, depPathTokenBigram));
			
			
			fa = fa.addNext(this.createFeatureArray(network, deps));
		}
		
		return fa;
	}
	
public ArrayList<Integer> extractRelationFeatures_Old(EntityRelationInstance inst, Network network, ArrayList<String[]> inputs, int i, int k, String indicator, String relationType, String entity1Type, String entity2Type) {
		
		ArrayList<Integer> featureList = new ArrayList<Integer>();
		
		if (!EntityRelationGlobal.REL_FEATURE) {
			return featureList;
		}
		
		String labelId = relationType + "-" + entity1Type + "-" + entity2Type;
		String depPathToken = "";//inst.depPathToken[i][k];
		String depPathDepLabel = "";//inst.depPathDepLabel[i][k];
		String depParent = "";
		inst.getDep(i, k);
		int[] path = inst.depPath[i][k];
		
		/*if (EntityRelationGlobal.ADD_SELF_RELATION && r == 0) {
			//assert T-I1-X
		} else 
		*/
		{
			String depPathTokenBigram = "";
			for(int j = 0; j < path.length; j++) {
				String word = inputs.get(path[j])[0];
				String dep = inputs.get(path[j])[3];
				featureList.add(param_g.toFeature(network, indicator + "_tokenpath_contains_" + FeatureType.REL.name() , relationType, word));
				featureList.add(param_g.toFeature(network, indicator + "_deppath_contains_" + FeatureType.REL.name() , relationType, dep));
				
				depPathToken += "-" + word;
				depPathDepLabel += "-" + dep;
				depPathTokenBigram += "-" + word + "_" + (j < path.length - 1 ? inputs.get(path[j + 1]) : "<End>");
			}
			
			featureList.add(param_g.toFeature(network, indicator + "_tokenpath_" + FeatureType.REL.name() , relationType, depPathToken));
			featureList.add(param_g.toFeature(network, indicator + "+deppath_" +FeatureType.REL.name() , relationType, depPathDepLabel));
			
			/*
			featureList.add(param_g.toFeature(network, indicator + "_tokenpath_" + FeatureType.REL.name() +"_labelId" , labelId, depPathToken));
			featureList.add(param_g.toFeature(network, indicator + "+deppath_" +FeatureType.REL.name()+"_labelId" , labelId, depPathDepLabel));
			*/
			
			featureList.add(param_g.toFeature(network, indicator + "_1headword_" + FeatureType.REL.name() , relationType, inputs.get(i)[0]));
			featureList.add(param_g.toFeature(network, indicator + "_2headword_" +FeatureType.REL.name() , relationType, inputs.get(k)[0]));
			
			depParent = inst.depIdx[i] >=0 ? inputs.get(inst.depIdx[i])[0] : "<root>";
			featureList.add(param_g.toFeature(network, indicator + "_1depparent_" + FeatureType.REL.name() , relationType, depParent));
			depParent = inst.depIdx[k] >=0 ? inputs.get(inst.depIdx[k])[0] : "<root>";
			featureList.add(param_g.toFeature(network, indicator + "_2depparent_" +FeatureType.REL.name() , relationType, depParent));
						
			//featureList.add(param_g.toFeature(network, indicator + "_tokebigramnpath_" + FeatureType.REL.name() , relationType, depPathTokenBigram));
			
			int size = inputs.size();
			/*
			String ngram1 = "";
			String ngram2 = "";
			for(int ngram = 1; ngram < 2; ngram++) {
				if (i + ngram - 1 < size && k + ngram - 1 < size) {
					ngram1 += "-" + (inputs.get(i + ngram - 1)[0]);
					ngram2 += "-" + (inputs.get(k + ngram - 1)[0]);
					featureList.add(param_g.toFeature(network,indicator + " " +  FeatureType.RELATION.name() + "ngram" + ngram + "_", labelId, ngram1 + "-" + ngram2));
				}
			}*/
			
			
			
			
		}
		
//		featureList.add(param_g.toFeature(network,indicator + " " +   FeatureType.RELATION.name(), relationType, "relationtype:"));
//		
//		
//		featureList.add(param_g.toFeature(network,indicator + " " +   FeatureType.RELATION.name(), labelId, "fulltype:"));
		
		return featureList;
	}
	
	
	

	

}
