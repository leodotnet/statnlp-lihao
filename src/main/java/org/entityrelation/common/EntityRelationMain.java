package org.entityrelation.common;

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
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.function.Function;

import org.entityrelation.common.EntityRelationFeatureManager.FeatureType;
import org.statnlp.commons.ml.opt.GradientDescentOptimizer.BestParamCriteria;
import org.statnlp.commons.ml.opt.OptimizerFactory;
import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.example.descriptor.semeval.RelationCNN;
import org.statnlp.hypergraph.DiscriminativeNetworkModel;
import org.statnlp.hypergraph.FeatureManager;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.NetworkCompiler;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.NetworkConfig.ModelType;
import org.statnlp.hypergraph.NetworkModel;
import org.statnlp.hypergraph.StringIndex;
import org.statnlp.hypergraph.decoding.Metric;
import org.statnlp.hypergraph.neural.BiLSTMCharCNNWord;
import org.statnlp.hypergraph.neural.BiLSTMCharWord;
import org.statnlp.hypergraph.neural.BidirectionalLSTM;
import org.statnlp.hypergraph.neural.MLP;
import org.statnlp.hypergraph.neural.EmbeddingLayer;
import org.statnlp.hypergraph.neural.GlobalNeuralNetworkParam;
import org.statnlp.hypergraph.neural.NeuralNetworkCore;

import org.statnlp.ui.visualize.type.VisualizationViewerEngine;
import org.statnlp.ui.visualize.type.VisualizeGraph;
import org.statnlp.ui.visualize.type.VisualizerFrame;


public class EntityRelationMain {
	
	
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
	public static String neuralType =  "none";
	public static String nnOptimizer = "lbfgs";
	public static boolean iobes = true;
	public static OptimizerFactory optimizer = OptimizerFactory.getLBFGSFactory();
	public static boolean DEBUG = false;
	public static String SEPERATOR = "\t";
	public static int evaluateEvery = 0;
	public static String UNK = "<UNK>";
	public static int embeddingSize = 100;
	public static int charHiddenSize = 25;
	public static int evalFreq = -1;
	public static int batchSize = 64;
	public static int test_batchSize = -1;
	public static int numLabel = 3;
	public static String embeddingModelpath = "models//";
	public static int numFilter = 50;
	public static double learningRate = 0.05;
	public static boolean EVALTEST = false;
	public static boolean TESTTRAIN = false;
	public static boolean TESTDEV = true;
	public static boolean USEPERL = true;
	public static int posEmbeddingSize = 25;
	public static int hiddenSize = 128;
	public static int layer2HiddenSize = 128;
	public static int cnnWindowSize = 3;
	public static int gruHiddenSize = 128;
	public static double dropOut = 0.5;
	
	public static boolean SKIP_TRAIN = false;
	public static boolean SKIP_TEST = false;
	public static String in_path = "data//Twitter_";
	public static String out_path = "experiments//EntityRelation//models//<modelname>//en";
	public static String feature_file_path = in_path + "//feature_files//";
	public static boolean visual = false;
	public static String lang = "en";
	public static String embedding_suffix = ""; 
	public static boolean word_feature_on = true;
	public static String subpath = "default";
	public static String modelname = "";
	public static NetworkModel model = null;
	
	public static String dataSet = "EntityRelation";
	public static int TRIAL = -1;
	public static int FOLDIDX = -1;
	
	public static EntityRelationMetric summary_train = new EntityRelationMetric(true);
	public static EntityRelationMetric summary_dev = new EntityRelationMetric(true);
	public static EntityRelationMetric summary_test = new EntityRelationMetric(true);
	
	public static void main(String args[]) throws IOException, InterruptedException{
		
		
		processArgs(args);
		
		EntityRelationGlobal.init();
		
		NetworkConfig.AVOID_DUPLICATE_FEATURES = true;
		NetworkConfig.L2_REGULARIZATION_CONSTANT = l2;
		NetworkConfig.NUM_THREADS = numThreads;
		NetworkConfig.FEATURE_TOUCH_TEST = false;
		EntityRelationGlobal.modelname = modelname;
		out_path = out_path.replace("<modelname>", modelname);
		out_path = EntityRelationDataConfig.pathJoin(out_path, subpath);
		
		
		System.out.println("#iter=" + num_iter + " L2=" + NetworkConfig.L2_REGULARIZATION_CONSTANT + " lang=" + lang + " modelname="+modelname );
		
		if (NetworkConfig.USE_NEURAL_FEATURES && !embedding.equals(""))
		{
			EntityRelationGlobal.ENABLE_WORD_EMBEDDING = true;
			
			if (embedding_suffix.equals("fasttext"))
			{
				EntityRelationGlobal.EMBEDDING_WORD_LOWERCASE = true;
				UNK = "</s>";
			} else if (embedding.startsWith("glove")) {
				EntityRelationGlobal.EMBEDDING_WORD_LOWERCASE = true;
				UNK = "unk";
			} else if (embedding.equals("polyglot")) {
				EntityRelationGlobal.EMBEDDING_WORD_LOWERCASE = true;
				UNK = "<UNK>";
			}
			
			if (NetworkConfig.OS.equals("linux")) {
				WordEmbedding.MODELPATH = "/home/lihao/corpus/embedding/";
			}
		}
		
		EntityRelationGlobal.UNK = UNK;
		EntityRelationGlobal.neuralType = neuralType;
		
		
		if (EntityRelationGlobal.ENABLE_WORD_EMBEDDING )
		{
			if (neuralType.equals("continuous") || neuralType.equals("continuous0"))
			if (embedding.equals("polyglot") || embedding.startsWith("glove")) {
				EntityRelationGlobal.initWordEmbedding(lang + "_" + embedding, embeddingSize);
				
			}
		}
		System.out.println("ENABLE_WORD_EMBEDDING=" + EntityRelationGlobal.ENABLE_WORD_EMBEDDING);
		
		
		
		
		File directory = new File(out_path);
		if (!directory.exists())
        {
            directory.mkdirs();
        }
		
		EntityRelationGlobal.setLang(lang);
		
		
		
		String[] fold = EntityRelationDataConfig.getDataFold(dataSet, lang);
		
		
		if (fold != null) {
			runFolds(fold);
		} else {
			runNormalDataset();
		}
	
		
		return;
	}
	
	public static void runModel(String[] dataFiles, String dataSet, String dataFilePath, String outputPath) throws IOException, InterruptedException {
		
		String train_file = dataFiles[0];
		String dev_file = dataFiles[1] ;
		String test_file = dataFiles[2];
		String model_file = EntityRelationDataConfig.pathJoin(outputPath, modelname + "." + train_file +".model");
		String trainPath = null;
		
		
		String result_file = EntityRelationDataConfig.pathJoin(outputPath, test_file + ".out");
		String iter = num_iter + "";
		//String mentionpenalty = EntityRelationDataConfig.pathJoin(outputPath, dataFiles[2] + ".mentionpenalty");;
		
		StringBuffer stats = new StringBuffer(modelname + "\t" + dataSet + "\n");
		
		
		
		System.out.println("Execute data ");
		System.out.println("Result file:" + result_file);
		System.out.println();
		
		EntityRelationInstance<Label>[] trainInstances;
		EntityRelationInstance<Label>[] testInstances;
		EntityRelationInstance<Label>[] test2Instances;
		EntityRelationInstance<Label>[] devInstances;
		
		double ratio = 1;
		
		trainInstances = EntityRelationReader.readData(dataSet, EntityRelationDataConfig.pathJoin(dataFilePath, train_file), true, true, TRIAL, false);
		testInstances = EntityRelationReader.readData(dataSet, EntityRelationDataConfig.pathJoin(dataFilePath, test_file), true, false, -1, false);
		if (EVALTEST) {
			devInstances = EntityRelationReader.readData(dataSet, EntityRelationDataConfig.pathJoin(dataFilePath, test_file), true, false, -1, false);
		} else {
			devInstances = EntityRelationReader.readData(dataSet, EntityRelationDataConfig.pathJoin(dataFilePath, dev_file), true, false,-1 ,false);
		}
		
		
		EntityRelationGlobal.testVocabFile = EntityRelationDataConfig.pathJoin(dataFilePath, test_file) + ".vocab";
		
		if (NetworkConfig.USE_NEURAL_FEATURES)
			Utils.writeVocab(EntityRelationGlobal.testVocabFile, new EntityRelationInstance[][]{devInstances, testInstances}, true);
		
		EntityRelationGlobal.NUM_ENTITY_TYPE = EntityRelationOutput.ENTITYTYPE.size();
		EntityRelationGlobal.NUM_RELATION_TYPE = EntityRelationOutput.RELATIONS.size();
		
		System.out.println("Configurations:\n" + NetworkConfig.getConfig());
		System.out.println("Global:\n" + EntityRelationGlobal.getConfig());
		
		int num_iter_recount = num_iter;
		int eval_freq_recount = evalFreq;
		
		if(NetworkConfig.USE_BATCH_TRAINING)
		{
			NetworkConfig.FEATURE_TOUCH_TEST = true;
			NetworkConfig.BATCH_SIZE = batchSize;
			System.out.println("#epoch=" + num_iter);
			System.out.println("#batch=" + batchSize);
			num_iter_recount = (trainInstances.length / batchSize) * num_iter;
			eval_freq_recount = (trainInstances.length / batchSize  ) * evalFreq;
			System.out.println("#evalFreq [epoch]=" + eval_freq_recount);
			System.out.println("#iter in epochs=" + num_iter_recount);
		}
		else
		{
			System.out.println("#evalFreq [iter]=" + evalFreq);
		}
				
		List<NeuralNetworkCore> fvps = new ArrayList<NeuralNetworkCore>();
		if(NetworkConfig.USE_NEURAL_FEATURES){
			NetworkConfig.FEATURE_TOUCH_TEST = true;
			
			//int numLabel = EntityRelationGlobal.NUMLABEL ;//Integer.parseInt(modelname.substring(modelname.length() - 1));
			System.out.println("neuralType:" + neuralType + " with #Label = " + numLabel);
//			gnp =  new GlobalNetworkParam(OptimizerFactory.getGradientDescentFactory());
			if (neuralType.equals("lstm")) {
				
				String optimizer = nnOptimizer;
				boolean bidirection = true;
				fvps.add(new BidirectionalLSTM(embeddingSize, bidirection, optimizer, 0.05, 5, EntityRelationGlobal.NUM_ENTITY_TYPE + 1, gpuId, embedding).setModelFile(model_file + ".nn"));

			}else if (neuralType.equals("cnn")) { 
				fvps.add(new RelationCNN("RelationCNN",EntityRelationGlobal.NUM_RELATION_TYPE + 1, hiddenSize, embedding, gpuId, cnnWindowSize, embeddingSize, layer2HiddenSize, dropOut, true, posEmbeddingSize, false, gruHiddenSize));
				System.out.println("Use Relation CNN");
			}else if (neuralType.equals("continuous0")) {
				NetworkConfig.USE_FEATURE_VALUE = true;
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
				Metric result = EntityRelationEval.eval(t);
				System.out.println("Eval on dev:\n" + result);
				return result;
			}
			
		};
		
		
		EntityRelationGlobal.clearTemporalData();
		//model.visualize(visualizerClass, trainInstances);
		if (!SKIP_TRAIN)
		{
//			if (EntityRelationGlobal.INIT_WEIGHT_FROM_OTHERS != null && !EntityRelationGlobal.INIT_WEIGHT_FROM_OTHERS.equals("none")) {
//				initModelfromMH(model, EntityRelationGlobal.INIT_WEIGHT_FROM_OTHERS, trainInstances);  //"mentionHypergraph.model"
//			}
			
			model.train(trainInstances, num_iter_recount, devInstances, evalFunc, eval_freq_recount);
			
			if (!neuralType.equals("continuous*")) 
			{
				saveModel(model, gnp, model_file);
			}
			
			if (visual) model.visualize(visualizerClass, trainInstances);
		}
		
		
		
		
		if (!SKIP_TEST)
		{
			if (SKIP_TRAIN)
			{
				EntityRelationGlobal.INIT_WEIGHT_FROM_OTHERS = null;
				model = loadModel(model, model_file, trainInstances);
			}
			
			
			
			stats.append("Best Dev Result is at iter=" + NetworkConfig.BEST_ITER_DEV + "\n");
			Instance[] predictions;

			org.statnlp.commons.Utils.DEBUG = true;
			
			
			/*
			Double oldMP = setMP(model.getFeatureManager().getParam_G());
			
			ArrayList<Double> MPList = new ArrayList<Double>();
			MPList.add(oldMP);
			
			
			if (EntityRelationGlobal.ENTITY_MP != null) {
				for(int i = 0; i < 10; i++) {
					MPList.add(oldMP + i * 0.2);
				}
			}*/
			
			
			
			if (TESTTRAIN) {
				System.out.println("Test on Train Data...");
				EntityRelationGlobal.clearTemporalData();
				trainInstances = EntityRelationReader.readData(dataSet, EntityRelationDataConfig.pathJoin(dataFilePath, train_file), true, false, TRIAL, false);
				predictions = model.test(trainInstances);
				stats.append(printStat(predictions, "Train ", summary_train));
				EntityRelationReader.writeResult(predictions, EntityRelationDataConfig.pathJoin(dataFilePath, train_file), result_file.replace(test_file, train_file));
				predictions = null;
			}
			
			trainInstances = null;


			
			if (TESTDEV) {
				EntityRelationGlobal.clearTemporalData();
				predictions = model.test(devInstances);
				predictions = EntityRelationReader.postprocess(EntityRelationDataConfig.pathJoin(dataFilePath, dev_file), predictions);
				stats.append(printStat(predictions, "Dev ", summary_dev));
				EntityRelationReader.writeResult(predictions, EntityRelationDataConfig.pathJoin(dataFilePath, dev_file), result_file.replace(test_file, dev_file));
				predictions = null;
			}
				
			devInstances = null;
				
			EntityRelationGlobal.clearTemporalData();
			predictions = model.test(testInstances);
			predictions = EntityRelationReader.postprocess(EntityRelationDataConfig.pathJoin(dataFilePath, test_file), predictions);
			stats.append(printStat(predictions, "Test ", summary_test));
			EntityRelationReader.writeResult(predictions, EntityRelationDataConfig.pathJoin(dataFilePath,test_file), result_file); 
				
			stats.append("\n");

			/*
			if (visual) {
				for(int k = 0; k <predictions.length; k++) {
					EntityRelationInstance inst = (EntityRelationInstance)predictions[k];
					inst.setPredictionAsOutput();
					inst.setLabeled();
				}
				model.visualize(visualizerClass, predictions);
			}*/
			
			

		}
		
		nn.closeNNConnections();
	
		
		System.out.println(stats.toString());
		
		Utils.writeLog(train_file + ".log", stats.toString());
		
	}
	
	
	public static Double setMP(GlobalNetworkParam gnp) {
		
		if (EntityRelationGlobal.ENTITY_MP != null) {
			double newWeight = EntityRelationGlobal.ENTITY_MP;
			return setMP( gnp,  FeatureType.MENTION_PENALTY.name(), newWeight);
		}
		
		return null;
		/*
		if (EntityRelationGlobal.SCOPE_MP != null) {
			double newWeight = EntityRelationGlobal.SCOPE_MP;
			setMP( fm,  FEATURE_TYPES.MP + "Scope", newWeight);
		}*/
		
	}
	
	public static Double setMP(GlobalNetworkParam gnp, double newWeight) {		
		return setMP( gnp,  FeatureType.MENTION_PENALTY.name(), newWeight);
	}
	
	public static double setMP(GlobalNetworkParam gnp, String featureName, double W) {
		int MentionPenaltyFeatureIndex = gnp.getFeatureId(featureName, "MP", "MP");
		double MPWeight = gnp.getWeight(MentionPenaltyFeatureIndex);
		gnp.setWeight(MentionPenaltyFeatureIndex, W);
		System.out.println( featureName + " MPWeight: from " + MPWeight + " to " + W);
		return MPWeight;
	}
	
	public static void runNormalDataset() throws IOException, InterruptedException {
		
		summary_train = new EntityRelationMetric(true);
		summary_dev = new EntityRelationMetric(true);
		summary_test = new EntityRelationMetric(true);
		
		String[] dataFiles = EntityRelationDataConfig.getDataFiles(dataSet, lang).clone();
		String dataPath = EntityRelationDataConfig.getDataPath(dataSet, lang);
		runModel(dataFiles, dataSet,dataPath , out_path);

	}
	
	public static void runFolds(String[] fold) throws InterruptedException, IOException {
		
		//begin_index = 0;
		//end_index = 9;
		TESTTRAIN = false;
		TESTDEV = false;
		
		summary_train = new EntityRelationMetric(true);
		summary_dev = new EntityRelationMetric(true);
		summary_test = new EntityRelationMetric(true);
		
		
		for(int i = 0; i < fold.length; i++)
		{	
			String[] dataFiles = EntityRelationDataConfig.getDataFiles(dataSet, lang).clone();
			for(int j = 0; j < dataFiles.length; j++)
				dataFiles[j] = dataFiles[j].replace("[*]", fold[i]);
			FOLDIDX = i;
			runModel(dataFiles, dataSet, EntityRelationDataConfig.getDataPath(dataSet, lang), out_path);
			
			if (dataSet.startsWith("cnesp") ) {
				SKIP_TRAIN = true;
				TESTDEV = false;
			}
		}
		FOLDIDX = -1;
		
		if (TESTTRAIN) {
			System.out.println("Summary Train");
			System.out.println(summary_train.compute());
		}
		
		if (TESTDEV) {
			System.out.println("Summary Dev");
			System.out.println(summary_dev.compute());
		}
		
		System.out.println("Summary Test");
		System.out.println(summary_test.compute());
		
	}
	
	public static String printStat(Instance[] predictions, String expname, EntityRelationMetric summary) {
		StringBuffer sb = new StringBuffer(expname + "\n");

		Metric metric = EntityRelationEval.eval(predictions);
		sb.append(metric.toString() + "\n");
		
		summary.aggregate((EntityRelationMetric)metric);

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
					case "-beginindex": begin_index = Integer.valueOf(args[i+1]); break;    //default:all
					case "-endindex": end_index = Integer.valueOf(args[i+1]); break;   //default:100;
					case "-lang": lang = args[i+1]; break;
					case "-mentionpenalty" : mentionpenalty = Boolean.getBoolean(args[i+1]); break;
					case "-subpath" : subpath = args[i+1]; break;
					case "-NEMaxLength": NEMaxLength = Integer.valueOf(args[i+1]); break;
					case "-thread": numThreads = Integer.valueOf(args[i+1]); break;   //default:5
					case "-emb" : embedding = args[i+1]; break;
					case "-gpuid": gpuId = Integer.valueOf(args[i+1]); break;
					case "-fixemb" : EntityRelationGlobal.FIX_EMBEDDING = Boolean.parseBoolean(args[i+1]); break;
					
					
					case "-dumpfeature" : EntityRelationGlobal.DUMP_FEATURE = Boolean.parseBoolean(args[i+1]); break;
					case "-visual" : visual = Boolean.parseBoolean(args[i+1]); break;
					case "-visualID" : VisualizerFrame.INSTANCE_INIT_ID = Integer.valueOf(args[i+1]); break;
					case "-skiptest" : SKIP_TEST = Boolean.parseBoolean(args[i+1]); break;
					case "-skiptrain" : SKIP_TRAIN = Boolean.parseBoolean(args[i+1]); break;
					//case "-windows":ECRFEval.windows = true; break;            //default: false (is using windows system to run the evaluation script)
					//case "-batch": NetworkConfig.USE_BATCH_TRAINING = true;
					//				batchSize = Integer.valueOf(args[i+1]); break;
					//case "-model": NetworkConfig.MODEL_TYPE = args[i+1].equals("crf")? ModelType.CRF:ModelType.SSVM;   break;
					case "-neural": if(args[i+1].equals("cnn") || args[i+1].startsWith("lstm")|| args[i+1].startsWith("continuous")){ 
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
					case "-lr":
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
					case "-ngram" : EntityRelationGlobal.NGRAM = Boolean.parseBoolean(args[i+1]); break;
					case "-hidesink" : VisualizeGraph.hideEdgetoSink = Boolean.parseBoolean(args[i+1]); break;
					case "-embsize": embeddingSize = Integer.valueOf(args[i+1]); break;    //default:all
					case "-charhiddensize": charHiddenSize = Integer.valueOf(args[i+1]); break;    //default:all
					case "-evalfreq": evalFreq = Integer.valueOf(args[i+1]); break;  
					case "-batch": batchSize = Integer.valueOf(args[i+1]); 
							if (batchSize > 0)
								NetworkConfig.USE_BATCH_TRAINING = true;
							break;
					case "-testbatch": test_batchSize = Integer.valueOf(args[i+1]); break;
					case "-label": numLabel = Integer.valueOf(args[i+1]); break;
					case "-numfilter": numFilter = Integer.valueOf(args[i+1]); break;
					case "-cnnwindowSize": cnnWindowSize = Integer.valueOf(args[i+1]); break;
					case "-dataset": dataSet = args[i+1]; EntityRelationGlobal.dataSet = dataSet; break;
					
					case "-trial" : TRIAL = Integer.valueOf(args[i+1]); break;
					case "-demo" : boolean demo = Boolean.parseBoolean(args[i + 1]); 
								if (demo) {
									for(String dataset : EntityRelationDataConfig.dataset2Files.keySet()) {
										EntityRelationDataConfig.dataset2Files.put(dataset, new String[] { "trial.data", "trial.data","trial.data" }); 
									}
								}
								break;
					
					//case "-posemb" : EntityRelationGlobal.USE_POSITION_EMBEDDEING = Boolean.parseBoolean(args[i+1]); break;
					case "-discrete" : EntityRelationGlobal.ENABLE_DISCRETE_FEATURE = Boolean.parseBoolean(args[i+1]); break;
					case "-evaltest" : EVALTEST = Boolean.parseBoolean(args[i+1]); break;
					case "-debug" : EntityRelationGlobal.DEBUG = Boolean.parseBoolean(args[i+1]); break;
					
					case "-testrain" : TESTTRAIN = Boolean.parseBoolean(args[i+1]); break;
					case "-testdev" : TESTDEV = Boolean.parseBoolean(args[i+1]); break;
					case "-selfrelation" : EntityRelationGlobal.ADD_SELF_RELATION = Boolean.parseBoolean(args[i+1]); break;
					case "-norelation" : EntityRelationGlobal.ADD_NO_RELATION = Boolean.parseBoolean(args[i+1]); break;
					case "-dummy" : EntityRelationGlobal.DUMMY_FEATURE = Boolean.parseBoolean(args[i+1]); break;
					case "-outputerror" : EntityRelationGlobal.OUTPUT_ERROR = Boolean.parseBoolean(args[i+1]); break;
					case "-beston" : EntityRelationMetric.BESTON = EntityRelationMetric.EVALOPTION.valueOf(args[i+1]); break;
					
					case "-modeltype" : if (args[i+1].equals("softmaxmargin")) {
											NetworkConfig.MODEL_TYPE = ModelType.SOFTMAX_MARGIN;
											System.out.println("Model Type:" + NetworkConfig.MODEL_TYPE);
										} else if (args[i+1].equals("ssvm")) {
											NetworkConfig.MODEL_TYPE = ModelType.SSVM;
											System.out.println("Model Type:" + NetworkConfig.MODEL_TYPE);
										} else if (args[i+1].equals("crf")) {
											NetworkConfig.MODEL_TYPE = ModelType.CRF;
											System.out.println("Model Type:" + NetworkConfig.MODEL_TYPE);
										}
										break;
					case "-recallbeta": 
						String[] tmp = args[i+1].split(",");
						for(int k = 0; k < tmp.length; k++) {
							EntityRelationGlobal.RECALL_BETA[k] = Double.valueOf(tmp[k]);  
						}
						break;
					case "-llimit": EntityRelationGlobal.L_SPAN_MAX_LIMIT = Integer.valueOf(args[i+1]); break;
					case "-rdistlimit": 
						String[] fields = args[i+1].split(",");
						EntityRelationGlobal.RELATION_ENTITY_DISTANCE_MAX_LIMIT[0] = Integer.valueOf(fields[0]); 
						EntityRelationGlobal.RELATION_ENTITY_DISTANCE_MAX_LIMIT[1] = Integer.valueOf(fields[1]); 
						break;
					case "-entityorder" : EntityRelationGlobal.ENABLE_ENTITY_ORDER_CONSTRAINT = Boolean.parseBoolean(args[i+1]); break;
					case "-entitydist" : EntityRelationGlobal.ENABLE_ENTITY_DIST_CONSTRAINT = Boolean.parseBoolean(args[i+1]); break;
					case "-onlyself" : 
						EntityRelationGlobal.ONLY_SELF_RELATION = Boolean.parseBoolean(args[i+1]); 
						if (EntityRelationGlobal.ONLY_SELF_RELATION) {
							EntityRelationGlobal.ADD_SELF_RELATION = true;
						}
 						break;
					case "-fixentitypair" : 
						EntityRelationGlobal.FIX_ENTITY_PAIR = Boolean.parseBoolean(args[i+1]); 
						if (EntityRelationGlobal.FIX_ENTITY ) {
							EntityRelationGlobal.ADD_SELF_RELATION = false;
						}
						break;
					case "-fixentity" : 
						EntityRelationGlobal.FIX_ENTITY = Boolean.parseBoolean(args[i+1]);
						if (EntityRelationGlobal.FIX_ENTITY ) {
							EntityRelationGlobal.ADD_SELF_RELATION = false;
						}
						//EntityRelationGlobal.ADD_NO_RELATION = false;
						break;
					case "-mp" : 
						EntityRelationGlobal.ENTITY_MP = Double.parseDouble(args[i+1]); 
						break;
					case "-usehead" : EntityRelationGlobal.HEAD_AS_SPAN = Boolean.parseBoolean(args[i+1]); break;
					case "-savelight" : 
						EntityRelationGlobal.SAVE_LIGHT_MODEL = Boolean.parseBoolean(args[i+1]); 
						break;
					case "-depf" : EntityRelationGlobal.DEP_FEATURE = Boolean.parseBoolean(args[i+1]); break;
					case "-relf" : EntityRelationGlobal.REL_FEATURE = Boolean.parseBoolean(args[i+1]); break;
					case "-flip" : EntityRelationGlobal.FLIP = Boolean.parseBoolean(args[i+1]); break;
					case "-igeneral" : EntityRelationGlobal.I_GENERAL = Boolean.parseBoolean(args[i+1]); break;
					case "-initweight" : 
						if (args[i+1].equals("none")) {
							EntityRelationGlobal.INIT_WEIGHT_FROM_OTHERS = new String[0];
						} else if (args[i+1].equals("default")) {
							EntityRelationGlobal.INIT_WEIGHT_FROM_OTHERS = new String[] {"mentionHypergraph.model", "relation.model"};
							EntityRelationGlobal.INIT_WEIGHT_FROM_LIGHT_MODEL = false;
						}else if (args[i+1].equals("self")) {
							EntityRelationGlobal.INIT_WEIGHT_FROM_OTHERS = new String[] {"jointer.model"};
							EntityRelationGlobal.INIT_WEIGHT_FROM_LIGHT_MODEL = true;
						} else {
							EntityRelationGlobal.INIT_WEIGHT_FROM_OTHERS = args[i+1].split(","); 
						}	
						break;
					case "-buildfromlabel" : NetworkConfig.BUILD_FEATURES_FROM_LABELED_ONLY = Boolean.parseBoolean(args[i+1]); break;
					case "-pipelinespan" : 
							EntityRelationGlobal.PIPELINE_SPAN = args[i+1]; 
							if (EntityRelationGlobal.PIPELINE_SPAN.equals("none"))
								EntityRelationGlobal.PIPELINE_SPAN = null;
							break;
					case "-removeduplicate" : EntityRelationGlobal.REMOVE_DUPLICATE = Boolean.parseBoolean(args[i+1]); break;
					case "-norelationprob" : EntityRelationGlobal.NO_RELATION_PROB = Double.parseDouble(args[i+1]); break;
					case "-propr2i" : EntityRelationGlobal.PROPGATE_R_TO_I_NODE = Boolean.parseBoolean(args[i+1]); break;
					case "-ignoreanotheri" : EntityRelationGlobal.IGNORE_ANOTHER_I = Boolean.parseBoolean(args[i+1]); break;
					case "-expandwithnorelation" : 
							EntityRelationGlobal.EXPAND_WITH_NO_RELATION = Boolean.parseBoolean(args[i+1]); 
							EntityRelationGlobal.ADD_NO_RELATION = true;
							break;

					
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
		String modelpath = "org.entityrelation";
		EntityRelationFeatureManager fm = null;
		EntityRelationCompiler compiler = null;


		if (modelname.equals("Linear")) {
			fm = new org.entityrelation.linear.EntityRelationLinearFeatureManager(gnp, neuralType, EntityRelationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.entityrelation.linear.EntityRelationLinearCompiler();
		} else if (modelname.equals("Semi")) {
			fm = new org.entityrelation.semi.EntityRelationSemiFeatureManager(gnp, neuralType, EntityRelationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.entityrelation.semi.EntityRelationSemiCompiler();
		}  else if (modelname.equals("LR")) {
			fm = new org.entityrelation.logisticregression.EntityRelationLRFeatureManager(gnp, neuralType, EntityRelationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.entityrelation.logisticregression.EntityRelationLRCompiler();
		} else if (modelname.equals("LinearCRF")) {
			fm = new org.entityrelation.crf.EntityRelationLinearCRFeatureManager(gnp, neuralType, EntityRelationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.entityrelation.crf.EntityRelationLinearCRFCompiler();
		} else if (modelname.equals("SemiBT")) {
			fm = new org.entityrelation.semi.EntityRelationSemiBTFeatureManager(gnp, neuralType, EntityRelationGlobal.ENABLE_DISCRETE_FEATURE);
			compiler = new org.entityrelation.semi.EntityRelationSemiBTCompiler();
		} 
		
		
		fm.setCompiler(compiler);
		NetworkModel model = DiscriminativeNetworkModel.create(fm, compiler);

		return model;
	}
	
	public static Class<? extends VisualizationViewerEngine> getViewer(String modelname)
	{
		String visualModelPath = null;
		
		if (modelname.startsWith("Linear")) {
			visualModelPath = "linear.EntityRelationLinearViewer";
		} else if (modelname.startsWith("Semi")) {
			visualModelPath = "semi.EntityRelationSemiViewer";
		}
		
		if (visualModelPath == null)
			return null;
		
		String visualizerModelName = "org.entityrelation." + visualModelPath;
		Class<? extends VisualizationViewerEngine> visualizerClass = null;
		
		try {
			visualizerClass = (Class<VisualizationViewerEngine>) Class.forName(visualizerModelName);
		} catch (ClassNotFoundException e) {
			System.err.println("Class not found");
		}
		
		return visualizerClass;
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

		for(int featureTypeId: Utils.sorted(featureIntMap.keys())){ //sorted

		     //.println(featureType);

			 TIntObjectHashMap<TIntIntHashMap> outputInputMap = featureIntMap.get(featureTypeId);

		     for(int outputId: Utils.sorted(outputInputMap.keys())){ //sorted

		          //modelTextWriter.println("\t"+output);

		    	 TIntIntHashMap inputMap = outputInputMap.get(outputId);

		          for(int inputId: inputMap.keys()){

		               int featureId = inputMap.get(inputId);
		               
		               String featureType = string2Idx.get(featureTypeId);
		               String input = string2Idx.get(inputId);
		               String output = string2Idx.get(outputId);

		               modelTextWriter.println(featureType + " " + input+ " := " + output + " = "+param.getWeight(featureId));
		               if (EntityRelationGlobal.ECHO_FEATURE)
		            	   System.out.println(featureType + input+ ":= " + output + "="+param.getWeight(featureId));
		          
		          }

		     }
		     
		     modelTextWriter.flush();

		}

		modelTextWriter.close();
	}
	
	public static void saveModel(NetworkModel model, GlobalNetworkParam gnp, String filename_model ) throws IOException {
		
		
		System.out.println();
		System.err.println("Saving Model:" +  filename_model + " ...");
		ObjectOutputStream out;
		out = new ObjectOutputStream(new FileOutputStream(filename_model));
		
		
		if (EntityRelationGlobal.SAVE_LIGHT_MODEL) {
			out.writeObject(model.getFeatureManager().getParam_G().getWeights());
		} else {
		
			out.writeObject(model);
		}
//		out.writeObject(gnp);
		out.flush();
		out.close();
		System.err.println("Model Saved.");
		
		if (EntityRelationGlobal.DUMP_FEATURE)
			printFeature(gnp, filename_model);
		
	}

	public static NetworkModel loadModel(NetworkModel model, String filename_model, Instance[] trainInsts) throws IOException, InterruptedException {
		
		
		
		System.err.println("Loading Model...");
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename_model));
		try {
			if (EntityRelationGlobal.SAVE_LIGHT_MODEL) {
				double[] weights = (double[]) in.readObject();
				GlobalNetworkParam gnp = model.getFeatureManager().getParam_G();
				System.err.println("Re-touch on training data");
				model.train(trainInsts, -1);
				gnp.setWeights(weights);
			} else {
				model = (NetworkModel) in.readObject();
			}

		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		in.close();
			
		
		System.err.println("Model Loaded.");

		return model;		
	}
	
	public static void initModelfromMH(NetworkModel model, String filename_model, Instance[] trainInsts) throws FileNotFoundException, IOException {
		
		
		ObjectInputStream in = new ObjectInputStream(new FileInputStream(filename_model));
		try {
			
			
			NetworkModel MHmodel = (NetworkModel) in.readObject();
			GlobalNetworkParam MHgnp = MHmodel.getFeatureManager().getParam_G();
			//model.otherGNP = MHgnp;
			model.getFeatureManager().getParam_G().setStoreFeatureReps();
			System.err.println("MH Model Loaded for weight init. #weight:" + MHgnp.getWeights().length);
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		in.close();
		//return model;
	}
}
