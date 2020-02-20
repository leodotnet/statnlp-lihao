package org.statnlp.targetedsentiment.f.latent;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Scanner;







import java.util.AbstractMap.SimpleImmutableEntry;

import org.statnlp.commons.types.Instance;
import org.statnlp.example.linear_ne.ECRFInstance;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.FeatureManager;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.NetworkIDMapper;
import org.statnlp.hypergraph.neural.NeuralNetworkCore;
import org.statnlp.targetedsentiment.common.TSFeature;
import org.statnlp.targetedsentiment.common.TSInstance;
import org.statnlp.targetedsentiment.common.TargetSentimentGlobal;
import org.statnlp.targetedsentiment.f.latent.TargetSentimentCompiler.*;
import org.statnlp.targetedsentiment.f.latent.TargetSentimentOverlapFeatureManager.WORD_FEATURE_TYPES;







public class TargetSentimentFeatureManager extends FeatureManager {
	
	private NeuralNetworkCore net;
	public String neuralType;
	public boolean moreBinaryFeatures = false;
	
	public TargetSentimentFeatureManager(GlobalNetworkParam param_g) {
		super(param_g, null);
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			this.net = (NeuralNetworkCore) param_g.getNNParamG().getNet(0);
		}
	}
	
	
	public TargetSentimentFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
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
	
	int PolarityTypeSize = PolarityType.values().length;
	int SubNodeTypeSize = SubNodeType.values().length;
	int WordFeatureTypeSize = WORD_FEATURE_TYPES.values().length;
	
	public static String[] Subjectivity = new String[]{"non-neutral1", "non-neutral1", "neutral1"};
	
	
	static HashMap<String, ArrayList<String>> LinguisticFeaturesLibaryList = new HashMap<String, ArrayList<String>>();
	
	
	
	public void loadLinguisticFeatureLibary()
	{
		LinguisticFeaturesLibaryList.clear();
		
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
		}
	}
	
	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k, int children_k_index) {
			
		if(children_k.length>1)
			throw new RuntimeException("The number of children should be at most 1, but it is "+children_k.length);
		
		TSInstance inst = ((TSInstance)network.getInstance());
		
		long node_parent = network.getNode(parent_k);
		
		if(children_k.length == 0)
			return FeatureArray.EMPTY;

		
		int size = inst.size();
		
		long node_child = network.getNode(children_k[0]);
		
		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(node_parent);
		int[] ids_child = NetworkIDMapper.toHybridNodeArray(node_child);

		
		ArrayList<String[]> inputs = (ArrayList<String[]>)inst.getInput();
		ArrayList<String> outputs = (ArrayList<String>)inst.getOutput();
		
		int pos_parent = size - ids_parent[0];
		int pos_child = size - ids_child[0];
		
		int polar_parent = PolarityTypeSize - ids_parent[1];
		int polar_child = PolarityTypeSize - ids_child[1];
		
		int subnode_parent = SubNodeTypeSize - ids_parent[2];
		int subnode_child = SubNodeTypeSize - ids_child[2];
		
		int num_sent = 0;

		String word_parent = "";
		String word_child = "";
		
		if (pos_parent >= 0 && pos_parent < size)
			word_parent = inputs.get(pos_parent)[0];
		
		if (pos_child >= 0 && pos_child < size)
			word_child = inputs.get(pos_child)[0];
		

	
		FeatureArray fa = FeatureArray.EMPTY;
		
		ArrayList<Integer> feature = new ArrayList<Integer>();
		ArrayList<String[]> featureArr = new ArrayList<String[]>();
		String WordEmbeddingFeature = null;
		
		String Out = "O";
		int eId = 0;
		int entityId = 0;
		int sentId = -1;
		//System.out.println("network.getNetworkId():" + network.getNetworkId());
		
		
		TSFeature mynetwork = TargetSentimentGlobal.getFeaturebyNetworkId(inst.getInstanceId(), size);
		if (mynetwork.feature == null) {
			this.get_features(mynetwork, inputs, size);
			
		}
		
		
		//left to left, right to right, left to entity
		if ((subnode_parent == SubNodeType.B.ordinal() && subnode_child == SubNodeType.B.ordinal())
				|| (subnode_parent == SubNodeType.A.ordinal() && subnode_child == SubNodeType.A.ordinal()) 
				|| (subnode_child == SubNodeType.e.ordinal()))
		{
			//word level feature for each position
			
			
			if (subnode_child == SubNodeType.e.ordinal())
			{
				Out = "E-" + PolarityType.values()[polar_parent].name();
				eId = polar_parent + 1;
			}
			
			
			/*
			 feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Bigram + "-" + word_feature_type , Out, prev_feature + "|||" + current_feature ));
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Trigram + "-" + word_feature_type , Out, prev_feature + "|||" + current_feature + "|||" + next_feature ));
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Fourgram + "-" + word_feature_type , Out, prev_prev_feature + "|||" + prev_feature + "|||" + current_feature + "|||" + next_feature ));
				
			 * */
			
			{
				String prefix = "";
				int pos = pos_parent;
				
				String[] current = inputs.get(pos);
				
				String Sent = PolarityType.values()[polar_parent].toString();
				String NE = Out;
				
				//word embedding
				//feature.add(this._param_g.toFeature(network, "<WordEmbeddingonWord>", Out, current.getName()));
				if (subnode_parent == SubNodeType.B.ordinal() && subnode_child == SubNodeType.e.ordinal())
				{
					WordEmbeddingFeature = current[0];
					
					if (TargetSentimentGlobal.USE_PREPROCESS_SCRIPT) WordEmbeddingFeature = current[9];
					
				} else if (subnode_parent == SubNodeType.e.ordinal() && subnode_child == SubNodeType.A.ordinal()) 
				{
					WordEmbeddingFeature = current[0];
					
					if (TargetSentimentGlobal.USE_PREPROCESS_SCRIPT) WordEmbeddingFeature = current[9];
				}
				
				
				prefix = "word";
				
				
				for (String[] f : mynetwork.feature[pos]) {
					addPrefix(featureArr, f, prefix, Sent, NE);
					
				}

				/*
				for (int i = 0; i < pos - 3; i++) {
					
					prefix = "_prev";
					for (String[] f : mynetwork.horizon_feature[i]) 
					{
						addPrefix(featureArr, f, prefix, Sent, NE);
					}

				}*/

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

			
				/*
				for (int i = pos + 3 + 1; i < size; i++) {
					
					prefix = "_next";
					for (String[] f : mynetwork.horizon_feature[i]) {
						addPrefix(featureArr, f, prefix, Sent, NE);
					}

				}*/
				
				
				//int num_sent = 0;
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
						//if (item[0].startsWith("word_id"))
						//System.out.println(Arrays.toString(item));
						feature.add(this._param_g.toFeature(network, item[0] + item[2], Out,  "_link(NE,SENT)"));
						/*
						if ( item[0].contains("_id_")) //item[0].contains("_sent_") ||
						{
							
							//feature.add(new String[]{item[0] + item[2], "[" + Sent + "]",   "_sent(SENT)" });	
							//feature.add(new String[]{item[0] + item[2], "[" + NE + "_VOLITIONAL," + Sent + "]",   "_link(NE,SENT)"});
							//feature.add(this._param_g.toFeature(network, item[0] + item[2], "[" + Sent + "]",   "_sent(SENT)"));
							//feature.add(this._param_g.toFeature(network, item[0] + item[2], "[" + NE + "_VOLITIONAL," + Sent + "]",   "_link(NE,SENT)"));
							
							
						}*/
					}

					//feature.add(new String[]{"sent", "[" + NE + "_VOLITIONAL," + Sent + "]", "_link(NE,SENT)"});
					//feature.add(this._param_g.toFeature(network, "sent", "[" + NE + "_VOLITIONAL," + Sent + "]", "_link(NE,SENT)"));
					
					
				}
				
			}
			
			
			
			
		}
		
		String prev_prev_feature="", prev_feature ="", current_feature ="", next_feature = "";
		String prev_brown_cluster = "<START>", current_brown_cluster ="<NULL>", next_brown_cluster="<END>";
		String prev_jerboa = "<START>", current_jerboa = "<NULL>", next_jerboa = "<END>";
		String[] previous_polarity = new String[]{"<START>", "<NULL>", "<END>"};
		String[] ther_sentiment_polarity = new String[]{"<START>", "<NULL>", "<END>"};
		
		current_feature = this.clean(word_parent);
		word_parent = current_feature;
		
		if (pos_parent >= 0 && pos_parent < size)
		{
			current_brown_cluster = inputs.get(pos_parent)[4];
			current_jerboa = inputs.get(pos_parent)[3];
			
			previous_polarity[1] = inputs.get(pos_parent)[5];
			
			
		}
		
		
		if (pos_parent <= 0)
		{
			prev_feature = "<START>";
		} else 
		{
			prev_feature =  this.clean(inputs.get(pos_parent - 1)[0]);
			prev_brown_cluster = inputs.get(pos_parent - 1)[4];
			prev_jerboa = inputs.get(pos_parent - 1)[3];
			
			previous_polarity[0] = inputs.get(pos_parent - 1)[5];
			ther_sentiment_polarity[0] = inputs.get(pos_parent - 1)[6];
		}
		
		if (pos_parent >= size - 1)
		{
			next_feature = "<END>";
		} else 
		{
			next_feature =  this.clean(inputs.get(pos_parent + 1)[0]);
			next_brown_cluster = inputs.get(pos_parent + 1)[4];
			next_jerboa = inputs.get(pos_parent + 1)[3];
			
			previous_polarity[2] = inputs.get(pos_parent + 1)[5];
			ther_sentiment_polarity[2] = inputs.get(pos_parent+1)[6];
		}
		word_child = next_feature;
		
		if (subnode_parent == SubNodeType.B.ordinal() && subnode_child == SubNodeType.e.ordinal())
		{
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start_Feature", "", ""));
			
			if (polar_parent < 2)
			{
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_sentiment_Feature", "", ""));
			}
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , "", "current_word:" +word_parent));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , "" , "current_word:" +word_parent + "|||" + "next_word:" +next_feature));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , "" , "last_word:" +prev_feature + "|||" + "current_word:" +word_parent));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , "" , "last_word:" +prev_feature + "|||" + "current_word:" +word_parent + "|||" + "next_word:" + next_feature));
			
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , PolarityType.values()[polar_parent].name() , "current_word:" + word_parent + "|||" + next_brown_cluster));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , PolarityType.values()[polar_parent].name() , "current_word:" +  word_parent + "-jerboa:" +  current_jerboa));
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , PolarityType.values()[polar_parent].name() ,"current_word:" +  word_parent));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , PolarityType.values()[polar_parent].name() ,  "last_word:" + prev_feature + "|||" + "current_word:" + word_parent));
			

			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , PolarityType.values()[polar_parent].name() , "current_word:" + word_parent + "next_word:" +  next_feature));
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , PolarityType.values()[polar_parent].name() ,  "last_word:" + prev_feature + "|||" + "current_word:" + word_parent + "next_word:" +  next_feature));
			
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , PolarityType.values()[polar_parent].name() ,  "current_previous_polarity:" + previous_polarity[1]));
			
			
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , PolarityType.values()[polar_parent].name() ,  "current_ther_sentiment_polarity:" + ther_sentiment_polarity[1]));
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , PolarityType.values()[polar_parent].name() , "current_word:" + word_parent + "first_letter_upper:" +  Character.isUpperCase(word_parent.charAt(0))));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , PolarityType.values()[polar_parent].name() , "first_letter_upper:" +  Character.isUpperCase(word_parent.charAt(0))));
			
			
			/***non nuetral**/
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , Subjectivity[polar_parent] , "current_word:" + word_parent + "|||" + next_brown_cluster));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , Subjectivity[polar_parent] , "current_word:" +  word_parent + "-jerboa:" +  current_jerboa));
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , Subjectivity[polar_parent] ,"current_word:" +  word_parent));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , Subjectivity[polar_parent] ,  "last_word:" + prev_feature + "|||" + "current_word:" + word_parent));
			

			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , Subjectivity[polar_parent] , "current_word:" + word_parent + "next_word:" +  next_feature));
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , Subjectivity[polar_parent] ,  "last_word:" + prev_feature + "|||" + "current_word:" + word_parent + "next_word:" +  next_feature));
			
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , Subjectivity[polar_parent] ,  "current_previous_polarity:" + previous_polarity[1]));
			
			
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , Subjectivity[polar_parent] ,  "current_ther_sentiment_polarity:" + ther_sentiment_polarity[1]));
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , Subjectivity[polar_parent] , "current_word:" + word_parent + "first_letter_upper:" +  Character.isUpperCase(word_parent.charAt(0))));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , Subjectivity[polar_parent], "first_letter_upper:" +  Character.isUpperCase(word_parent.charAt(0))));
			
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , Subjectivity[polar_parent] , "current_word:" + word_parent ));		
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , Subjectivity[polar_parent] ,  "last_word:" + prev_feature + "|||" + "current_word:" +  word_parent + "|||" + "next_word:" +  next_feature));
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , Subjectivity[polar_parent] , "current_word:" + word_parent + "first_letter_upper:" +  Character.isUpperCase(word_parent.charAt(0))));
			/*****/
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start" , "", "first_letter_upper:" +  Character.isUpperCase(word_parent.charAt(0))));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , "", "first_letter_upper:" +  Character.isUpperCase(word_parent.charAt(0))));
			
			
		
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , "", "current_word:" + word_parent ));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , PolarityType.values()[polar_parent].name() , "current_word:" + word_parent ));		
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , PolarityType.values()[polar_parent].name() ,  "last_word:" + prev_feature + "|||" + "current_word:" +  word_parent + "|||" + "next_word:" +  next_feature));
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , PolarityType.values()[polar_parent].name() , "current_word:" + word_parent + "first_letter_upper:" +  Character.isUpperCase(word_parent.charAt(0))));
			
			//feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , PolarityType.values()[polar_parent].name() ,  "current_ther_sentiment_polarity:" + ther_sentiment_polarity[1]));
			
			
		} else if (subnode_parent == SubNodeType.e.ordinal() && subnode_child == SubNodeType.e.ordinal()) {
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Continue" , PolarityType.values()[polar_parent].name() , "current_word:" + word_parent));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Continue" , PolarityType.values()[polar_parent].name() , "current_word:" + word_parent + "|||" + "next_word:" + next_feature));
			
			
			
			if (polar_parent < 2)
			{
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_sentiment_Feature", "", ""));
			}
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , "", "current_word:" +word_parent ));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , PolarityType.values()[polar_parent].name() , "current_word:" +word_parent ));
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Bigram + "-" + "NE_contains" , "", "current_word:" +word_parent + "|||" + "next_word:" + word_child ));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Bigram + "-" + "NE_contains" , PolarityType.values()[polar_parent].name() ,"current_word:" + word_parent + "|||" + "next_word:" + word_child ));
			
			//feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_sentiment" , "", PolarityType.values()[polar_parent].name() ));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , PolarityType.values()[polar_parent].name() , "current_word:" + word_parent + "first_letter_upper:" +  Character.isUpperCase(word_parent.charAt(0))));
			
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , "", "first_letter_upper:" +  Character.isUpperCase(word_parent.charAt(0))));
			
			
			/***non nuetral**/
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , Subjectivity[polar_parent]  , "current_word:" +word_parent ));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Bigram + "-" + "NE_contains" , Subjectivity[polar_parent]  ,"current_word:" + word_parent + "|||" + "next_word:" + word_child ));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , Subjectivity[polar_parent] , "current_word:" + word_parent + "first_letter_upper:" +  Character.isUpperCase(word_parent.charAt(0))));
			
			/********/
			
			
		} else if (subnode_parent == SubNodeType.e.ordinal() && subnode_child == SubNodeType.A.ordinal()) {
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_End" , PolarityType.values()[polar_parent].name() , "current_word:" + word_parent ));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_End" , PolarityType.values()[polar_parent].name() , "current_word:" + word_parent + "|||" + "next_word:" + next_feature));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_End" , PolarityType.values()[polar_parent].name(), "last_word:" + prev_feature +"|||" + "current_word:" + word_parent ));
			
			
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , "", word_parent ));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , PolarityType.values()[polar_parent].name() , "current_word:" +word_parent ));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , PolarityType.values()[polar_parent].name() , "current_word:" +word_parent + "|||" + "next_word:" + next_feature));
			
			
			//feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_sentiment" , "", PolarityType.values()[polar_parent].name() ));
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , PolarityType.values()[polar_parent].name() , "current_word:" + word_parent + "first_letter_upper:" +  Character.isUpperCase(word_parent.charAt(0))));
			
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , "", "first_letter_upper:" +  Character.isUpperCase(word_parent.charAt(0))));
			
			
			/***non nuetral**/
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_End" , Subjectivity[polar_parent]  , "current_word:" + word_parent ));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_End" ,  Subjectivity[polar_parent] , "current_word:" + word_parent + "|||" + "next_word:" + next_feature));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_End" ,  Subjectivity[polar_parent] , "last_word:" + prev_feature +"|||" + "current_word:" + word_parent ));
			

			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" ,  Subjectivity[polar_parent] , "current_word:" +word_parent ));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" ,  Subjectivity[polar_parent]  , "current_word:" +word_parent + "|||" + "next_word:" + next_feature));
			
			
			//feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_sentiment" , "", PolarityType.values()[polar_parent].name() ));
			
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains" , Subjectivity[polar_parent]  , "current_word:" + word_parent + "first_letter_upper:" +  Character.isUpperCase(word_parent.charAt(0))));
			
			/******/
			
		}
		
		
		if (subnode_parent == SubNodeType.B.ordinal() && subnode_child == SubNodeType.B.ordinal())
		{
			if (!TargetSentimentGlobal.WITHOUT_HIDDEN_SENTIMENT)
			{
			
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , PolarityType.values()[polar_parent].name()+"_Before", "current_word:" +word_parent ));
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , PolarityType.values()[polar_parent].name(), "current_word:" +word_parent ));
				//feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , PolarityType.values()[polar_parent].name(),   "current_word:" +word_parent + "|||" +  "next_word:" +next_feature ));
			
				//new
				if (!previous_polarity[1].equals("_") && previous_polarity[1].toLowerCase().contains(PolarityType.values()[polar_parent].name()))
				{
					feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , PolarityType.values()[polar_parent].name() ,  "current_previous_polarity:" + previous_polarity[1]));
				}
				//???
				if (!ther_sentiment_polarity[1].equals("_"))
					feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , PolarityType.values()[polar_parent].name() ,  "current_ther_sentiment_polarity:" + ther_sentiment_polarity[1]));
				
				
				/***non nuetral**/
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , Subjectivity[polar_parent] +"_Before", "current_word:" +word_parent ));
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , Subjectivity[polar_parent] , "current_word:" +word_parent ));
				//feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , PolarityType.values()[polar_parent].name(),   "current_word:" +word_parent + "|||" +  "next_word:" +next_feature ));
			
				//new
				if (!previous_polarity[1].equals("_") && previous_polarity[1].toLowerCase().contains(PolarityType.values()[polar_parent].name()))
				{
					feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , Subjectivity[polar_parent]  ,  "current_previous_polarity:" + previous_polarity[1]));
				}
				//???
				if (!ther_sentiment_polarity[1].equals("_"))
					feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , Subjectivity[polar_parent] ,  "current_ther_sentiment_polarity:" + ther_sentiment_polarity[1]));
				/*******/
			
			}
		}
		else if (subnode_parent == SubNodeType.A.ordinal() && subnode_child == SubNodeType.A.ordinal())
		{
			if (!TargetSentimentGlobal.WITHOUT_HIDDEN_SENTIMENT)
			{
			
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , PolarityType.values()[polar_parent].name()+"_After","current_word:" + word_parent  ));
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , PolarityType.values()[polar_parent].name(),"current_word:" + word_parent  ));
				//feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , PolarityType.values()[polar_parent].name(),   "current_word:" +word_parent + "|||" +  "next_word:" + next_feature ));
			
			
				//new
				if (!previous_polarity[1].equals("_") && previous_polarity[1].toLowerCase().contains(PolarityType.values()[polar_parent].name()))
					feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , PolarityType.values()[polar_parent].name() ,  "current_previous_polarity:" + previous_polarity[1]));
				//????
				if (!ther_sentiment_polarity[1].equals("_"))
					feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , PolarityType.values()[polar_parent].name() ,  "current_ther_sentiment_polarity:" + ther_sentiment_polarity[1]));
			}
			
			/***non nuetral**/
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , Subjectivity[polar_parent]+"_After","current_word:" + word_parent  ));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , Subjectivity[polar_parent],"current_word:" + word_parent  ));
			//feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , PolarityType.values()[polar_parent].name(),   "current_word:" +word_parent + "|||" +  "next_word:" + next_feature ));
		
		
			//new
			if (!previous_polarity[1].equals("_") && previous_polarity[1].toLowerCase().contains(PolarityType.values()[polar_parent].name()))
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , Subjectivity[polar_parent],  "current_previous_polarity:" + previous_polarity[1]));
			//????
			if (!ther_sentiment_polarity[1].equals("_"))
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment" , Subjectivity[polar_parent],  "current_ther_sentiment_polarity:" + ther_sentiment_polarity[1]));
			
			/******/
			
		} else if (subnode_parent == SubNodeType.A.ordinal() && subnode_child == SubNodeType.B.ordinal())
		{
			//feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment_change" , PolarityType.values()[polar_child].name(), PolarityType.values()[polar_parent].name() ));
			//feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment_change" ,  PolarityType.values()[polar_parent].name(),PolarityType.values()[polar_child].name() ));
			//feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O_hidden_sentiment_transition" , PolarityType.values()[polar_parent].name() + "|||" + PolarityType.values()[polar_child].name(),  word_parent));
			
		}
		
		
		
		
		
		if (feature.size() > 0)
		{
			if (TargetSentimentGlobal.ENABLE_WORD_EMBEDDING)
			{
				
				
				if(NetworkConfig.USE_NEURAL_FEATURES && WordEmbeddingFeature != null){
					
					Object input = null;
					if(neuralType.startsWith("lstm")) {
						
						if (subnode_child == SubNodeType.e.ordinal()) {
							if (subnode_parent == SubNodeType.B.ordinal()) { //B
								entityId = 1;
								input = new SimpleImmutableEntry<String, Integer>(inst.getSentence(), pos_parent);
							}
							else //I
							{
								entityId = 2;
								input = new SimpleImmutableEntry<String, Integer>(inst.getSentence(), pos_child);
							}
						} else { //O
							if (subnode_parent == SubNodeType.B.ordinal() && subnode_child == SubNodeType.B.ordinal()) {
								entityId = 0;
								input = new SimpleImmutableEntry<String, Integer>(inst.getSentence(), pos_parent);
							} else if  (subnode_parent == SubNodeType.A.ordinal() && subnode_child == SubNodeType.A.ordinal()) {
								input = new SimpleImmutableEntry<String, Integer>(inst.getSentence(), pos_child);
							}
						}
						
						sentId = polar_parent;
						
						this.addNeural(network, 0, parent_k, children_k_index, input, entityId);
						//this.addNeural(network, 1, parent_k, children_k_index, input, null, sentId);
					} 
					//else if(neuralType.equals("mlp")){
					//	input = llw+IN_SEP+lw+IN_SEP+currWord+IN_SEP+rw+IN_SEP+rrw+OUT_SEP+llt+IN_SEP+lt+IN_SEP+currTag+IN_SEP+rt+IN_SEP+rrt;
					//} 
					else {
						input = WordEmbeddingFeature;//.toLowerCase();
						
						if (TargetSentimentGlobal.EMBEDDING_WORD_LOWERCASE)
							input = WordEmbeddingFeature.toLowerCase();
						this.addNeural(network, 0, parent_k, children_k_index, input, eId);
					}
					
				} 
			}
			
			
			int f[] = new int[feature.size()];
			
			for(int i = 0; i < f.length; i++)
				f[i] = feature.get(i);
	
			fa =  new FeatureArray(f);
		}
		
		
		return fa;
	}
	
	public void get_features(TSFeature mynetwork, List<String[]> inputs, int size)
	{

		mynetwork.feature = new ArrayList[size];
		mynetwork.horizon_feature = new ArrayList[size];

		for (int i = 0; i < size; i++) {

			ArrayList<String[]> featureArr = new ArrayList<String[]>();
			String[] current = inputs.get(i);
			String word = current[WORD_FEATURE_TYPES._id_.ordinal()];
			String escape_word = word.toLowerCase();
			escape_word = this.clean(escape_word);
			
			if (TargetSentimentGlobal.USE_WORD_ONLY) {
				
				featureArr
				.add(new String[] {
						WORD_FEATURE_TYPES._id_.toString(),
						"",escape_word
						 });
				
				mynetwork.feature[i] = featureArr;
				mynetwork.horizon_feature[i] = new ArrayList<String[]>();
				
				for(String[] item : featureArr)
				{
					if (item[0].contains("sent_"))
					{
						mynetwork.horizon_feature[i].add(item);
					}
				}
				
				continue;
				
			}
			
			
			String prior_polarity = current[WORD_FEATURE_TYPES._sent_.ordinal()];
			prior_polarity = clean(prior_polarity);
			
			if (!prior_polarity.endsWith("_"))
			{
				featureArr.add(new String[] {"_is_sent","","" });
			}
			
			String prior_ther_polarity = current[WORD_FEATURE_TYPES._sent_ther_.ordinal()];
			prior_ther_polarity = prior_ther_polarity.replaceAll(":", "_").toLowerCase();
			
			if (!prior_ther_polarity.endsWith("_"))
			{
				featureArr.add(new String[] {"_is_sent_ther", "","" });
			}
			
			String Out = "";

			featureArr
					.add(new String[] {
							WORD_FEATURE_TYPES._id_.toString(),
							"",escape_word
							 });
			featureArr.add(new String[] {
					WORD_FEATURE_TYPES._sent_.toString(),
					"",
					prior_polarity });
			featureArr.add(new String[] {
					WORD_FEATURE_TYPES._sent_ther_.toString(),
					"",
					prior_ther_polarity });
			featureArr.add(new String[] {
					WORD_FEATURE_TYPES.brown_clusters3.toString() + ":",
					"",
					current[WORD_FEATURE_TYPES.brown_clusters3
							.ordinal()] });
			
			featureArr.add(new String[] {
					WORD_FEATURE_TYPES.brown_clusters5.toString() + ":",
					"",
					current[WORD_FEATURE_TYPES.brown_clusters5
							.ordinal()] });
			
			if (TargetSentimentGlobal.USE_POS_TAG)
				featureArr.add(new String[] {
					WORD_FEATURE_TYPES.postag.toString() + ":",
					"",
					current[WORD_FEATURE_TYPES.postag
							.ordinal()] });
			
			String word_parent = word;

			if (TargetSentimentGlobal.word_feature_on) {
				
				featureArr.add(new String[] { "_NE_word_length", Out,
						word_parent.length() + "" });

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

				featureArr.add(new String[] { "_NE_sentence_pos:", Out,
						sentence_pos });

				String message_len_group = "";
				if (size <= 5)
					message_len_group = "1";
				else if (size <= 10)
					message_len_group = "2";
				else
					message_len_group = "3";

				featureArr.add(new String[] { "_NE_message_len_group:", Out,
						message_len_group });

				if (word_parent.length() == 3 || word_parent.length() == 4)
					featureArr.add(new String[] { "_NE_3_4_letters", Out, "" });

				
				if (Character.isUpperCase(word_parent.charAt(0)))
					featureArr.add(new String[] { "_NE_first_letter_upper",
							Out, "" });
				
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
					featureArr.add(new String[] {
							"_NE_more_than_one_letter_upper", Out, "" });
				}

				if (hasDash)
					featureArr.add(new String[] { "_NE_has_dash", Out, "" });

				if (hasDigit)
					featureArr.add(new String[] { "_NE_has_digit", Out, "" });

				if (allLowercase)
					featureArr
							.add(new String[] { "_NE_is_lower_case", Out, "" });

				if (repeat > 1)
					featureArr.add(new String[] { "_sent_has_repeat", Out,
							"" });

				if (word_parent.indexOf('!') >= 0)
					featureArr.add(new String[] { "_sent_has_exclaim", Out,
							"" });

				if (word_parent.indexOf("!!") >= 0)
					featureArr.add(new String[] { "_sent_has_many_exclaim",
							Out, "" });

				if (word_parent.indexOf("...") >= 0)
					featureArr.add(new String[] { "_sent_has_ellipse", Out,
							"" });

				if (word_parent.indexOf("?") >= 0)
					featureArr.add(new String[] { "_sent_has_question",
							Out, "" });

				if (word_parent.indexOf("??") >= 0)
					featureArr.add(new String[] {
							"_sent_has_many_question", Out, "" });

				String lowercase = word_parent.toLowerCase();
				String[] laugh = new String[] { "haha", "jaja", "jeje",	"hehe", "hihi", "jiji" };
				for (int j = 0; j < laugh.length; j++)
					if (lowercase.indexOf(laugh[j]) >= 0) {
						featureArr.add(new String[] { "_sent_has_laugh", Out, "" });
						break;
					}

				
				String jerboa = current[WORD_FEATURE_TYPES.jerboa.ordinal()].toLowerCase();
				
				if (jerboa.indexOf("emoticon") >= 0 || jerboa.indexOf("smiling") >= 0 || jerboa.indexOf("frowning") >= 0)
				{
					if (word_parent.indexOf(')') >= 0 || word_parent.indexOf('D') >= 0 	|| word_parent.indexOf(']') >= 0)
						featureArr.add(new String[] {"_sent_jerb_happy", Out, "" });

					if (word_parent.indexOf('(') >= 0 || word_parent.indexOf('[') >= 0) 
						featureArr.add(new String[] { "_sent_jerb_sad", Out, "" });

				}

				for (String filename : TargetSentimentGlobal.LinguisticFeaturesLibaryName) {
					if (filename
							.equals(TargetSentimentGlobal.LinguisticFeaturesLibaryNamePart[0]))
						continue;

					ArrayList<String> libarylist = LinguisticFeaturesLibaryList
							.get(filename);
					if (libarylist == null)
						continue;
					
					String featurename =  LinguisticFeatureLibNameFeatureMapping.get(filename);
					if (featurename == null)
					{
						featurename = filename;
						if (featurename.endsWith(".txt"))
						{
							featurename = featurename.substring(0, featurename.length() - 4);
						}
					}

					if (libarylist.indexOf(word_parent) >= 0) {
						featureArr.add(new String[] { "_sent_" + featurename,
								"", "" });
						continue;

					}
				}

				String filename = TargetSentimentGlobal.LinguisticFeaturesLibaryNamePart[0];
				ArrayList<String> libarylist = LinguisticFeaturesLibaryList.get(filename);
				String featurename = LinguisticFeatureLibNameFeatureMapping.get(filename);
				if (featurename == null)
				{
					featurename = filename;
					if (featurename.endsWith(".txt"))
					{
						featurename = featurename.substring(0, featurename.length() - 4);
					}
				}
				if (libarylist != null)
				for (String prefix : libarylist)
					if (word_parent.indexOf(prefix) >= 0) {
						featureArr
								.add(new String[] {
										"_sent_"
												+ featurename,
										"", "" });
					}
				
				
				if (escape_word.equals("my") || escape_word.equals("mis") || escape_word.equals("mi"))
				{
					featureArr.add(new String[]{"_sent_has_mi", "", ""});
				}
		        if (escape_word.equals("nunca") || escape_word.equals("nadie"))
		        {
		        	featureArr.add(new String[]{"_sent_has_no_word", "", ""});
		            
		        }
			}

			mynetwork.feature[i] = featureArr;
			mynetwork.horizon_feature[i] = new ArrayList<String[]>();
			
			for(String[] item : featureArr)
			{
				if (item[0].contains("sent_"))
				{
					mynetwork.horizon_feature[i].add(item);
				}
			}
		}

		
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
	

};
	
	
	
	

}


