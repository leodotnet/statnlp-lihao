package org.statnlp.abbr.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.statnlp.commons.types.Label;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.NetworkConfig;



public class AbbrGlobal {
	
	public static boolean DEBUG = false;

	public static String modelname = null;
	
	public static boolean ENABLE_WORD_EMBEDDING = false;
	
	public static WordEmbedding Word2Vec = null;
	
	public static HashMap<String, Integer> LABEL_MAPPING = new HashMap<String, Integer>();
	
	public static String feature_file_path = "";
	
	public static boolean EMBEDDING_WORD_LOWERCASE = false;
	
	
	public static boolean DISCARD_NONEG_SENTENCE_IN_TEST = true;
	

	public static boolean NGRAM = false;
	
	
	public static boolean USE_WORD_ONLY = false;
	
	public static String SEPERATOR = "\t";
	
	
	public static String testVocabFile = null;
	
	public static boolean BA_CONSTRIANT = false;
	

	
	public static String UNK = "<UNK>";
	
	public static String neuralType =  "none";
	
	public static boolean considerPunc = false;
	

	
	public static String dataSet = "";
	

	
	

	
	public static void init()
	{
		createLabelMapping();
		
	}
	
	public static void clearTemporalData()
	{
		
	}

	
	
	public static void createLabelMapping()
	{
	
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
	


	public static boolean DUMP_FEATURE = false;

	public static int MAX_LENGTH_LENGTH = 200;

	public static boolean FIX_EMBEDDING = false;


	public static boolean USE_POSITION_EMBEDDEING = false;

	public static boolean ENABLE_DISCRETE_FEATURE = false;

	public static int NER_SPAN_MAX = 10;

	
	public static boolean OUTPUT_ERROR = true;

	public static boolean ECHO_FEATURE = false;

	public static int MAX_SENTENCE_LENGTH = 100;
	
	public static boolean OUPUT_HTML = false;

	public static Double RECALL_BETA = null;
	
	

}
