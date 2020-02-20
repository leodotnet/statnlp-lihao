package org.statnlp.abbr.common;

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
import java.util.Map;

import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.commons.types.LinearInstance;
import org.statnlp.commons.types.Sentence;
import org.statnlp.example.base.BaseInstance;
import org.statnlp.hypergraph.NetworkConfig;


public class AbbrInstance<T> extends LinearInstance<T> {

	private static final long serialVersionUID = 1851514046050983662L;

	String sentence = null;
	public String rawData = null;
	public List<T> outputBak = null;
	
	String[] tokens = null;
	String[] words = null;
	String[] wordPOSTags = null;

	public enum FEATURE_TYPES {
		token, segmentation, segmentPOSTag
	};

	public AbbrInstance(int instanceId, double weight) {
		this(instanceId, weight, null);
	}

	public AbbrInstance(int instanceId, double weight, ArrayList<String[]> input) {
		this(instanceId, weight, input, null, null);
	}

	public AbbrInstance(int instanceId, double weight, ArrayList<String[]> input, ArrayList<T> output) {
		this(instanceId, weight, input, output, null);
	}

	public AbbrInstance(int instanceId, double weight, ArrayList<String[]> input, ArrayList<T> output, ArrayList<T> prediction) {
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
	public AbbrInstance duplicate() {
		AbbrInstance inst = (AbbrInstance) super.duplicate();
		inst.rawData = this.rawData;
		inst.tokens = this.tokens;
		inst.words = this.words;
		inst.wordPOSTags = this.wordPOSTags;
		return inst;
	}

	public void preprocess() {
		preprocess(false);
	}

	public void preprocess(boolean onlyWord) {

	}

	public String getSentence() {
		if (this.sentence == null) {
			this.sentence = "";
			for (int i = 0; i < input.size(); i++) {
				String word = input.get(i)[FEATURE_TYPES.token.ordinal()];
				if (AbbrGlobal.EMBEDDING_WORD_LOWERCASE)
					word = word.toLowerCase();

				this.sentence += word + " ";
			}
		}

		this.sentence = this.sentence.trim();

		return this.sentence;
	}
	
	public String toStringPred() {
		String[] fields = this.rawData.split("\\|\\|\\|");
		List<String> predList = Utils.toListOfString((List<Label>)this.prediction);
		String pred = Utils.join(" ", predList);
		fields[fields.length - 1] = pred;
		String line = Utils.join("\\|\\|\\|", fields);
		return line;
	}
	
	public String toStringGold() {
		return this.rawData;
	}

	
	public static AbbrInstance<Label>[] readData(String dataSet, String fileName, boolean withLabels, boolean isLabeled, int TRIAL, boolean discardNoNgeation) throws IOException {

		AbbrInstance[] insts = null;
		if (dataSet.startsWith("abbr")) {
			insts = readAbbrData(fileName, withLabels, isLabeled, discardNoNgeation);
			
		}

		if (TRIAL > 0)
			insts = Utils.portionInstances(insts, TRIAL);

		return insts;
	}
	
	
	public static final Map<String, Label> LABELS = new HashMap<String, Label>();
	public static final Map<Integer, Label> LABELS_INDEX = new HashMap<Integer, Label>();
	
	public static Label getLabel(String form){
		if(!LABELS.containsKey(form)){
			Label label = new Label(form, LABELS.size());
			LABELS.put(form, label);
			LABELS_INDEX.put(label.getId(), label);
		}
		return LABELS.get(form);
	}
	
	public static Label getLabel(int id){
		return LABELS_INDEX.get(id);
	}
	
	public static void reset(){
		LABELS.clear();
		LABELS_INDEX.clear();
	}
	
	


	@SuppressWarnings("unchecked")
	private static AbbrInstance<Label>[] readAbbrData(String fileName, boolean withLabels, boolean isLabeled, boolean discardNoNgeation) throws IOException {
		System.out.println("Read " + fileName);
		InputStreamReader isr = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
		BufferedReader br = new BufferedReader(isr);
		ArrayList<AbbrInstance<Label>> result = new ArrayList<AbbrInstance<Label>>();
		ArrayList<String[]> words = null;
		ArrayList<String[]> negs = null;
		ArrayList<Label> labels = null;
		int numNegationinSentence = 0;
		int numDiscardInstance = 0;
		int numNegation = 0;
		int instanceId = 1;
		int numSentence = 0;
		HashMap<Integer, Integer> entityLengthStat = new HashMap<Integer, Integer>();
		while (br.ready()) {
			if (words == null) {
				words = new ArrayList<String[]>();
			}
			if (negs == null) {
				negs = new ArrayList<String[]>();
			}
			if (withLabels && labels == null) {
				labels = new ArrayList<Label>();
			}
			String line = br.readLine().trim();
			if (line.startsWith("##")) {
				continue;
			}
			if (line.length() == 0) {

			} else {

				numSentence++;
				

				String[] fields = line.split("\\|\\|\\|");
				String[] tokens = fields[0].trim().split(" ");
				String[] segmentation = fields[2].trim().split(" ");
				String[] segmentationPosTags = fields[3].trim().split(" ");
				String[] tags = fields[fields.length - 1].trim().split(" ");
				
				

				assert (tokens.length == tags.length);

				int size = tokens.length;
				
				String[] segmentTags = new String[size];
				String[] segmentPOSTags = new String[size];
				Arrays.fill(segmentTags, null);
				Arrays.fill(segmentPOSTags, null);
				
				int pos = 0;
				for(int i = 0; i < segmentation.length; i++) {
					String word = segmentation[i];
					
					segmentTags[pos + 0] = "B";
					segmentPOSTags[pos + 0] = segmentationPosTags[i];
					for(int j = 1; j < word.length(); j++) {
						segmentTags[pos + j] = "I";
					}
					
					pos += word.length();
				}
				

				for (int i = 0; i < size; i++) {
					String[] features = new String[FEATURE_TYPES.values().length];
					Arrays.fill(features, null);

					features[FEATURE_TYPES.token.ordinal()] = tokens[i];
					features[FEATURE_TYPES.segmentation.ordinal()] = segmentTags[i];
					features[FEATURE_TYPES.segmentPOSTag.ordinal()] = segmentPOSTags[i];
					
					words.add(features);
					
					if(withLabels){
						Label label = getLabel(tags[i]);
						labels.add(label);
					}
				}
				
				AbbrInstance<Label> instance = new AbbrInstance<Label>(instanceId, 1, words, labels);
				instance.tokens = tokens;
				instance.words = segmentation;
				instance.wordPOSTags = segmentationPosTags;
				

				if (isLabeled) {
					instance.setLabeled(); // Important!
				} else {
					instance.setUnlabeled();
				}
				instanceId++;
				instance.preprocess();
				result.add(instance);

				numNegation++;
				

				words = null;
				labels = null;
				negs = null;

			}
		}
		br.close();

		System.out.println("There are " + numNegation + " negation left in current total #inst: " + result.size() + " in " + numSentence + " sentences.");
		System.out.println(numDiscardInstance + " instances are discarded.");
		// System.out.println("Entity Length: " + entityLengthStat.toString());
		System.out.println();
		return result.toArray(new AbbrInstance[result.size()]);
	}
	

	public static void writeResult(Instance[] preds, String goldfile, String filename_output) {
		// String filename_output = (String) getParameters("filename_output");
		// String filename_standard = (String)
		// getParameters("filename_standard");

		PrintWriter p = null;

		

			try {
				p = new PrintWriter(new File(filename_output), "UTF-8");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (AbbrGlobal.DEBUG)
				System.out.println("Result: ");

			HashMap<String, AbbrInstance> accuInsts = new HashMap<String, AbbrInstance>();
			// HashMap<String, ArrayList<Negation>> outputs = new
			// HashMap<String, ArrayList<Negation>>();
			ArrayList<String> sentList = new ArrayList<String>();

			for (int i = 0; i < preds.length; i++) {
				AbbrInstance inst = (AbbrInstance) preds[i];
				p.write(inst.toStringPred());
				p.write("\n");

			}

			p.close();
		

		System.out.println(AbbrGlobal.modelname + " Evaluation Completed");

		// NegEval.evalbyScript(goldfile, filename_output);

		/*
		if (AbbrGlobal.OUPUT_HTML) {
			try {
				p = new PrintWriter(new File(filename_output + ".html"), "UTF-8");
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

			

			for (int i = 0; i < preds.length; i++) {
				AbbrInstance inst = (AbbrInstance) preds[i];
				ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();

				ArrayList<Label> gold = (ArrayList<Label>) inst.getOutput();
				ArrayList<Label> pred = (ArrayList<Label>) inst.getPrediction();

				ArrayList<int[]> scopes = inst.scopes;

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

		}*/
		
		
		


	}

	public void setPredictionAsOutput() {
		this.outputBak = this.output;
		this.output = this.prediction;
	}
	
	public void revertOutput() {
		if (this.outputBak != null)
			this.output = this.outputBak;
	}

	
}
