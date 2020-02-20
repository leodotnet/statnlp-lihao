package org.citationspan.common;

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
import java.util.Map;
import java.util.Scanner;

import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.negation.common.NegationGlobal;
import org.statnlp.negation.common.NegationInstance;
import org.citationspan.common.CitationSpanInstance.FEATURE_TYPES;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class CitationSpanReader {
	
	
	public static final Map<String, Label> LABELS = new HashMap<String, Label>();
	public static final Map<Integer, Label> LABELS_INDEX = new HashMap<Integer, Label>();

	public static Label getLabel(String form) {
		if (!LABELS.containsKey(form)) {
			Label label = new Label(form, LABELS.size());
			LABELS.put(form, label);
			LABELS_INDEX.put(label.getId(), label);
		}
		return LABELS.get(form);
	}

	public static Label getLabel(int id) {
		return LABELS_INDEX.get(id);
	}

	public static void reset() {
		LABELS.clear();
		LABELS_INDEX.clear();
	}

	public static int getSubGlobalIdx(int sentId, int subSentId, int[] sentenceCounter) {
		int idx = 0;

		for (int i = 0; i < sentId; i++) {
			idx += sentenceCounter[i];
		}

		idx += subSentId;

		return idx;
	}

	public static CitationSpanInstance<Label>[] readData(String dataSet, String fileName, boolean withLabels, boolean isLabeled, int TRIAL, boolean discardNoNgeation) {

		CitationSpanInstance[] insts = null;

		if (dataSet.endsWith("json")) {
			try {
				insts = readCSJSONInstances(fileName, withLabels, isLabeled);
			} catch (FileNotFoundException | ParseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		if (TRIAL > 0)
			insts = portionInstances(insts, TRIAL);

		return insts;
	}

	public static CitationSpanInstance[] readCSJSONInstances(String fileName, boolean withLabels, boolean isLabeled) throws FileNotFoundException, ParseException {

		CitationSpanReader.getLabel("O");
		CitationSpanReader.getLabel("I");
		
		ArrayList<CitationSpanInstance> result = new ArrayList<CitationSpanInstance>();
		Scanner scanner = new Scanner(new File(fileName), "UTF-8");

		int instanceId = 0;

		while (scanner.hasNextLine()) {

			String line = scanner.nextLine();

			JSONParser parser = new JSONParser();
			JSONObject cs = (JSONObject) parser.parse(line);
			// JSONArray a = (JSONArray) parser.parse(line);

			// for (Object o : a) {
			// JSONObject cs = (JSONObject) o;

			String[] features = new String[9];

			String id = (Long) cs.get("id") + "";
			String entity = (String) cs.get("entity");
			String cite_type = (String) cs.get("cite_type");
			String cite_id = (Long) cs.get("cite_id") + "";
			String citing_paragraph = (String) cs.get("citing_paragraph");
			String cite_span_ground_truth = (String) cs.get("cite_span_ground_truth");
			String sentence_sub_ids = (String) cs.get("sentence-sub-ids");
			String cite_url = (String) cs.get("cite_url");
			String url_content = (String) cs.get("url_content");

			int idx = 0;
			for (Object key : cs.keySet()) {
				String value = cs.get(key) + "";
				features[idx++] = value;
			}

			ArrayList<String[]> inputs = new ArrayList<String[]>();

			String citing_paragraph_processed = citing_paragraph;

			citing_paragraph_processed = citing_paragraph_processed.replace("{{" + cite_id + "}}", "##" + cite_id + "##");

			citing_paragraph_processed = citing_paragraph_processed.replaceAll("\\{\\{[0-9]+\\}\\}", "");

			citing_paragraph_processed = citing_paragraph_processed.replaceAll("U.S.", "US");

			citing_paragraph_processed = citing_paragraph_processed.replace("##" + cite_id + "##" + "##" + cite_id + "##", "##" + cite_id + "##");

			for (char ch : new char[] { '.', '?', '!' }) {
				citing_paragraph_processed = citing_paragraph_processed.replace(ch + "##" + cite_id + "##", "##" + cite_id + "##" + ch);
			}

			// citing_paragraph_processed =
			// citing_paragraph_processed.replace("##" + cite_id + "##", "");
			// //get pos

			String[] sentences = citing_paragraph_processed.split("\\! |\\? |\\. |\\.\"|\\.]|\\.\'\'\'|\\.\\)");

			int[] sentenceCounter = new int[sentences.length];

			ArrayList<int[]> cite_id_pos = new ArrayList<int[]>(); // globalSubSentId,
																	// strPosId

			for (int i = 0; i < sentences.length; i++) {
				String sentence = sentences[i];
				String[] subsentences = sentence.split(",|;|:| \\(|\\) ");
				sentenceCounter[i] = subsentences.length;
				for (int j = 0; j < subsentences.length; j++) {

					String subsentence = subsentences[j].trim();

					int pos = subsentence.indexOf("##" + cite_id + "##");
					if (pos >= 0) {
						subsentence = subsentence.replace("##" + cite_id + "##", "");
						int globalSubSentId = getSubGlobalIdx(i, j, sentenceCounter);
						cite_id_pos.add(new int[] { globalSubSentId, pos });
					}

					String[] tokens = subsentence.trim().split(" ");
					inputs.add(tokens);
				}

			}

			int[] outputsArr = new int[inputs.size()];
			Arrays.fill(outputsArr, 0);
			ArrayList<Label> outputs = new ArrayList<Label>();
			for (int i = 0; i < inputs.size(); i++) {
				outputs.add(CitationSpanReader.getLabel("O"));
			}

			String[] subSentenceListSpan = sentence_sub_ids.split(";");
			for (String subSentenceSpan : subSentenceListSpan) {
				if (subSentenceSpan.trim().length() == 0)
					continue;

				String[] fields = subSentenceSpan.split("-");
				int globalSubSentId = -1;

				try {
					int sentId = Integer.parseInt(fields[0]);
					int subSentId = Integer.parseInt(fields[1]);

					globalSubSentId = getSubGlobalIdx(sentId, subSentId, sentenceCounter);

					if (id.equals("972") || id.equals("510") || id.equals("789")) {
						if (globalSubSentId >= outputsArr.length) {
							continue;
						}
					}

					outputs.set(globalSubSentId, CitationSpanReader.getLabel("I"));
					outputsArr[globalSubSentId] = 1;
				} catch (Exception e) {
					System.out.println("fields:" + Arrays.toString(fields));
					System.out.println("instanceId:" + instanceId);
					System.exit(-1);
				}
			}

			instanceId++;
			CitationSpanInstance inst = new CitationSpanInstance(instanceId, 1.0, inputs, outputs);

			inst.data = cs;
			inst.cite_id_pos = cite_id_pos;
			inst.sentenceCounter = sentenceCounter;
			inst.outputsArr = outputsArr;
			inst.sentences = sentences;

			if (isLabeled) {
				inst.setLabeled(); // Important!
			} else {
				inst.setUnlabeled();
			}

			inst.preprocess();

			result.add(inst);

			if (isLabeled) {

				ArrayList<int[]> spans = Utils.getAllSpans(outputsArr, 1);
				for (int[] span : spans) {
					int l = span.length;
					// for(int j = span[0]; j < span[1]; j++) {
					// l += inputs.get(j).length;
					// }

					if (CitationSpanGlobal.L_MAX < l) {
						CitationSpanGlobal.L_MAX = l + 1;
					}
				}

				if (CitationSpanGlobal.MAX_NUM_SPAN < spans.size()) {
					CitationSpanGlobal.MAX_NUM_SPAN = spans.size();
				}

				spans = Utils.getAllSpans(outputsArr, 0);
				for (int[] span : spans) {
					int m = span.length;
					// for(int j = span[0]; j < span[1]; j++) {
					// m += inputs.get(j).length;
					// }

					if (CitationSpanGlobal.M_MAX < m) {
						CitationSpanGlobal.M_MAX = m + 1;
					}
				}
			}

		}

		scanner.close();

		System.out.println("L_MAX:" + CitationSpanGlobal.L_MAX + "\t\tM_MAX:" + CitationSpanGlobal.M_MAX);
		System.out.println("MAX_NUM_SPAN:" + CitationSpanGlobal.MAX_NUM_SPAN);

		return result.toArray(new CitationSpanInstance[result.size()]);
	}

	public static CitationSpanInstance[] portionInstances(CitationSpanInstance[] instances, double percentage) {
		return portionInstances(instances, (int) (percentage * instances.length));
	}

	public static CitationSpanInstance[] portionInstances(CitationSpanInstance[] instances, int num) {
		// CitationSpanInstance[] insts = new CitationSpanInstance[num];
		if (num > instances.length)
			num = instances.length;
		System.out.println("Truncate " + num + " instances.");
		return Arrays.copyOf(instances, num);
	}

	public static CitationSpanInstance[][] splitData(CitationSpanInstance[] insts, String splitMethod, double ratio) {

		CitationSpanInstance[][] splitInsts = new CitationSpanInstance[2][];

		if (splitMethod.equals("static")) {
			int trainSize = (int) (ratio * insts.length);
			splitInsts[0] = new CitationSpanInstance[trainSize];
			splitInsts[1] = new CitationSpanInstance[insts.length - trainSize];

			for (int i = 0; i < trainSize; i++) {
				splitInsts[0][i] = insts[i];
			}

			for (int i = trainSize; i < insts.length; i++) {
				splitInsts[1][i - trainSize] = insts[i];
			}
		} else if (splitMethod.equals("folds")) {

		}

		return splitInsts;
	}
	
	
	public static void writeResult(Instance[] predictions, String pathJoin, String result_file) {
		PrintWriter p = null;
		String filename_output = result_file;
		
		/*
		if (CitationSpanGlobal.OUTPUT_SEM2012_FORMAT) {

			
			p = new PrintWriter(new File(filename_output), "UTF-8");
			
			if (CitationSpanGlobal.DEBUG)
				System.out.println("Result: ");

			for (int i = 0; i < predictions.length; i++) {
				CitationSpanInstance inst = (CitationSpanInstance) predictions[i];

				
				p.write(inst2Str(inst));
				p.write("\n");

			}
			p.close();
		}
		*/
		
		try {
			p = new PrintWriter(new File(filename_output + ".seq"), "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (int i = 0; i < predictions.length; i++) {
			CitationSpanInstance inst = (CitationSpanInstance) predictions[i];
			p.write(inst2SeqStr(inst));
		}

		p.close();
		
		
		
		if (CitationSpanGlobal.OUTPUT_HTML_SPAN) {
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


			for (int i = 0; i < predictions.length; i++) {
				CitationSpanInstance inst = (CitationSpanInstance) predictions[i];
				ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();

				ArrayList<Label> gold = (ArrayList<Label>) inst.getOutput();
				ArrayList<Label> pred = (ArrayList<Label>) inst.getPrediction();
				
//				String goldText = "";
//				for(int j = 0; j < gold.size(); j++) {
//					
//				}

				//ArrayList<int[]> scopes = inst.scopes;

				String t = "";

//				t += gold + "<br>\n";
//
//				t += pred + "<br>\n";

				
				t += outputSent(gold, inst, "positive");

				t += outputSent(pred, inst, "negative");

				t += "<br>\n";
				
				t += "<hr>\n";

				p.println(t);

			}

			p.write(footer);

			p.close();

		}
		
		
		

		if (CitationSpanGlobal.DEBUG) {
			System.out.println("\n");
		}
		System.out.println(CitationSpanGlobal.modelname + " Evaluation Completed");

		// NegEval.evalbyScript(goldfile, filename_output);

	}
	

	
	@SuppressWarnings("unchecked")
	public static String inst2SeqStr(CitationSpanInstance inst) {
		StringBuffer sb = new StringBuffer();
		ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();
		ArrayList<Label> gold = (ArrayList<Label>) inst.getOutput();
		ArrayList<Label> pred = (ArrayList<Label>) inst.getPrediction();

		for (int i = 0; i < inst.size(); i++) {
			String[] fields = input.get(i);
			//sb.append(input.get(i)[FEATURE_TYPES.word.ordinal()] + "\t");
			sb.append((gold.get(i).getForm().startsWith("I") ? 1 : 0) + "\t");
			sb.append((pred.get(i).getForm().startsWith("I") ? 1 : 0));
			sb.append("\n");
		}

		sb.append("\n");

		return sb.toString();
	}
	
	public static int[] convertArr(ArrayList<Label> output) {
		int[] ret = new int[output.size()];
		for(int i = 0; i < ret.length; i++) {
			ret[i] = output.get(i).getForm().startsWith("I") ? 1 : 0;
		}
		return ret;
	}
	
	
	
	static String outputSent(ArrayList<Label> output, CitationSpanInstance inst, String color) {

		String t = "";
		char lastTag = 'O';
		ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();
		int[] ret = convertArr(output);

		for (int k = 0; k < input.size(); k++) {
			
			String labelStr = output.get(k).getForm();
			char tag = labelStr.charAt(0);
			char nextTag = (k + 1 < input.size()) ? output.get(k + 1).getForm().charAt(0) : 'O';

			if (tag != 'O' && lastTag == 'O') {
				t += "<div class='tooltip entity_" + color + "'>";
			}

			/*
			if (inst.negation.cue[k] == 1) {
				t += "<div class='tooltip entity_neutral_incorrect'>";
			}*/
			
			for(int i = 0; i < input.get(k).length; i++) {
				t += input.get(k)[i] + " ";
			}
			t += "&nbsp;";

			/*
			if (inst.negation.cue[k] == 1) {
				t += "</div>&nbsp;";
			}*/

			if (tag != 'O' && nextTag == 'O') {
				t += "</div>&nbsp;";
			}

			lastTag = tag;
		}

		t += "<br>\n";

		return t;

	}
	

}
