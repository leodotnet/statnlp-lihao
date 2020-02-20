package org.entityrelation.crf;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.entityrelation.common.AttributedWord;
import org.entityrelation.common.Entity;
import org.entityrelation.common.EntityRelationFeatureManager;
import org.entityrelation.common.EntityRelationGlobal;
import org.entityrelation.common.EntityRelationInstance;
import org.entityrelation.common.EntityRelationOutput;
import org.entityrelation.common.Relation;
import org.entityrelation.crf.EntityRelationLinearCRFCompiler.NodeType;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkIDMapper;



public class EntityRelationLinearCRFeatureManager extends EntityRelationFeatureManager {
	
	private enum RelFeaType {wm1, hm1, wm2, hm2, hm12,
		wbnull, wbfl, wbf, wbl, wbo, lexbigram, bm1f, bm1l, am2f, am2l,
		et12, etsub12, ml12, ml12et12, ml12etsub12, nummb,numwb, m1cm2, m2cm1, et12m1cm2, et12m2cm1, etsub12m1cm2, etsub12m2cm1, hm12m1cm2, hm12m2cm1,
		cm1m1, cp1p1, cm2m1, cm1p1, cp1p2,
		cphbnull, cphbfl, cphbf, cphbl, cphbo, cphbm1f, cphbm1l,cpham2f, cpham2l, cpp, cpph,
		h1head, h2head, depPath, depLabel, et12samenp, et12samepp, et12samevp}

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
	

	public EntityRelationLinearCRFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}

	public EntityRelationLinearCRFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
		super(param_g, neuralType, moreBinaryFeatures);
	}
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k, int children_k_index) {
		//if (children_k.length > 2) throw new RuntimeException("The number of children should be at most 2, but it is " + children_k.length);

		EntityRelationInstance inst = ((EntityRelationInstance) network.getInstance());
		EntityRelationOutput output = (EntityRelationOutput)inst.output;
		
		
		long node_parent = network.getNode(parent_k);

		if (children_k.length == 0)
			return FeatureArray.EMPTY;

		int size = inst.size();
		String sentence = inst.getSentence();

		long node_child = network.getNode(children_k[0]);

		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(node_parent);
		int[] ids_child = NetworkIDMapper.toHybridNodeArray(node_child);

		ArrayList<String[]> inputs = (ArrayList<String[]>) inst.getInput();
	
		int pos_parent = size - ids_parent[0];
		int pos_child = size - ids_child[0];
		
		int nodetype_parent = ids_parent[1];
		int nodetype_child = ids_child[1];
		
		int entitytype_parent = ids_parent[2];
		int entitytype_child = ids_child[2];

		NodeType nodetypeParent = NodeType.values()[nodetype_parent];
		NodeType nodetypeChild = NodeType.values()[nodetype_child];
		

		ArrayList<Integer> featureList = new ArrayList<Integer>();
		ArrayList<Integer> continuousFeatureList = new ArrayList<Integer>();
		ArrayList<Double> continuousFeatureValueList = new ArrayList<Double>();

		
		FeatureArray fa = this.createFeatureArray(network, new int[] {});
		FeatureArray fp = fa;
		
		if (EntityRelationGlobal.DUMMY_FEATURE) { // for debug			
			int id = inst.getInstanceId();
			
			if (id < 0)
				id = -id;
			
			String dummyStr = id + " " + NodeType.values()[ids_parent[0]] + node_parent + ":" + Arrays.toString(ids_parent) + " ";

			String type =  nodetypeParent + " ";
			
			for(int i = 0; i < children_k.length; i++) {
				long child = network.getNode(children_k[i]);
				int[] child_ids = NetworkIDMapper.toHybridNodeArray(child);
				
				
				String nodeType = NodeType.values()[child_ids[0]].toString();
				
				dummyStr += " - " + child + ":" + nodeType +  Arrays.toString(child_ids) + " ";
				
				type +=  "-" + nodeType;
			}

			featureList.add(this._param_g.toFeature(network, "dummy ", dummyStr, ""));
			
			
			fp = fp.addNext(this.createFeatureArray(network, featureList));
			return fa;
		}
		
	
		if(nodetypeParent == NodeType.X){
			return fa;
		}
		
		
		featureList.add(this._param_g.toFeature(network, FeatureType.TRANSITION.name(), nodetypeParent.name(), nodetypeChild.name()));
		featureList.add(this._param_g.toFeature(network, FeatureType.TRANSITION.name(), nodetypeParent.name() + entitytype_parent, nodetypeChild.name() + entitytype_child));
		
		if(nodetypeParent == NodeType.Root){
			return fa;
		}
		
		if (EntityRelationGlobal.ENABLE_DISCRETE_FEATURE) {
			
			String indicator = nodetypeParent.name() + "-" + nodetypeChild.name();
			String entityType = nodetypeParent == NodeType.O ? "O" : EntityRelationOutput.getENTITY(entitytype_parent).form;
			
			try {
			ArrayList<Integer> featureAtPos = extractFeatures(inst, network, inputs, pos_parent, size, indicator, entityType);
			featureList.addAll(featureAtPos);
			} catch (Exception e)
			{
				System.out.println();
				System.out.println();
			}
			
		}
		

		//FeatureArray contFa = this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList);
		fp = fp.addNext(this.createFeatureArray(network, featureList));
		fp = fp.addNext(this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList));
		
		return fa;

	}	
	
	public ArrayList<Integer> extractRelationFeatures(EntityRelationInstance inst, Network network, ArrayList<String[]> inputs, int i, int k, String indicator, String relationType, String entity1Type, String entity2Type) {
		
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
	
	
	public ArrayList<Integer> extractFeatures(EntityRelationInstance inst, Network network, ArrayList<String[]> inputs, 
			int pos, int size, String indicator, String entityType) {
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
	


}
