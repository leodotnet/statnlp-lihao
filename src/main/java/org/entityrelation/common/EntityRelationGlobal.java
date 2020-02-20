package org.entityrelation.common;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.statnlp.commons.types.Label;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.NetworkConfig;



public class EntityRelationGlobal {
	
	

	
	public static boolean USE_GENERAL_TAGS = true;

	public static double NO_RELATION_PROB = 2;
	
	public static boolean DEBUG = false;
	
	public static boolean DUMMY_FEATURE  = false;

	public static String modelname = null;
	
	public static boolean ENABLE_WORD_EMBEDDING = false;
	
	public static WordEmbedding Word2Vec = null;
		
	public static boolean EMBEDDING_WORD_LOWERCASE = false;
	
	public static boolean NGRAM = false;
	
	public static String SEPERATOR = "\t";
	
	
	public static String testVocabFile = null;

	//public static double RECALL_BETA = 1;
	
	public static double[] RECALL_BETA = new double[] {1, 1};
	
	public static int L_SPAN_MAX = 0;
	public static int L_SPAN_MAX_LIMIT = 25;
	public static int L_HEAD_MAX = 0;
	public static int SENTENCE_LENGTH_MAX = 100;
	public static int RELATION_MAX = 0;
	public static int ENTITY_TYPE_MAX = 0;
	public static int[] RELATION_ENTITY_DISTANCE_MAX = new int[] {0, 0};  //distance of the starting position of two entities.
	public static int[] RELATION_ENTITY_DISTANCE_MAX_LIMIT = new int[] {-32, 41};  //distance of the starting position of two entities.

	public static boolean OUTPUT_HTML_SPAN = false;
	
	public static boolean ADD_SELF_RELATION = false;
	public static boolean ADD_NO_RELATION = false;
	public static boolean EXPAND_WITH_NO_RELATION = false;
	
	public static int NoRelationIndex = -1;
	
	public static boolean ENABLE_ENTITY_ORDER_CONSTRAINT = false;
	public static boolean ENABLE_ENTITY_DIST_CONSTRAINT = false;
	
	public static boolean ONLY_SELF_RELATION = false;
	public static boolean REMOVE_OL_ENTITY = false;
	
	public static boolean FIX_ENTITY_PAIR = false;
	public static boolean FIX_ENTITY = false;
	
	
	public static boolean HEAD_AS_SPAN = false;
	
	public static boolean DEP_FEATURE = true;
	public static boolean REL_FEATURE = true;
	
	public static Double ENTITY_MP = null;
	
	public static boolean SAVE_LIGHT_MODEL = false;
	
	public static int NUM_ENTITY_TYPE = 0;
	public static int NUM_RELATION_TYPE = 0;
	
	public static boolean FLIP = false;
	
	public static boolean I_GENERAL = false;
	
	public static String[] INIT_WEIGHT_FROM_OTHERS = null;
	
	public static boolean INIT_WEIGHT_FROM_LIGHT_MODEL = false;
	
	public static boolean REMOVE_DUPLICATE = true;
	
	public static boolean LAST_WORD_AS_HEAD = true;
	
	public static boolean PROPGATE_R_TO_I_NODE = false;
	
	public static boolean IGNORE_ANOTHER_I = false;
	
	public static boolean ADD_REVERSE_RELATION = false;
	
	
	
	
	
	public static String getConfig(){
		StringBuilder builder = new StringBuilder();
		for(Field field: EntityRelationGlobal.class.getDeclaredFields()){
			try {
				builder.append(field.getName()+"="+field.get(null)+"\n");
			} catch (IllegalArgumentException | IllegalAccessException e) {
				return null;
			}
		}
		return builder.toString();
	}


	
	public static void init()
	{
		relationEntityTriples.clear();
	}
	
	public static void clearTemporalData()
	{
		
	}

	
	public static void initWordEmbedding(String lang, int embeddingSize)
	{
		Word2Vec = new WordEmbedding(lang, embeddingSize);
	}
	


	public static boolean DUMP_FEATURE = false;

	public static boolean FIX_EMBEDDING = false;

	public static boolean ENABLE_DISCRETE_FEATURE = false;
	
	public static boolean OUTPUT_ERROR = true;

	public static boolean ECHO_FEATURE = false;

	public static String neuralType = "none";

	public static String UNK = "<UNK>";
	
	public static String lang = "en";

	public static String dataSet = "ace2005";



	public static void setLang(String lang) {
		EntityRelationGlobal.lang = lang;
	}

	
	public static ArrayList<int[]> relationEntityTriples = new ArrayList<int[]>();

	public static String PIPELINE_SPAN = null;

	public static boolean usePositionEmbedding = true;

	public static boolean zero_digit = true;

	
	
	public static int getTripleIdx(int[] triple) {
		for(int i = 0; i < relationEntityTriples.size(); i++) {
			if (Arrays.equals(triple, relationEntityTriples.get(i)))
				return i;
		}
		
		return -1;
	}
	


}
