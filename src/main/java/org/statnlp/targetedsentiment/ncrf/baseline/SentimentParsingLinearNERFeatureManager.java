package org.statnlp.targetedsentiment.ncrf.baseline;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;
import java.util.AbstractMap.SimpleImmutableEntry;

import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.FeatureManager;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.NetworkIDMapper;
import org.statnlp.hypergraph.neural.MultiLayerPerceptron;
import org.statnlp.hypergraph.neural.NeuralNetworkCore;
import org.statnlp.targetedsentiment.common.TSFeature;
import org.statnlp.targetedsentiment.common.TSInstance;
import org.statnlp.targetedsentiment.common.TargetSentimentGlobal;
import org.statnlp.targetedsentiment.f.latent.TargetSentimentCompiler.PolarityType;
import org.statnlp.targetedsentiment.f.latent.TargetSentimentCompiler.SubNodeType;
import org.statnlp.targetedsentiment.ncrf.baseline.SentimentParsingLinearNERCompiler.*;
import org.statnlp.targetedsentiment.ncrf.linear.SentimentParsingLinearFeatureManager.WORD_FEATURE_TYPES;

public class SentimentParsingLinearNERFeatureManager extends FeatureManager {

	private NeuralNetworkCore net;
	public String neuralType;
	public boolean moreBinaryFeatures = false;
	
	public enum FeaType{
		word, tag, lw, lt, ltt, rw, rt, prefix, suffix,
		transition};
	private String OUT_SEP = MultiLayerPerceptron.OUT_SEP; 
	private String IN_SEP = MultiLayerPerceptron.IN_SEP;
	private final String START = "STR";
	private final String END = "END";

	public SentimentParsingLinearNERFeatureManager(GlobalNetworkParam param_g) {
		super(param_g, null);
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			this.net = (NeuralNetworkCore) param_g.getNNParamG().getNet(0);
		}
	}

	public SentimentParsingLinearNERFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
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

	public enum FEATURE_TYPES {
		Unigram, Bigram, Trigram, Fourgram, Transition
	}



	

	public static final HashMap<String, String> LinguisticFeatureLibNameFeatureMapping = new HashMap<String, String>() {
		{
			put("Neg_Prefix_Eng.txt", "Neg_Prefix_Eng");
			put("Neg_Prefix_Sp.txt", "Neg_Prefix_Sp");

			put("bad_words", "_sent_has_mal");
			put("good_words", "_sent_has_bien");
			put("curse_words", "_sent_has_curse_word");
			put("determiners", "_is_determiner");
			put("prepositions", "_is_preposition");

		}
	};

	static HashMap<String, ArrayList<String>> LinguisticFeaturesLibaryList = new HashMap<String, ArrayList<String>>();

	
	

	public void addPrefix(ArrayList<String[]> featureArr, String[] f, String prefix, String Sent, String NE) {

		featureArr.add(new String[] { prefix + f[0], f[1], f[2] });
	}

	public String escape(String s) {
		for (Character val : string_map.keySet()) {
			String target = val + "";
			if (s.indexOf(target) >= 0) {
				String repl = string_map.get(val);
				s = s.replace(target, repl);

			}
		}

		return s;
	}

	public String norm_digits(String s) {
		s = s.replaceAll("\\d+", "0");

		return s;

	}

	public String clean(String s) {
		s = s.toLowerCase();
		if (s.contains("http:") || s.contains("https:")) {
			s = "<WEBLINK>";
		} else {
			s = norm_digits(s);
			s = escape(s);
			s = s.replaceAll("[^A-Za-z0-9_]", "_");
		}

		return s;
	}

	public static final HashMap<Character, String> string_map = new HashMap<Character, String>() {
		{
			put('.', "_P_");
			put(',', "_C_");
			put('\'', "_A_");
			put('%', "_PCT_");
			put('-', "_DASH_");
			put('$', "_DOL_");
			put('&', "_AMP_");
			put(':', "_COL_");
			put(';', "_SCOL_");
			put('\\', "_BSL_");
			put('/', "_SL_");
			put('`', "_QT_");
			put('?', "_Q_");
			put('¿', "_QQ_");
			put('=', "_EQ_");
			put('*', "_ST_");
			put('!', "_E_");
			put('¡', "_EE_");
			put('#', "_HSH_");
			put('@', "_AT_");
			put('(', "_LBR_");
			put(')', "_RBR_");
			put('\"', "_QT0_");
			put('Á', "_A_ACNT_");
			put('É', "_E_ACNT_");
			put('Í', "_I_ACNT_");
			put('Ó', "_O_ACNT_");
			put('Ú', "_U_ACNT_");
			put('Ü', "_U_ACNT0_");
			put('Ñ', "_N_ACNT_");
			put('á', "_a_ACNT_");
			put('é', "_e_ACNT_");
			put('í', "_i_ACNT_");
			put('ó', "_o_ACNT_");
			put('ú', "_u_ACNT_");
			put('ü', "_u_ACNT0_");
			put('ñ', "_n_ACNT_");
			put('º', "_deg_ACNT_");
		}
	};

	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k, int children_k_index) {
		if (children_k.length > 1)
			throw new RuntimeException("The number of children should be at most 1, but it is " + children_k.length);

		FeatureArray fa = FeatureArray.EMPTY;
		
		TSInstance inst = ((TSInstance) network.getInstance());
		
		String sentence = inst.getSentence();
		
		long node_parent = network.getNode(parent_k);

		if (children_k.length == 0)
			return FeatureArray.EMPTY;

		ArrayList<String[]> inputs = (ArrayList<String[]>) inst.getInput();
		//ArrayList<String> outputs = (ArrayList<String>) inst.getOutput();
		ArrayList<Integer> featureList = new ArrayList<Integer>();

		int size = inst.size();

		long node_child = network.getNode(children_k[0]);

		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(node_parent);
		int[] ids_child = NetworkIDMapper.toHybridNodeArray(node_child);

		int pos_parent = size - ids_parent[0];
		int pos_child = size - ids_child[0];

		// int polar_parent = SentTypeSize - ids_parent[1];
		// int polar_child = SentTypeSize - ids_child[1];

		int nodetype_parent = ids_parent[2];//SubNodeTypeSize - ids_parent[1];
		int nodetype_child = ids_child[2];//SubNodeTypeSize - ids_child[1];

		int eId = -1;
		
		

		ArrayList<String[]> featureArr = new ArrayList<String[]>();
		//ArrayList<String[]> feature = new ArrayList<String[]>();
		
		TSFeature mynetwork = TargetSentimentGlobal.getFeaturebyNetworkId(inst.getInstanceId(), size);
		if (mynetwork.feature == null) {
			this.get_features(mynetwork, inputs, size);

		}
		
		featureList.add(this._param_g.toFeature(network,"transition_", eId + "", NodeType.values()[nodetype_parent] + "=>" + NodeType.values()[nodetype_child]));
		
		
		//String prevEntity =  childEId + "";
		
		
		if (nodetype_parent == NodeType.Root.ordinal()) {
			return this.createFeatureArray(network, featureList);
		} else {
			eId = nodetype_parent;
		}
		
		
		String entity = NodeType.values()[nodetype_parent].toString(); 
		String nextEntity = NodeType.values()[nodetype_child].toString(); 
		
		int pos = pos_parent;
		
		
		String word  = getToken(inputs, pos_parent, 0);

		
		//String tag = pos != inst.size() ?  inst.getInput().get(pos).getTag() : END;
		String lw  = getToken(inputs, pos_parent - 1, 0);
		String llw  = getToken(inputs, pos_parent - 2, 0);
		//String llt = pos > 1 ? sent.get(pos-2).getTag(): START;
		//String lt = pos > 0 ? sent.get(pos-1).getTag():START;
		String rw  = getToken(inputs, pos_parent + 1, 0);
		//String rt = pos < sent.length()-1? sent.get(pos + 1).getTag() :END;
		String rrw = getToken(inputs, pos_parent + 2, 0);
		//String rrt = pos < sent.length()-2? sent.get(pos + 2).getTag() :END;
		
		
		/*
		{
			String prefix = "";
			
			String[] current = inputs.get(pos);
			
			String NE = entity;
			String Sent = "";
			
	
			prefix = "word";
			
			
			for (String[] f : mynetwork.feature[pos]) {
				addPrefix(featureArr, f, prefix, Sent, NE);
				
			}

		

			for (int i = pos - 1; i >= pos - 3 && i >= 0; i--) {
				prefix = "_immediate_prev";
				for (String[] f : mynetwork.horizon_feature[i]) 
				{
					addPrefix(featureArr, f, prefix, Sent, NE);
				}
				
				prefix = "wordm" + (pos - i);
				for (String[] f : mynetwork.feature[i]) 
				
				{
					addPrefix(featureArr, f, prefix, Sent, NE);
				}


			}

			

			for (int i = pos + 1; i <= pos + 3 && i < size; i++) {
				prefix = "_immediate_next";
				for (String[] f : mynetwork.horizon_feature[i]) {
					addPrefix(featureArr, f, prefix, Sent, NE);
				}
				
				prefix = "wordp" + (i - pos );
				for (String[] f : mynetwork.feature[i]) {
					addPrefix(featureArr, f, prefix, Sent, NE);
				}


			}

			
			
			int num_sent = 0;
			for (String[] f : mynetwork.feature[pos]) {
				if (f[0].indexOf("_is_sent") >= 0)
					num_sent++;
			}
			
			prefix = "_sent_overall";
			String[] sent_overall = null;
	
			if (num_sent == 1)
				sent_overall = new String[] {"", "", "_has_one_sent" };

			if (num_sent == 2)
				sent_overall = new String[] { "", "", "_has_two_sent"};

			if (num_sent == 3)
				sent_overall = new String[] {"", "",  "_has_three_sent" };

			if (num_sent > 3)
				sent_overall = new String[] { "", "", "_has_alotta_sent"};
			
			if (num_sent > 0)
				addPrefix(featureArr, sent_overall, prefix, Sent, NE);
			
			
			if (featureArr.size() > 0) {
				

				for (String[] item : featureArr) {
				
					featureList.add(this._param_g.toFeature(network, item[0] , entity,  item[2]));
					
				}

			
				
			}
			
		}*/
		
		
		
		
		if(NetworkConfig.USE_NEURAL_FEATURES){
			if (eId != -1) {
				Object input = null;
				if(neuralType.startsWith("lstm")) {
					String sentenceInput = sentence;
					input = new SimpleImmutableEntry<String, Integer>(sentenceInput, pos);
				} else if(neuralType.equals("mlp")){
					input = llw+IN_SEP+lw+IN_SEP+word+IN_SEP+rw+IN_SEP+rrw ;//+OUT_SEP+llt+IN_SEP+lt+IN_SEP+tag+IN_SEP+rt+IN_SEP+rrt;
					
					//OUT_SEP 
				} else {
					input = word;
				}
				//since we don't want to use start tag. later need to add one.
				this.addNeural(network, 0, parent_k, children_k_index, input, eId - 1);
			}
		} else {
			featureList.add(this._param_g.toFeature(network,FeaType.word.name(), entity, word));
		}
		
		if (moreBinaryFeatures) {
			featureList.add(this._param_g.toFeature(network,FeaType.word.name(), entity, word));
			//featureList.add(this._param_g.toFeature(network,FeaType.tag.name(), entity,	tag));
			featureList.add(this._param_g.toFeature(network,FeaType.lw.name(), 	entity,	lw));
			//featureList.add(this._param_g.toFeature(network,FeaType.lt.name(), 	entity,	lt));
			featureList.add(this._param_g.toFeature(network,FeaType.rw.name(), 	entity,	rw));
			//featureList.add(this._param_g.toFeature(network,FeaType.rt.name(), 	entity,	rt));
			//featureList.add(this._param_g.toFeature(network,FeaType.ltt.name(), entity,	lt+","+tag));
			/****Add some prefix features******/
			for(int plen = 1;plen <= 3; plen++){
				if(word.length()>=plen){
					String suff = word.substring(word.length()-plen, word.length());
					featureList.add(this._param_g.toFeature(network,FeaType.suffix.name()+plen, entity, suff));
					String pref = word.substring(0,plen);
					featureList.add(this._param_g.toFeature(network,FeaType.prefix.name()+plen, entity, pref));
				}
			}
			featureList.add(this._param_g.toFeature(network,FeaType.transition.name()+"-wne", entity, word+":"+nextEntity));
			featureList.add(this._param_g.toFeature(network,FeaType.transition.name()+"lwne", entity,	lw+":"+nextEntity));
			featureList.add(this._param_g.toFeature(network,FeaType.transition.name()+"rwne", entity,	rw+":"+nextEntity));
			
		}
		
		
		fa = this.createFeatureArray(network, featureList);
		return fa;

	}
	
	String getToken(ArrayList<String[]> inputs, int pos) 
	{
		return getToken(inputs, pos, 0);
	}
	
	String getToken(ArrayList<String[]> inputs, int pos, int idx) 
	{
		if (pos >= 0 && pos < inputs.size()) 
		{
			return inputs.get(pos)[idx];
		} else if (pos < 0) {
			return "<Root>";
		} else {
			return "<X>";
		}
	}
	
	public void get_features(TSFeature mynetwork, List<String[]> inputs, int size) {

		mynetwork.feature = new ArrayList[size];
		mynetwork.horizon_feature = new ArrayList[size];

		for (int i = 0; i < size; i++) {

			ArrayList<String[]> featureArr = new ArrayList<String[]>();
			String[] current = inputs.get(i);
			String word = current[WORD_FEATURE_TYPES._id_.ordinal()];
			String escape_word = word.toLowerCase();
			escape_word = this.clean(escape_word);

			String prior_polarity = current[WORD_FEATURE_TYPES._sent_.ordinal()];
			prior_polarity = clean(prior_polarity);

			if (!prior_polarity.endsWith("_")) {
				featureArr.add(new String[] { "_is_sent", "", "" });
			}

			String prior_ther_polarity = current[WORD_FEATURE_TYPES._sent_ther_.ordinal()];
			prior_ther_polarity = prior_ther_polarity.replaceAll(":", "_").toLowerCase();

			if (!prior_ther_polarity.endsWith("_")) {
				featureArr.add(new String[] { "_is_sent_ther", "", "" });
			}

			String Out = "";

			featureArr.add(new String[] { WORD_FEATURE_TYPES._id_.toString(), "", escape_word });
			featureArr.add(new String[] { WORD_FEATURE_TYPES._sent_.toString(), "", prior_polarity });
			featureArr.add(new String[] { WORD_FEATURE_TYPES._sent_ther_.toString(), "", prior_ther_polarity });
			featureArr.add(new String[] { WORD_FEATURE_TYPES.brown_clusters3.toString() + ":", "", current[WORD_FEATURE_TYPES.brown_clusters3.ordinal()] });

			featureArr.add(new String[] { WORD_FEATURE_TYPES.brown_clusters5.toString() + ":", "", current[WORD_FEATURE_TYPES.brown_clusters5.ordinal()] });

			if (TargetSentimentGlobal.USE_POS_TAG)
				featureArr.add(new String[] { WORD_FEATURE_TYPES.postag.toString() + ":", "", current[WORD_FEATURE_TYPES.postag.ordinal()] });

			String word_parent = word;

			if (TargetSentimentGlobal.word_feature_on) {

				featureArr.add(new String[] { "_NE_word_length", Out, word_parent.length() + "" });

				String sentence_pos = "";
				if (i == 0)
					sentence_pos = "is_first";
				else if (i == 1)
					sentence_pos = "is_second";
				else if (i == 2)
					sentence_pos = "is_third";
				else if (i == size - 1)
					sentence_pos = "is_last";
				else if (i == size - 2)
					sentence_pos = "is_second_last";
				else if (i == size - 3)
					sentence_pos = "is_third_last";
				else
					sentence_pos = "in_middle";

				featureArr.add(new String[] { "_NE_sentence_pos:", Out, sentence_pos });

				String message_len_group = "";
				if (size <= 5)
					message_len_group = "1";
				else if (size <= 10)
					message_len_group = "2";
				else
					message_len_group = "3";

				featureArr.add(new String[] { "_NE_message_len_group:", Out, message_len_group });

				if (word_parent.length() == 3 || word_parent.length() == 4)
					featureArr.add(new String[] { "_NE_3_4_letters", Out, "" });

				if (Character.isUpperCase(word_parent.charAt(0)))
					featureArr.add(new String[] { "_NE_first_letter_upper", Out, "" });

				int uppercase_counter = 0;
				boolean hasDash = false, hasDigit = false, allLowercase = true;
				String punc = "";
				char prev = 0;
				int repeat = 0;
				for (int j = 0; j < word_parent.length(); j++) {
					char ch = word_parent.charAt(j);
					if (Character.isUpperCase(ch))
						uppercase_counter++;

					if (ch == '-')
						hasDash = true;

					if (Character.isDigit(ch))
						hasDigit = true;

					if (ch == prev)
						repeat++;

					prev = ch;
				}
				if (uppercase_counter > 1) {
					allLowercase = false;
					featureArr.add(new String[] { "_NE_more_than_one_letter_upper", Out, "" });
				}

				if (hasDash)
					featureArr.add(new String[] { "_NE_has_dash", Out, "" });

				if (hasDigit)
					featureArr.add(new String[] { "_NE_has_digit", Out, "" });

				if (allLowercase)
					featureArr.add(new String[] { "_NE_is_lower_case", Out, "" });

				if (repeat > 1)
					featureArr.add(new String[] { "_sent_has_repeat", Out, "" });

				if (word_parent.indexOf('!') >= 0)
					featureArr.add(new String[] { "_sent_has_exclaim", Out, "" });

				if (word_parent.indexOf("!!") >= 0)
					featureArr.add(new String[] { "_sent_has_many_exclaim", Out, "" });

				if (word_parent.indexOf("...") >= 0)
					featureArr.add(new String[] { "_sent_has_ellipse", Out, "" });

				if (word_parent.indexOf("?") >= 0)
					featureArr.add(new String[] { "_sent_has_question", Out, "" });

				if (word_parent.indexOf("??") >= 0)
					featureArr.add(new String[] { "_sent_has_many_question", Out, "" });

				String lowercase = word_parent.toLowerCase();
				String[] laugh = new String[] { "haha", "jaja", "jeje", "hehe", "hihi", "jiji" };
				for (int j = 0; j < laugh.length; j++)
					if (lowercase.indexOf(laugh[j]) >= 0) {
						featureArr.add(new String[] { "_sent_has_laugh", Out, "" });
						break;
					}

				String jerboa = current[WORD_FEATURE_TYPES.jerboa.ordinal()].toLowerCase();

				if (jerboa.indexOf("emoticon") >= 0 || jerboa.indexOf("smiling") >= 0 || jerboa.indexOf("frowning") >= 0) {
					if (word_parent.indexOf(')') >= 0 || word_parent.indexOf('D') >= 0 || word_parent.indexOf(']') >= 0)
						featureArr.add(new String[] { "_sent_jerb_happy", Out, "" });

					if (word_parent.indexOf('(') >= 0 || word_parent.indexOf('[') >= 0)
						featureArr.add(new String[] { "_sent_jerb_sad", Out, "" });

				}

				for (String filename : TargetSentimentGlobal.LinguisticFeaturesLibaryName) {
					if (filename.equals(TargetSentimentGlobal.LinguisticFeaturesLibaryNamePart[0]))
						continue;

					ArrayList<String> libarylist = LinguisticFeaturesLibaryList.get(filename);
					if (libarylist == null)
						continue;

					String featurename = LinguisticFeatureLibNameFeatureMapping.get(filename);
					if (featurename == null) {
						featurename = filename;
						if (featurename.endsWith(".txt")) {
							featurename = featurename.substring(0, featurename.length() - 4);
						}
					}

					if (libarylist.indexOf(word_parent) >= 0) {
						featureArr.add(new String[] { "_sent_" + featurename, "", "" });
						continue;

					}
				}

				String filename = TargetSentimentGlobal.LinguisticFeaturesLibaryNamePart[0];
				ArrayList<String> libarylist = LinguisticFeaturesLibaryList.get(filename);
				String featurename = LinguisticFeatureLibNameFeatureMapping.get(filename);
				if (featurename == null) {
					featurename = filename;
					if (featurename.endsWith(".txt")) {
						featurename = featurename.substring(0, featurename.length() - 4);
					}
				}
				if (libarylist != null)
					for (String prefix : libarylist)
						if (word_parent.indexOf(prefix) >= 0) {
							featureArr.add(new String[] { "_sent_" + featurename, "", "" });
						}

				if (escape_word.equals("my") || escape_word.equals("mis") || escape_word.equals("mi")) {
					featureArr.add(new String[] { "_sent_has_mi", "", "" });
				}
				if (escape_word.equals("nunca") || escape_word.equals("nadie")) {
					featureArr.add(new String[] { "_sent_has_no_word", "", "" });

				}
			}

			mynetwork.feature[i] = featureArr;
			mynetwork.horizon_feature[i] = new ArrayList<String[]>();

			for (String[] item : featureArr) {
				if (item[0].contains("sent_")) {
					mynetwork.horizon_feature[i].add(item);
				}
			}
		}

	}

}
