package org.statnlp.targetedsentiment.ncrf.linear;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
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
import org.statnlp.hypergraph.neural.NeuralNetworkCore;
import org.statnlp.targetedsentiment.common.TSFeature;
import org.statnlp.targetedsentiment.common.TSInstance;
import org.statnlp.targetedsentiment.common.TargetSentimentGlobal;
import org.statnlp.targetedsentiment.ncrf.linear.SentimentParsingLinearCompiler;
import org.statnlp.targetedsentiment.ncrf.linear.SentimentParsingLinearCompiler.*;

public class CopyOfSentimentParsingLinearFeatureManager extends FeatureManager {

	/**
	 * 
	 */
	private static final long serialVersionUID = 592253662868854534L;
	private NeuralNetworkCore net;
	public String neuralType;
	public boolean moreBinaryFeatures = false;

	public CopyOfSentimentParsingLinearFeatureManager(GlobalNetworkParam param_g) {
		super(param_g, null);
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			this.net = (NeuralNetworkCore) param_g.getNNParamG().getNet(0);
		}
	}

	public CopyOfSentimentParsingLinearFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
		super(param_g);
		if (NetworkConfig.USE_NEURAL_FEATURES) {
			this.net = (NeuralNetworkCore) param_g.getNNParamG().getNet(0);
		}
		this.neuralType = neuralType;
		this.moreBinaryFeatures = moreBinaryFeatures;
	}

	public enum FEATURE_TYPES {
		Unigram, Bigram, Trigram, Fourgram, Transition
	}

	public enum WORD_FEATURE_TYPES {
		_id_, ne, brown_clusters5, jerboa, brown_clusters3, _sent_, _sent_ther_, sentiment, postag
	};

	int NodeTypeSize = NodeType.values().length;
	int PolarityTypeSize = PolarityType.values().length;
	int WordFeatureTypeSize = WORD_FEATURE_TYPES.values().length;

	public static String[] Subjectivity = new String[] { "non-neutral1", "non-neutral1", "neutral1" };

	static HashMap<String, ArrayList<String>> LinguisticFeaturesLibaryList = new HashMap<String, ArrayList<String>>();

	public void loadLinguisticFeatureLibary() {
		LinguisticFeaturesLibaryList.clear();
		/*
		 * Scanner scan = null; for(String filename :
		 * TargetSentimentGlobal.LinguisticFeaturesLibaryName) {
		 * ArrayList<String> libarylist = new ArrayList<String>(); try { scan =
		 * new Scanner(new File(TargetSentimentGlobal.feature_file_path +
		 * filename)); } catch (FileNotFoundException e) { // TODO
		 * Auto-generated catch block e.printStackTrace(); }
		 * 
		 * 
		 * while(scan.hasNextLine()) { libarylist.add(scan.nextLine().trim()); }
		 * 
		 * scan.close();
		 * 
		 * LinguisticFeaturesLibaryList.put(filename, libarylist); }
		 */
	}

	public void addPrefix(ArrayList<String[]> featureArr, String[] f, String prefix, String Sent, String NE) {

		featureArr.add(new String[] { prefix + f[0], f[1], f[2] });
	}

	/*
	 * public void addWordEmbedding(ArrayList<Integer> feature, String word,
	 * String output) { Double[] vector = Word2Vec.getVector(word); int word_id
	 * = Word2Vec.getWordID(word);
	 * 
	 * for(int i = 0; i < Word2Vec.ShapeSize; i++) {
	 * feature.add(this.getParam_G().toFeature(
	 * WordEmbedding.WORD_EMBEDDING_FEATURE, output, "D" + i, word_id,
	 * vector[i])); } }
	 */

	public String escape(String s) {
		for (Character val : string_map.keySet()) {
			String target = val + "";
			if (s.indexOf(target) >= 0) {
				String repl = string_map.get(val);
				s = s.replace(target, "");

			}
		}

		return s;
	}

	public String norm_digits(String s) {
		s = s.replaceAll("\\d+", "0");

		return s;

	}

	public String clean(String s) {
		String str;
		if (s.startsWith("http://") || s.startsWith("https://")) {
			str = "<WEBLINK>";
		} else if (s.startsWith("@")) {
			str = "<USERNAME>";
		} else {
			str = norm_digits(s.toLowerCase());
			// String str1 = escape(str);
			// if (str1.length() >= 0)

			String str1 = str.replaceAll("[^A-Za-z0-9_]", "");
			if (str1.length() > 0)
				str = str1;
		}

		return str;
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

	String getToken(ArrayList<String[]> inputs, int pos, int idx) {
		if (pos >= 0 && pos < inputs.size()) {
			return inputs.get(pos)[idx];
		} else if (pos < 0) {
			return "<START>";
		} else {
			return "<END>";
		}
	}

	boolean isAllCap(String word) {
		for (int i = 0; i < word.length(); i++)
			if (Character.isLowerCase(word.charAt(i)))
				return false;

		return true;
	}

	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k, int children_k_index) {
		if (children_k.length > 2)
			throw new RuntimeException("The number of children should be at most 2, but it is " + children_k.length);

		TSInstance inst = ((TSInstance) network.getInstance());

		long node_parent = network.getNode(parent_k);

		if (children_k.length == 0)
			return FeatureArray.EMPTY;

		int size = inst.size();

		long node_child = network.getNode(children_k[0]);

		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(node_parent);
		int[] ids_child = NetworkIDMapper.toHybridNodeArray(node_child);

		ArrayList<String[]> inputs = (ArrayList<String[]>) inst.getInput();
		ArrayList<String> outputs = (ArrayList<String>) inst.getOutput();

		int pos_parent = 2 * size - ids_parent[0];
		int pos_child = 2 * size - ids_child[0];

		int polar_parent = ids_parent[2];
		int polar_child = ids_child[2];

		int subnode_parent = ids_parent[1];
		int subnode_child = ids_child[1];

		if (subnode_child == NodeType.X.ordinal()) {
			pos_child = size;
		}

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
		String Sent = "";
		String NE = "O";
		String prefix = null;
		int eId = 0;
		// System.out.println("network.getNetworkId():" +
		// network.getNetworkId());

		TSFeature mynetwork = TargetSentimentGlobal.getFeaturebyNetworkId(inst.getInstanceId(), size);
		if (mynetwork.feature == null) {
			this.get_features(mynetwork, inputs, size);

		}

		if (subnode_parent == NodeType.B.ordinal()) {

			String curr_word = null;
			String next_word = null;
			String prev_word = null;

			/****** NER Begin ******/
			curr_word = getToken(inputs, pos_parent, 0);
			prev_word = getToken(inputs, pos_parent - 1, 0);
			next_word = getToken(inputs, pos_parent + 1, 0);

			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start", "", "current_word:" + curr_word));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start", "", "current_word:" + curr_word + "|||" + "next_word:" + next_word));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start", "", "last_word:" + prev_word + "|||" + "current_word:" + curr_word));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start", "", "last_word:" + prev_word + "|||" + "current_word:" + curr_word + "|||" + "next_word:" + next_word));

			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start", "", "first_letter_upper:" + Character.isUpperCase(curr_word.charAt(0))));

			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Start", "", "is_all_cap:" + isAllCap(curr_word)));

			/*
			 * feature.add(this._param_g.toFeature(network,
			 * FEATURE_TYPES.Unigram + "-" + "NE_contains", "",
			 * "first_letter_upper:" +
			 * Character.isUpperCase(curr_word.charAt(0))));
			 * 
			 * feature.add(this._param_g.toFeature(network,
			 * FEATURE_TYPES.Unigram + "-" + "NE_contains", "", "current_word:"
			 * + curr_word)); feature.add(this._param_g.toFeature(network,
			 * FEATURE_TYPES.Unigram + "-" + "NE_contains", "", "is_all_cap:" +
			 * isAllCap(curr_word)));
			 */
			/****** NER Begin ******/

			/****** NER Sentiment Polarity ******/

			if (children_k.length == 2) {
				long node_child_sentWord = network.getNode(children_k[1]);
				int[] ids_child_sentWord = NetworkIDMapper.toHybridNodeArray(node_child_sentWord);

				int pos_child_sentWord = -1; // 2 * size - ids_parent[0];
				int subnode_child_sentWord = ids_parent[1];

				if (subnode_child_sentWord == NodeType.B0.ordinal()) {
					subnode_child_sentWord = ids_parent[0] - 1;
				} else {
					subnode_child_sentWord = 2 * size - ids_parent[0];
				}

				String[] sentWordFeature = inputs.get(subnode_child_sentWord);
				String sentWord = sentWordFeature[TSInstance.WORD_FEATURE_TYPES._id_.ordinal()];
				String sent = sentWordFeature[TSInstance.WORD_FEATURE_TYPES._sent_.ordinal()];
				String sent_ther = sentWordFeature[TSInstance.WORD_FEATURE_TYPES._sent_ther_.ordinal()];
				Sent = PolarityType.values()[polar_parent].name();

				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Polarity", Sent, "sent_word:" + sentWord));
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Polarity", Sent, "_sent_:" + sent));
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Polarity", Sent, "_sent_ther:" + sent_ther));
				
				// Extract features within window
				for (int w = -3; w <= 3; w++) {
					if (w != 0) {
						String w_word = getToken(inputs, pos_parent + w, TSInstance.WORD_FEATURE_TYPES._id_.ordinal());
						String w_sentWord = getToken(inputs, pos_parent + w, TSInstance.WORD_FEATURE_TYPES._sent_.ordinal());
						String w_sent_ther = getToken(inputs, pos_parent + w, TSInstance.WORD_FEATURE_TYPES._sent_ther_.ordinal());

						feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Polarity", Sent, "sent_word:" + w_word));
						feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Polarity", Sent, "sent_word:" + w_sentWord));
						feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Polarity", Sent, "sent_word:" + w_sent_ther));
					}
				}
			} else {
				Sent = PolarityType.values()[polar_parent].name();
				
				
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Polarity", Sent, "no_sentiment_word_found"));
				
				
				// Extract features within window
				for (int w = -3; w <= 3; w++) {
					if (w != 0) {
						String w_word = getToken(inputs, pos_parent + w, TSInstance.WORD_FEATURE_TYPES._id_.ordinal());
						String w_sentWord = getToken(inputs, pos_parent + w, TSInstance.WORD_FEATURE_TYPES._sent_.ordinal());
						String w_sent_ther = getToken(inputs, pos_parent + w, TSInstance.WORD_FEATURE_TYPES._sent_ther_.ordinal());

						feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Polarity", Sent, "sent_word:" + w_word));
						feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Polarity", Sent, "sent_word:" + w_sentWord));
						feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_Polarity", Sent, "sent_word:" + w_sent_ther));
					}
				}
			}

			/****** NER Sentiment Polarity ******/

		}

		if (subnode_parent == NodeType.B.ordinal() || subnode_parent == NodeType.I.ordinal()) {

			/****** NER Inside ******/
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_sentiment_Feature", "", ""));
			for (int pos = pos_parent; pos < pos_child; pos++) {
				if (pos == pos_parent) {
					NE = "B";
				} else {
					NE = "I";
				}

				Out = NE + Sent;

				String curr_word = getToken(inputs, pos, 0);
				String prev_word = getToken(inputs, pos - 1, 0);
				String next_word = getToken(inputs, pos + 1, 0);

				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains", "", "current_word:" + curr_word));

				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains", "", "current_word:" + curr_word + "|||" + "next_word:" + next_word));
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains", "", "last_word:" + prev_word + "|||" + "current_word:" + curr_word));
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains", "", "last_word:" + prev_word + "|||" + "current_word:" + curr_word + "|||" + "next_word:" + next_word));

				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains", "", "first_letter_upper:" + Character.isUpperCase(curr_word.charAt(0))));
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains", "", "is_all_cap:" + isAllCap(curr_word)));

				int index = pos - pos_parent;
				int rindex = pos_child - pos - 1;

				String word_shape = "";// getShape(curr_word);

				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_indexed_word:", NE, index + "_" + curr_word));
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_rindexed_word:", NE, rindex + "_" + curr_word));

				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_indexed_wordshape:", NE, index + "_" + word_shape));
				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_rindexed_wordshape:", NE, rindex + "_" + word_shape));

				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_wordshape:", NE, word_shape));

				feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_contains", NE, "first_letter_upper:" + Character.isUpperCase(word_parent.charAt(0))));

				// /////ugiram
				prefix = "word";

				for (String[] f : mynetwork.feature[pos]) {
					addPrefix(featureArr, f, "indexed_" + prefix + index, Sent, NE);
					addPrefix(featureArr, f, "rindexed_" + prefix + rindex, Sent, NE);
					addPrefix(featureArr, f, prefix, Sent, NE);

				}

				for (String[] f : mynetwork.feature[pos]) {
					if (f[0].indexOf("_is_sent") >= 0)
						num_sent++;
				}

			}
			/****** NER Inside ******/
		}

		if ((subnode_parent == NodeType.B.ordinal() || subnode_parent == NodeType.I.ordinal()) && (subnode_child == NodeType.O.ordinal() || subnode_child == NodeType.X.ordinal())) {

			/****** NER End ******/
			String curr_word = getToken(inputs, pos_child - 1, 0);
			String prev_word = getToken(inputs, pos_child - 1 - 1, 0);
			String next_word = getToken(inputs, pos_child - 1 + 1, 0);

			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_End", "", "current_word:" + curr_word));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_End", "", "current_word:" + word_parent + "|||" + "next_word:" + next_word));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_End", "", "last_word:" + prev_word + "|||" + "current_word:" + curr_word));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_End", "", "last_word:" + prev_word + "|||" + "current_word:" + curr_word + "|||" + "next_word:" + next_word));

			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_End", "", "first_letter_upper:" + Character.isUpperCase(curr_word.charAt(0))));

			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "NE_End", "", "is_all_cap:" + isAllCap(curr_word)));

			/*
			 * feature.add(this._param_g.toFeature(network,
			 * FEATURE_TYPES.Unigram + "-" + "NE_contains", "",
			 * "first_letter_upper:" +
			 * Character.isUpperCase(curr_word.charAt(0))));
			 * 
			 * feature.add(this._param_g.toFeature(network,
			 * FEATURE_TYPES.Unigram + "-" + "NE_contains", "", "current_word:"
			 * + word_parent)); feature.add(this._param_g.toFeature(network,
			 * FEATURE_TYPES.Unigram + "-" + "NE_contains", "", "is_all_cap:" +
			 * isAllCap(curr_word)));
			 */

		}
		/****** NER End ******/

		if (subnode_parent <= NodeType.B.ordinal() && subnode_parent >= NodeType.O.ordinal()) {
			int segment_start = pos_parent;
			int segment_end = pos_child - 1;

			for (int i = segment_start - 1; i >= segment_start - 3 && i >= 0; i--) {
				prefix = "_immediate_prev";
				for (String[] f : mynetwork.horizon_feature[i]) {
					addPrefix(featureArr, f, prefix, Sent, NE);
				}

				prefix = "wordm" + (segment_start - i);
				for (String[] f : mynetwork.feature[i])

				{
					addPrefix(featureArr, f, prefix, Sent, NE);
				}

			}

			for (int i = segment_end + 1; i <= segment_end + 3 && i < size; i++) {
				prefix = "_immediate_next";
				for (String[] f : mynetwork.horizon_feature[i]) {
					addPrefix(featureArr, f, prefix, Sent, NE);
				}

				prefix = "wordp" + (i - segment_end);
				for (String[] f : mynetwork.feature[i]) {
					addPrefix(featureArr, f, prefix, Sent, NE);
				}

			}

			prefix = "_sent_overall";
			String[] sent_overall = null;

			if (num_sent == 1)
				sent_overall = new String[] { "", "", "_has_one_sent" };

			if (num_sent == 2)
				sent_overall = new String[] { "", "", "_has_two_sent" };

			if (num_sent == 3)
				sent_overall = new String[] { "", "", "_has_three_sent" };

			if (num_sent > 3)
				sent_overall = new String[] { "", "", "_has_alotta_sent" };

			if (num_sent > 0)
				addPrefix(featureArr, sent_overall, prefix, Sent, NE);
		}

		if (featureArr.size() > 0) {

			for (String[] item : featureArr) {

				feature.add(this._param_g.toFeature(network, item[0] + item[2], Out, "_link(NE,SENT)"));

			}

		}

		if (subnode_parent == NodeType.O.ordinal()) {

			Sent = "";
			NE = "O";

			Out = NE + Sent;

			String curr_word = null;
			String next_word = null;
			String prev_word = null;

			/****** O Inside ******/
			// feature.add(this._param_g.toFeature(network,
			// FEATURE_TYPES.Unigram + "-" + "NE_sentiment_Feature", "", ""));

			curr_word = getToken(inputs, pos_parent, 0);
			prev_word = getToken(inputs, pos_parent - 1, 0);
			next_word = getToken(inputs, pos_parent + 1, 0);

			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O", "", "current_word:" + curr_word));

			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O", "", "first_letter_upper:" + Character.isUpperCase(curr_word.charAt(0))));
			feature.add(this._param_g.toFeature(network, FEATURE_TYPES.Unigram + "-" + "O", "", "is_all_cap:" + isAllCap(curr_word)));

		}

		if (featureArr.size() > 0) {
			for (String[] item : featureArr) {
				feature.add(this._param_g.toFeature(network, item[0], Out, item[2]));

			}
		}

		if (feature.size() > 0) {
			if (TargetSentimentGlobal.ENABLE_WORD_EMBEDDING) {

				if (NetworkConfig.USE_NEURAL_FEATURES && WordEmbeddingFeature != null) {

					Object input = null;
					if (neuralType.equals("lstm")) {
						input = new SimpleImmutableEntry<String, Integer>(inst.getSentence(), pos_parent);
					}
					// else if(neuralType.equals("mlp")){
					// input =
					// llw+IN_SEP+lw+IN_SEP+currWord+IN_SEP+rw+IN_SEP+rrw+OUT_SEP+llt+IN_SEP+lt+IN_SEP+currTag+IN_SEP+rt+IN_SEP+rrt;
					// }
					else {
						input = WordEmbeddingFeature;// .toLowerCase();

						if (TargetSentimentGlobal.EMBEDDING_WORD_LOWERCASE)
							input = WordEmbeddingFeature.toLowerCase();
					}
					this.addNeural(network, 0, parent_k, children_k_index, input, eId);
				}
			}

			int f[] = new int[feature.size()];

			for (int i = 0; i < f.length; i++)
				f[i] = feature.get(i);

			fa = new FeatureArray(f);
		}

		return fa;
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
