package org.citationspan.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.statnlp.commons.types.Label;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.NetworkConfig;



public class CitationSpanGlobal {
	
	public static boolean DEBUG = false;

	public static String modelname = null;
	
	public static boolean ENABLE_WORD_EMBEDDING = false;
	
	public static WordEmbedding Word2Vec = null;
	
	public static HashMap<String, Integer> LABEL_MAPPING = new HashMap<String, Integer>();
	
	public static String feature_file_path = "";
	
	public static boolean EMBEDDING_WORD_LOWERCASE = false;
	
	public static boolean MULTI_NEGATIONS = false;
	
	public static boolean DISCARD_NONEG_SENTENCE_IN_TEST = true;
	

	public static boolean NGRAM = false;
	
	
	public static boolean USE_WORD_ONLY = false;
	
	public static String SEPERATOR = "\t";
	
	
	public static String testVocabFile = null;
	
	public static boolean BA_CONSTRIANT = false;
	
	public static boolean SYNTAX_FEATURE = true;
	
	public static boolean USE_UNIVERSAL_POSTAG = false;
	
	public static String UNK = "<UNK>";
	
	public static String neuralType =  "none";
	
	public static boolean considerPunc = false;
	
	public static boolean NO_CUE_CONSTRAINT_IN_TRAIN = false;
	
	public static boolean NO_CUE_CONSTRAINT_IN_TEST = false;
	
	public static String dataSet = "";
	
	public static Double CUE_MP = null;
	public static Double SCOPE_MP = null;
	
	public static double RECALL_BETA = 1;
	

	public static boolean TOKEN_AS_CUEFORM = true;
	
	
	public static int G0_1 = 63;
	public static int L1_1 = 37;
	
	
	public static int G0_2 = 63;
	public static int L1_2 = 29;
	public static int G1_2 = 42;
	public static int L2_2 = 29;
	

	public static int G0_3 = 18;
	public static int L1_3 = 6;
	public static int G1_3 = 17;
	public static int L2_3 = 10;
	public static int G2_3 = 18;
	public static int L3_3 = 47;
	
	public static int MAX_NUM_SPAN = 7;
	
	public static int L_MAX = 0;
	public static int M_MAX = 0;
	
	public static int SPAN_MAX = 3;
	
	public static int MAX_LATENT_NUMBER = SPAN_MAX;
	
	public static boolean REVERSE_INPUT = false;
	
	

	
	public static void resetNegExpList() {
		NegExpList.clear();
		for(String neg : NegExpListBak)
			NegExpList.add(neg);
	}
	
	public static HashSet<String> NegExpListBak = new HashSet<String>(){
		{
			add("n't");
			add("neglected");
			add("except");
			add("nor");
			add("neither");
			add("without");
			add("nobody");
			add("none");
			add("nothing");
			add("nowhere");
			//add("on the contrary");
			add("never");
			add("not");
			add("no");
			add("nowhere");
			add("non");
			add("save for");
			add("save upon");
			add("by no means");
		}
	};
	
	public static HashSet<String> NegExpList = new HashSet<String>(){
		{
			add("n't");
			add("neglected");
			add("except");
			add("nor");
			add("neither");
			add("without");
			add("nobody");
			add("none");
			add("nothing");
			add("nowhere");
			//add("on the contrary");
			add("never");
			add("not");
			add("no");
			add("nowhere");
			add("non");
			add("save for");
			add("save upon");
			add("by no means");
		}
	};
	
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

	public static boolean OUTPUT_SENTIMENT_SPAN = false;
	
	public static boolean OUTPUT_SEM2012_FORMAT = true;

	public static boolean USE_POSITION_EMBEDDEING = false;

	public static boolean ENABLE_DISCRETE_FEATURE = false;

	public static int NER_SPAN_MAX = 10;
	
	public static boolean NEG_CUE_DETECTION = false;
	
	public static boolean USE_SYSTEM_CUE = false;
	
	public static int CUE_MAX_L = 5;
	
	public static boolean SORT_NEGATION_CUE = false;
	
	public static boolean ENABLE_OCUE_CHAIN = true;
	
	public static boolean SCOPE_CHAIN_U_APPROACH = false;
	
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

	public static boolean OUTPUT_HTML_SPAN = true;

	

	public static boolean OUTPUT_ERROR = true;

	public static boolean ECHO_FEATURE = false;

	public static int MAX_SENTENCE_LENGTH = 1000;

	public static double SPLIT_RATIO = 0.8;
	
	
	
	

}
