package org.statnlp.sentiment.spanmodel.common;

import java.util.ArrayList;
import java.util.HashMap;

import org.statnlp.commons.types.Label;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.targetedsentiment.common.WordEmbedding;



public class SpanModelGlobal {

	public static boolean OUTPUT_SENTIMENT_SPAN = false;
	
	public static boolean USE_POS_TAG = false;
	
	public static boolean word_feature_on = true;
	
	public static boolean WITHOUT_HIDDEN_SENTIMENT = false;
	
	public static boolean ENABLE_WORD_EMBEDDING = false;
	
	public static boolean ECHO_FEATURE = false;
	
	public static boolean DUMP_FEATURE = false;
	
	public static WordEmbedding Word2Vec = null;
	
	public static ArrayList<Integer> SENTIMENT_SPAN_SPLIT = new ArrayList<Integer>();
	
	
	public static String[] LABELS = new String[] {};
	
	public static int NUM_HighFreqWords = 10;
	
	public static Counter dataCounter = new Counter(NUM_HighFreqWords);
	
	public static ArrayList<String> highFreqWords = new ArrayList<String>();
	
	
	public static HashMap<String, Integer> LABEL_MAPPING = new HashMap<String, Integer>();
	
	public static SentimentDict dict = null;
	
	public static String SentimentDictLexicons = "mpqa,sst";
	
	public static int SemiL = 1;
	
	public static boolean Use_Global_Info = false;
	
	public static boolean USE_FULL_UNLABELNETWORK = false;
	
	public static boolean USE_TRANSITION =false;
	
	public static int NGRAM = 1;
	
	public static void init()
	{

		dict = new SentimentDict();
		
		/*
		dict.loadOpinionLexicon();
		dict.loadSSTLexicon();*/
		dict.loadLexicons(SentimentDictLexicons);
		dict.printStat();
		
		//dict.loadMPQALexicon();
		dataCounter.clear();
		highFreqWords.clear();
		createLabelMapping();
	}
	
	
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
	
	
	public static void initWordEmbedding(String lang)
	{
		Word2Vec = new WordEmbedding(lang, -1);
	}
	
	public static void clearTemporalData()
	{
		
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
			put("sentimentspan_latent_semeval", "data//semeval2016//");
			
		}
	};
	
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
	
	public static boolean startOfEntity(int pos, int size, ArrayList<Label> outputs) {
		String label = outputs.get(pos).getForm();
		if (label.startsWith("B"))
			return true;

		if (pos == 0 && label.startsWith("I"))
			return true;

		if (pos > 0) {
			String prev_label = outputs.get(pos - 1).getForm();
			if (label.startsWith("I") && prev_label.startsWith("O"))
				return true;
		}

		return false;
	}

	public static boolean endofEntity(int pos, int size, ArrayList<Label> outputs) {
		String label = outputs.get(pos).getForm();
		if (!label.startsWith("O")) {
			if (pos == size - 1)
				return true;
			else {
				String next_label = outputs.get(pos + 1).getForm();
				if (next_label.startsWith("O") || next_label.startsWith("B"))
					return true;
			}
		}

		return false;
	}
	
	
	public static String norm_digits(String s)
	{
		s = s.replaceAll("\\d+", "0");
		
		return s;
		
	}
	
	
	
	

}
