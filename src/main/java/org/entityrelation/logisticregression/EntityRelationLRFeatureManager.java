package org.entityrelation.logisticregression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.AbstractMap.SimpleImmutableEntry;

import org.entityrelation.common.AttributedWord;
import org.entityrelation.common.Entity;
import org.entityrelation.common.EntityRelationFeatureManager;
import org.entityrelation.common.EntityRelationGlobal;
import org.entityrelation.common.EntityRelationInstance;
import org.entityrelation.common.EntityRelationOutput;
import org.entityrelation.common.Relation;
import org.entityrelation.common.Utils;
import org.entityrelation.logisticregression.EntityRelationLRCompiler.NodeType;
import org.statnlp.commons.types.Sentence;
import org.statnlp.example.descriptor.CandidatePair;
import org.statnlp.example.descriptor.Config;
import org.statnlp.example.descriptor.Span;
import org.statnlp.example.descriptor.semeval.SemEvalLRMain.NeuralType;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.NetworkIDMapper;



public class EntityRelationLRFeatureManager extends EntityRelationFeatureManager {
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 651614281501396833L;
	
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
	
	protected boolean useGeneralTags = true;
	protected boolean dependencyFeatures = true;
	

	public EntityRelationLRFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}

	public EntityRelationLRFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
		super(param_g, neuralType, moreBinaryFeatures);
	}
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k, int children_k_index) {
		//if (children_k.length > 2) throw new RuntimeException("The number of children should be at most 2, but it is " + children_k.length);

		EntityRelationInstance inst = ((EntityRelationInstance) network.getInstance());
		EntityRelationOutput output = (EntityRelationOutput)inst.output;
		Relation goldRelation = output.relations.get(0);
		Entity entity1 = goldRelation.arg1;
		Entity entity2 = goldRelation.arg2;
		//int sentenceId = inst.sentenceId;
		int arg1Idx = entity1.entityIdx;
		int arg2Idx = entity2.entityIdx;
		
		int leftIdx = arg1Idx < arg2Idx ? arg1Idx : arg2Idx;
		int rightIdx = arg1Idx < arg2Idx ? arg2Idx : arg1Idx;
		
		long node_parent = network.getNode(parent_k);

		if (children_k.length == 0)
			return FeatureArray.EMPTY;

		int size = inst.size();
		String sentence = inst.getSentence();

		long node_child = network.getNode(children_k[0]);

		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(node_parent);
		int[] ids_child = NetworkIDMapper.toHybridNodeArray(node_child);

		ArrayList<String[]> inputs = (ArrayList<String[]>) inst.getInput();
	

		int nodetype_parent = ids_parent[0];
		int nodetype_child = ids_child[0];

		NodeType nodetypeParent = NodeType.values()[nodetype_parent];
		NodeType nodetypeChild = NodeType.values()[nodetype_child];
		

		ArrayList<Integer> featureList = new ArrayList<Integer>();
		ArrayList<Integer> continuousFeatureList = new ArrayList<Integer>();
		ArrayList<Double> continuousFeatureValueList = new ArrayList<Double>();

		
		FeatureArray fa = this.createFeatureArray(network, new int[] {});
		FeatureArray fp = fa;
		
	
		if(nodetypeParent == NodeType.Root || nodetypeParent == NodeType.X){
			return fa;
		}
		

		int r = ids_parent[1];
		
		
		boolean isSelfRelation = (EntityRelationGlobal.ADD_SELF_RELATION && r== 0);
		
		
		String relationType = EntityRelationOutput.getRELATIONS(r).form;
		String entity1Type = entity1.type.form;
		String entity2Type = entity2.type.form;
		String labelId = "[" + relationType + "," + entity1Type + "," + entity2Type + "]";
		
		
		
		if (EntityRelationGlobal.ENABLE_DISCRETE_FEATURE) {
			
			if (Relation.isNORelation(r)) {
				relationType = Relation.NO_RELATION;
			}
			
			//fp = addRelFeatures(fp, network, inst, entity1, entity2, relationType, parent_k, children_k_index);
			fp = addPartialRelFeatures(fp, network, inst, entity1, entity2, relationType, parent_k, children_k_index);

		}
		
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			//String indicator = nodetypeParent+"-"+childNodeType + "-";
	
			int i = entity1.span[0];
			int k = entity2.span[0];
			
			
			Object input = null;
			/*if (neuralType.startsWith("lstm")) {
				String sentenceInput = sentence;
				input = new SimpleImmutableEntry<String, Integer>(sentenceInput, pos);
				this.addNeural(network, 0, parent_k, children_k_index, input, r);
			} else*/ 
			if (neuralType.equals("continuous0")) { 
				
				inst.getDep(i, k);
				int[] path = inst.depPath[i][k];
				
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
				

			} else if (neuralType.equals("cnn")) {
				addCNNFirstNeuralFeatures(network, parent_k, children_k_index, r, inst, entity1, entity2);
			}
				
		}
	

		//FeatureArray contFa = this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList);
		fp = fp.addNext(this.createFeatureArray(network, featureList));
		fp = fp.addNext(this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList));
		
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
			if (this.useGeneralTags) {
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
			if (this.useGeneralTags) {
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
		
		if (this.dependencyFeatures) {
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
	
	
	
	public FeatureArray addPartialRelFeatures (FeatureArray fa, Network network, EntityRelationInstance inst,
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
			if (this.useGeneralTags) {
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
		//a1s.add(this._param_g.toFeature(network, RelFeaType.a1.name(), relationLabel, a1Seq.toString()));
		fa = fa.addNext(this.createFeatureArray(network, a1s));

		
		//arg2 features
		List<Integer> a2s = new ArrayList<>(arg2Span.span[1] - arg2Span.span[0] + 2);
		StringBuilder a2Seq = new StringBuilder("");
		for (int i = arg2Span.span[0]; i < arg2Span.span[1]; i++) {
			String curr_word = input.get(i)[0];
			String curr_tag = input.get(i)[1];
			if (this.useGeneralTags) {
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
		//a2s.add(this._param_g.toFeature(network, RelFeaType.a2.name(), relationLabel, a2Seq.toString()));
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
		//bet.add(this._param_g.toFeature(network, RelFeaType.bettseq.name(), relationLabel, tagSeq.toString()));
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
		
		if (this.dependencyFeatures) {
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

public ArrayList<Integer> extractRelationFeatures_old(EntityRelationInstance inst, Network network, ArrayList<String[]> inputs, int i, int k, String indicator, String relationType, String entity1Type, String entity2Type) {
		
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
			
			featureList.add(param_g.toFeature(network, indicator + "_tokenpath_" + FeatureType.REL.name() +"_labelId" , labelId, depPathToken));
			featureList.add(param_g.toFeature(network, indicator + "+deppath_" +FeatureType.REL.name()+"_labelId" , labelId, depPathDepLabel));
			
			
			featureList.add(param_g.toFeature(network, indicator + "_1headword_" + FeatureType.REL.name() , relationType, inputs.get(i)[0]));
			featureList.add(param_g.toFeature(network, indicator + "_2headword_" +FeatureType.REL.name() , relationType, inputs.get(k)[0]));
			
			depParent = inst.depIdx[i] >=0 ? inputs.get(inst.depIdx[i])[0] : "<root>";
			featureList.add(param_g.toFeature(network, indicator + "_1depparent_" + FeatureType.REL.name() , relationType, depParent));
			depParent = inst.depIdx[k] >=0 ? inputs.get(inst.depIdx[k])[0] : "<root>";
			featureList.add(param_g.toFeature(network, indicator + "_2depparent_" +FeatureType.REL.name() , relationType, depParent));
						
			//featureList.add(param_g.toFeature(network, indicator + "_tokebigramnpath_" + FeatureType.REL.name() , relationType, depPathTokenBigram));
			
			int size = inputs.size();
			String ngram1 = "";
			String ngram2 = "";
			for(int ngram = 1; ngram < 3; ngram++) {
				if (i + ngram - 1 < size && k + ngram - 1 < size) {
					ngram1 += "-" + (inputs.get(i + ngram - 1)[0]);
					ngram2 += "-" + (inputs.get(k + ngram - 1)[0]);
					featureList.add(param_g.toFeature(network,indicator + " " +  FeatureType.RELATION.name() + "ngram" + ngram + "_", labelId, ngram1 + "-" + ngram2));
					featureList.add(param_g.toFeature(network,indicator + " " +  FeatureType.RELATION.name() + "ngram1" + ngram + "_", labelId, ngram1));
					featureList.add(param_g.toFeature(network,indicator + " " +  FeatureType.RELATION.name() + "ngram2" + ngram + "_", labelId, ngram1));
					
				}
			}
			
			
			
			
		}
		
		featureList.add(param_g.toFeature(network,indicator + " " +   FeatureType.RELATION.name(), relationType, "relationtype:"));
		
		
		featureList.add(param_g.toFeature(network,indicator + " " +   FeatureType.RELATION.name(), labelId, "fulltype:"));
		
		return featureList;
	}
	
	

}
