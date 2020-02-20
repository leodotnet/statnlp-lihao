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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;

import org.statnlp.commons.ml.opt.GradientDescentOptimizer.BestParamCriteria;
import org.statnlp.commons.ml.opt.OptimizerFactory;
import org.statnlp.commons.types.Instance;
import org.statnlp.hypergraph.DiscriminativeNetworkModel;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.NetworkModel;
import org.statnlp.hypergraph.StringIndex;
import org.statnlp.hypergraph.decoding.Metric;
import org.statnlp.hypergraph.neural.BidirectionalLSTM;
import org.statnlp.hypergraph.neural.GlobalNeuralNetworkParam;
import org.statnlp.hypergraph.neural.NeuralNetworkCore;
import org.statnlp.sentiment.spanmodel.common.Counter;
import org.statnlp.sentiment.spanmodel.common.EmbeddingLayer;
import org.statnlp.sentiment.spanmodel.common.SentimentEval;
import org.statnlp.sentiment.spanmodel.common.SentimentInstance;
import org.statnlp.sentiment.spanmodel.common.SpanModelFeatureValueProvider;
import org.statnlp.sentiment.spanmodel.common.SpanModelGlobal;
import org.statnlp.sentiment.spanmodel.common.SpanModelSuperCompiler;
import org.statnlp.sentiment.spanmodel.common.SpanModelSuperFeatureManager;
import org.statnlp.sentiment.spanmodel.common.WordEmbedding;
import org.statnlp.targetedsentiment.common.TSEval;
import org.statnlp.ui.visualize.type.VisualizationViewerEngine;



public class SpanModelMain {
	
	
	
	//public static String[] LABELS = new String[] {"Entity-positive", "Entity-neutral", "Entity-negative", "O"};
	
	public static int num_iter = 1000;
	public static String dataset = "sst";
	public static boolean mentionpenalty = false;
	public static int NEMaxLength = 7;
	public static int SpanMaxLength = 8;
	public static int numThreads = 20;
	public static double l2 = 0.0001;
	public static String embedding = "glove";
	public static int gpuId = -1;
	public static String neuralType =  "continuous";
	public static String nnOptimizer = "lbfgs";
	public static String nerOut = "nn-crf-interface/nlp-from-scratch/me/output/ner_out.txt";
	public static String neural_config = "nn-crf-interface/neural_server/neural.debug.config";
	public static boolean iobes = true;
	public static OptimizerFactory optimizer = OptimizerFactory.getLBFGSFactory();
	public static boolean DEBUG = false;
	public static String SEPERATOR = "\t";
	public static int hiddenSize = 300;
	public static int evalFreq = -1;
	public static int batchSize = 64;
	public static String OS = "linux";
	public static int numOP = 10;
	public static int numSentimentState = 5;
	public static int test_batchSize = -1;
	public static WordEmbedding emb = null;
	public static String UNK = "<UNK>";
	public static boolean fixEmbedding = false;
	
	public static boolean SKIP_TRAIN = false;
	public static boolean SKIP_TEST = false;
	public static boolean VISUALIZE = true;
	public static String in_path = "data//sentiment";
	public static String out_path = "experiments//sentiment//spanmodel";
	public static String feature_file_path = in_path + "//feature_files//";
	public static boolean visual = false;
	public static String lang = "en";
	public static String embedding_suffix = ""; 
	public static boolean word_feature_on = true;
	public static String subpath = "default1";
	public static String modelname = "spanmodel.semi";
	public static NetworkModel model = null;
	
	public static void main(String args[]) throws IOException, InterruptedException{
		
		processArgs(args);
		
		SpanModelGlobal.DUMP_FEATURE = true;
		
		SpanModelGlobal.init();

		
		NetworkConfig.L2_REGULARIZATION_CONSTANT = l2;
		NetworkConfig.NUM_THREADS = numThreads;
		NetworkConfig.BATCH_SIZE = batchSize; //need to enable batch training first
		NetworkConfig.OS = OS;
		

		System.out.println("#iter=" + num_iter + " L2=" + NetworkConfig.L2_REGULARIZATION_CONSTANT + " lang=" + lang + " modelname="+modelname );
		
		if (!embedding.equals(""))
		{
			SpanModelGlobal.ENABLE_WORD_EMBEDDING = true;
			emb = new WordEmbedding(embedding);
			
			if (embedding.equals("en_googlenews")) {
				UNK = "</s>";
			}
		}
		
		System.out.println("ENABLE_WORD_EMBEDDING=" + SpanModelGlobal.ENABLE_WORD_EMBEDDING);
		//if (SpanModelGlobal.ENABLE_WORD_EMBEDDING)
		{
			//SpanModelGlobal.initWordEmbedding(lang + embedding_suffix);
			//NetworkConfig.USE_NEURAL_FEATURES = true;
		}
		
		in_path = in_path + "//" + dataset + "//";
		out_path = out_path + "//" + dataset + "//" + subpath + "//";
		feature_file_path = in_path + "//feature_files//";
	
		File directory = new File(out_path);
		if (!directory.exists())
        {
            directory.mkdirs();
        }
	
		SpanModelGlobal.setLang(lang);
		
		
		


		//for(int i = begin_index; i <= end_index; i++)
		{
			
			
			
			
			String train_file;
			String test_file;
			String dev_file;
			String model_file;
			String result_file;
			String iter = num_iter + "";
			String weight_push;
			
			
			System.out.println("Executing Data ");
			train_file = in_path + "train.ptb.txt";
			test_file = in_path + "test.ptb.txt";
			dev_file = in_path + "dev.ptb.txt";
			
			model_file = out_path + dataset + "." + modelname + "." + l2 + ".model";
			result_file = out_path + dataset + "." + l2 + ".";
			weight_push = in_path + "weight0.data";
			
			System.out.println("Execute data " + dataset);
			
			
			SentimentInstance<Integer>[] trainInstances = readCoNLLData(train_file, true, true);
			SentimentInstance<Integer>[] devInstances = readCoNLLData(dev_file, true, false);
			SentimentInstance<Integer>[] testInstances = readCoNLLData(test_file, true, false);
			
			if(NetworkConfig.USE_BATCH_TRAINING)
			{
				num_iter = (trainInstances.length / batchSize) * num_iter;
				evalFreq = (trainInstances.length / batchSize) * evalFreq;
				System.out.println("#evalFreq [epoch]=" + evalFreq);
				System.out.println("#iter in epoch=" + num_iter);
			}
			else
			{
				System.out.println("#evalFreq [iter]=" + evalFreq);
			}
			
			
			
			//SpanModelGlobal.highFreqWords = SpanModelGlobal.dataCounter.getHighFrequencyWords("train");
			System.out.println(SpanModelGlobal.dataCounter);
			
			System.err.println("[Info] "+SpanModelGlobal.LABELS.length+" labels: "+ Arrays.toString(SpanModelGlobal.LABELS));
			
			List<NeuralNetworkCore> fvps = new ArrayList<NeuralNetworkCore>();
			
			
			if(NetworkConfig.USE_NEURAL_FEATURES){
//				gnp =  new GlobalNetworkParam(OptimizerFactory.getGradientDescentFactory());
				if (neuralType.equals("lstm")) {
					//hiddenSize = 300;
					String optimizer = nnOptimizer;
					boolean bidirection = true;
					fvps.add(new BidirectionalLSTM(hiddenSize, true, optimizer, 0.05, 5, numSentimentState, gpuId, embedding).setModelFile(model_file + ".nn1"));
					//ßßfvps.add(new BidirectionalLSTM(numSentimentState, false, optimizer, 0.05, 5, numOP, gpuId, null).setModelFile(model_file + ".nn2"));
				} 
				else if (neuralType.equals("continuous")) {
					fvps.add(new SpanModelFeatureValueProvider(emb, 5).setUNK(UNK).setModelFile(model_file+ ".cont"));
				}else if (neuralType.equals("continuous*")) {
					System.out.println("neuralType:" + neuralType);
					fvps.add(new EmbeddingLayer(5, hiddenSize, embedding, fixEmbedding).setModelFile(model_file+ ".cont"));
				}  
				
				/*else if (neuralType.equals("mlp")) {
					fvps.add(new MultiLayerPerceptron(neural_config, TargetSentimentGlobal.LABELS.length));
				} */else {
					throw new RuntimeException("Unknown neural type: " + neuralType);
				}
			} 
			GlobalNetworkParam gnp = new GlobalNetworkParam(optimizer, new GlobalNeuralNetworkParam(fvps));
			
			
						
			Class<? extends VisualizationViewerEngine> visualizerClass = getViewer(modelname);
			model = createNetworkModel(modelname, gnp, neuralType);
			Function<Instance[], Metric> evalFunc = new Function<Instance[], Metric>() {
				@Override
				public Metric apply(Instance[] t) {
					Metric result = SentimentEval.eval(t);
					System.out.println("Eval on dev:" + result);
					return result;
				}
				
			};
			
			
			//model.visualize(visualizerClass, trainInstances);
			
			if (!SKIP_TRAIN)
			{
				SpanModelGlobal.clearTemporalData();
				//model.visualize(visualizerClass, trainInstances);
				if (evalFreq == -1) evalFunc = null;
				model.train(trainInstances, num_iter, devInstances, evalFunc, evalFreq);
				saveModel(model, gnp, model_file);
			}
			
			if (!SKIP_TEST)
			{
				if (SKIP_TRAIN)
				{
					model = loadModel(model_file);
				}
				
				Instance[] predictions;
				
				/*
				SpanModelGlobal.clearTemporalData();
				predictions = model.decode(trainInstances);
				writeResult("train", predictions, result_file + "train.out");
				
				
				SpanModelGlobal.clearTemporalData();
				predictions = model.decode(devInstances);
				writeResult("dev", predictions, result_file + "dev.out");
					
				
				SpanModelGlobal.clearTemporalData();
				predictions = model.decode(testInstances);
				writeResult("test", predictions, result_file+ "test.out");
				*/
				recordConfig(model, args, result_file + "config.txt");
				evaluate(model, trainInstances, "train", result_file + "train.out");
				evaluate(model, devInstances, "dev", result_file + "dev.out");
				evaluate(model, testInstances, "test", result_file + "test.out");
				
			}
		}
		return;
	}
	
	public static void recordConfig(NetworkModel model, String[] args, String filename_output)
	{
		PrintWriter p = null;
		try {
			p = new PrintWriter(new File(filename_output));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		p.write(Arrays.toString(args) + "\n");
		
		p.close();
	}
	
	public static void evaluate(NetworkModel model, Instance[] evalInstances, String evalName, String outputPath) throws InterruptedException
	{
		SpanModelGlobal.clearTemporalData();
		
		if (test_batchSize <= 0)
		{
			Instance[] predictions = model.decode(evalInstances);
			writeResult(model, evalName, predictions, outputPath);
		}
		else
		{
			Instance[] predictions = new Instance[evalInstances.length];
			int idx = 0;
			while(idx < evalInstances.length)
			{
				int fromIdx = idx;
				int toIdx = idx + test_batchSize;
				
				if (toIdx > evalInstances.length)
					toIdx = evalInstances.length;
				
				Instance[] partial_evalInstances = new Instance[toIdx - fromIdx];
				for(int i = 0; i < partial_evalInstances.length; i++)
					partial_evalInstances[i] = evalInstances[fromIdx + i];
					
				Instance[] partial_predictions = model.decode(partial_evalInstances);
				
				for(int i = 0; i < partial_predictions.length; i++)
					predictions[fromIdx + i] = partial_predictions[i];
				
				idx = toIdx;
				
			}
			
			writeResult(model, evalName, predictions, outputPath);
		}
	}
	
	public static void visualize(NetworkModel networkModel, Instance[] instances) {
		Class<? extends VisualizationViewerEngine> visualizerClass = null;
		if(visualizerClass == null){
			String visualizerModelName = "org.statnlp.sentiment.spanmodel.SpanModelViewer";
			try{
				visualizerClass = (Class<VisualizationViewerEngine>)Class.forName(visualizerModelName);
			} catch (ClassNotFoundException e) {
				System.err.println("Visualization Class Class not found");
				return;
			}
		}
		try {
			networkModel.visualize(visualizerClass, instances);
		} catch (InterruptedException e) {
			System.err.println("[%s]Visualizer was interrupted.");
		}     
	}
	
	@SuppressWarnings("unchecked")
	private static SentimentInstance<Integer>[] readCoNLLData(String fileName, boolean withLabels, boolean isLabeled) throws IOException{
		InputStreamReader isr = new InputStreamReader(new FileInputStream(fileName), "UTF-8");
		BufferedReader br = new BufferedReader(isr);
		ArrayList<SentimentInstance<Integer>> result = new ArrayList<SentimentInstance<Integer>>();
		//ArrayList<String[]> words = null;
		//ArrayList<Label> labels = null;
		ArrayList<Integer> labels = new ArrayList<Integer>();
		int instanceId = 1;
		while(br.ready()){
			
			String line = br.readLine().trim();
			if(line.startsWith("##")){
				continue;
			}
			if(line.length() == 0){
				
			} else {
				
				String[] fields = line.split(SEPERATOR);
				Integer label = Integer.parseInt(fields[1]) + 2;
				labels.add(label);
				
				/*
				String sentence = fields[2];
				
				ArrayList<String> words = new ArrayList<String>();
				
				String[] words_arr = sentence.split(" ");//(" |-");
				sentence = "";
				for(int i = 0; i < words_arr.length; i++)
				{
					String word = words_arr[i].toLowerCase();
					word = SpanModelGlobal.norm_digits(word);
					words.add(word);
					if (i == 0)
						sentence = word;
					else
						sentence += " " + word;
					
					if (isLabeled)
						SpanModelGlobal.dataCounter.addWordForClass(word, "train");
					
				}
				
				
				SentimentInstance<Integer> instance = new SentimentInstance<Integer>(instanceId, 1, words, label);
				
				if(isLabeled){
					instance.setLabeled(); // Important!
				} else {
					instance.setUnlabeled();
				}
				
				instance.setSentence(sentence);
				instance.preprocess(SpanModelGlobal.dict);
				
				if (instance.useInstance())
				{
					instanceId++;
					result.add(instance);
				}*/
				/*
				if(withLabels){
					
					Label label = SpanModelGlobal.getLabel(labelStr);
					labels.add(label);
				}*/
			}
		}
		br.close();
		
		
		isr = new InputStreamReader(new FileInputStream(fileName + ".conll"), "UTF-8");
		br = new BufferedReader(isr);
		instanceId = 1; 
		//ArrayList<String[]> words = null;
		//ArrayList<Label> labels = null;
		ArrayList<String> POSTags = new ArrayList<String>();
		String sentence = "";
		ArrayList<String> words = new ArrayList<String>();
		
		while(br.ready()){
			
			String line = br.readLine().trim();
			if (line.length() == 0) {
				
				if (words.isEmpty()) continue;
				
				int label = labels.get(instanceId - 1);
				SentimentInstance<Integer> instance = new SentimentInstance<Integer>(instanceId, 1, new ArrayList<String>(words), label);
				
				if(isLabeled){
					instance.setLabeled(); // Important!
				} else {
					instance.setUnlabeled();
				}
				
				instance.setSentence(new String(sentence.trim()));
				instance.setPosTag(new ArrayList<String>(POSTags));
				instance.preprocess(SpanModelGlobal.dict);
				
				if (instance.useInstance())
				{
					instanceId++;
					result.add(instance);
				}
				
				POSTags.clear();
				words.clear();
				sentence = "";
				if (instanceId > labels.size()) break;
				
			}
			else {
				String[] fields = line.split("\t");
				String word = fields[0].toLowerCase(); 
				words.add(word);
				sentence += word + " ";
				POSTags.add(fields[1]);
			}
			
		}
		br.close();
		
		
		
		return result.toArray(new SentimentInstance[result.size()]);
	}
	
	
	private static void writeResult(NetworkModel model, String expname, Instance[] pred, String filename_output) 
	{
		
		
		PrintWriter p = null;
		try {
			p = new PrintWriter(new File(filename_output));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		int[] predict = new int[pred.length];
		int[] predict_2ndlast = new int[pred.length];
		int[] gold = new int[pred.length];
		

		for(int i = 0; i < pred.length; i++)
		{
			if (DEBUG)
				System.out.println("Testing case #" + i + ":");
			
				
			List<String> input = (List<String>)pred[i].getInput();
			predict[i] = (Integer)pred[i].getPrediction();
			gold[i] = (Integer)pred[i].getOutput();
			
			
			p.write(predict[i] + "\t" + gold[i]);
			p.write("\n");
			
		}
		
		p.close();
		
	
		System.out.println(modelname + " Evaluation Completed");
		
			
		try {
			p = new PrintWriter(new File(filename_output + ".stat"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		//5 class
		{
			String all_class_stats = getStatString("5-class", predict, gold);
			System.out.println(all_class_stats);
			p.println(all_class_stats);
		}
		
		//3-class
		{
			int[] predict_3class = predict.clone();
			int[] gold_3class = gold.clone();
			
			for(int i = 0; i < predict.length; i++)
			{
				if (predict_3class[i] > 0)
					predict_3class[i] = 1;
				else if (predict_3class[i] < 0)
					predict_3class[i] = -1;
				else 
					predict_3class[i] = 0;
				
				if (gold_3class[i] > 0)
					gold_3class[i] = 1;
				else if (gold_3class[i] < 0)
					gold_3class[i] = -1;
				else 
					gold_3class[i] = 0;
			}
			
			String all_class_stats = getStatString("3-class", predict_3class, gold_3class);
			System.out.println(all_class_stats);
			p.println(all_class_stats);
			
			
		}
		
		// 2-class
		{
			ArrayList<Integer> predict_2class_arr = new ArrayList<Integer>();
			ArrayList<Integer> gold_2class_arr =  new ArrayList<Integer>();
			
			for(int i = 0; i < predict.length; i++)
			{
				if (gold[i] != 0)
				{
					predict_2class_arr.add(predict[i]);
					gold_2class_arr.add(gold[i]);
				}
			}
			
			int[] predict_2class = new int[predict_2class_arr.size()];
			int[] gold_2class = new int[predict_2class_arr.size()];
			
			for(int i = 0; i < predict_2class_arr.size(); i++)
			{
				predict_2class[i] = predict_2class_arr.get(i);
				gold_2class[i] = gold_2class_arr.get(i);
			}
			

			for (int i = 0; i < predict_2class.length; i++) {
				if (predict_2class[i] > 0)
					predict_2class[i] = 1;
				else if (predict_2class[i] < 0)
					predict_2class[i] = -1;
				else
					predict_2class[i] = 0;

				if (gold_2class[i] > 0)
					gold_2class[i] = 1;
				else if (gold_2class[i] < 0)
					gold_2class[i] = -1;
				else
					gold_2class[i] = 0;
			}

			String all_class_stats = getStatString("2-class", predict_2class, gold_2class);
			System.out.println(all_class_stats);
			p.println(all_class_stats);

		}
		
		
		
		
		p.close();
		
		
		try {
			p = new PrintWriter(new File(filename_output + ".detail"));
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		
		SpanModelSuperCompiler compiler = (SpanModelSuperCompiler)model.getNetworkCompiler();
		Counter counter = new Counter(20);
		
		for(int i = 0; i < pred.length; i++)
		{
			SentimentInstance inst = (SentimentInstance)pred[i];	
			String explanation = compiler.getExplanation(inst);
			p.println(explanation + " " + predict[i] + " " + gold[i]);
		}

		//5 class
		{
				String all_class_stats = getStatString("5-class 2ndlast", predict_2ndlast, gold);
				System.out.println(all_class_stats);
				p.println(all_class_stats);
		}
		
		System.out.println("dataCounter:\n");
		System.out.println(SpanModelGlobal.dataCounter);

		System.out.println("Counter:\n");
		System.out.println(counter);
		
		p.close();

		
	}
	
	public static String getStatString(String expname, int[] predict, int[] gold)
	{
		/*
		double acc = Stats.Accuracy(predict, gold);
		double mse = Stats.MSE(predict, gold);
		double rmse = Math.sqrt(mse);
		double r = Stats.PearsonCorrelation(predict, gold);*/
		StringBuffer sb = new StringBuffer();
		sb.append("Exp Original:\t" + expname + "\n");
		sb.append(SentimentEval.eval(predict, gold).toString());
		/*
		sb.append("Accuracy:\t" + acc + "\n");
		sb.append("MSE:\t" + mse + "\n");
		sb.append("RMSE:\t" + rmse + "\n");
		sb.append("r:\t" + r + "\n");
		sb.append("\n");
		*/
		return sb.toString();
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
					case "-dataset": dataset = args[i+1]; break;    //default:all
					//case "-endindex": end_index = Integer.valueOf(args[i+1]); break;   //default:100;
					//case "-lang": lang = args[i+1]; break;
					case "-mentionpenalty" : mentionpenalty = Boolean.getBoolean(args[i+1]); break;
					case "-dumpfeature" : SpanModelGlobal.DUMP_FEATURE = Boolean.getBoolean(args[i+1]); break;
					case "-subpath" : subpath = args[i+1]; break;
					case "-NEMaxLength": NEMaxLength = Integer.valueOf(args[i+1]); break;
					case "-thread": numThreads = Integer.valueOf(args[i+1]); break;   //default:5
					case "-emb" : embedding = args[i+1]; break;
					case "-gpuid": gpuId = Integer.valueOf(args[i+1]); break;
					case "-usepostag" : SpanModelGlobal.USE_POS_TAG = Boolean.parseBoolean(args[i+1]); break;
					case "-hiddensize": hiddenSize = Integer.valueOf(args[i+1]); break;
					case "-skiptrain" : SKIP_TRAIN = Boolean.parseBoolean(args[i+1]); break;
					case "-fixembedding" : fixEmbedding = Boolean.parseBoolean(args[i+1]); break;
					case "-usetransition" : SpanModelGlobal.USE_TRANSITION = Boolean.parseBoolean(args[i+1]); break;
					case "-evalfreq": evalFreq = Integer.valueOf(args[i+1]); break;
					case "-semiL": SpanModelGlobal.SemiL = Integer.valueOf(args[i+1]); break;
					case "-ngram": SpanModelGlobal.NGRAM = Integer.valueOf(args[i+1]); break;
					case "-lexicons" : SpanModelGlobal.SentimentDictLexicons = args[i+1]; break;
					case "-useglobalinfo" : SpanModelGlobal.Use_Global_Info = Boolean.parseBoolean(args[i+1]); break;

					
					
					//case "-windows":ECRFEval.windows = true; break;            //default: false (is using windows system to run the evaluation script)
					//case "-batch": NetworkConfig.USE_BATCH_TRAINING = true;
					//				batchSize = Integer.valueOf(args[i+1]); break;
					//case "-model": NetworkConfig.MODEL_TYPE = args[i+1].equals("crf")? ModelType.CRF:ModelType.SSVM;   break;
					case "-neural": if(args[i+1].equals("mlp") || args[i+1].equals("lstm")|| args[i+1].startsWith("continuous")){ 
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
						if (!args[i+1].equals("none"))
						{
							NetworkConfig.OPTIMIZE_NEURAL = true;
							nnOptimizer = args[i+1];
						}
						/*
						NetworkConfig.OPTIMIZE_NEURAL = args[i+1].equals("true") ? true : false; //optimize the neural features or not
						if (!NetworkConfig.OPTIMIZE_NEURAL) {
							nnOptimizer = args[i+2];
							i++;
						}*/break;
					case "-optimizer":
						 if(args[i+1].equals("sgd")) {
							 System.out.println("[Info] Using SGD with gradient clipping, take best parameter on development set.");
							 optimizer = OptimizerFactory.getGradientDescentFactoryUsingGradientClipping(BestParamCriteria.BEST_ON_DEV, 0.05, 5);
						 }
						break;
					
					
					
					//case "-lr": adagrad_learningRate = Double.valueOf(args[i+1]); break;
					case "-backend": NetworkConfig.NEURAL_BACKEND = args[i+1]; break;
					case "-os": OS = args[i+1]; break; // for Lua native lib, "osx" or "linux" 
					case "-batch": NetworkConfig.USE_BATCH_TRAINING = true;
									batchSize = Integer.valueOf(args[i+1]); break;
					case "-testbatch": test_batchSize = Integer.valueOf(args[i+1]); break;
					case "-fullunlabel": SpanModelGlobal.USE_FULL_UNLABELNETWORK = Boolean.parseBoolean(args[i+1]);break;
									
					
					default: System.err.println("Invalid arguments "+args[i]+", please check usage."); System.exit(0);
				}
			}
			System.err.println("[Info] dataset: "+ dataset);
			System.err.println("[Info] numIter: "+ num_iter);
			System.err.println("[Info] numThreads: "+numThreads);
			System.err.println("[Info] Regularization Parameter: "+ l2);
		}
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
		
		if (SpanModelGlobal.DUMP_FEATURE)
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
	
	
	public static NetworkModel createNetworkModel(String modelname, GlobalNetworkParam gnp, String neuralType) {
		String modelpath = "org.statnlp.sentiment.";
		SpanModelSuperFeatureManager fm = null;
		SpanModelSuperCompiler compiler = null;


		if (modelname.equals("spanmodel")) {
			fm = new org.statnlp.sentiment.spanmodel.SpanModelFeatureManager(gnp, neuralType, false);
			compiler = new org.statnlp.sentiment.spanmodel.SpanModelCompiler(SpanModelGlobal.dict);
		} else if (modelname.equals("spanmodel.globalinfo")) {
			fm = new org.statnlp.sentiment.spanmodel.globalinfo.SpanModelFeatureManager(gnp, neuralType, false);
			compiler = new org.statnlp.sentiment.spanmodel.globalinfo.SpanModelCompiler(SpanModelGlobal.dict);
		}else if (modelname.equals("spanmodel.globalinfo.relax")) {
			fm = new org.statnlp.sentiment.spanmodel.globalinfo.SpanModelFeatureManager(gnp, neuralType, false);
			compiler = new org.statnlp.sentiment.spanmodel.globalinfo.SpanModelRelaxCompiler(SpanModelGlobal.dict);
		} 
		else if (modelname.equals("spanmodel.globalinfo.relax.uselexicon")) {
			fm = new org.statnlp.sentiment.spanmodel.globalinfo.SpanModelFeatureManager(gnp, neuralType, false);
			compiler = new org.statnlp.sentiment.spanmodel.globalinfo.SpanModelRelaxUseLexiconCompiler(SpanModelGlobal.dict);
		} else if (modelname.equals("spanmodel.globalinfo.scalar")) {
			fm = new org.statnlp.sentiment.spanmodel.globalinfo.SpanModelScalarFeatureManager(gnp, neuralType, false);
			compiler = new org.statnlp.sentiment.spanmodel.globalinfo.SpanModelScalarCompiler(SpanModelGlobal.dict);
		} 
		else if (modelname.equals("spanmodel.semi")) {
			fm = new org.statnlp.sentiment.spanmodel.semi.SpanModelFeatureManager(gnp, neuralType, false);
			compiler = new org.statnlp.sentiment.spanmodel.semi.SpanModelCompiler(SpanModelGlobal.dict);
		}

		fm.setSentimentDict(SpanModelGlobal.dict);
		compiler.setSentimentDict(SpanModelGlobal.dict);
		
		NetworkModel model = DiscriminativeNetworkModel.create(fm, compiler);

		return model;
	}
	
	public static Class<? extends VisualizationViewerEngine> getViewer(String modelname)
	{
		if (modelname.startsWith("spanmodel.globalinfo"))
			modelname = "spanmodel.globalinfo";
		String visualizerModelName = "org.statnlp.sentiment." + modelname + ".SpanModelViewer";
		Class<? extends VisualizationViewerEngine> visualizerClass = null;
		
		try {
			visualizerClass = (Class<VisualizationViewerEngine>) Class.forName(visualizerModelName);
		} catch (ClassNotFoundException e) {
			System.err.println("Class not found");
		}
		
		return visualizerClass;
	}
	

}
