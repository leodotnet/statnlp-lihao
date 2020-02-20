package org.statnlp.example.tagging;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.statnlp.commons.io.RAWF;
import org.statnlp.commons.ml.opt.OptimizerFactory;
import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.commons.types.Sentence;
import org.statnlp.commons.types.WordToken;
import org.statnlp.hypergraph.DiscriminativeNetworkModel;
import org.statnlp.hypergraph.GenerativeNetworkModel;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.NetworkConfig.ModelType;
import org.statnlp.hypergraph.NetworkModel;
import org.statnlp.hypergraph.decoding.Metric;
import org.statnlp.targetedsentiment.common.TSInstance;
import org.statnlp.targetedsentiment.common.TargetSentimentGlobal;

public class TagMain {

	public static String trainFile = "data/conll2000/sample_train.txt";
	public static String testFile = "data/conll2000/sample_test.txt";
	public static String test2File = "data/conll2000/sample_test.txt";
	public static String devFile = "";
	public static int trainNum = 30;
	public static int testNum = 20;
	public static int numThreads = 20;
	public static double l2 = 0.00;
	public static int numIterations = 500;
	public static List<String> labels;
	public static String SEPERATOR = "\t";
	public static String dataset = "2019Fall";//"targeted_sentiment";//"twitter_chunking";//"chinese_address_parsing";
	public static String modelname = "CRF";
	public static String resultPath = "experiments/sentiment/models/";
	public static String subpath = "default";
	public static String lang = "EN";


	public static void main(String[] args) throws IOException, InterruptedException {

		NetworkConfig.TRAIN_MODE_IS_GENERATIVE = true;
		TaggingGlobal.filterLowFreqWords = false;
		TaggingGlobal.dict.useLowerCase = false;

		if (TaggingGlobal.filterLowFreqWords) {
			subpath += "_filterlowfreq";
		}

		if (NetworkConfig.TRAIN_MODE_IS_GENERATIVE) {
			modelname = "HMM";
			numThreads = 1;
			l2 = 0;
		} else {
			l2 = 0;
		}

		NetworkConfig.L2_REGULARIZATION_CONSTANT = l2;
		NetworkConfig.NUM_THREADS = numThreads;

		StringBuffer stats = new StringBuffer("ML Course Project: Data feasibility check\n");
		


		for (String lang : new String[] {"cn_weibo"}) {//{"EN", "ES", "FR", "RU"}) { //{"EN", , "ES", "FR", "RU" }) {

			String result = "";
			String out_path = "";
			String path = "";

			//if (dataset.equals("semeval2016")) {

				SEPERATOR = " ";

				path = "data/ml/" + dataset + "/" + lang + "/";
				String prefix = "";
				trainFile = prefix + "train";
				testFile = prefix + "test.out";
				test2File = prefix + "test2.out";
				devFile = prefix + "dev.out";

				out_path = resultPath + modelname + "/ml_" + lang + "/" + subpath + "/";
				result = out_path + "data.test.out";

				File directory = new File(out_path);
				if (!directory.exists()) {
					directory.mkdirs();
				}

			//}

			labels = new ArrayList<>();
			TaggingGlobal.resetDict();
			TagInstance[] trainInstances = readCoNLLData(path + trainFile, true, true);
			TagInstance[] testInstances = readCoNLLData(path + testFile, true, false);
			TagInstance[] devInstances = readCoNLLData(path + devFile, true, false);
			TagInstance[] test2Instances = readCoNLLData(path + test2File, true, false);
			
			System.out.println("Labels:" + labels.toString());

			NetworkConfig.MODEL_TYPE = ModelType.CRF;// ModelType.STRUCTURED_PERCEPTRON;

			GlobalNetworkParam gnp = new GlobalNetworkParam();// (OptimizerFactory.getGradientDescentFactoryUsingAdaGrad(0.1));
			TagFeatureManager fa = new TagFeatureManager(gnp);
			TagNetworkCompiler compiler = new TagNetworkCompiler(labels);

			NetworkModel model = NetworkConfig.TRAIN_MODE_IS_GENERATIVE ? GenerativeNetworkModel.create(fa, compiler) : DiscriminativeNetworkModel.create(fa, compiler);
			TaggingGlobal.isTraining = true;
			model.train(trainInstances, numIterations);
			// model.visualize(TaggingViewer.class, trainInstances);

			TaggingGlobal.lockDict();
			System.out.println(TaggingGlobal.dict);

			TaggingGlobal.isTraining = false;
			Instance[] predictions = null;
			
			System.out.println("Decoding Dev set on " + devInstances.length + " instances " + devFile);
			predictions = model.test(devInstances);//, 5);
//			for (Instance inst : predictions) {
//				List<List<String>> topk =  (List<List<String>>) inst.getTopKPredictions();
//				inst.setPrediction(topk.get(0));
//			}
			//stats.append(printStat(predictions, lang + "_Dev"));
			writeResult(predictions, out_path + devFile);
			
			System.out.println("Decoding  set on " + testInstances.length + " instances " + testFile);
			predictions = model.test(testInstances);//, 5);
//			for (Instance inst : predictions) {
//				List<List<String>> topk =  (List<List<String>>) inst.getTopKPredictions();
//				inst.setPrediction(topk.get(0));
//			}
			//stats.append(printStat(predictions, lang + "_Test"));
			writeResult(predictions, out_path + testFile);
			
			
			System.out.println("Decoding  set on " + test2Instances.length + " instances " + test2File);
			predictions = model.test(test2Instances);//, 5);
//			for (Instance inst : predictions) {
//				List<List<String>> topk =  (List<List<String>>) inst.getTopKPredictions();
//				inst.setPrediction(topk.get(0));
//			}
			//stats.append(printStat(predictions, lang + "_Test2"));
			writeResult(predictions, out_path + test2File);

//			System.out.println("Decoding Test set on " + testInstances.length + " instances.");
//			predictions = model.decode(testInstances);
//			stats.append(printStat(predictions, lang + "_Test"));
//			writeResult(predictions, out_path + testFile);

			stats.append("\n");
		}

		// System.out.printf("[Accuracy]: %.2f%%\n", corr * 1.0 / total * 100);
		System.out.println(stats.toString());
	}

	public static String printStat(Instance[] predictions, String expname) {
		StringBuffer sb = new StringBuffer(expname + "\n");

		Metric metric = TSEval.eval(predictions);
		sb.append(metric.toString() + "\n");

		return sb.toString();
	}

	public static void writeResult(Instance[] predictions, String filename) throws FileNotFoundException, UnsupportedEncodingException {
		// evaluation
		System.out.println("Writing to " + filename);
		PrintWriter p = new PrintWriter(filename, "utf-8");
		int corr = 0;
		int total = 0;
		int idx = 0;
		for (Instance pred : predictions) {
			idx++;
			TagInstance inst = (TagInstance) pred;
			Sentence input = inst.getInput();
			List<String> gold = inst.getOutput();
			List<String> prediction = inst.getPrediction();

			for (int i = 0; i < prediction.size(); i++) {
				String output = input.get(i).getForm() + "\t" +  gold.get(i) + "\t" + prediction.get(i);
				p.println(output);
				// System.out.println(output);
			}
			p.println();
			// System.out.println(idx);

			/*
			 * for (int i = 0; i < gold.size(); i++) { if
			 * (gold.get(i).equals(prediction.get(i))) corr++; } total +=
			 * gold.size();
			 */

		}

		p.flush();
		p.close();
	}

	/**
	 * Read the data.
	 * 
	 * @param path
	 * @param isTraining
	 * @param number
	 * @return
	 * @throws IOException
	 */
	public static TagInstance[] readData(String path, boolean isTraining, int number) throws IOException {
		BufferedReader br = RAWF.reader(path);
		String line = null;
		List<TagInstance> insts = new ArrayList<TagInstance>();
		int index = 1;
		ArrayList<WordToken> words = new ArrayList<WordToken>();
		ArrayList<String> tags = new ArrayList<String>();
		while ((line = br.readLine()) != null) {
			if (line.equals("")) {
				WordToken[] wordsArr = new WordToken[words.size()];
				words.toArray(wordsArr);
				Sentence sent = new Sentence(wordsArr);
				TagInstance inst = new TagInstance(index++, 1.0, sent, tags);
				if (isTraining) {
					inst.setLabeled();
				} else {
					inst.setUnlabeled();
				}
				insts.add(inst);
				words = new ArrayList<WordToken>();
				tags = new ArrayList<String>();
				if (number != -1 && insts.size() == number)
					break;
				continue;
			}
			String[] values = line.split(" ");
			String tag = values[1];
			
			
			if (isTraining && !labels.contains(tag)) {
				labels.add(tag);
			}
			words.add(new WordToken(values[0]));
			tags.add(tag);
		}
		br.close();
		List<TagInstance> myInsts = insts;
		System.out.println("#instance:" + myInsts.size() + " Instance. ");
		return myInsts.toArray(new TagInstance[myInsts.size()]);
	}

	@SuppressWarnings("unchecked")
	private static TagInstance[] readCoNLLData(String fileName, boolean withLabels, boolean isLabeled) throws IOException {
		InputStreamReader isr = new InputStreamReader(new FileInputStream(fileName), "utf-8");
		BufferedReader br = new BufferedReader(isr);
		ArrayList<TagInstance> result = new ArrayList<TagInstance>();
		ArrayList<String[]> words = null;
		ArrayList<String> tags = null;
		Set<String> tagSet = new HashSet<String>();
		int numEntityinSentence = 0;
		int numDiscardInstance = 0;
		int numEntity = 0;
		int instanceId = 1;
		Set<String> labeltypeset = new HashSet<String>();
		while (br.ready()) {
			if (words == null) {
				words = new ArrayList<String[]>();
			}
			if (withLabels && tags == null) {
				tags = new ArrayList<String>();
			}
			String line = br.readLine().trim();
			if (line.startsWith("##")) {
				continue;
			}
			if (line.length() == 0) {
				if (words.size() == 0) {
					continue;
				}

				if ((isLabeled && numEntityinSentence > 0) || !isLabeled) {

					WordToken[] wordsArr = new WordToken[words.size()];
					for (int i = 0; i < wordsArr.length; i++) {
						String word = words.get(i)[0];
						TaggingGlobal.dict.addWord(word);
						wordsArr[i] = new WordToken(word);
					}
					Sentence sent = new Sentence(wordsArr);

					TagInstance instance = new TagInstance(instanceId, 1, sent, tags);
					if (isLabeled) {
						instance.setLabeled(); // Important!
					} else {
						instance.setUnlabeled();
					}
					instanceId++;

					result.add(instance);
					numEntity += numEntityinSentence;
				} else {
					numDiscardInstance++;
				}
				words = null;
				tags = null;
				numEntityinSentence = 0;
			} else {
				int lastSpace = line.lastIndexOf(SEPERATOR);
				String[] features = line.substring(0, lastSpace).split(SEPERATOR);
				words.add(features);
				if (withLabels) {
					String labelStr = line.substring(lastSpace + 1);
					// labelStr = labelStr.replace("B-", "I-");
					// Label label = TargetSentimentGlobal.getLabel(labelStr);
/*
					if (labelStr.contains("conflict")) {
						labelStr = labelStr.replace("conflict", "neutral");
					}
	*/				
					tags.add(labelStr);
					
					//String labelType = labelStr.substring(2);
					//if (!TaggingGlobal.tag2id.containsKey(labelType)) {
						
					//}
					
					if (isLabeled)
					if (labelStr.equals("O")) {
						labeltypeset.add(labelStr);
					} else {
						labeltypeset.add(labelStr.substring(2));
					}

					if (isLabeled && !labels.contains(labelStr)) {
						labels.add(labelStr);
					}

					if (!labelStr.equals("O"))
						numEntityinSentence++;
				}
			}
		}
		br.close();
		System.out.println("Read " + fileName + " with " + result.size() + " instances ...");
		System.out.println("There are " + numEntity + " entities in total.");
		System.out.println(numDiscardInstance + " instances are discarded.");
		System.out.println("labeltypeset:\n" + labeltypeset);
		System.out.println();
		
		
		return result.toArray(new TagInstance[result.size()]);
	}

}
