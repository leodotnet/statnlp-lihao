package org.statnlp.negationfocus.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.AbstractMap.SimpleImmutableEntry;

import org.statnlp.commons.types.Label;
import org.statnlp.hypergraph.FeatureArray;
import org.statnlp.hypergraph.FeatureManager;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.NetworkIDMapper;
import org.statnlp.negationfocus.common.NegationCompiler.NodeType;
import org.statnlp.negationfocus.common.NegationFeatureManager;
import org.statnlp.negationfocus.common.NegationGlobal;
import org.statnlp.negationfocus.common.NegationInstance;
import org.statnlp.negationfocus.common.SemanticRole;
import org.statnlp.negationfocus.common.Utils;

import edu.stanford.nlp.trees.Tree;

public class NegationFocusSemiFeatureManager extends NegationFeatureManager {

	/**
	 * 
	 */
	private static final long serialVersionUID = -4402871346085113082L;

	public NegationFocusSemiFeatureManager(GlobalNetworkParam param_g) {
		super(param_g);
	}

	public NegationFocusSemiFeatureManager(GlobalNetworkParam param_g, String neuralType, boolean moreBinaryFeatures) {
		super(param_g, neuralType, moreBinaryFeatures);
	}

	@Override
	protected FeatureArray extract_helper(Network network, int parent_k, int[] children_k, int children_k_index) {
		if (children_k.length > 2)
			throw new RuntimeException("The number of children should be at most 2, but it is " + children_k.length);

		NegationInstance inst = ((NegationInstance) network.getInstance());

		long node_parent = network.getNode(parent_k);

		if (children_k.length == 0)
			return FeatureArray.EMPTY;

		int size = inst.size();
		String sentence = inst.getSentence();

		long node_child = network.getNode(children_k[0]);

		int[] ids_parent = NetworkIDMapper.toHybridNodeArray(node_parent);
		int[] ids_child = NetworkIDMapper.toHybridNodeArray(node_child);

		ArrayList<String[]> inputs = (ArrayList<String[]>) inst.getInput();
		ArrayList<Label> outputs = (ArrayList<Label>) inst.getOutput();

		int pos_parent = size - ids_parent[0];
		int pos_child = size - ids_child[0];

		int tag_parent = ids_parent[1];
		int tag_child = ids_child[1];

		int nodetype_parent = ids_parent[2];
		int nodetype_child = ids_child[2];

		ArrayList<SemanticRole> srList = inst.srList;

		ArrayList<Integer> featureList = new ArrayList<Integer>();
		ArrayList<Integer> continuousFeatureList = new ArrayList<Integer>();
		ArrayList<Double> continuousFeatureValueList = new ArrayList<Double>();

		FeatureArray fa = FeatureArray.EMPTY;

		int tagId = -1;
		String tagStr = null;
		String word = null;

		ArrayList<String[]> featureArr = new ArrayList<String[]>();

		if (nodetype_parent == NodeType.Root.ordinal()) {
			featureList.add(this._param_g.toFeature(network, "transition_", START, NodeType.values()[nodetype_parent] + "=>" + NodeType.values()[nodetype_child]));

		} else {
			tagId = tag_parent;
			tagStr = this.compiler._labels[tagId].getForm();

			if (nodetype_child == NodeType.Node.ordinal())
				featureList.add(this._param_g.toFeature(network, "transition_", tagStr, NodeType.values()[nodetype_parent] + "=>" + NodeType.values()[nodetype_child]));
			else
				featureList.add(this._param_g.toFeature(network, "transition_", tagStr, NodeType.values()[nodetype_parent] + "=>" + END));
			
			
		

			// int pos = pos_parent;
			for (int pos = pos_parent; pos < pos_child; pos++) {
				int index = pos - pos_parent;
				int rindex = pos_child - pos;
				
				
				

				String w = Utils.getToken(inputs, pos, NegationInstance.FEATURE_TYPES.word.ordinal());
				String POStag = Utils.getToken(inputs, pos, NegationInstance.FEATURE_TYPES.pos_tag.ordinal());

				// if (!Utils.contentWordPOSTag.contains(POStag)) continue;

				String lw = Utils.getToken(inputs, pos - 1, NegationInstance.FEATURE_TYPES.word.ordinal());
				String llw = Utils.getToken(inputs, pos - 2, NegationInstance.FEATURE_TYPES.word.ordinal());
				String rw = Utils.getToken(inputs, pos + 1, NegationInstance.FEATURE_TYPES.word.ordinal());
				String rrw = Utils.getToken(inputs, pos + 2, NegationInstance.FEATURE_TYPES.word.ordinal());

				word = Utils.getToken(inputs, pos, NegationInstance.FEATURE_TYPES.word.ordinal());

				String ct = tagStr;
				String nt = (nodetype_child == NodeType.Node.ordinal()) ? this.compiler._labels[tag_child].getForm() : this.END;

				if (NegationGlobal.ENABLE_DISCRETE_FEATURE) {
					
					if (tagStr.startsWith("I")) {
						if (index == 0) {
							featureList.add(this._param_g.toFeature(network, FeaType.word + "begin", tagStr, w));
							featureList.add(this._param_g.toFeature(network, FeaType.word + "begin", tagStr, w + rw));
							featureList.add(this._param_g.toFeature(network, FeaType.word + "begin", tagStr, lw + w));
							featureList.add(this._param_g.toFeature(network, FeaType.word + "begin", tagStr, lw + w + rw));
							featureList.add(this._param_g.toFeature(network, FeaType.word + "begin", tagStr, llw + lw + w + rw + rrw));
						} else if (rindex == 1) {
							featureList.add(this._param_g.toFeature(network, FeaType.word + "end", tagStr, w));
							featureList.add(this._param_g.toFeature(network, FeaType.word + "end", tagStr, w + rw));
							featureList.add(this._param_g.toFeature(network, FeaType.word + "end", tagStr, lw + w));
							featureList.add(this._param_g.toFeature(network, FeaType.word + "end", tagStr, lw + w + rw));
							featureList.add(this._param_g.toFeature(network, FeaType.word + "end", tagStr, llw + lw + w + rw + rrw));

						}
					}
					
					

					featureList.add(this._param_g.toFeature(network, FeaType.word + "index_" + index, tagStr, w));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "index_" + index, tagStr, w + rw));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "index_" + index, tagStr, lw + w));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "index_" + index, tagStr, lw + w + rw));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "index_" + index, tagStr, llw + lw + w + rw + rrw));
					
					/*
					featureList.add(this._param_g.toFeature(network, FeaType.word + "rindex_" + rindex, tagStr, w));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "rindex_" + rindex, tagStr, w + rw));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "rindex_" + rindex, tagStr, lw + w));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "rindex_" + rindex, tagStr, lw + w + rw));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "rindex_" + rindex, tagStr, llw + lw + w + rw + rrw));*/

					featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, w));
					featureList.add(this._param_g.toFeature(network, FeaType.word + " ", tagStr, w + rw));
					featureList.add(this._param_g.toFeature(network, FeaType.word + " ", tagStr, lw + w));
					featureList.add(this._param_g.toFeature(network, FeaType.word + " ", tagStr, lw + w + rw));
					featureList.add(this._param_g.toFeature(network, FeaType.word + " ", tagStr, llw + lw + w + rw + rrw));

					int[] featureCandicate = new int[] { 0, 2, 3, 4, 5, 7 };

					if (inst.focusVerb[pos] == 1) {
						String focusVerb = Utils.getToken(inputs, pos, NegationInstance.FEATURE_TYPES.word.ordinal());
						featureList.add(this._param_g.toFeature(network, FeaType.word + "index_" + index, tagStr, focusVerb));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "index_" + index, tagStr, focusVerb + rw));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "index_" + index, tagStr, lw + focusVerb));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "index_" + index, tagStr, lw + focusVerb + rw));
						featureList.add(this._param_g.toFeature(network, FeaType.word + "index_" + index, tagStr, llw + lw + focusVerb + rw + rrw));

						
					}

					SemanticRole sr = inst.sr;

					if (sr != null) {

						int idx = sr.roletype[pos];
						if (idx != -1) {
							String roleName = sr.roleNameList.get(idx);
							featureList.add(this._param_g.toFeature(network, FeaType.SR + "index_" + index, tagStr, roleName));
							featureList.add(this._param_g.toFeature(network, FeaType.SR + "index_" + index, tagStr, roleName.substring(0, 1)));

							featureList.add(this._param_g.toFeature(network, FeaType.SR + "index_" + index, tagStr, Utils.getToken(inputs, pos, NegationInstance.FEATURE_TYPES.pos_tag.ordinal())));
							featureList.add(this._param_g.toFeature(network, FeaType.SR + "index_" + index, tagStr, Utils.getToken(inputs, pos, NegationInstance.FEATURE_TYPES.word.ordinal())));

							if (roleName.contains("-")) {
								String[] tmp = roleName.split("-");
								featureList.add(this._param_g.toFeature(network, FeaType.SR + "index_" + index, tagStr, tmp[0]));
								featureList.add(this._param_g.toFeature(network, FeaType.SR + "index_" + index, tagStr, tmp[1]));
							}
						}
					}

					// for(int i =
					// NegationInstance.FEATURE_TYPES.word.ordinal(); i
					// <= NegationInstance.FEATURE_TYPES.syntax.ordinal(); i++)
					for (int i : featureCandicate) {
						if (i == 7) {

							String featureType = NegationInstance.FEATURE_TYPES.values()[i].name();

							featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct, "curr_" + featureType + ":head:" + Utils.getTokenDPHead(inputs, pos, i - 1)));
							featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct, "curr_" + featureType + ":lable:" + Utils.getToken(inputs, pos, i)));
							featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct, "curr_" + featureType + ":head_lable:" + Utils.getTokenDPHead(inputs, pos, i - 1) + "-" + Utils.getToken(inputs, pos, i)));

							featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "curr_" + featureType + ":head:" + Utils.getTokenDPHead(inputs, pos, i - 1)));
							featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "curr_" + featureType + ":lable:" + Utils.getToken(inputs, pos, i)));
							featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "curr_" + featureType + ":head_lable:" + Utils.getTokenDPHead(inputs, pos, i - 1) + "-" + Utils.getToken(inputs, pos, i)));

						} else {

							int roleIdx = sr.roletype[pos];
							String roleName = roleIdx >= 0 ? sr.roleNameList.get(roleIdx) : "*";

							String featureType = NegationInstance.FEATURE_TYPES.values()[i].name();
							featureList.add(this._param_g.toFeature(network, FeaType.tag + "index_" + index, ct + "=>" + nt, "curr_" + featureType + ":" + Utils.getToken(inputs, pos, i)));
							featureList.add(this._param_g.toFeature(network, FeaType.tag + "index_" + index, ct + "=>" + nt, "next_" + featureType + ":" + Utils.getToken(inputs, pos + 1, i)));
							featureList.add(this._param_g.toFeature(network, FeaType.tag + "index_" + index, ct + "=>" + nt, "last_" + featureType + ":" + Utils.getToken(inputs, pos - 1, i)));

							featureList.add(this._param_g.toFeature(network, FeaType.tag + "index_" + index, ct + "=>" + nt, "next_" + featureType + ":" + Utils.getToken(inputs, pos, i) + Utils.getToken(inputs, pos + 1, i)));
							featureList.add(this._param_g.toFeature(network, FeaType.tag + "index_" + index, ct + "=>" + nt, "last_" + featureType + ":" + Utils.getToken(inputs, pos - 1, i) + Utils.getToken(inputs, pos + 0, i)));

							featureList.add(this._param_g.toFeature(network, FeaType.tag + "index_" + index, ct + "=>" + nt, "curr_" + featureType + ":" + Utils.getToken(inputs, pos, i) + "-" + roleName));

							featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "curr_" + featureType + ":" + Utils.getToken(inputs, pos, i)));
							featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "next_" + featureType + ":" + Utils.getToken(inputs, pos + 1, i)));
							featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "last_" + featureType + ":" + Utils.getToken(inputs, pos - 1, i)));

							featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "next_" + featureType + ":" + Utils.getToken(inputs, pos, i) + Utils.getToken(inputs, pos + 1, i)));
							featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "last_" + featureType + ":" + Utils.getToken(inputs, pos - 1, i) + Utils.getToken(inputs, pos + 0, i)));

							featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "curr_" + featureType + ":" + Utils.getToken(inputs, pos, i) + "-" + roleName));

							if ((ct.startsWith("O") || ct.equals("Root")) && (nt.startsWith("I"))) {

								featureList.add(this._param_g.toFeature(network, FeaType.tag + "", "FOCUS_BEGIN", "curr_" + featureType + ":" + Utils.getToken(inputs, pos, i)));
								if (i > 0)
									continue;

								if (roleName.startsWith("A1")) {

									if (Utils.A1_POSTags.contains(POStag)) {
										featureList.add(this._param_g.toFeature(network, FeaType.tag + "", "FOCUS_BEGIN", "curr_" + featureType + "contains:" + POStag));
										featureList.add(this._param_g.toFeature(network, FeaType.tag + "", "FOCUS_BEGIN", "curr_" + featureType + "contains_desired_POStag"));

									}
								}

								String negatedVerb = Utils.join("-", Utils.getSeqTokens(inputs, inst.focusVerb, NegationInstance.FEATURE_TYPES.word.ordinal()));
								featureList.add(this._param_g.toFeature(network, FeaType.tag + "", "FOCUS_BEGIN", "curr_" + featureType + "_negatedVerb:" + negatedVerb));
								int pos_verb = Utils.getFirstOccurence(inst.focusVerb);
								featureList.add(this._param_g.toFeature(network, FeaType.tag + "", "FOCUS_BEGIN", "curr_" + featureType + "_negatedVerbPOS:" + Utils.getToken(inputs, pos_verb, NegationInstance.FEATURE_TYPES.pos_tag.ordinal())));

								// int pos_verb =
								// Utils.getFirstOccurence(inst.focusVerb);

								featureType = "syntactic";

								int[] featureCandicateSyn = new int[] { 0, 2 };

								Tree VPTopMost = Utils.getAncestorTopMost(inst.tree, pos_verb, "VP");
								if (VPTopMost != null) {
									int pos_begin = Utils.getTextSpanBeginPosUntil(inst.tree, VPTopMost, pos_verb);

									for (int fIdx : featureCandicateSyn) {
										ArrayList<String> textspanTopMostList = Utils.getSeqTokens(inputs, pos_begin, pos_verb, fIdx);
										String textspanTopMost = Utils.join("-", textspanTopMostList);
										featureList.add(this._param_g.toFeature(network, FeaType.tag + "", "VPTOPMOST_" + NegationInstance.FEATURE_TYPES.values()[fIdx], featureType + ":" + textspanTopMost));
									}
								} else {
									// System.out.println();
									// VPTopMost =
									// Utils.getAncestorTopMost(inst.tree, pos,
									// "VP");
								}

								Tree VPBtmMost = Utils.getAncestorBtmMost(inst.tree, pos_verb, "VP");
								if (VPBtmMost != null) {
									int pos_begin = Utils.getTextSpanBeginPosUntil(inst.tree, VPBtmMost, pos_verb);

									for (int fIdx : featureCandicateSyn) {
										ArrayList<String> textspanBtmMostList = Utils.getSeqTokens(inputs, pos_begin, pos_verb, fIdx);
										String textspanBtmMost = Utils.join("-", textspanBtmMostList);
										featureList.add(this._param_g.toFeature(network, FeaType.tag + "", "VPBTMMOST_" + NegationInstance.FEATURE_TYPES.values()[fIdx], featureType + ":" + textspanBtmMost));
									}
								}
							}

							if (nt.startsWith("I")) {

								if (i > 0)
									continue;

								featureType = "length";
								featureList.add(this._param_g.toFeature(network, FeaType.tag + "", "FOCUS", "curr_" + featureType + ":1"));
							}
						}
					}

					// featureList.add(this._param_g.toFeature(network,
					// FeaType.word
					// + "", tagStr, "Is1stCap:" + Utils.isFirstCap(w)));
					// featureList.add(this._param_g.toFeature(network,
					// FeaType.word
					// + "", tagStr, "Is1stCap:" + Utils.isFirstCap(w) + w));

					featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "IsAllCap:" + Utils.isAllCap(w)));
					// featureList.add(this._param_g.toFeature(network,
					// FeaType.word
					// + "", tagStr, "IsAllCap:" + Utils.isAllCap(w) + w));

					featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "IsPunc:" + Utils.isPunctuation(w)));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "", tagStr, "IsPunc:" + Utils.isPunctuation(w) + w));

					featureList.add(this._param_g.toFeature(network, FeaType.word + "before", tagStr, "IsPunc:" + Utils.isPunctuation(lw)));
					featureList.add(this._param_g.toFeature(network, FeaType.word + "after", tagStr, "IsPunc:" + Utils.isPunctuation(rw)));

					// for(int i =
					// NegationInstance.FEATURE_TYPES.word.ordinal(); i
					// <= NegationInstance.FEATURE_TYPES.syntax.ordinal(); i++)
					for (int i : featureCandicate) {
						String featureType = NegationInstance.FEATURE_TYPES.values()[i].name();

						featureList.add(this._param_g.toFeature(network, featureType + "before", tagStr, Utils.getToken(inputs, pos - 1, i)));
						featureList.add(this._param_g.toFeature(network, featureType + "after", tagStr, Utils.getToken(inputs, pos + 1, i)));

						featureList.add(this._param_g.toFeature(network, featureType + "", tagStr, Utils.getToken(inputs, pos, i)));
					}

					if (inst.focusVerb[pos] == 1) {
						String focusVerb = Utils.getToken(inputs, pos, NegationInstance.FEATURE_TYPES.word.ordinal());
						featureList.add(this._param_g.toFeature(network, FeaType.word + ":focusVerbForm", tagStr, focusVerb));
						featureList.add(this._param_g.toFeature(network, FeaType.word + ":focusVerbWord", tagStr, w));

						// for(int i =
						// NegationInstance.FEATURE_TYPES.word.ordinal(); i <=
						// NegationInstance.FEATURE_TYPES.syntax.ordinal(); i++)
						for (int i : featureCandicate) {
							String featureType = NegationInstance.FEATURE_TYPES.values()[i].name();
							featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "curr_N_" + featureType + ":" + Utils.getToken(inputs, pos, i)));

							featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "before_N_" + featureType + ":" + Utils.getToken(inputs, pos + 1, i)));
							featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "after_N_" + featureType + ":" + Utils.getToken(inputs, pos - 1, i)));

							featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "before_N_" + featureType + ":" + Utils.getToken(inputs, pos, i) + Utils.getToken(inputs, pos + 1, i)));
							featureList.add(this._param_g.toFeature(network, FeaType.tag + "", ct + "=>" + nt, "after_N_" + featureType + ":" + Utils.getToken(inputs, pos - 1, i) + Utils.getToken(inputs, pos, i)));

						}

					}

				}

			}

		}

		if (NetworkConfig.USE_NEURAL_FEATURES) {
			if (tagId != -1) {
				Object input = null;
				if (neuralType.startsWith("lstm")) {
					String sentenceInput = sentence;
					input = new SimpleImmutableEntry<String, Integer>(sentenceInput, pos_parent);

					this.addNeural(network, 0, parent_k, children_k_index, input, tagId);
				} else if (neuralType.equals("continuous0")) {

					if (tagStr.startsWith("I")) {

						double[] avg = new double[NegationGlobal.Word2Vec.ShapeSize];
						Arrays.fill(avg, 0);
						int numVec = 0;
						
						double[] pooling = new double[NegationGlobal.Word2Vec.ShapeSize];
						Arrays.fill(pooling, 0);
						int numPooling = 0;
						
						for (int pos = pos_parent; pos < pos_child; pos++) {

							String w = Utils.getToken(inputs, pos, NegationInstance.FEATURE_TYPES.word.ordinal());
							String POStag = Utils.getToken(inputs, pos, NegationInstance.FEATURE_TYPES.pos_tag.ordinal());

							if (NegationGlobal.EMBEDDING_WORD_LOWERCASE)
								w = w.toLowerCase();
							String ww = w + "";
							try {
								w = Utils.strProcess(w);
							} catch (Exception e) {
								System.out.println(ww);
								w = Utils.strProcess(ww);
								System.exit(-1);
							}

							//if (!Utils.contentWordPOSTag.contains(w)) continue;

							double[] vec = NegationGlobal.Word2Vec.getVector(w);
							if (vec == null) {
								vec = NegationGlobal.Word2Vec.getVector(NegationGlobal.UNK);

								if (vec == null) {
									vec = new double[NegationGlobal.Word2Vec.ShapeSize];
									Arrays.fill(vec, 0);
								}
							}

							avg = Utils.vectorAdd(avg, vec);
							pooling = Utils.vectorMax(pooling, vec);
							numVec++;

						}

						for (int i = 0; i < NegationGlobal.Word2Vec.ShapeSize; i++) {
							continuousFeatureList.add(this._param_g.toFeature(network, "continuous-avgemb:", tagStr, "dim" + i + ":"));
							continuousFeatureValueList.add(avg[i]);
							
							continuousFeatureList.add(this._param_g.toFeature(network, "continuous-maxemb:", tagStr, "dim" + i + ":"));
							continuousFeatureValueList.add(pooling[i]);
						}
					}

				} else {
					input = word;// .toLowerCase();

					if (tagId != -1) {

						if (NegationGlobal.EMBEDDING_WORD_LOWERCASE)
							input = word.toLowerCase();
						this.addNeural(network, 0, parent_k, children_k_index, input, tagId);
					}
				}

			}
		} else {
			// featureList.add(this._param_g.toFeature(network,
			// FeaType.word.name(), entity, word));
		}

		FeatureArray contFa = this.createFeatureArray(network, continuousFeatureList, continuousFeatureValueList);
		fa = this.createFeatureArray(network, featureList, contFa);
		return fa;

	}

}
