package org.statnlp.negationfocus.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.commons.types.LinearInstance;
import org.statnlp.commons.types.Sentence;
import org.statnlp.example.base.BaseInstance;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.negationfocus.common.NegationInstance;
import org.statnlp.negationfocus.common.NegationInstance.FEATURE_TYPES;
import org.statnlp.negationfocus.common.Utils;

import edu.stanford.nlp.trees.Tree;


public class NegationInstance<T> extends LinearInstance<T> {

	

	/**
	 * 
	 */
	private static final long serialVersionUID = 1774596480790313564L;

	String sentence = null;
	List<T> outputBackup = null;
	public ArrayList<SemanticRole> srList = new ArrayList<SemanticRole>();
	public boolean hasFocusVerb = true;
	public boolean hasFocusScope = true;
	public int[] focusVerb = null;
	public int[] focusScope = null;
	public Tree tree = null;
	public SemanticRole sr = null;
	
	public ArrayList<Label> outputSemi = null;
	public ArrayList<int[]> outputPosition = null;

	public enum FEATURE_TYPES {
		word, token_id, pos_tag, NER, chunk, syntax, DPIdx, DPName
	};

	public NegationInstance(int instanceId, double weight) {
		this(instanceId, weight, null);
	}

	public NegationInstance(int instanceId, double weight, ArrayList<String[]> input) {
		this(instanceId, weight, input, null, null);
	}

	public NegationInstance(int instanceId, double weight, ArrayList<String[]> input, ArrayList<T> output) {
		this(instanceId, weight, input, output, null);
	}

	public NegationInstance(int instanceId, double weight, ArrayList<String[]> input, ArrayList<T> output, ArrayList<T> prediction) {
		super(instanceId, weight);
		this.input = input;
		this.output = output;
		this.prediction = prediction;
	}

	public void setOutput(ArrayList<T> output) {
		this.output = output;
	}

	@Override
	public int size() {
		return this.input.size();
	}

	public List<String[]> duplicateInput() {
		return input;
	}

	@SuppressWarnings("unchecked")
	public ArrayList<T> duplicateOutput() {
		// ArrayList<T> o = (ArrayList<T>) this.output;
		// return (ArrayList<T>) o.clone();
		return null;
	}

	@Override
	public NegationInstance duplicate() {
		NegationInstance inst = (NegationInstance) super.duplicate();
		inst.srList = this.srList;
		inst.focusVerb = this.focusVerb;
		inst.focusScope = this.focusScope;
		inst.hasFocusScope = this.hasFocusScope;
		inst.hasFocusVerb = this.hasFocusVerb;
		inst.sentence = this.sentence;
		inst.tree = this.tree;
		inst.srList = this.srList;
		inst.sr = this.sr;
		
		return inst;
	}

	public void preprocess() {
		preprocess(false);
	}

	public void preprocess(boolean onlyWord) {

	}
	
	public String getSyntaxTreeStr() {
		ArrayList<String[]> inputs = (ArrayList<String[]>) this.getInput();
		String syntaxTreeStr = "";
		for(int i = 0; i < this.size(); i++) {
			String syntax = inputs.get(i)[FEATURE_TYPES.syntax.ordinal()];
			String postag = inputs.get(i)[FEATURE_TYPES.pos_tag.ordinal()];
			
			syntax = syntax.replace("*", " " + postag);
			syntaxTreeStr += syntax;
		}
		return syntaxTreeStr;
	}

	public String getSentence() {
		if (this.sentence == null) {
			this.sentence = "";
			for (int i = 0; i < input.size(); i++) {
				String word = input.get(i)[FEATURE_TYPES.word.ordinal()];
				if (NegationGlobal.EMBEDDING_WORD_LOWERCASE)
					word = word.toLowerCase();

				this.sentence += word + " ";
			}
		}

		this.sentence = this.sentence.trim();

		return this.sentence;
	}


	public void setPredictionAsOutput() {
		this.outputBackup = this.output;
		this.output = this.prediction;
	}
	

	public String getSentID() {
		return null;
	}



	public static NegationInstance<Label>[] readData(String dataSet, String fileName, boolean withLabels, boolean isLabeled, int TRIAL, boolean discardNoNgeation) throws IOException {

		NegationInstance[] insts = null;
		if (dataSet.startsWith("pbfoc")) {
			insts = readCoNLLData(fileName, withLabels, isLabeled, discardNoNgeation);
		} 

		if (TRIAL > 0)
			insts = Utils.portionInstances(insts, TRIAL);

		return insts;
	}

	
	@SuppressWarnings("unchecked")
	private static NegationInstance<Label>[] readCoNLLData(String fileName, boolean withLabels, boolean isLabeled, boolean discardNoNgeation) throws IOException {
		System.out.println("Read " + fileName);
		InputStreamReader isr = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
		BufferedReader br = new BufferedReader(isr);
		ArrayList<NegationInstance<Label>> result = new ArrayList<NegationInstance<Label>>();
		ArrayList<String[]> words = null;
		ArrayList<String[]> focus = null;
		ArrayList<Label> labels = null;
		
		Utils.Counter counter = new Utils.Counter();
		int numTokens = 0;
		
		int numNegationinSentence = 0;
		int numDiscardInstance = 0;
		int instanceId = 1;
		int numSentence = 0;
		int numNegFocusSentence = 0;
		int SRNoMatched = 0;
		int SR2 = 0; 
		int FOCUSNOROLE = 0;
		int focusScopeSpanStrictlyContained = 0;
		
		
		while (br.ready()) {
			if (words == null) {
				words = new ArrayList<String[]>();
			}
			if (focus == null) {
				focus = new ArrayList<String[]>();
			}
			if (withLabels && labels == null) {
				labels = new ArrayList<Label>();
			}
			String line = br.readLine().trim();
			if (line.startsWith("##")) {
				continue;
			}
			if (line.length() == 0) {
				if (words.size() == 0) {
					continue;
				}

				int size = words.size();
				
				int width = focus.get(0).length;
				
				String pennString = "";
				for(int i = 0; i < words.size(); i++) {
					String[] word = words.get(i);
					String tmp = word[FEATURE_TYPES.pos_tag.ordinal()];
					if (tmp.equals("(") || tmp.equals(")")) {
						tmp = word[FEATURE_TYPES.word.ordinal()];
					}
					pennString += word[FEATURE_TYPES.syntax.ordinal()].replace("*", " " + tmp) + " ";
				}
				
				
				String sentence = "";
				for(int i = 0; i < size; i++)
					sentence +=  words.get(i)[0] + " ";
				
				Tree tree = null;
				tree = Utils.getParseTree(pennString);
				tree = Utils.addLeaves(tree, Utils.table2Col(words, FEATURE_TYPES.word.ordinal()));
				
				if (tree == null) {
					System.out.println();
					System.exit(-1);
				}
				
				/*
				System.out.println(pennString);
				for(int i = 0; i < size; i++)
					System.out.print(words.get(i)[0] + " ");
				System.out.println();
				
				Utils.getSentence(tree);
				System.out.println();
				*/
				
				ArrayList<SemanticRole> srList = new ArrayList<SemanticRole>();
				for(int i = 0; i < width - 2; i++) {
					String[] col = Utils.table2Col(focus, i);
					SemanticRole sr = new SemanticRole(col);
					srList.add(sr);
				}
				
				
				
				
				
				int[] focusVerb = new int[size];
				int[] focusScope = new int[size];
				Arrays.fill(focusVerb, 0);
				Arrays.fill(focusScope, 0);
				
				String[] focusVerbCol = Utils.table2Col(focus, width - 2);
				String[] focusScopeCol = Utils.table2Col(focus, width - 1);
				
				boolean hasFocusVerb = false;
				boolean hasFocusScope = false;
				
				for(int i = 0; i < focusVerbCol.length; i++) {
					if (focusVerbCol[i].equals("N")) {
						focusVerb[i] = 1;
						hasFocusVerb = true;
					}
					if (focusScopeCol[i].equals("FOCUS")) {
						focusScope[i] = 1;
						hasFocusScope = true;
					}
				}
				
				assert(hasFocusVerb == hasFocusScope);
				
				SemanticRole sr = Utils.getSemanticRole(srList, focusVerb);
				if (hasFocusScope && sr == null) {
					System.err.println(sentence);
				}
				
				
				/*
				if (hasFocusScope &&  !hasFocusVerb) {
					System.err.println("no verb but has scope...");
					for(int i = 0; i < size; i++)
						System.out.print(words.get(i)[0] + " ");
					System.out.println();
					System.exit(-1);
				}*/
				

				if (hasFocusScope == false || (isLabeled == true && sr == null)) {

					if (discardNoNgeation) {
						numDiscardInstance++;
					} else {
						
						numSentence++;
						NegationInstance<Label> instance = new NegationInstance<Label>(instanceId, 1, words, null);

					
						instance.srList = srList;
						instance.focusVerb = focusVerb;
						instance.focusScope = focusScope;
						instance.hasFocusVerb = hasFocusVerb;
						instance.hasFocusScope = hasFocusScope;
						instance.tree = tree;
						instance.sr = sr;
						
						if (isLabeled) {
							instance.setLabeled(); // Important!
						} else {
							instance.setUnlabeled();
						}
						instanceId++;
						instance.preprocess();
						result.add(instance);
					}
				} else {
					numSentence++;
					numNegFocusSentence++;
					
					
					NegationInstance<Label> instance = new NegationInstance<Label>(instanceId, 1, words, null);
					
					/*
					for(int i = 0; i < words.size(); i++)
						System.out.print(words.get(i)[0] + " ");
					System.out.println();
					 */
					instance.srList = srList;
					instance.focusVerb = focusVerb;
					instance.focusScope = focusScope;
					instance.hasFocusVerb = hasFocusVerb;
					instance.hasFocusScope = hasFocusScope;
					instance.tree = tree;
					instance.sr = sr;
					
					if (sr == null)
						SRNoMatched++;
					
					int spanCount = Utils.spanCount(focusScope);
					
					ArrayList<int[]> spans = Utils.getSpans(focusScope);
					if (spanCount > 1) {
						//System.out.println(spanCount + " | " + instance.getSentence());
						for(int i = 0; i < spans.size(); i++) {
							int idx = spans.get(i)[0];
							int roleIdx = sr != null ? sr.roletype[idx] : -1;
							String roleName = (roleIdx >= 0) ? sr.roleNameList.get(roleIdx) : "***";
							

							//System.out.print(roleName + " ");
						
						}
						
						//System.out.println();
						SR2++;
					} else {
						
						int idx = spans.get(0)[0];
						int roleIdx = sr != null ? sr.roletype[idx] : -1;
						String roleName = (roleIdx >= 0) ? sr.roleNameList.get(roleIdx) : "***";
						if (roleName.startsWith("***")) {
							//System.err.println(spanCount + " | " + instance.getSentence());
							FOCUSNOROLE++;
							//System.err.println(roleName + " ");
						}
						
						int[] focusScopeSpan = spans.get(0);
						int startIdx = 0;
						boolean strictly_found = false;
						int[] roleSpan = null;
						while (true) {

							int endIdx = Utils.getSeqEndPos(startIdx, sr.roletype, sr.roletypeBegin); // [startIdx, endIdx)
							int roleSpanIdx = sr.roletype[startIdx];
							
							if (roleSpanIdx >= 0 ) {
								roleSpan = new int[] {startIdx, endIdx};
								
								if (Utils.spanContains(roleSpan, focusScopeSpan)) {
									if (!Utils.spanEquals(roleSpan, focusScopeSpan)) {
										focusScopeSpanStrictlyContained++;
										strictly_found = true;
										break;
									}
								}
								
							}
							
							if (endIdx >= size) break;
							
							startIdx = endIdx;
						}
						
						if (strictly_found) {
							
							if (isLabeled)
								Utils.fillSpan(focusScope, roleSpan);
							/*
							for(int i = 0; i < size; i++)
								System.err.print(words.get(i)[0] + " ");
							System.err.println();*/
						}
					}
					
				
										
					if (isLabeled) {
						instance.setLabeled(); // Important!
					} else {
						instance.setUnlabeled();
					}
					instanceId++;
					instance.preprocess();
					result.add(instance);
					
					

				}

				words = null;
				labels = null;
				focus = null;
			} else {
				// int lastSpace = line.lastIndexOf(NegationGlobal.SEPERATOR);
				line = line.trim().replaceAll(" +", " ");
				String[] fields = line.split(NegationGlobal.SEPERATOR);
				String[] features = Arrays.copyOf(fields, FEATURE_TYPES.values().length);
				
				String[] focus_features = Arrays.copyOfRange(fields, FEATURE_TYPES.values().length, fields.length);
				words.add(features);
				focus.add(focus_features);
				
				counter.addWord(features[FEATURE_TYPES.word.ordinal()]);
				numTokens++;
			}
		}
		br.close();
		System.out.println("#inst: " + result.size());
		System.out.println("#NegFocusSentence: " + numNegFocusSentence);
		System.out.println("#Discarded Sentence: " + numDiscardInstance);
		System.out.println("#Unique words: " + counter.getVocabSize() + " in #tokens: " + numTokens);
		System.out.println("SRNoMatched: " + SRNoMatched);
		System.out.println("SR2: " + SR2);
		System.out.println("FOCUSNOROLE: " + FOCUSNOROLE);
		System.out.println("focusScopeSpanStrictlyContained: " + focusScopeSpanStrictlyContained);
		System.out.println();
		return result.toArray(new NegationInstance[result.size()]);
	}
	
	
	@SuppressWarnings("unchecked")
	public static String inst2Str(ArrayList<String[]> input, ArrayList<Label> output) {
		StringBuffer sb = new StringBuffer();
		/*
		ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();
		ArrayList<Label> output = (ArrayList<Label>) inst.getPrediction();
		*/
		for (int i = 0; i < input.size(); i++) {
			String[] fields = input.get(i);
			for (String item : fields) {
				sb.append(item + " ");
			}

			sb.append((output.get(i).getForm().startsWith("I") ? "FOCUS" : "*"));
			//Sematic role, focusverb, focusscope

			sb.append("\n");
		}
		sb.append("\n");
		return sb.toString();
	}

	@SuppressWarnings("unchecked")
	public static String inst2SeqStr(NegationInstance inst) {
		StringBuffer sb = new StringBuffer();
		ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();
		ArrayList<Label> gold = (ArrayList<Label>) inst.getOutput();
		ArrayList<Label> pred = (ArrayList<Label>) inst.getPrediction();

		for (int i = 0; i < inst.size(); i++) {
			String[] fields = input.get(i);
			sb.append(input.get(i)[FEATURE_TYPES.word.ordinal()] + "\t");
			sb.append((gold.get(i).getForm().startsWith("I") ? 1 : 0) + "\t");
			sb.append((pred.get(i).getForm().startsWith("I") ? 1 : 0));
			sb.append("\n");
		}

		sb.append("\n");

		return sb.toString();
	}
	
	
	public static void writeResult(Instance[] preds, String goldfile, String filename_output) {
		PrintWriter p = null;

		if (NegationGlobal.OUTPUT_SEM2012_FORMAT) {

			//output prediction
			try {
				p = new PrintWriter(new File(filename_output), "UTF-8");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
					
			for (int i = 0; i < preds.length; i++) {
				NegationInstance inst = (NegationInstance) preds[i];
				if (inst.hasFocusScope)
					p.write(inst2Str((ArrayList<String[]>) inst.getInput(),  (ArrayList<Label>)inst.getPrediction() ));
			}

			p.close();
			
			//output gold
			try {
				p = new PrintWriter(new File(filename_output.replace(".out", ".gold")), "UTF-8");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
					
			for (int i = 0; i < preds.length; i++) {
				NegationInstance inst = (NegationInstance) preds[i];
				if (inst.hasFocusScope)
					p.write(inst2Str((ArrayList<String[]>) inst.getInput(),  (ArrayList<Label>)inst.getOutput()));
			}

			p.close();
		}

		try {
			p = new PrintWriter(new File(filename_output + ".seq"), "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		for (int i = 0; i < preds.length; i++) {
			NegationInstance inst = (NegationInstance) preds[i];
			if (inst.hasFocusScope)
				p.write(inst2SeqStr(inst));
		}

		p.close();

		if (NegationGlobal.DEBUG) {
			System.out.println("\n");
		}
		System.out.println(NegationGlobal.modelname + " Evaluation Completed");

		// NegEval.evalbyScript(goldfile, filename_output);

		if (NegationGlobal.OUTPUT_SENTIMENT_SPAN) {
			try {
				p = new PrintWriter(new File(filename_output + ".span.html"), "UTF-8");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String css = "/Users/Leo/workspace/ui/overlap.css";
			if (NetworkConfig.OS.equals("linux")) {
				css = "/home/lihao/workspace/ui/overlap.css";
			}

			String header = "<html><head><link rel='stylesheet' type='text/css' href='" + css + "' /></head> <body><br><br>\n";
			String footer = "\n</body></html>";
			p.write(header);

			int pInst = 0, counter = 0;
			int[][] splits = new int[preds.length][];
			ArrayList<Integer> split = new ArrayList<Integer>();

			for (int i = 0; i < preds.length; i++) {
				NegationInstance inst = (NegationInstance) preds[i];
				ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();

				ArrayList<Label> gold = (ArrayList<Label>) inst.getOutput();
				ArrayList<Label> pred = (ArrayList<Label>) inst.getPrediction();


				String t = "";

				t += gold + "<br>\n";

				t += pred + "<br>\n";

				t += outputSent(gold, inst, "positive");

				t += outputSent(pred, inst, "negative");

				t += "<br>\n";

				p.println(t);

			}

			p.write(footer);

			p.close();

		}
		
		
		if (NegationGlobal.OUTPUT_ERROR) {
			try {
				p = new PrintWriter(new File(filename_output + ".span.error.html"), "UTF-8");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String css = "/Users/Leo/workspace/ui/overlap.css";
			if (NetworkConfig.OS.equals("linux")) {
				css = "/home/lihao/workspace/ui/overlap.css";
			}

			String header = "<html><head><link rel='stylesheet' type='text/css' href='" + css + "' /><title>Error Analysis</title></head> <body><br><br>\n";
			String footer = "\n</body></html>";
			p.write(header);

			int pInst = 0, counter = 0;
			int[][] splits = new int[preds.length][];
			ArrayList<Integer> split = new ArrayList<Integer>();
			
			int numError = 0;
			int numTotal = 0;
			int[] errorType = new int[2];
			Arrays.fill(errorType, 0);
			
			int totalNumGoldTokenInWrongInstance = 0;
			int totalNumMisMatchTokenInWrongInstance = 0;
			
			int totalNumGoldTokenInRightInstance = 0;
			int totalNumMisMatchTokenInRightInstance = 0;
			
			Utils.IntStat correctStat = new Utils.IntStat();
			Utils.IntStat inCorrectStat = new Utils.IntStat();
			Utils.IntStat totalStat = new Utils.IntStat();

			for (int i = 0; i < preds.length; i++) {
				NegationInstance inst = (NegationInstance) preds[i];
				
				if (!inst.hasFocusScope) continue;
				
				ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();

				ArrayList<Label> gold = (ArrayList<Label>) inst.getOutput();
				ArrayList<Label> pred = (ArrayList<Label>) inst.getPrediction();
				
				NegMetric m = new NegMetric();
				
				NegEval.evalNegationSpanonSingleSentence(gold, pred, m, input);
				
				numTotal++;
				totalStat.addInt(pred.size());
				
				if (m.numSpanExact[2] == 1 || (m.numSpanExact[0] == 0 && m.numSpanExact[1] == 0)) {
					totalNumGoldTokenInRightInstance += m.numSpanToken[0];
					totalNumMisMatchTokenInRightInstance += m.numSpanToken[0] - m.numSpanToken[2];
					
					correctStat.addInt(pred.size());
					
					continue;
				}
				
				numError++;
				inCorrectStat.addInt(pred.size());
				
				totalNumGoldTokenInWrongInstance += m.numSpanToken[0];
				totalNumMisMatchTokenInWrongInstance += m.numSpanToken[0] - m.numSpanToken[2];
				
				if (m.numSpanToken[0] >= m.numSpanToken[1]) {
					errorType[0]++;
				} else {
					errorType[1]++;
				}


				String t = "";

				t += gold + "<br>\n";

				t += pred + "<br>\n";

				t += outputSent(gold, inst, "positive");

				t += outputSent(pred, inst, "negative");

				t += "<br>\n";

				p.println(t);

			}
			
			p.write("<br><p>numTotal:" + numTotal + "</p>\n");
			
			p.write("<br><p> #Errors:" + numError + " . Type 1:" + errorType[0]  + " , Type 2:" + errorType[1] + " , </p>\n");
			
			p.write("<br><p> In Wrong, totalNumGoldToken:" + totalNumGoldTokenInWrongInstance + " , totalNumMisMatchToken:" + totalNumMisMatchTokenInWrongInstance + "  </p>\n");

			p.write("<br><p> In Right, totalNumGoldToken:" + totalNumGoldTokenInRightInstance + " , totalNumMisMatchToken:" + totalNumMisMatchTokenInRightInstance + "  </p>\n");

			p.write("<br><p>Stat  Total:<br>" + totalStat.toString() + " \ncorrect:<br>" + correctStat.toString()  + "\n incorect:<br>" + inCorrectStat+ "</p>\n");
			
			p.write(footer);

			p.close();

		}

	}
	
	static String outputSent(ArrayList<Label> output, NegationInstance inst, String color) {

		String t = "";
		char lastTag = 'O';
		ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();

		for (int k = 0; k < input.size(); k++) {

			String labelStr = output.get(k).getForm();
			char tag = labelStr.charAt(0);
			char nextTag = (k + 1 < input.size()) ? output.get(k + 1).getForm().charAt(0) : 'O';

			if (tag != 'O' && lastTag == 'O') {
				t += "<div class='tooltip entity_" + color + "'>";
			}

			if (inst.focusVerb[k] == 1) {
				t += "<div class='tooltip entity_neutral_incorrect'>";
			}
			t += input.get(k)[NegationInstance.FEATURE_TYPES.word.ordinal()] + "&nbsp;";

			if (inst.focusVerb[k] == 1) {
				t += "</div>&nbsp;";
			}

			if (tag != 'O' && nextTag == 'O') {
				t += "</div>&nbsp;";
			}

			lastTag = tag;
		}

		t += "<br>\n";

		return t;

	}



}
