package org.statnlp.negationfocus.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import org.statnlp.commons.types.Label;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.NetworkConfig;



public class NegationGlobal {
	
	public static boolean DEBUG = false;

	public static String modelname = null;
	
	public static boolean ENABLE_WORD_EMBEDDING = false;
	
	public static WordEmbedding Word2Vec = null;
	
	public static HashMap<String, Integer> LABEL_MAPPING = new HashMap<String, Integer>();
	
	public static String feature_file_path = "";
	
	public static boolean EMBEDDING_WORD_LOWERCASE = true;
	
	public static boolean MULTI_NEGATIONS = false;
	
	public static boolean DISCARD_NONEG_SENTENCE_IN_TEST = true;
	

	public static boolean NGRAM = false;
	
	
	public static boolean USE_WORD_ONLY = false;
	
	public static String SEPERATOR = " ";
	
	
	public static String testVocabFile = null;
	
	public static boolean BA_CONSTRIANT = false;
	
	public static boolean SYNTAX_FEATURE = true;
	
	public static boolean USE_UNIVERSAL_POSTAG = false;
	
	public static String UNK = "<UNK>";
	
	public static String neuralType =  "none";
	
	public static boolean considerPunc = false;
	
	public static boolean NO_CUE_CONSTRAINT_IN_TRAIN = false;
	
	public static boolean NO_CUE_CONSTRAINT_IN_TEST = false;
	
	public static int MAX_SPAN_LENGTH = 20;
	
	public static boolean SEMI_UNLABEL_PRUNING = false;
	
	
	

	public static HashMap<Integer, NegationFeature> networkId2feature = new HashMap<Integer, NegationFeature>();
	public static HashMap<Integer, NegationFeature> networkId2featureDev = new HashMap<Integer, NegationFeature>();
	
	public static Set<String> stopwords = new HashSet<String>();
	
	public static void init()
	{
		createLabelMapping();
		readStopWords();
	}
	
	public static void clearTemporalData()
	{
		networkId2feature.clear();
		networkId2featureDev.clear();
	}
	
	public static void readStopWords() {
		
		try {
			Scanner scanner = new Scanner(new File("models//stopwords.txt"), "UTF-8");
			while(scanner.hasNextLine()) {
				String line = scanner.nextLine().trim();
				if (line.length() > 0) {
					stopwords.add(line);
				}
			}
			scanner.close();
			System.out.println("stop-words:" + stopwords);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	
	public static NegationFeature getFeaturebyNetworkId(int networkId, int size)
	{
		NegationFeature feature = null;
		
		if (NetworkConfig.STATUS == NetworkConfig.ModelStatus.TRAINING || NetworkConfig.STATUS == NetworkConfig.ModelStatus.TESTING) {
			feature = networkId2feature.get(networkId);

			if (feature == null) {
				feature = new NegationFeature();
				networkId2feature.put(networkId, feature);
			}

			
		} else {  //trainDev
			feature = networkId2featureDev.get(networkId);

			if (feature == null) {
				feature = new NegationFeature();
				networkId2featureDev.put(networkId, feature);
			}
		}
		
		return feature;
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

	public static boolean OUTPUT_SENTIMENT_SPAN = false;
	
	public static boolean OUTPUT_SEM2012_FORMAT = true;

	public static boolean USE_POSITION_EMBEDDEING = false;

	public static boolean ENABLE_DISCRETE_FEATURE = false;

	public static int NER_SPAN_MAX = 10;
	
	public static void setLang(String lang)
	{
		
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

	public static boolean OUTPUT_ERROR = true;

	public static boolean ECHO_FEATURE = false;

	public static int MAX_SENTENCE_LENGTH = 100;
	
	
	
	

}
