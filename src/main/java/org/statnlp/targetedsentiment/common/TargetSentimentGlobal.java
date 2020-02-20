package org.statnlp.targetedsentiment.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.statnlp.commons.types.Label;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.NetworkConfig;



public class TargetSentimentGlobal {

	public static boolean OUTPUT_SENTIMENT_SPAN = false;
	
	public static boolean USE_POS_TAG = false;
	
	public static boolean word_feature_on = true;
	
	public static boolean WITHOUT_HIDDEN_SENTIMENT = false;
	
	public static boolean ENABLE_WORD_EMBEDDING = false;
	
	public static WordEmbedding Word2Vec = null;
	
	public static ArrayList<Integer> SENTIMENT_SPAN_SPLIT = new ArrayList<Integer>();
	
	
	public static String[] LABELS = new String[] {"O", "B-positive", "I-positive", "B-neutral", "I-neutral", "B-negative", "I-negative"};//, "B-unknown","I-unknown"};
	
	
	public static HashMap<String, Integer> LABEL_MAPPING = new HashMap<String, Integer>();
	
	public static String feature_file_path = "";
	
	public static boolean ENABLE_ADDITIONAL_DATA = false;
	
	public static String[] NERTYPE = new String[]{"person", "geo-loc", "movie", "facility", "positive", "negative", "neutral"};
	
	public static HashSet NETHashSet = new HashSet();
	
	public static boolean USE_PREPROCESS_SCRIPT = false;
	
	public static boolean EMBEDDING_WORD_LOWERCASE = true;
	
	public static boolean FIXNE = false;
	
	public static boolean NGRAM = false;
	
	public static boolean OVERLAPPING_FEATURES = false;
	
	public static int NER_SPAN_MAX = -1;
	
	public static int O_SPAN_MAX = 10;
	
	public static boolean ENABLE_DISCRETE_FEATURE = true;
	
	public static int SENTIMENT_WINDOW_SIZE = 2;
	
	public static boolean USE_UNIGRAM_DISCRETE_FEATURES = true;
	
	public static boolean USE_WORD_ONLY = false;
	
	public static SentimentDict SentDict = new SentimentDict();
	
	public static boolean ALLOW_NULL_TARGET = false;
	
	public static boolean EVAL_EXCLUDE_NULL_TARGET = false;
	
	public static boolean USE_POSITION_EMBEDDEING = false;
	
	public static int POSITION_EMBEDDING_SIZE = 25;
	
	public static int POSITION_VOCAB_SZIE = 800;
	
	public static boolean USE_SENTIMENTYPE_EMBEDDING = false;
	
	public static boolean USE_ALLWORDS_AS_CANDIDICATE = false;
	
	public static String testVocabFile = null;
	
	public static boolean INCLUDE_FEATURE_AFTER_OR_BEFORE = true;
	public static boolean INCLUDE_FEATURE_ORIG = true;
	public static boolean INCLUDE_FEATURE_ORIG_WITH_POLARITY = true;
	public static boolean INCLUDE_FEATURE_SPLIT = true;
	public static boolean INCLUDE_FEATURE_SPLIT_WITH_POLARITY = true;
	public static boolean SPLIT_SCOPE_BEGIN_END = true;

	
	
	
	public static void init()
	{
		createLabelMapping();
		for(int i = 0; i < NERTYPE.length; i++)
			NETHashSet.add(NERTYPE[i]);
		
		SentDict.loadLexicons("mpqa,sent140,negation,intensity");
	}
	
	public static void clearTemporalData()
	{
		networkId2feature.clear();
		networkId2featureDev.clear();
	}
	
	
	public static HashMap<Integer, TSFeature> networkId2feature = new HashMap<Integer, TSFeature>();
	public static HashMap<Integer, TSFeature> networkId2featureDev = new HashMap<Integer, TSFeature>();
	
	
	public static TSFeature getFeaturebyNetworkId(int networkId, int size)
	{
		TSFeature feature = null;
		
		if (NetworkConfig.STATUS == NetworkConfig.ModelStatus.TRAINING || NetworkConfig.STATUS == NetworkConfig.ModelStatus.TESTING) {
			feature = networkId2feature.get(networkId);

			if (feature == null) {
				feature = new TSFeature();
				networkId2feature.put(networkId, feature);
			}

			
		} else {  //trainDev
			feature = networkId2featureDev.get(networkId);

			if (feature == null) {
				feature = new TSFeature();
				networkId2featureDev.put(networkId, feature);
			}
		}
		
		return feature;
	}
	/*
	public static TSFeature getFeaturebyNetworkId(int networkId, int size)
	{
		TSFeature feature = null;
		
			feature = networkId2feature.get(networkId);

			if (feature == null) {
				feature = new TSFeature();
				networkId2feature.put(networkId, feature);
			}
		
		return feature;
	}*/
	
	/*public static HashMap<Integer, ArrayList<String[]>[]> networkId2feature = new HashMap<Integer, ArrayList<String[]>[]>();
	
	public static HashMap<Integer, ArrayList<String[]>[]> networkId2horizon_feature = new HashMap<Integer, ArrayList<String[]>[]>();
	
	public static ArrayList<String[]>[] getFeaturebyNetworkId(int networkId, int size)
	{
		ArrayList<String[]>[] feature = networkId2feature.get(networkId);
		
		if (feature == null)
		{
			feature = new ArrayList[size];
			networkId2feature.put(networkId, feature);
		}
		
		return feature;
	}
	
	public static ArrayList<String[]>[] getHorizonFeaturebyNetworkId(int networkId, int size)
	{
		ArrayList<String[]>[] feature = networkId2horizon_feature.get(networkId);
		
		if (feature == null)
		{
			feature = new ArrayList[size];
			networkId2horizon_feature.put(networkId, feature);
		}
		
		return feature;
	}*/
	
	
	public static void createLabelMapping()
	{
		for (int i = 0; i < LABELS.length; i++)
		{
			LABEL_MAPPING.put(LABELS[i], i);
		}
	}
	
	public static Label getLabel(String labelform)
	{
		Integer id = LABEL_MAPPING.get(labelform);
		if (id == null)
		{
			System.err.println("Unexpected label:" + labelform);
			System.exit(0);
		}
		Label label = new Label(labelform, id);
		return label;
	}
	
	
	public static void initWordEmbedding(String lang, int embeddingSize)
	{
		Word2Vec = new WordEmbedding(lang, embeddingSize);
	}
	
	
	
	public static String[] LinguisticFeaturesLibaryName = new String[] {
		"bad_words", "good_words", "curse_words", "prepositions",
		"determiners", "syllables", "Intensifiers_Eng.txt", "Neg_Abbrev_Eng.txt",
		"Neg_Prefix_Eng.txt", "Neg_Slang_Eng.txt", "Pos_Abbrev_Eng.txt",
		"Pos_Slang_Eng.txt" };

	public static String[] LinguisticFeaturesLibaryName_es = new String[] {
		"bad_words", "good_words", "curse_words", "prepositions",
		"determiners", "syllables","Intensifiers_Sp.txt", "Neg_Abbrev_Sp.txt",
		"Neg_Prefix_Sp.txt", "Neg_Slang_Sp.txt", "Pos_Abbrev_Sp.txt",
		"Pos_Slang_Sp.txt" };
	
	public static String[] LinguisticFeaturesLibaryNamePart = new String[]{"Neg_Prefix_Eng.txt"};
	
	public static String[] LinguisticFeaturesLibaryNamePart_es = new String[]{"Neg_Prefix_Sp.txt"};
	
	public static final HashMap<String , String> dataSet = new HashMap<String , String>(){
		{
			put("semeval", "data//semeval2016");
			put("Z_data", "data//Z-data");
			
		}
	};

	public static boolean DUMP_FEATURE = false;

	public static int MAX_LENGTH_LENGTH = 200;

	public static boolean FIX_EMBEDDING = false;
	
	public static String getInPath(String modelname)
	{
		String inPath = dataSet.get(modelname);
		
		if (inPath == null)
		{
			inPath = "data//Twitter_";
		}
		
		return inPath;
	}
	
	public static String getInPath(String modelname, String datasetname)
	{
		String inPath = dataSet.get(modelname);
		
		if (inPath == null)
		{
			inPath = "data//" + datasetname + "_";
		}
		
		return inPath;
	}
	
	
	public static void setLang(String lang)
	{
		if (lang.equals("es"))
		{
			LinguisticFeaturesLibaryName = LinguisticFeaturesLibaryName_es;
			LinguisticFeaturesLibaryNamePart = LinguisticFeaturesLibaryNamePart_es;
		}
		
		if (lang.equals("bilingual"))
		{
			LinguisticFeaturesLibaryName = LinguisticFeaturesLibaryName_es;
			LinguisticFeaturesLibaryNamePart = LinguisticFeaturesLibaryNamePart_es;
		}
		
	}
	
	public static boolean startOfEntity(int pos, int size, ArrayList<Label> outputs)
	{
		String label = outputs.get(pos).getForm();
		if (label.startsWith("B"))
			return true;
		
		if (pos == 0 && label.startsWith("I"))
			return true;
		
		if (pos > 0)
		{
			String prev_label =  outputs.get(pos - 1).getForm();
			if (label.startsWith("I") && prev_label.startsWith("O"))
				return true;
		}
		
		
		return false;
	}
	
	public static boolean endofEntity(int pos, int size,  ArrayList<Label> outputs)
	{
		String label = outputs.get(pos).getForm();
		if (!label.startsWith("O"))
		{
			if (pos == size - 1)
				return true;
			else {
				String next_label =  outputs.get(pos + 1).getForm();
				if (next_label.startsWith("O") || next_label.startsWith("B"))
					return true;
			}
		}
		
		return false;
	}
	
	
	public static boolean startOfEntityStr(int pos, int size, ArrayList<String> outputs)
	{
		String label = outputs.get(pos);
		if (label.startsWith("B"))
			return true;
		
		if (pos == 0 && label.startsWith("I"))
			return true;
		
		if (pos > 0)
		{
			String prev_label =  outputs.get(pos - 1);
			if (label.startsWith("I") && prev_label.startsWith("O"))
				return true;
		}
		
		
		return false;
	}
	
	public static boolean endofEntityStr(int pos, int size,  ArrayList<String> outputs)
	{
		String label = outputs.get(pos);
		if (!label.startsWith("O"))
		{
			if (pos == size - 1)
				return true;
			else {
				String next_label =  outputs.get(pos + 1);
				if (next_label.startsWith("O") || next_label.startsWith("B"))
					return true;
			}
		}
		
		return false;
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
	
	public static String norm_digits(String s)
	{
		s = s.replaceAll("\\d+", "0");
		
		return s;
		
	}
	
	public static String clean(String s)
	{
		if (s.startsWith("http://") || s.startsWith("https://"))
		{
			s = "<url>";
		} else if (s.startsWith("@")) {
			s = "<user>";
		}
		else
		{
			s = norm_digits(s.toLowerCase());
			//s = escape(s);
			//s = s.replaceAll("[^A-Za-z0-9_]", "_");
		}
		
		return s;
	}
	
	public String getShape(String s)
	{
		String shape = s;
		if (shape.startsWith("http://") || shape.startsWith("https://"))
		{
			shape = "<url>";
		}
		else
		{
			shape = norm_digits(s.toLowerCase());
			
			shape = shape.replaceAll("[A-Z]", "X");
			shape = shape.replaceAll("[a-z]", "x");
			
		}
		
		return shape;
	}
	
	public static final HashMap<String , String> LinguisticFeatureLibNameFeatureMapping = new HashMap<String , String>(){
	{
		put("Neg_Prefix_Eng.txt", "Neg_Prefix_Eng");
		put("Neg_Prefix_Sp.txt", "Neg_Prefix_Sp");
		
		put("bad_words","_sent_has_mal");
		put("good_words", "_sent_has_bien");
		put("curse_words", "_sent_has_curse_word");
		put("determiners", "_is_determiner");
		put("prepositions", "_is_preposition");
		
		
	}};
	
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
	
	
	
	

}
