package org.statnlp.sentiment.spanmodel;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.NetworkIDMapper;
import org.statnlp.hypergraph.neural.NeuralNetworkCore;
import org.statnlp.sentiment.spanmodel.SpanModelCompiler.NodeType;
import org.statnlp.sentiment.spanmodel.SpanModelCompiler.OPNodeType;
import org.statnlp.sentiment.spanmodel.SpanModelCompiler.SentimentStateType;
import org.statnlp.sentiment.spanmodel.common.SentimentInstance;
import org.statnlp.sentiment.spanmodel.common.SpanModelGlobal;
import org.statnlp.sentiment.spanmodel.common.SpanModelSuperFeatureManager;






public class SpanModelFeatureManager extends SpanModelSuperFeatureManager {
	
	private NeuralNetworkCore net;
	public String neuralType;
	public boolean moreBinaryFeatures = false;

	
	public SpanModelFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			this.net = (NeuralNetworkCore) param_g.getNNParamG().getNet(0);
		}
	}
	
	
	public SpanModelFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
		super(param_g);
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			this.net = (NeuralNetworkCore) param_g.getNNParamG().getNet(0);
		}
		this.neuralType = neuralType;
		this.moreBinaryFeatures = moreBinaryFeatures;
	}
	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1376229168119316535L;
	
	public enum FEATURE_TYPES {Unigram, Bigram, Trigram, Fourgram, Transition}
	
	public enum WORD_FEATURE_TYPES {
		_id_, ne, brown_clusters5, jerboa, brown_clusters3, _sent_, _sent_ther_, sentiment, postag
	};
	
	int OPNodeTypeSize = OPNodeType.values().length;
	int NodeTypeSize = NodeType.values().length;
	int WordFeatureTypeSize = WORD_FEATURE_TYPES.values().length;
	
	//public static String[] Subjectivity = new String[]{"non-neutral1", "non-neutral1", "neutral1"};
	
	
	static HashMap<String, ArrayList<String>> LinguisticFeaturesLibaryList = new HashMap<String, ArrayList<String>>();
	
	
	
	public void loadLinguisticFeatureLibary()
	{
		LinguisticFeaturesLibaryList.clear();
		/*
		Scanner scan = null;
		for(String filename : TargetSentimentGlobal.LinguisticFeaturesLibaryName)
		{
			ArrayList<String> libarylist = new ArrayList<String>();
			try {
				scan = new Scanner(new File(TargetSentimentGlobal.feature_file_path + filename));
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			
			while(scan.hasNextLine())
			{
				libarylist.add(scan.nextLine().trim());
			}
			
			scan.close();
			
			LinguisticFeaturesLibaryList.put(filename, libarylist);
		}*/
	}
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k, int children_k_index) {
			
		if(children_k.length > 2)
			throw new RuntimeException("The number of children should be at most 2, but it is "+children_k.length);
		
		SentimentInstance inst = ((SentimentInstance)network.getInstance());
		
		List<String> inputs = (List<String>)inst.getInput();
		Integer output = (Integer)inst.getOutput();		
		
		
		long node_parent = network.getNode(parent_k);
		
		if(children_k.length == 0)
			return FeatureArray.EMPTY;

		
		int size = inst.size();
		
		long node_child_sentiment = network.getNode(children_k[0]);
		
		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(node_parent);
		int[] ids_child_sentiment = NetworkIDMapper.toHybridNodeArray(node_child_sentiment);


		int pos_parent = ids_parent[0];
		int pos_child_sentiment = ids_child_sentiment[0];
		
		int nodetype_parent = ids_parent[4];
		int nodetype_child_sentiment = ids_child_sentiment[4];
		
		int sentimentState_parent = ids_parent[1];
		int sentimentState_child = ids_child_sentiment[1];
		
		int num_sent = 0;

		String word_parent = "";
		String word_child = "";
		
		int pos_lstm = -1;
		
	
		FeatureArray fa = FeatureArray.EMPTY;
		
		ArrayList<Integer> feature = new ArrayList<Integer>();
		ArrayList<String[]> featureArr = new ArrayList<String[]>();
		String WordEmbeddingFeature = null;
		
		String Out = "O";
		int eId = sentimentState_parent;
		
		if (nodetype_parent == NodeType.Root.ordinal())
		{
			
		}
		else if (nodetype_child_sentiment == NodeType.Leaf.ordinal())
		{
			
		}
		else
		{
			long node_child_op = network.getNode(children_k[1]);
			int[] ids_child_op = NetworkIDMapper.toHybridNodeArray(node_child_op);
			int pos_child_op = ids_child_op[0];
			int op_child = ids_child_op[1];
			int nodetype_child_op = ids_child_op[4];
			
			int wordIdxFrom = ids_child_op[2];
			int wordIdxTo = ids_child_op[3];
			
			pos_lstm = wordIdxTo - 1;
			
			ArrayList<String> words = new ArrayList<String>();
			
			String OPForm =  OPNodeType.values()[op_child].name();
			String OPFormPrefix = OPForm.substring(0, OPForm.lastIndexOf('_'));
			String sentimentState_parent_Form = SentimentStateType.values()[sentimentState_parent].name();
			String sentimentState_child_Form = SentimentStateType.values()[sentimentState_child].name();
			
			
			for(int i = wordIdxFrom; i < wordIdxTo; i++)
			{
				String word = inputs.get(i).toLowerCase();
				if (!SpanModelGlobal.highFreqWords.contains(word))
					words.add(word);
			}
			
			for(int ngram = 1; ngram <= NGRAM; ngram++)
			{
				for(int i = 0; i + ngram - 1 < words.size(); i++)
				{
					String featureForm = null;
					for(int j = 0; j < ngram; j++)
					{
						if (featureForm == null)
							featureForm = words.get(i + j);
						else
							featureForm += "-" + words.get(i + j);
					}
					
					//feature.add(this._param_g.toFeature(network, "NGRAM_" + ngram + ":" , OPForm,  featureForm));
					feature.add(this._param_g.toFeature(network, "NGRAM_" + ngram + ":" , OPFormPrefix,  featureForm));
					feature.add(this._param_g.toFeature(network, "NGRAM_" + ngram + ":" , OPForm + "-" + sentimentState_child_Form + "-" + sentimentState_parent_Form,  featureForm));
					//feature.add(this._param_g.toFeature(network, "NGRAM_" + ngram + ":" , OPFormPrefix + "-" + sentimentState_child_Form + "-" + sentimentState_parent_Form,  featureForm));
					
				}
			}
			
			
			if (wordIdxTo < size)
			for(int i = 0; i < words.size(); i++)
			{
				String featureForm = words.get(i) + "-" + inputs.get(wordIdxTo);
				feature.add(this._param_g.toFeature(network, "NGRAM_1*" + ":" , OPFormPrefix,  featureForm));
				feature.add(this._param_g.toFeature(network, "NGRAM_1*" +  ":" , OPForm + "-" + sentimentState_child_Form + "-" + sentimentState_parent_Form,  featureForm));
				
				
			}
			
			/*
			for(int i = 0; i < words.size(); i++)
				for(int j = i + 1; j < words.size(); j++)
				{
					String featureForm = words.get(i) + "-" + words.get(j);
					//feature.add(this._param_g.toFeature(network, "*BiGRAM_" + ":" , OPForm, "featureForm:" + featureForm));
					feature.add(this._param_g.toFeature(network, "BiGRAM_" +  ":" , OPFormPrefix, "featureForm:" + featureForm));
					feature.add(this._param_g.toFeature(network, "BiGRAM_" + ":" , OPForm + "-" + sentimentState_child_Form + "-" + sentimentState_parent_Form, "featureForm:" + featureForm));
					
				}*/
			
			
			
			feature.add(this._param_g.toFeature(network, "SentimentTransition:" , sentimentState_parent_Form, sentimentState_child_Form + OPForm));
			
			
		}
		

		
		if (feature.size() > 0)
		{
			if (SpanModelGlobal.ENABLE_WORD_EMBEDDING)
			{
				
				if(NetworkConfig.USE_NEURAL_FEATURES && pos_lstm != -1) // && WordEmbeddingFeature != null)
				{
					
					Object input = null;
					if(neuralType.equals("lstm")) {
						input = new SimpleImmutableEntry<String, Integer>(inst.getSentence(), pos_lstm);
					} 
					//else if(neuralType.equals("mlp")){
					//	input = llw+IN_SEP+lw+IN_SEP+currWord+IN_SEP+rw+IN_SEP+rrw+OUT_SEP+llt+IN_SEP+lt+IN_SEP+currTag+IN_SEP+rt+IN_SEP+rrt;
					//} 
					else {
						input = WordEmbeddingFeature;
					}
					this.addNeural(network, 0, parent_k, children_k_index, input, eId);
				} 
			}
			
			
			int f[] = new int[feature.size()];
			
			for(int i = 0; i < f.length; i++)
				f[i] = feature.get(i);
	
			fa =  new FeatureArray(f);
		}
		
		
		return fa;
	}
	
	
	
	public void addPrefix(ArrayList<String[]> featureArr, String[] f, String prefix, String Sent, String NE)
	{
		
		featureArr.add(new String[] { prefix + f[0], f[1], f[2]});
	}
	
	
	public String escape(String s)
	{
		for(Character val : string_map.keySet())
		{
			String target = val + "";
			if (s.indexOf(target) >= 0)
			{
				String repl = string_map.get(val);
				s = s.replace(target, repl);
				
			}
		}
		
		return s;
	}
	
	public String norm_digits(String s)
	{
		s = s.replaceAll("\\d+", "0");
		
		return s;
		
	}
	public String clean(String s)
	{
		String str;
		if (s.startsWith("http://") || s.startsWith("https://"))
		{
			str = "<WEBLINK>";
		}
		/*else if (s.startsWith("@"))
		{
			str = "<USERNAME>";
		}*/
		else
		{
			str = norm_digits(s.toLowerCase());
			str = escape(str);
			str = str.replaceAll("[^A-Za-z0-9_]", "_");
		}
		
		return str;
	}
	
	
	public static final HashMap<Character , String> string_map = new HashMap<Character , String>(){
		{
			put('.', "_P_");	put(',', "_C_");
			put('\'', "_A_"); 	put('%', "_PCT_");
			put('-', "_DASH_");	put('$', "_DOL_");
			put('&', "_AMP_");	put(':', "_COL_");
			put(';', "_SCOL_");	put('\\', "_BSL_");
			put('/', "_SL_");	put('`',"_QT_");
			put('?',"_Q_");		put('¿',"_QQ_");
			put('=', "_EQ_");	put('*', "_ST_");
			put('!', "_E_");	put('¡', "_EE_");
			put('#', "_HSH_");	put('@', "_AT_");
			put('(', "_LBR_");	put(')', "_RBR_"); 
	        put('\"', "_QT0_"); put('Á',"_A_ACNT_"); 
	        put('É',"_E_ACNT_");put('Í',"_I_ACNT_");
            put('Ó',"_O_ACNT_");put('Ú',"_U_ACNT_"); put('Ü',"_U_ACNT0_"); put('Ñ',"_N_ACNT_");
            put('á',"_a_ACNT_");put('é',"_e_ACNT_"); put('í',"_i_ACNT_"); put('ó',"_o_ACNT_");
            put('ú',"_u_ACNT_");put('ü',"_u_ACNT0_"); put('ñ',"_n_ACNT_"); put('º',"_deg_ACNT_");
		}
	};
	


	public static final HashMap<String , String> LinguisticFeatureLibNameFeatureMapping = new HashMap<String , String>(){
	{
		put("Neg_Prefix_Eng.txt", "Neg_Prefix_Eng");
		put("Neg_Prefix_Sp.txt", "Neg_Prefix_Sp");
		
		put("bad_words","_sent_has_mal");
		put("good_words", "_sent_has_bien");
		put("curse_words", "_sent_has_curse_word");
		put("determiners", "_is_determiner");
		put("prepositions", "_is_preposition");
		
		
	}
	
	
	
	//public static final String[] gg = new String[] { "a", "b" };
	//public static final Set<String> ggg = new HashSet<>(Arrays.asList(gg));

};
	
	
	
	

}


