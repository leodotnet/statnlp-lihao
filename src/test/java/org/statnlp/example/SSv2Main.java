package org.statnlp.example;

import gnu.trove.map.hash.TIntIntHashMap;
import gnu.trove.map.hash.TIntObjectHashMap;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import org.statnlp.commons.ml.opt.GradientDescentOptimizer.BestParamCriteria;
import org.statnlp.commons.ml.opt.OptimizerFactory;
import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.hypergraph.DiscriminativeNetworkModel;
import org.statnlp.hypergraph.FeatureManager;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.NetworkCompiler;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.NetworkModel;
import org.statnlp.hypergraph.StringIndex;
import org.statnlp.hypergraph.decoding.Metric;
import org.statnlp.hypergraph.neural.BiLSTMCharCNNWord;
import org.statnlp.hypergraph.neural.BiLSTMCharWord;
import org.statnlp.hypergraph.neural.BidirectionalLSTM;
import org.statnlp.hypergraph.neural.MLP;
import org.statnlp.hypergraph.neural.GlobalNeuralNetworkParam;
import org.statnlp.hypergraph.neural.NeuralNetworkCore;
import org.statnlp.sentiment.spanmodel.common.EmbeddingLayer;
import org.statnlp.sentiment.spanmodel.common.SentimentEval;
import org.statnlp.sentiment.spanmodel.common.SpanModelGlobal;
import org.statnlp.targetedsentiment.common.SPDataConfig;
import org.statnlp.targetedsentiment.common.TSEval;
import org.statnlp.targetedsentiment.common.TSFeatureValueProvider;
import org.statnlp.targetedsentiment.common.TSInstance;
import org.statnlp.targetedsentiment.common.TSMetric;
import org.statnlp.targetedsentiment.common.TargetSentimentGlobal;
import org.statnlp.targetedsentiment.common.Utils;
import org.statnlp.targetedsentiment.common.WordEmbedding;
import org.statnlp.ui.visualize.type.VisualizationViewerEngine;
import org.statnlp.ui.visualize.type.VisualizeGraph;
import org.statnlp.ui.visualize.type.VisualizerFrame;


public class SSv2Main {
	
	public static String[] LABELS = new String[] {"Entity-positive", "Entity-neutral", "Entity-negative", "O"};
	
	public static int num_iter = 3000;
	public static int begin_index = 0;
	public static int end_index = 0;
	public static boolean mentionpenalty = false;
	public static int NEMaxLength = 7;
	public static int SpanMaxLength = 8;
	public static int numThreads = 20;
	public static double l2 = 0.0005;
	public static String embedding = "";
	public static int gpuId = -1;
	public static String neuralType =  "continuous";
	public static String nnOptimizer = "lbfgs";
	public static String nerOut = "nn-crf-interface/nlp-from-scratch/me/output/ner_out.txt";
	public static String neural_config = "nn-crf-interface/neural_server/neural.debug.config";
	public static boolean iobes = true;
	public static OptimizerFactory optimizer = OptimizerFactory.getLBFGSFactory();
	public static boolean DEBUG = false;
	public static String SEPERATOR = "\t";
	public static int evaluateEvery = 0;
	public static String additionalDataset = "none";
	public static String UNK = "<UNK>";
	public static int embeddingSize = 100;
	public static int charHiddenSize = 25;
	public static int evalFreq = -1;
	public static int batchSize = 64;
	public static int test_batchSize = -1;
	public static int numLabel = 3;
	public static String embeddingModelpath = "models//";
	public static int numFilter = 50;
	//public static int windowSize = 3;
	public static int numSentimentLabel = 3;
	public static int cnnWindowSize = 3;
	public static double learningRate = 0.05;
	public static boolean EVALTEST = false;

	
	public static boolean SKIP_TRAIN = false;
	public static boolean SKIP_TEST = false;
	public static String in_path = "data//Twitter_";
	public static String out_path = "experiments//sentiment//models//<modelname>//Twitter_";
	public static String feature_file_path = in_path + "//feature_files//";
	public static boolean visual = false;
	public static String lang = "en";
	public static String embedding_suffix = ""; 
	public static boolean word_feature_on = true;
	public static String subpath = "default";
	public static String modelname = "sentimentspan_latent";
	public static NetworkModel model = null;
	public static String dataSet = "Z-dataset";
	public static int TRIAL = -1;
	
	public static void main(String args[]) throws IOException, InterruptedException{
		
		
		processArgs(args);
		
		TargetSentimentGlobal.init();
		
		NetworkConfig.L2_REGULARIZATION_CONSTANT = l2;
		NetworkConfig.NUM_THREADS = numThreads;
		
		
		System.out.println("#iter=" + num_iter + " L2=" + NetworkConfig.L2_REGULARIZATION_CONSTANT + " lang=" + lang + " modelname="+modelname );
		
		if (NetworkConfig.USE_NEURAL_FEATURES && !embedding.equals(""))
		{
			TargetSentimentGlobal.ENABLE_WORD_EMBEDDING = true;
			
			if (embedding_suffix.equals("fasttext"))
			{
				TargetSentimentGlobal.EMBEDDING_WORD_LOWERCASE = true;
				UNK = "</s>";
			} else if (embedding.startsWith("glove")) {
				TargetSentimentGlobal.EMBEDDING_WORD_LOWERCASE = true;
				UNK = "unk";
			} else if (embedding.equals("polyglot")) {
				TargetSentimentGlobal.EMBEDDING_WORD_LOWERCASE = true;
			}
			
			if (NetworkConfig.OS.equals("linux")) {
				WordEmbedding.MODELPATH = "/home/lihao/corpus/embedding/";
			}
		}
		
		
		if (TargetSentimentGlobal.ENABLE_WORD_EMBEDDING )
		{
			if (neuralType.equals("continuous"))
			if (embedding.equals("polyglot") || embedding.startsWith("glove")) {
				TargetSentimentGlobal.initWordEmbedding(lang + "_" + embedding, embeddingSize);
				
			}
		}
		System.out.println("ENABLE_WORD_EMBEDDING=" + TargetSentimentGlobal.ENABLE_WORD_EMBEDDING);
		
		in_path = TargetSentimentGlobal.getInPath(modelname) + lang + "//";
		
		out_path = out_path + lang + "//" + subpath + "//";
		feature_file_path = in_path + "//feature_files//";
		TargetSentimentGlobal.feature_file_path = feature_file_path;
		
		out_path = out_path.replace("<modelname>", modelname);
		
		File directory = new File(out_path);
		if (!directory.exists())
        {
            directory.mkdirs();
        }
		
		TargetSentimentGlobal.setLang(lang);
		
		
		System.out.println("Configurations:\n" + NetworkConfig.getConfig());
		
		if (dataSet.equals("opendomain")) {
			run10Folds();
		} else {
			
			/*TargetSentimentGlobal.ALLOW_NULL_TARGET = false;
			if (dataSet.startsWith("semeval2016"))
				TargetSentimentGlobal.ALLOW_NULL_TARGET = true;
			*/
			runNormalDataset();
		}
		
		
	
		
		return;
	}
	
	public static void runModel(String[] dataFiles, String dataSet, String dataFilePath, String outputPath) throws IOException, InterruptedException {
		
		String train_file = dataFiles[0];
		String dev_file = dataFiles[1];
		String test_file = dataFiles[2];
		String model_file = SPDataConfig.pathJoin(outputPath, modelname + ".model");
		String result_file = SPDataConfig.pathJoin(outputPath, dataFiles[2] + ".out");
		String iter = num_iter + "";
		//String mentionpenalty = SPDataConfig.pathJoin(outputPath, dataFiles[2] + ".mentionpenalty");;
		
		StringBuffer stats = new StringBuffer(modelname + "\t" + dataSet + "\n");
		TSMetric summary_train = new TSMetric(true);
		TSMetric summary_dev = new TSMetric(true);
		TSMetric summary_test = new TSMetric(true);
		
		System.out.println("Execute data ");
		System.out.println("Result file:" + result_file);
		
		TSInstance<Label>[] trainInstances;
		TSInstance<Label>[] testInstances;
		TSInstance<Label>[] devInstances;
				
		
		trainInstances = readData(dataSet, SPDataConfig.pathJoin(dataFilePath, train_file), true, true);
		testInstances = readData(dataSet, SPDataConfig.pathJoin(dataFilePath, test_file), true, false);
		
		if (EVALTEST) {
			devInstances = readData(dataSet, SPDataConfig.pathJoin(dataFilePath, test_file), true, false);
		} else {
			devInstances = readData(dataSet, SPDataConfig.pathJoin(dataFilePath, dev_file), true, false);
		}
		
		trainInstances = Utils.mergeInstances(trainInstances, readData(dataSet, SPDataConfig.pathJoin(dataFilePath, dev_file), true, true));
		System.out.println("trainInstances[train+dev]:" + trainInstances.length);
		devInstances = testInstances;
		
		TargetSentimentGlobal.testVocabFile = SPDataConfig.pathJoin(dataFilePath, test_file) + ".vocab";
		Utils.writeVocab(TargetSentimentGlobal.testVocabFile, new TSInstance[][]{devInstances, testInstances}, true);

		
		int num_iter_recount = num_iter;
		int eval_freq_recount = evalFreq;
		
		if(NetworkConfig.USE_BATCH_TRAINING)
		{
			NetworkConfig.BATCH_SIZE = batchSize;
			System.out.println("#epoch=" + num_iter);
			System.out.println("#batch=" + batchSize);
			num_iter_recount = (trainInstances.length / batchSize) * num_iter;
			eval_freq_recount = (trainInstances.length / batchSize / 10 ) * evalFreq;
			System.out.println("#evalFreq [epoch]=" + eval_freq_recount);
			System.out.println("#iter in epochs=" + num_iter_recount);
		}
		else
		{
			System.out.println("#evalFreq [iter]=" + evalFreq);
		}
				
		List<NeuralNetworkCore> fvps = new ArrayList<NeuralNetworkCore>();
		if(NetworkConfig.USE_NEURAL_FEATURES){
//			gnp =  new GlobalNetworkParam(OptimizerFactory.getGradientDescentFactory());
			if (neuralType.equals("lstm")) {
				
				String optimizer = nnOptimizer;
				boolean bidirection = true;
				fvps.add(new BidirectionalLSTM(embeddingSize, bidirection, optimizer, 0.05, 5, 3, gpuId, embedding).setModelFile(model_file + ".nn"));
				//fvps.add(new BidirectionalLSTM(hiddenSize, bidirection, optimizer, 0.05, 5, 3, gpuId, embedding));
			}else if (neuralType.equals("lstmchar")) { 
				String optimizer = nnOptimizer;
				boolean bidirection = true;
				fvps.add(new BiLSTMCharWord(embeddingSize, charHiddenSize ,bidirection, optimizer, 0.05, 5, 3, gpuId, embedding).setModelFile(model_file + ".nn"));
				System.out.println("Use Bi-LSTM Word Char");
			}else if (neuralType.equals("lstmcharcnn")) { 
				String optimizer = nnOptimizer;
				boolean bidirection = true;
				fvps.add(new BiLSTMCharCNNWord(embeddingSize, charHiddenSize, numFilter, cnnWindowSize, bidirection, optimizer, 0.05, 5, 3, gpuId, embedding).setModelFile(model_file + ".nn"));
				System.out.println("Use Bi-LSTM Word Char");
			}else if (neuralType.equals("mlp")) { 
				fvps.add(new MLP(numSentimentLabel, embeddingSize, TargetSentimentGlobal.SENTIMENT_WINDOW_SIZE, numFilter, cnnWindowSize, embedding, TargetSentimentGlobal.USE_POSITION_EMBEDDEING).setModelFile(model_file + ".mlp.nn"));
				System.out.println("Use MLP CNN");
			}else if (neuralType.equals("continuous")) {
				fvps.add(new TSFeatureValueProvider(TargetSentimentGlobal.Word2Vec, numLabel).setUNK(UNK).setModelFile(model_file + ".nn"));
			}else if (neuralType.equals("continuous*")) {
				System.out.println("neuralType:" + neuralType);
				fvps.add(new EmbeddingLayer(TSInstance.PolarityType.values().length, embeddingSize,embedding, true).setModelFile(model_file+ ".cont"));
			}   
			else {
				throw new RuntimeException("Unknown neural type: " + neuralType);
			}
		} 
		GlobalNeuralNetworkParam nn = new GlobalNeuralNetworkParam(fvps);
		GlobalNetworkParam gnp = new GlobalNetworkParam(optimizer, nn);
		
		
		
		Class<? extends VisualizationViewerEngine> visualizerClass = getViewer(modelname);
		
		
		model = createNetworkModel(modelname, gnp, neuralType);//NetworkModel model = DiscriminativeNetworkModel.create(fa, compiler);
		
		Function<Instance[], Metric> evalFunc = new Function<Instance[], Metric>() {
			@Override
			public Metric apply(Instance[] t) {
				Metric result = TSEval.eval(t);
				System.out.println("Eval on dev:\n" + result);
				return result;
			}
			
		};
		
		
		TargetSentimentGlobal.clearTemporalData();
		
		if (!SKIP_TRAIN)
		{
			
			model.train(trainInstances, num_iter_recount, devInstances, evalFunc, eval_freq_recount);
			
			if (!neuralType.equals("continuous")) 
			{
				saveModel(model, gnp, model_file);
			}
		}
		if (visual) model.visualize(visualizerClass, trainInstances);
		
		
		
		if (!SKIP_TEST)
		{
			if (SKIP_TRAIN)
			{
				model = loadModel(model_file);
			}
			
			stats.append("Best Dev Result is at iter=" + NetworkConfig.BEST_ITER_DEV + "\n");
			Instance[] predictions;
			
			
			TargetSentimentGlobal.clearTemporalData();
			trainInstances = readData(dataSet, SPDataConfig.pathJoin(dataFilePath, train_file), true, false);
			predictions = model.test(trainInstances);
			stats.append(printStat(predictions, "Train " , summary_train));
			writeResult(predictions, result_file.replace(test_file, train_file)); 
			
			TargetSentimentGlobal.clearTemporalData();
			predictions = model.test(devInstances);
			stats.append(printStat(predictions, "Dev " , summary_dev));
			writeResult(predictions, result_file.replace(test_file, dev_file)); 
			
			TargetSentimentGlobal.clearTemporalData();
			predictions = model.test(testInstances);
			stats.append(printStat(predictions, "Test ", summary_test));
			writeResult(predictions, result_file); 
			
			
			if (visual) {
				for(int k = 0; k <predictions.length; k++) {
					TSInstance inst = (TSInstance)predictions[k];
					inst.setPredictionAsOutput();
					inst.setLabeled();
				}
				model.visualize(visualizerClass, predictions);
			}
			
			

		}
		
		nn.closeNNConnections();
	
		
		System.out.println(stats.toString());
		System.out.println("Summary Train");
		System.out.println(summary_train.compute());
		System.out.println("Summary Dev");
		System.out.println(summary_dev.compute());
		System.out.println("Summary Test");
		System.out.println(summary_test.compute());
		
	}
	
	public static void runNormalDataset() throws IOException, InterruptedException {

		runModel(SPDataConfig.getDataFiles(dataSet, lang).clone(), dataSet, SPDataConfig.getDataPath(dataSet, lang), out_path);

	}
	
	public static void run10Folds() throws InterruptedException, IOException {
		
		
		for(int i = begin_index; i <= end_index; i++)
		{	

			String[] dataFiles = SPDataConfig.getDataFiles(dataSet, lang).clone();
			for(int j = 0; j < dataFiles.length; j++)
				dataFiles[j] = dataFiles[j].replace("[*]", i + "");
			
			runModel(dataFiles, dataSet, SPDataConfig.getDataPath(dataSet, lang), out_path);			
		}
	}
	
	public static String printStat(Instance[] predictions, String expname, TSMetric summary) {
		StringBuffer sb = new StringBuffer(expname + "\n");

		Metric metric = TSEval.eval(predictions);
		sb.append(metric.toString() + "\n");
		
		summary.aggregate((TSMetric)metric);

		return sb.toString();
	}
	
	
	private static TSInstance<Label>[] readData(String dataSet, String fileName, boolean withLabels, boolean isLabeled) throws IOException{
		
		TSInstance[] insts = null;
		
		if (dataSet.startsWith("Z_data") || dataSet.startsWith("Zc_data")  || dataSet.startsWith("T_data") || dataSet.startsWith("semeval2016")) {
			insts = readTZData(fileName, withLabels, isLabeled);
		} else {
			insts = readCoNLLData(fileName, withLabels, isLabeled);
		}
		
		if (TRIAL > 0)
			insts = Utils.portionInstances(insts, TRIAL);
		
		return insts;
	}
	
	
	@SuppressWarnings("unchecked")
	private static TSInstance<Label>[] readTZData(String fileName, boolean withLabels, boolean isLabeled) throws IOException{
		
		//String[] polarity = new String[]{"negative", "neutral", "positive"};
		
		System.out.println("Read " + fileName);
		InputStreamReader isr = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
		BufferedReader br = new BufferedReader(isr);
		ArrayList<TSInstance<Label>> result = new ArrayList<TSInstance<Label>>();
		ArrayList<String[]> words = null;
		ArrayList<Label> labels = null;
		int numEntityinSentence = 0;
		int numDiscardInstance = 0;
		int numEntity = 0;
		int instanceId = 1;
		
		int numErrTokenize = 0;
		
		InputStreamReader isr_features = new InputStreamReader(new FileInputStream(fileName + ".f"), "UTF-8");
		BufferedReader br_features = new BufferedReader(isr_features);
		
		HashMap<Integer, Integer> entityLengthStat = new HashMap<Integer, Integer>();
		while(br.ready()){
			if(words == null){
				words = new ArrayList<String[]>();
			}
			if(withLabels && labels == null){
				labels = new ArrayList<Label>();
			}
			String line = br.readLine().trim();
			if(line.startsWith("##")){
				continue;
			}
			if(line.length() == 0){
				if(words.size() == 0){
					continue;
				}
				
			} else {
				
				String f_line = br_features.readLine().trim();
				String[] f_fields = f_line.split("\t");
				String[] postag = f_fields[0].trim().split(" ");
				
				words = new ArrayList<String[]>();
				labels = new ArrayList<Label>();
				
				String[] field = line.split("\\|\\|\\|");
				String[] word = field[0].split(" ");
				
				if (word.length != postag.length) {
					System.err.println("word != postag");
					System.exit(0);
					numErrTokenize++;
				}
				
				for (int i = 0; i < word.length; i++) {
					String[] feature = new String[TSInstance.WORD_FEATURE_TYPES.values().length];
					Arrays.fill(feature, null);
					feature[TSInstance.WORD_FEATURE_TYPES._id_.ordinal()] = word[i];
					feature[TSInstance.WORD_FEATURE_TYPES.postag.ordinal()] = postag[i];
					words.add(feature);
				}
				

				int numNULLEntity = 0;
				String NULLTarget = null;
				numEntityinSentence = 0;
				ArrayList<int[]> targets = new ArrayList<int[]>();
				
				if (withLabels) {

					for (int i = 0; i < word.length; i++) {
						labels.add(new Label("O", 0));
					}

					for (int i = 1; i < field.length; i += 2) {
						String[] entityIdxStr = field[i].trim().split(" ");
						int entityBeginIdx = Integer.parseInt(entityIdxStr[0].trim());
						int entityEndIdx = Integer.parseInt(entityIdxStr[1].trim());
						int sentIdx = Integer.parseInt(field[i + 1].trim());
						sentIdx = -(sentIdx - 1);
						
						numEntityinSentence++;
						
						targets.add(new int[]{entityBeginIdx, entityEndIdx, sentIdx});
						
						String polar = TSInstance.PolarityType.values()[sentIdx].name();//polarity[sentIdx];
						
						
						if (entityBeginIdx == -1) {
							numNULLEntity++;
							NULLTarget = polar;
							continue;
						}

						
						labels.set(entityBeginIdx, new Label("B-" + polar, sentIdx));

						for (int j = entityBeginIdx + 1; j <= entityEndIdx; j++) {
							labels.set(j, new Label("I-" + polar, sentIdx));
						}

						
						
						{
							int length = entityEndIdx - entityBeginIdx + 1;
							Integer v = entityLengthStat.get(length);
							if (v == null)
								v = 0;
							v++;
							entityLengthStat.put(length, v);
						}
					}
				}
				if (numEntityinSentence <= 0){
					numDiscardInstance++;
					continue;
				}
				
				TSInstance<Label> instance = new TSInstance<Label>(instanceId, 1, words, labels);
				if (isLabeled) {
					instance.setLabeled(); // Important!
				} else {
					instance.setUnlabeled();
				}
				instanceId++;
				
				if (TargetSentimentGlobal.ALLOW_NULL_TARGET ) {
					
					if (numNULLEntity > 0) {
						instance.NULLTarget = NULLTarget;
						instance.numNULLTarget = numNULLEntity;
					}
					instance.setTarget(targets);
				} else {
					if (numNULLEntity == numEntityinSentence) continue;
				}
				
				instance.preprocess(TargetSentimentGlobal.USE_WORD_ONLY);
				result.add(instance);
				numEntity += numEntityinSentence;
				
			}
		}
		br.close();
		br_features.close();
		
		System.out.println("There are " + numEntity + " entities in total.");
		System.out.println(numDiscardInstance + " instances are discarded.");
		System.out.println("Entity Length: " + entityLengthStat.toString());
		System.out.println("numErrTokenize:" + numErrTokenize);
		System.out.println();
		return result.toArray(new TSInstance[result.size()]);
	}
	
	@SuppressWarnings("unchecked")
	private static TSInstance<Label>[] readCoNLLData(String fileName, boolean withLabels, boolean isLabeled) throws IOException{
		System.out.println("Read " + fileName);
		InputStreamReader isr = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
		BufferedReader br = new BufferedReader(isr);
		ArrayList<TSInstance<Label>> result = new ArrayList<TSInstance<Label>>();
		ArrayList<String[]> words = null;
		ArrayList<Label> labels = null;
		int numEntityinSentence = 0;
		int numDiscardInstance = 0;
		int numEntity = 0;
		int instanceId = 1;
		HashMap<Integer, Integer> entityLengthStat = new HashMap<Integer, Integer>();
		while(br.ready()){
			if(words == null){
				words = new ArrayList<String[]>();
			}
			if(withLabels && labels == null){
				labels = new ArrayList<Label>();
			}
			String line = br.readLine().trim();
			if(line.startsWith("##")){
				continue;
			}
			if(line.length() == 0){
				if(words.size() == 0){
					continue;
				}
				
				if (numEntityinSentence > 0) {
					TSInstance<Label> instance = new TSInstance<Label>(instanceId, 1, words, labels);
					
					int max_entity_length_in_sentence = 0;
					ArrayList<int[]> entityList = TSEval.getEntityList(labels);
					for(int[] entity : entityList) {
						int length = entity[1] - entity[0] + 1;
						
						if (max_entity_length_in_sentence < length)
							max_entity_length_in_sentence = length;
						
						if (max_entity_length_in_sentence > TargetSentimentGlobal.NER_SPAN_MAX)
							break;
					}
					
					
					if (!withLabels || TargetSentimentGlobal.NER_SPAN_MAX == -1 || max_entity_length_in_sentence <= TargetSentimentGlobal.NER_SPAN_MAX) {
						for(int[] entity : entityList) {
							int length = entity[1] - entity[0] + 1;
							int polar = entity[2];
							Integer v = entityLengthStat.get(length);
							if (v == null) v = 0;
							v++;
							entityLengthStat.put(length, v);
						}
						
						if (isLabeled) {
							instance.setLabeled(); // Important!
						} else {
							instance.setUnlabeled();
						}
						instanceId++;
						instance.preprocess();
						result.add(instance);
						numEntity += numEntityinSentence;
					} else {
						numDiscardInstance++;
						System.out.println("Discard the instance with max_entity_length:" + max_entity_length_in_sentence);
						System.out.print("\t");
						for(int k = 0; k < words.size(); k++) {
							System.out.print(words.get(k)[0] + " ");
						}
						System.out.print("\n");
						
						
					}
					
					
				} else {
					numDiscardInstance++;
				}
				words = null;
				labels = null;
				numEntityinSentence = 0;
			} else {
				int lastSpace = line.lastIndexOf(SEPERATOR);
				String[] features = line.substring(0, lastSpace).split(SEPERATOR);
				words.add(features);
				if(withLabels){
					String labelStr = line.substring(lastSpace+1);
					//labelStr = labelStr.replace("B-", "I-");
					Label label = TargetSentimentGlobal.getLabel(labelStr);
					labels.add(label);
					if (!labelStr.equals("O"))
						numEntityinSentence++;
				}
			}
		}
		br.close();
		
		System.out.println("There are " + numEntity + " entities in total.");
		System.out.println(numDiscardInstance + " instances are discarded.");
		System.out.println("Entity Length: " + entityLengthStat.toString());
		System.out.println();
		return result.toArray(new TSInstance[result.size()]);
	}
	
	
	
	private static void writeResult(Instance[] pred, String filename_output)
	{
		//String filename_output = (String) getParameters("filename_output");
		//String filename_standard =  (String) getParameters("filename_standard");
		
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
		
		if (DEBUG)
			System.out.println("Result: ");
		for(int i = 0; i < pred.length; i++)
		{
			if (DEBUG)
				System.out.println("Testing case #" + i + ":");
			
			TSInstance inst = (TSInstance)pred[i];
			
			ArrayList<String[]> input = (ArrayList<String[]>)inst.getInput();
			ArrayList<Label> output = (ArrayList<Label>)inst.getPrediction();
			
			
			
			if (DEBUG)
			{
				System.out.println(inst.getSentence());
				System.out.println(output);
			}
			for(int j = 0; j < input.size(); j++)
			{
				//try{
				p.write(output.get(j).getForm() + "\n");
				//} catch (Exception e) {
				//	System.err.println();
				//}
			}
			
			p.write("\n");
		}
		
		p.close();
		
		if (DEBUG)
		{
			System.out.println("\n");
		}
		System.out.println(modelname + " Evaluation Completed");
		
		if (TargetSentimentGlobal.OUTPUT_SENTIMENT_SPAN)
		{
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
			int[][] splits = new int[pred.length][];
			ArrayList<Integer> split = new ArrayList<Integer>();
			
			
			for(int i = 0; i < pred.length; i++)
			{
				TSInstance inst = (TSInstance)pred[i];
				ArrayList<String[]> input = (ArrayList<String[]>)inst.getInput();
				ArrayList<Label> gold = (ArrayList<Label>)inst.getOutput();
				ArrayList<Label> output = (ArrayList<Label>)inst.getPrediction();
				ArrayList<int[]> scopes = inst.scopes;
						
			
				String t = "";
				
				char lastTag = 'O';
				
				String scopeText = "";
				int entityIdx = 0;
				
				for(int k = 0; k < input.size(); k++) {
					
					String labelStr = output.get(k).getForm();
					char tag = labelStr.charAt(0);
					
					if (lastTag != 'O' && tag != 'I') {
						t += "<span class='tooltiptext'>" + scopeText + "</span></div>  ";
						scopeText = "";
					}
					
					
					if (tag == 'B')
					{
						String incorrect = "";
						
						String labelStrGold = gold.get(k).getForm();
						
						scopeText = "";
						
						if (TargetSentimentGlobal.FIXNE && labelStrGold.charAt(0) == 'B') {
							if (!labelStr.equals(labelStrGold)) {
								incorrect = "_incorrect";
								scopeText = "(" + labelStrGold.substring(2) + ") ";
							}
						}
						
						t += "<div class='tooltip entity_" + labelStr.substring(2) + incorrect + "'>";
						
						try {
						for(int j = scopes.get(entityIdx)[0]; j <  scopes.get(entityIdx)[1]; j++) {
							scopeText += input.get(j)[0] + " ";
						}} catch (Exception e) {
							System.out.println();
						}
						
						entityIdx++;
					}
					
					t += input.get(k)[0] + " ";
					
					
					lastTag = tag;
				}
				
				if (lastTag != 'O') {
					t += "<span class='tooltiptext'>" + scopeText + "</span></div>  ";;
				}
				
				if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0) {
					
					String scopeTextNULLTarget = "";
					String incorrect = "";
					if (!inst.NULLTargetPred.equals(inst.NULLTarget)) {
						incorrect = "_incorrect";
						scopeTextNULLTarget += "(" + inst.NULLTarget + ")";
					}
					
					
					for(int j = scopes.get(scopes.size() - 1)[0]; j <  scopes.get(scopes.size() - 1)[1]; j++) {
						//System.err.println("j=" + j + Arrays.toString(scopes.get(scopes.size() - 1)));
						scopeTextNULLTarget += input.get(j)[0] + " ";
					}
					t += "&nbsp;&nbsp;&nbsp;&nbsp;<div class='tooltip entity_" + inst.NULLTargetPred + incorrect + "'>NULL <span class='tooltiptext'>" + scopeTextNULLTarget + "</span></div>  ";
				}
				
				t += "<br>";
				p.println(t);
				

			
			}
			
			p.write(footer);
			
			p.close();
					
			
		}
	
		
	}
	
	
	
	public static void processArgs(String[] args){
		
		if (args.length == 0)
		{
			return;
		}
		
		if(args[0].equals("-h") || args[0].equals("help") || args[0].equals("-help") ){
			System.err.println("Sentiment Scope Version: Joint Entity Recognition and Sentiment Prediction TASK: ");
			//System.err.println("\t usage: java -jar dpe.jar -trainNum -1 -testNum -1 -thread 5 -iter 100 -pipe true");
			//System.err.println("\t put numTrainInsts/numTestInsts = -1 if you want to use all the training/testing instances");
			System.exit(0);
		}else{
			for(int i=0;i<args.length;i=i+2){
				switch(args[i]){
					case "-modelname": modelname = args[i+1]; break;   //default: all 
					case "-reg": l2 = Double.valueOf(args[i+1]);  break;
					case "-num_iter": num_iter = Integer.valueOf(args[i+1]); break;    //default:all
					case "-beginindex": begin_index = Integer.valueOf(args[i+1]); break;    //default:all
					case "-endindex": end_index = Integer.valueOf(args[i+1]); break;   //default:100;
					case "-lang": lang = args[i+1]; break;
					case "-mentionpenalty" : mentionpenalty = Boolean.getBoolean(args[i+1]); break;
					case "-subpath" : subpath = args[i+1]; break;
					case "-NEMaxLength": NEMaxLength = Integer.valueOf(args[i+1]); break;
					case "-thread": numThreads = Integer.valueOf(args[i+1]); break;   //default:5
					case "-emb" : embedding = args[i+1]; break;
					case "-gpuid": gpuId = Integer.valueOf(args[i+1]); break;
					case "-fixemb" : TargetSentimentGlobal.FIX_EMBEDDING = Boolean.parseBoolean(args[i+1]); break;
					case "-usepostag" : TargetSentimentGlobal.USE_POS_TAG = Boolean.parseBoolean(args[i+1]); break;
					case "-useadditional" : 
						additionalDataset = args[i+1];
						if (additionalDataset.equals("none")) break;
						TargetSentimentGlobal.ENABLE_ADDITIONAL_DATA = true; 
						break;
					case "-fixne" : TargetSentimentGlobal.FIXNE = Boolean.parseBoolean(args[i+1]); break;
					case "-dumpfeature" : TargetSentimentGlobal.DUMP_FEATURE = Boolean.parseBoolean(args[i+1]); break;
					case "-visual" : visual = Boolean.parseBoolean(args[i+1]); break;
					case "-visualID" : VisualizerFrame.INSTANCE_INIT_ID = Integer.valueOf(args[i+1]); break;
					case "-skiptest" : SKIP_TEST = Boolean.parseBoolean(args[i+1]); break;
					case "-skiptrain" : SKIP_TRAIN = Boolean.parseBoolean(args[i+1]); break;
					//case "-windows":ECRFEval.windows = true; break;            //default: false (is using windows system to run the evaluation script)
					//case "-batch": NetworkConfig.USE_BATCH_TRAINING = true;
					//				batchSize = Integer.valueOf(args[i+1]); break;
					//case "-model": NetworkConfig.MODEL_TYPE = args[i+1].equals("crf")? ModelType.CRF:ModelType.SSVM;   break;
					case "-neural": if(args[i+1].equals("mlp") || args[i+1].startsWith("lstm")|| args[i+1].startsWith("continuous")){ 
											NetworkConfig.USE_NEURAL_FEATURES = true;
											neuralType = args[i+1]; //by default optim_neural is false.
											NetworkConfig.IS_INDEXED_NEURAL_FEATURES = false; //only used when using the senna embedding.
											NetworkConfig.REGULARIZE_NEURAL_FEATURES = true;
									}
									break;
					case "-initNNweight": 
						NetworkConfig.INIT_FV_WEIGHTS = args[i+1].equals("true") ? true : false; //optimize the neural features or not
						break;
					case "-optimNeural": 
						NetworkConfig.OPTIMIZE_NEURAL = args[i+1].equals("true") ? true : false; //optimize the neural features or not
						if (!NetworkConfig.OPTIMIZE_NEURAL) {
							nnOptimizer = args[i+2];
							i++;
						}break;
					case "lr":
						learningRate = Double.valueOf(args[i+1]);  break;
					case "-optimizer":
						 if(args[i+1].equals("sgd")) {
							 optimizer = OptimizerFactory.getGradientDescentFactoryUsingGradientClipping(BestParamCriteria.BEST_ON_DEV, learningRate, 5); 
							 NetworkConfig.USE_BATCH_TRAINING = true;
							 batchSize = 1;
							 NetworkConfig.BATCH_SIZE = 1;
							 NetworkConfig.RANDOM_BATCH = false;
						 } else if (args[i+1].equals("gd")) {
							 optimizer = OptimizerFactory.getGradientDescentFactory();
						 } else if (args[i+1].equals("adagrad")) {
							 optimizer = OptimizerFactory.getGradientDescentFactoryUsingAdaGrad();
						 }
						break;
					case "-randombatch" : NetworkConfig.RANDOM_BATCH = Boolean.parseBoolean(args[i+1]); break;
					
					
					//case "-lr": adagrad_learningRate = Double.valueOf(args[i+1]); break;
					case "-backend": NetworkConfig.NEURAL_BACKEND = args[i+1]; break;
					case "-os": NetworkConfig.OS = args[i+1]; break; // for Lua native lib, "osx" or "linux" 
					case "-embsuffix": embedding_suffix = args[i+1]; break;
					case "-unk" : UNK = args[i+1]; System.out.println("set UNK = " + UNK);break;
					case "-ngram" : TargetSentimentGlobal.NGRAM = Boolean.parseBoolean(args[i+1]); break;
					case "-hidesink" : VisualizeGraph.hideEdgetoSink = Boolean.parseBoolean(args[i+1]); break;
					case "-overlap":TargetSentimentGlobal.OVERLAPPING_FEATURES = Boolean.parseBoolean(args[i+1]); break;
					case "-outputscope": TargetSentimentGlobal.OUTPUT_SENTIMENT_SPAN = Boolean.parseBoolean(args[i+1]); break;
					case "-embsize": embeddingSize = Integer.valueOf(args[i+1]); break;    //default:all
					case "-charhiddensize": charHiddenSize = Integer.valueOf(args[i+1]); break;    //default:all
					case "-NERL": TargetSentimentGlobal.NER_SPAN_MAX = Integer.valueOf(args[i+1]); break;  
					case "-evalfreq": evalFreq = Integer.valueOf(args[i+1]); break;  
					case "-batch": batchSize = Integer.valueOf(args[i+1]); 
							if (batchSize > 0)
								NetworkConfig.USE_BATCH_TRAINING = true;
							break;
					case "-testbatch": test_batchSize = Integer.valueOf(args[i+1]); break;
					case "-label": numLabel = Integer.valueOf(args[i+1]); break;
					case "-numfilter": numFilter = Integer.valueOf(args[i+1]); break;
					case "-cnnwindowSize": cnnWindowSize = Integer.valueOf(args[i+1]); break;
					case "-sentwindowsize": TargetSentimentGlobal.SENTIMENT_WINDOW_SIZE = Integer.valueOf(args[i+1]); break;
					case "-dataset": dataSet = args[i+1]; break;
					case "-unigramfeature" : TargetSentimentGlobal.USE_UNIGRAM_DISCRETE_FEATURES = Boolean.parseBoolean(args[i+1]); break;
					case "-wordonly" : TargetSentimentGlobal.USE_WORD_ONLY = Boolean.parseBoolean(args[i+1]); break;
					case "-trial" : TRIAL = Integer.valueOf(args[i+1]); break;
					case "-posemb" : TargetSentimentGlobal.USE_POSITION_EMBEDDEING = Boolean.parseBoolean(args[i+1]); break;
					case "-nulltarget" : TargetSentimentGlobal.ALLOW_NULL_TARGET = Boolean.parseBoolean(args[i+1]); break;
					case "-discrete" : TargetSentimentGlobal.ENABLE_DISCRETE_FEATURE = Boolean.parseBoolean(args[i+1]); break;
					case "-evaltest" : EVALTEST = Boolean.parseBoolean(args[i+1]); break;
					case "-allcandicate" : TargetSentimentGlobal.USE_ALLWORDS_AS_CANDIDICATE = Boolean.parseBoolean(args[i+1]); break;
					case "-debug" : DEBUG = Boolean.parseBoolean(args[i+1]); break;
					
					
					default: System.err.println("Invalid arguments "+args[i]+", please check usage."); System.exit(0);
				}
			}
			System.err.println("[Info] beginIndex: "+begin_index);
			System.err.println("[Info] endIndex: "+end_index);
			System.err.println("[Info] numIter: "+ num_iter);
			System.err.println("[Info] numThreads: "+numThreads);
			System.err.println("[Info] Regularization Parameter: "+ l2);
		}
	}
	
	public static NetworkModel createNetworkModel(String modelname, GlobalNetworkParam gnp, String neuralType) {
		String modelpath = "org.statnlp.targetedsentiment.f";
		FeatureManager fm = null;
		NetworkCompiler compiler = null;


		if (modelname.equals("sentimentspan_latent")) {
			fm = new org.statnlp.targetedsentiment.f.latent.TargetSentimentFeatureManager(gnp, neuralType, TargetSentimentGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.statnlp.targetedsentiment.f.latent.TargetSentimentCompiler();
		} else if (modelname.equals("sentimentspan_latent_transition")) {
			fm = new org.statnlp.targetedsentiment.f.latent.TargetSentimentOverlapFeatureManager(gnp, neuralType, TargetSentimentGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.statnlp.targetedsentiment.f.latent.TargetSentimentOverlapCompiler();
		}else if (modelname.equals("sentimentspan_latent_transition2")) {
			fm = new org.statnlp.targetedsentiment.f.latent.transition.TargetSentimentTransitionFeatureManager(gnp, neuralType, TargetSentimentGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.statnlp.targetedsentiment.f.latent.transition.TargetSentimentTransitionCompiler();
		} 
		else if (modelname.equals("baseline_collapse")) {
			fm = new org.statnlp.targetedsentiment.f.baseline.CollapseTSFeatureManager(gnp, neuralType, TargetSentimentGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.statnlp.targetedsentiment.f.baseline.CollapseTSCompiler();
		} else if (modelname.equals("baseline_collapse_simple")) {
			fm = new org.statnlp.targetedsentiment.f.baseline.simple.CollapseSimpleTSFeatureManager(gnp, neuralType, TargetSentimentGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.statnlp.targetedsentiment.f.baseline.CollapseTSCompiler();
		} else if (modelname.equals("sentimentscope_overlap")) {
			fm = new org.statnlp.targetedsentiment.overlap.cont.SSOverlapContFeatureManager(gnp, neuralType, TargetSentimentGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.statnlp.targetedsentiment.overlap.cont.SSOverlapContCompiler();
		}else if (modelname.equals("sentiment_parsing")) {
			fm = new org.statnlp.targetedsentiment.ncrf.semi.SentimentParsingSemiFeatureManager(gnp, neuralType, TargetSentimentGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.statnlp.targetedsentiment.ncrf.semi.SentimentParsingSemiCompiler();
		}else if (modelname.equals("sentiment_parsing_hybrid")) {
			fm = new org.statnlp.targetedsentiment.ncrf.semi.SentimentParsingSemiFeatureManager(gnp, neuralType, TargetSentimentGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.statnlp.targetedsentiment.ncrf.semi.SentimentParsingSemiHybridCompiler();
		}else if (modelname.equals("sentiment_parsing_linear")) {
			fm = new org.statnlp.targetedsentiment.ncrf.linear.SentimentParsingLinearFeatureManager(gnp, neuralType, TargetSentimentGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.statnlp.targetedsentiment.ncrf.linear.SentimentParsingLinearCompiler();
		}
		
		else if (modelname.equals("sentiment_parsing_linear_ner")) {
			fm = new org.statnlp.targetedsentiment.ncrf.baseline.SentimentParsingLinearNERFeatureManager(gnp, neuralType, TargetSentimentGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.statnlp.targetedsentiment.ncrf.baseline.SentimentParsingLinearNERCompiler();
		} else if (modelname.equals("sentiment_parsing_semi_ner")) {
			fm = new org.statnlp.targetedsentiment.ncrf.baseline.SentimentParsingSemiNERFeatureManager(gnp, neuralType, TargetSentimentGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.statnlp.targetedsentiment.ncrf.baseline.SentimentParsingSemiNERCompiler();
		}

		NetworkModel model = DiscriminativeNetworkModel.create(fm, compiler);

		return model;
	}
	
	public static Class<? extends VisualizationViewerEngine> getViewer(String modelname)
	{
		String visualModelPath = null;
		switch (modelname)
		{
		case "sentimentspan_latent": return null; 
		case "baseline_collapse":visualModelPath = "f.baseline.CollapseViewer";break;
		case "sentimentscope_overlap":visualModelPath = "overlap.cont.SSOverlapContViewer"; break;
		case "sentiment_parsing":visualModelPath = "ncrf.semi.SentimentParsingSemiViewer"; break;
		case "sentiment_parsing_hybrid":visualModelPath = "ncrf.semi.SentimentParsingSemiViewer"; break;
		case "sentiment_parsing_linear":visualModelPath = "ncrf.linear.SentimentParsingLinearViewer"; break;
		case "sentimentspan_latent_transition2": visualModelPath = "f.latent.transition.TargetSentimentTransitionViewer"; break;
		}
		
		if (visualModelPath == null)
			return null;
		
		String visualizerModelName = "org.statnlp.targetedsentiment." + visualModelPath;
		Class<? extends VisualizationViewerEngine> visualizerClass = null;
		
		try {
			visualizerClass = (Class<VisualizationViewerEngine>) Class.forName(visualizerModelName);
		} catch (ClassNotFoundException e) {
			System.err.println("Class not found");
		}
		
		return visualizerClass;
	}
	
	public static void saveModel(NetworkModel model, GlobalNetworkParam param, String filename_model ) throws IOException {
		
		
		System.out.println();
		System.err.println("Saving Model...");
		ObjectOutputStream out;
		out = new ObjectOutputStream(new FileOutputStream(filename_model));
		
		out.writeObject(model);
		out.flush();
		out.close();
		System.err.println("Model Saved.");
		
		if (TargetSentimentGlobal.DUMP_FEATURE)
			printFeature(param, filename_model);
		
	}
	
	public static void printFeature(GlobalNetworkParam param, String filename_model )
	{
		
		StringIndex string2Idx = param.getStringIndex();
		string2Idx.buildReverseIndex();
		
		PrintWriter modelTextWriter = null;
		try {
			modelTextWriter = new PrintWriter(filename_model + ".dump");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}


		modelTextWriter.println("Num features: "+param.countFeatures());

		modelTextWriter.println("Features:");

		TIntObjectHashMap<TIntObjectHashMap<TIntIntHashMap>> featureIntMap =  param.getFeatureIntMap();

		for(int featureTypeId: sorted(featureIntMap.keys())){ //sorted

		     //.println(featureType);

			 TIntObjectHashMap<TIntIntHashMap> outputInputMap = featureIntMap.get(featureTypeId);

		     for(int outputId: sorted(outputInputMap.keys())){ //sorted

		          //modelTextWriter.println("\t"+output);

		    	 TIntIntHashMap inputMap = outputInputMap.get(outputId);

		          for(int inputId: inputMap.keys()){

		               int featureId = inputMap.get(inputId);
		               
		               String featureType = string2Idx.get(featureTypeId);
		               String input = string2Idx.get(inputId);
		               String output = string2Idx.get(outputId);

		               modelTextWriter.println(featureType + input+ ":= " + output + "="+param.getWeight(featureId));
		               if (SpanModelGlobal.ECHO_FEATURE)
		            	   System.out.println(featureType + input+ ":= " + output + "="+param.getWeight(featureId));
		          
		          }

		     }
		     
		     modelTextWriter.flush();

		}

		modelTextWriter.close();
	}
	
	static int[] sorted(int[] arr)
	{
		int[] arr_sorted = arr.clone();
		Arrays.sort(arr_sorted);
		return arr_sorted;
	}

	public static NetworkModel loadModel(String filename_model) throws IOException {
		
		
		NetworkModel model = null;
		System.err.println("Loading Model...");
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename_model));
		try {
			model = (NetworkModel)in.readObject();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		in.close();
			
		
		System.err.println("Model Loaded.");

		return model;		
	}
}
