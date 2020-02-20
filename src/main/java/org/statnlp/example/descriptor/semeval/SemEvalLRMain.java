package org.statnlp.example.descriptor.semeval;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.statnlp.commons.io.RAWF;
import org.statnlp.commons.ml.opt.GradientDescentOptimizer.BestParamCriteria;
import org.statnlp.commons.ml.opt.OptimizerFactory;
import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Sentence;
import org.statnlp.example.descriptor.RelInstance;
import org.statnlp.example.descriptor.RelationDescriptor;
import org.statnlp.example.descriptor.RelationDescriptorEvaluator;
import org.statnlp.example.descriptor.RelationType;
import org.statnlp.example.descriptor.Span;
import org.statnlp.example.descriptor.emb.GloveWordEmbedding;
import org.statnlp.example.descriptor.emb.GoogleWordEmbedding;
import org.statnlp.example.descriptor.emb.TurianWordEmbedding;
import org.statnlp.example.descriptor.emb.WordEmbedding;
import org.statnlp.hypergraph.DiscriminativeNetworkModel;
import org.statnlp.hypergraph.GlobalNetworkParam;
import org.statnlp.hypergraph.NetworkConfig;
import org.statnlp.hypergraph.NetworkConfig.StoppingCriteria;
import org.statnlp.hypergraph.NetworkModel;
import org.statnlp.hypergraph.StringIndex;
import org.statnlp.hypergraph.decoding.Metric;
import org.statnlp.hypergraph.neural.GlobalNeuralNetworkParam;
import org.statnlp.hypergraph.neural.NeuralNetworkCore;

import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;

public class SemEvalLRMain {

	public static double l2val = 0.0001;
	public static int numThreads = 8;
	public static int numIteration = 1000;
	public static String trainFile = "data/semeval/sd_parseTag_train.txt";
	public static String devFile = "data/semeval/sd_parseTag_test.txt";
	public static String testFile = "data/semeval/sd_parseTag_test.txt";
	public static int trainNum = -1;
	public static int devNum = -1;
	public static int testNum = -1;
	public static double portion = 0.9;
	public static String resultFile = "results/semeval_res.txt";
	public static boolean saveModel = false;
	public static boolean readModel = false;
	public static String modelFile = "models/semeval_model.m";
	public static String nnModelFile = "models/lstm.m";
	public static int embeddingDimension = 300;
	public static int gpuId = -1;
	public static String embedding = "random";
	public static OptimizerFactory optimizer = OptimizerFactory.getLBFGSFactory();
	public static boolean revAsDifferentLabel = true;
	public static boolean evalDev = false;
	public static int evalK = 10;
	public static boolean readPreLabels = true;
	public static String preLabelsFile = "data/semeval/prevlabels.txt";
	public static boolean enableDescriptorArgRel = true;
	public static boolean descriptorBetArgs = true;
	public static boolean descriptorCoverArgs = false;
	public static boolean useGeneralTags = false;
	public static boolean dependencyFeatures = false;
	public static boolean useRelDiscreteFeatures = false;
	public static boolean useSimpleRelFeatures = false;
	public static boolean descriptorFeatures = false;
	public static boolean surroundingWords = false;
	public static boolean usePositionEmbeddings = false;
	public static int posEmbeddingSize = 25;
	public static boolean toLowercase = false;
	public static boolean zero_digit = false;
	public static boolean nnDescriptorFeature = false;
	public static boolean bilinear = false;
	public static int gruHiddenSize = 50;
	public static boolean useHeadWord = false;
	
	public static NeuralType nnStruct = NeuralType.continuous; //lstm
	public static String googleEmbeddingFile = "nn-crf-interface/neural_server/google/GoogleNews-vectors-negative300.txt";
//	public static String googleEmbeddingFile = "nn-crf-interface/neural_server/google/googleexample.txt";
	public static String googleEmbeddingRetrofitFile = "nn-crf-interface/neural_server/google/GoogleNews-vectors-negative300-retrofitted.txt";
	public static String gloveEmbeddingFile = "/Users/Leo/Documents/workspace/statnlp-lihao/models/glove.6B.300d.txt";
	public static String turianEmbeddingFile = "nn-crf-interface/neural_server/turian/turian_embeddings.50.txt";
	public static String gloveEmbeddingRetrofitFile = "/home/allan/data/embeddings/glove.6B.300d-retrofitted.txt";
//	public static String gloveEmbeddingFile = "nn-crf-interface/neural_server/google/gloveExample.txt";
	public static String testVocabFile = "data/semeval/sd_parseTag_test_vocab.txt";
	public static boolean useRetrofitVec = false;
	public static int hiddenSize = 128;
	public static int layer2HiddenSize = 128;
	public static int cnnWindowSize = 3;
	public static int epochLim = 3;
	public static double dropout;
	public static boolean randomBatch = false;
	public static boolean fixEmbedding = true;
	public static boolean wordNetFeat = false;
	public static boolean unkDescriptorAsSent = false;
	public static boolean tanhGRU = false;
	public static double gruDropout = 0.0;
	public static boolean useBetSentOnly = false;
	public static boolean positionIndicator = false;
	public static double embDropout = 0.0;
	public static boolean add = true;
	public static boolean attn = false;
	public static boolean twoLayerGRU = false;
	public static int lrbound = -1;
	public static boolean desEmb = false;
	
	public static boolean saveFeatWeights = false;
	public static String weightFile = "models/feat_weights.m";
	public static boolean useSpanPenalty = false;
	public static double penalty = 1.0;
	public static int maxSpanLen = -1;
	
	public static enum NeuralType {
		continuous,
		cnn,
		cnn_batch_one,
		gru,
		mlp, 
		lstm,
		rnnpool
	}
	
	/**
	 * Reading the label list
	 * @param file
	 */
	private static void readPreLabels(String file) {
		BufferedReader br;
		try {
			br = RAWF.reader(file);
			String line = null; 
			while ((line = br.readLine()) != null) {
				RelationType.get(line);
			}
			br.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws InterruptedException, IOException, ClassNotFoundException {
		setArgs(args);
		
		/***
		 * Parameter settings and model configuration
		 */
		NetworkConfig.L2_REGULARIZATION_CONSTANT = l2val;
		NetworkConfig.NUM_THREADS = numThreads;
		NetworkConfig.AVOID_DUPLICATE_FEATURES = true;
		NetworkConfig.OPTIMIZE_NEURAL = true;
		NetworkConfig.PRINT_BATCH_OBJECTIVE = false;
		NetworkConfig.USE_FEATURE_VALUE = true;
		if (readPreLabels) {
			readPreLabels(preLabelsFile);
		}
		SemEvalDataReader reader = new SemEvalDataReader();

		/**debug info**/
//		saveFeatWeights = false;
//		NetworkConfig.LOAD_EXTERNAL_WEIGHT = true;
		/***/
		
		Instance[] trainData = reader.read(trainFile, true, trainNum);
//		SemEvalStat.countRelation(trainData);
		reader.putInMaxLen(trainData);
		Instance[] devData = evalDev ?  reader.read(devFile, false, devNum) : null;
//		SemEvalStat.countRelation(devData);
//		System.exit(0);
		if (evalDev) {
			reader.putInMaxLen(devData);
		}
		
		Map<Integer, Integer> len2Num = new HashMap<>();
		for (Instance inst : trainData) {
			int len = inst.size();
			if (len2Num.containsKey(len)) {
				int num = len2Num.get(len);
				len2Num.put(len, num + 1);
			} else {
				len2Num.put(len, 1);
			}
		}
		if (devData != null) {
			for (Instance inst : devData) {
				int len = inst.size();
				if (len2Num.containsKey(len)) {
					int num = len2Num.get(len);
					len2Num.put(len, num + 1);
				} else {
					len2Num.put(len, 1);
				}
			}
		}
//		System.out.println("map size: " + len2Num.size());
//		System.out.println(len2Num.toString());
//		System.exit(0);
		
		System.out.println(RelationType.RELS.toString());
		System.out.println("#labels: " + RelationType.RELS.size());
		RelationDescriptorEvaluator evaluator = new RelationDescriptorEvaluator(true);
		NetworkModel model = null;
		if (readModel) {
			System.out.println("[Info] Reading Model....");
			ObjectInputStream in = RAWF.objectReader(modelFile);
			model = (NetworkModel)in.readObject();
			if (NetworkConfig.USE_NEURAL_FEATURES) {
				if (nnStruct == NeuralType.continuous) {
					SemEvalContinuousFeatureProvider nn = (SemEvalContinuousFeatureProvider)
							model.getFeatureManager().getParam_G().getNNParamG().getAllNets().get(0);
					WordEmbedding emb = obtainEmbedding();
					nn.emb = emb;
            	} 
			}
			WordEmbedding emb = null;
            if (desEmb) {
            	emb = obtainEmbedding();
            }
            SemEvalLRFeatureManager fm = (SemEvalLRFeatureManager)model.getFeatureManager();
            fm.emb = emb;
		} else {
			List<NeuralNetworkCore> nets = new ArrayList<>();
            if (NetworkConfig.USE_NEURAL_FEATURES) {
            	int numLabels = RelationType.RELS.size();
            	if (nnStruct == NeuralType.continuous) {
            		WordEmbedding emb = obtainEmbedding();
            		int numFeatureValues = nnDescriptorFeature ? 3 * embeddingDimension : 2 * embeddingDimension;
            		numFeatureValues += surroundingWords ? 4 * embeddingDimension :0;
            		nets.add(new SemEvalContinuousFeatureProvider(numFeatureValues, hiddenSize, layer2HiddenSize, dropout, numLabels, emb)
            				.setModelFile(nnModelFile));
            	} else if (nnStruct == NeuralType.cnn) {
            		nets.add(new RelationCNN("RelationCNN",numLabels, hiddenSize, embedding, gpuId, 
            				cnnWindowSize, embeddingDimension, layer2HiddenSize, dropout, usePositionEmbeddings, posEmbeddingSize, nnDescriptorFeature,
            				gruHiddenSize)
            				.setModelFile(nnModelFile));
            	} else if (nnStruct == NeuralType.cnn_batch_one) {
            		nets.add(new RelationCNN("RelationCNNBatchOne", numLabels, hiddenSize, embedding, gpuId, 
            				cnnWindowSize, embeddingDimension, layer2HiddenSize, dropout, usePositionEmbeddings, posEmbeddingSize, nnDescriptorFeature,
            				gruHiddenSize)
            				.setModelFile(nnModelFile));
            	} else if (nnStruct == NeuralType.gru || nnStruct == NeuralType.lstm){
            		nets.add(new RelationGRU("RelationGRU", numLabels, hiddenSize, embedding, gpuId, 
            				embeddingDimension, layer2HiddenSize, dropout, nnDescriptorFeature,
            				gruHiddenSize, fixEmbedding, testVocabFile, nnStruct.name(), useHeadWord,
            				tanhGRU, gruDropout, positionIndicator, embDropout, add, twoLayerGRU, bilinear).setModelFile(nnModelFile));
            	}	else if (nnStruct == NeuralType.rnnpool){
            		nets.add(new RNNPool("RNNPool", numLabels, hiddenSize, embedding, gpuId, 
            				embeddingDimension, layer2HiddenSize, dropout, nnDescriptorFeature,
            				gruHiddenSize, fixEmbedding, testVocabFile, nnStruct.name(), useHeadWord,
            				tanhGRU, gruDropout, positionIndicator, embDropout, add, attn).setModelFile(nnModelFile));
            	} else if (nnStruct == NeuralType.mlp){
            		nets.add(new RelationMLP("MLP", numLabels, hiddenSize, embedding, gpuId, 
            				embeddingDimension, layer2HiddenSize, dropout, nnDescriptorFeature,
            				gruHiddenSize, fixEmbedding, testVocabFile, bilinear).setModelFile(nnModelFile));
            	}
            }
            GlobalNeuralNetworkParam gnnp = new GlobalNeuralNetworkParam(nets);
            GlobalNetworkParam gnp = new GlobalNetworkParam(optimizer, gnnp);
            gnp.setStoreFeatureReps();
            WordEmbedding emb = null;
            if (desEmb) {
            	emb = obtainEmbedding();
            }
			SemEvalLRFeatureManager tfm = new SemEvalLRFeatureManager(gnp, useRelDiscreteFeatures, useGeneralTags, 
					dependencyFeatures, nnStruct, usePositionEmbeddings, toLowercase, zero_digit, descriptorFeatures, nnDescriptorFeature,
					surroundingWords, useSimpleRelFeatures, wordNetFeat, unkDescriptorAsSent, useHeadWord, useBetSentOnly, positionIndicator, lrbound > 0, emb);
			SemEvalLRNetworkCompiler tnc = new SemEvalLRNetworkCompiler(enableDescriptorArgRel, descriptorBetArgs, descriptorCoverArgs, lrbound, maxSpanLen);
			model = DiscriminativeNetworkModel.create(tfm, tnc);
			java.util.function.Function<Instance[], Metric> evalFunction = new java.util.function.Function<Instance[], Metric>() {
				@Override
				public Metric apply(Instance[] t) {
					return evaluator.evaluateRelation(t);
				}
			};
//			if (NetworkConfig.LOAD_EXTERNAL_WEIGHT) {
//				System.out.println("[Info] Settings the external weight file to: " + weightFile);
//				gnp.setExternalWeightFile(weightFile);
//				if (NetworkConfig.FIX_FEATURE_WEIGHT) System.out.println("[Info] the feature weights are fixed");
//			}
			model.train(trainData, numIteration, devData, evalFunction, evalK);//, epochLim);
		}
		if (saveModel) {
			ObjectOutputStream out = RAWF.objectWriter(modelFile);
			out.writeObject(model);
			out.close();
		}
		
		if (saveFeatWeights) {
			System.out.println("[Info] Saving the feature weights.");
			ObjectOutputStream out = RAWF.objectWriter(weightFile);
			out.writeObject(model.getFeatureManager().getParam_G().getWeights());
			out.close();
		}
		
		/***Tuning the Feature Weights***/
		if (readModel && useSpanPenalty) {
			GlobalNetworkParam gnp = model.getFeatureManager().getParam_G();
			StringIndex strIdx = gnp.getStringIndex();
			strIdx.buildReverseIndex();
			gnp.setStoreFeatureReps();
			for (int i = 0; i < gnp.size(); i++) {
				int[] fs = gnp.getFeatureRep(i);
				String type = strIdx.get(fs[0]);
				if (type.equals("spanLen")) {
					System.out.println(gnp.getWeight(i));
					gnp.overRideWeight(i, penalty);
					System.out.println(gnp.getWeight(i));
					break;
				}
			}
		}
		/*************/
		
		/**
		 * Testing Phase
		 */
		Instance[] testData = reader.read(testFile, false, testNum);
		reader.putInMaxLen(testData);
		Instance[] results = model.test(testData);
		
		evaluator.evaluateRelation(results);
		
		//print the results
		PrintWriter pw = RAWF.writer(resultFile);
		for (Instance res : results) {
			RelInstance inst = (RelInstance)res;
			Sentence sent = inst.getInput().sent;
			List<Span> spans = inst.getInput().spans;
			Span arg1Span = spans.get(inst.getInput().leftSpanIdx);
			Span arg2Span = spans.get(inst.getInput().rightSpanIdx);
			RelationDescriptor gold = inst.getOutput();
			RelationDescriptor pred = inst.getPrediction();
			String goldForm = gold.getType().form;
			String predForm = pred.getType().form;
			pw.println(sent.toString());
			pw.println(arg1Span.start+","+arg1Span.end + " " + arg2Span.start+","+arg2Span.end + " "
					+ gold.getLeft() + "," + gold.getRight() + " " + pred.getLeft() + "," + pred.getRight());
			pw.println(goldForm + "," + predForm);
			pw.println();
		}
		pw.close();
		
		//Write out scoring results
		/**** Verified. Our evaluation is same as the scorer.
		PrintWriter pw1 = RAWF.writer("results/proposed.txt");
		PrintWriter pw2 = RAWF.writer("results/key.txt");
		for (Instance res : results) {
			RelInstance inst = (RelInstance)res;
			RelationDescriptor gold = inst.getOutput();
			RelationDescriptor pred = inst.getPrediction();
			String goldForm = gold.getType().form.endsWith("-rev") ? gold.getType().form.replace("-rev", "(e2,e1)") : 
				gold.getType().form.equals("Other") ? gold.getType().form : gold.getType().form + "(e1,e2)";
			String predForm = pred.getType().form.endsWith("-rev") ? pred.getType().form.replace("-rev", "(e2,e1)") : 
				pred.getType().form.equals("Other") ? pred.getType().form: pred.getType().form + "(e1,e2)";
			pw1.println(inst.getInstanceId() + "\t" + predForm);
			pw2.println(inst.getInstanceId() + "\t" + goldForm);
		}
		pw1.close();
		pw2.close();
		**/
	}
	
	private static WordEmbedding obtainEmbedding() {
		if (embedding.equals("google")) {
			embeddingDimension = 300;
			return new GoogleWordEmbedding(useRetrofitVec ? googleEmbeddingRetrofitFile :  googleEmbeddingFile);
		} else if (embedding.equals("glove")) {
			return new GloveWordEmbedding(useRetrofitVec ? gloveEmbeddingRetrofitFile : gloveEmbeddingFile);
		} else if (embedding.equals("turian")) {
			embeddingDimension = 50;
			return new TurianWordEmbedding(turianEmbeddingFile);
		} else {
			throw new RuntimeException("unknown word embedding type: " + embedding);
		}
	}
	
	private static void setArgs(String[] args) {
		ArgumentParser parser = ArgumentParsers.newArgumentParser("")
				.defaultHelp(true).description("Latent Linear-chain CRF Model for Relation Extraction");
		parser.addArgument("-t", "--thread").type(Integer.class).setDefault(8).help("number of threads");
		parser.addArgument("--l2").type(Double.class).setDefault(l2val).help("L2 Regularization");
		parser.addArgument("--iter").type(Integer.class).setDefault(numIteration).help("The number of iteration.");
		parser.addArgument("--trainFile").setDefault(trainFile).help("The path of the traininigFile file");
		parser.addArgument("--testFile").setDefault(testFile).help("The path of the testFile file");
		parser.addArgument("--trainNum").type(Integer.class).setDefault(trainNum).help("The number of training instances");
		parser.addArgument("--devNum").type(Integer.class).setDefault(devNum).help("The number of development set instances");
		parser.addArgument("--testNum").type(Integer.class).setDefault(testNum).help("The number of testing instances");
		parser.addArgument("--portion").setDefault(portion).help("The portion of data to be used for training");
		parser.addArgument("--saveModel").type(Boolean.class).setDefault(saveModel).help("whether to save the model");
		parser.addArgument("--readModel").type(Boolean.class).setDefault(readModel).help("whether to read the model");
		parser.addArgument("--modelFile").setDefault(modelFile).help("specify the model file");
		parser.addArgument("--nnModel").type(String.class).setDefault(nnModelFile).help("specify the neural net model file");
		parser.addArgument("--resFile").setDefault(resultFile).help("specify the result file");
		parser.addArgument("--gpuid").type(Integer.class).setDefault(gpuId).help("The gpuId to be used, less than 0 means cpu");
		parser.addArgument("--embedding").setDefault(embedding).help("specify the embedding to be used");
		parser.addArgument("--embeddingDimension").type(Integer.class).setDefault(embeddingDimension).help("The dimension of the embeddings.");
		parser.addArgument("--neural").type(Boolean.class).setDefault(NetworkConfig.USE_NEURAL_FEATURES).help("Use neural network features");
		parser.addArgument("--regNeural").type(Boolean.class).setDefault(NetworkConfig.REGULARIZE_NEURAL_FEATURES).help("regularize the neural network features");
		parser.addArgument("--os").setDefault(NetworkConfig.OS).help("linux, osx");
		parser.addArgument("--useBatch").type(Boolean.class).setDefault(NetworkConfig.USE_BATCH_TRAINING).help("whether to use batch training");
		parser.addArgument("--randomBatch").type(Boolean.class).setDefault(NetworkConfig.RANDOM_BATCH).help("whether to use random batch");
		parser.addArgument("--batchSize").type(Integer.class).setDefault(NetworkConfig.BATCH_SIZE).help("size of batch");
		parser.addArgument("--optimizer").nargs(2).setDefault(new Object[]{"lbfgs", 0}).help("optimizer, can be sgd 0.05, adam 0.05");
		parser.addArgument("--revLabel").type(Boolean.class).setDefault(revAsDifferentLabel).help("use the reverse direction of features as different label");
		parser.addArgument("--evalDev").type(Boolean.class).setDefault(evalDev).help("evaluate on the development set");
		parser.addArgument("--evalK").type(Integer.class).setDefault(evalK).help("number of iterations each time to evaluate the development set");
		parser.addArgument("--enableda").type(Boolean.class).setDefault(enableDescriptorArgRel).help("enable the relationship between descriptor and args");
		parser.addArgument("--dabet").type(Boolean.class).setDefault(descriptorBetArgs).help("descriptor between arguments");
		parser.addArgument("--dacov").type(Boolean.class).setDefault(descriptorCoverArgs).help("descriptor cover args");
		parser.addArgument("--gtags").type(Boolean.class).setDefault(useGeneralTags).help("use the general tags");
		parser.addArgument("--nn").type(NeuralType.class).setDefault(nnStruct).help("continuous, lstm");
		parser.addArgument("--retrofit").type(Boolean.class).setDefault(useRetrofitVec).help("make the unknown descriptor embedding as 0");
		parser.addArgument("--hidden").type(Integer.class).setDefault(hiddenSize).help("The hidden size of the mlp/cnn/lstm layer");
		parser.addArgument("--dropout").type(Double.class).setDefault(dropout).help("The dropout rate of the mlp layer");
		parser.addArgument("-depf","--dependencyFeature").type(Boolean.class).setDefault(dependencyFeatures).help("add the dependency features");
		parser.addArgument("-reldiscrete","--useRelDiscreteFeat").type(Boolean.class).setDefault(useRelDiscreteFeatures).help("use discrete features or not.");
		parser.addArgument("-simplerel","--useSimpleRel").type(Boolean.class).setDefault(useSimpleRelFeatures).help("use simple discrete features or not. (zeng 2014)");
		parser.addArgument("-ws", "--windowSize").type(Integer.class).setDefault(cnnWindowSize).help("The window size of cnn");
		parser.addArgument("-pe","--usePosEmbedding").nargs(2).setDefault(new Object[]{usePositionEmbeddings, posEmbeddingSize}).help("use position embeddings in cnn or not.");
		parser.addArgument("--secondHidden").type(Integer.class).setDefault(layer2HiddenSize).help("The hidden size of the another hidden layer");
		parser.addArgument("-lc","--lowercase").type(Boolean.class).setDefault(toLowercase).help("make the input to neural net lower case");
		parser.addArgument("-zd","--zerodigit").type(Boolean.class).setDefault(zero_digit).help("replace all digits with 0");
		parser.addArgument("--descritorFeat").type(Boolean.class).setDefault(descriptorFeatures).help("use discrete features for the descriptor or not.");
		parser.addArgument("-nndf", "--nnDescriptorFeat").type(Boolean.class).setDefault(nnDescriptorFeature).help("use continuous features for the descriptor or not.");
		parser.addArgument("-gh","--gruHiddenSize").type(Integer.class).setDefault(gruHiddenSize).help("The hidden size of Gated Recurrent Unit");
		parser.addArgument("-sw","--surroundWords").type(Boolean.class).setDefault(surroundingWords).help("Add surrounding words as features to continuous as well");
		parser.addArgument("-fe","--fixEmbedding").type(Boolean.class).setDefault(fixEmbedding).help("fixEmbeddings or not");
		parser.addArgument("-wnf", "--wordnetfeat").type(Boolean.class).setDefault(wordNetFeat).help("Wordnet features");
		parser.addArgument("-unkds", "--unkdessent").type(Boolean.class).setDefault(unkDescriptorAsSent).help("Wordnet features");
		parser.addArgument("--epochLim").type(Integer.class).setDefault(epochLim).help("The limit of the epoch to evaluate");
		parser.addArgument("-hw", "--headword").type(Boolean.class).setDefault(useHeadWord).help("Whether to use head word in MLP/continuous features");
		parser.addArgument("--tanhGRU").type(Boolean.class).setDefault(tanhGRU).help("use one more hidden layer after the GRU");
		parser.addArgument("--gruDropout").type(Double.class).setDefault(gruDropout).help("The dropout rate for the GRU layer");
		parser.addArgument("--betSent").type(Boolean.class).setDefault(useBetSentOnly).help("Use the word between nomials only");
		parser.addArgument("-pi", "--positionIndicator").type(Boolean.class).setDefault(positionIndicator).help("Position Indicator");
		parser.addArgument("-ed", "--embDropout").type(Double.class).setDefault(embDropout).help("The dropout rate of embedding");
		parser.addArgument("--add").type(Boolean.class).setDefault(add).help("whether to add up the two hidden vector");
		parser.addArgument("--attn").type(Boolean.class).setDefault(attn).help("whether to use attention or max-pooling");
		parser.addArgument("--saveFeatWeights").type(Boolean.class).setDefault(saveFeatWeights).help("save the feature weights");
		parser.addArgument("--loadFeatWeights").type(Boolean.class).setDefault(NetworkConfig.LOAD_EXTERNAL_WEIGHT).help("load the external saved weights");
		parser.addArgument("--fixFeatWeights").type(Boolean.class).setDefault(NetworkConfig.FIX_FEATURE_WEIGHT).help("fix the feature weights");
		parser.addArgument("--twoGRU").type(Boolean.class).setDefault(twoLayerGRU).help("Stacked the GRU");
		parser.addArgument("--bilinear").type(Boolean.class).setDefault(bilinear).help("Adding the bilinear term");
		parser.addArgument("--lrbound").type(Integer.class).setDefault(lrbound).help("The left right bound");
		parser.addArgument("--desEmb").type(Boolean.class).setDefault(desEmb).help("use the descriptor average embeddings");
		parser.addArgument("--useSpanPenalty").type(Boolean.class).setDefault(useSpanPenalty).help("whether to use the span penalty during dev set");
		parser.addArgument("--penalty").type(Double.class).setDefault(penalty).help("penalty for the span len");
		parser.addArgument("-ml", "--maxSpanLen").type(Integer.class).setDefault(maxSpanLen).help("max descriptor length");
		Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }
        numThreads = ns.getInt("thread");
        l2val = ns.getDouble("l2");
        numIteration = ns.getInt("iter");
        trainFile = ns.getString("trainFile");
        testFile = ns.getString("testFile");
        trainNum = ns.getInt("trainNum");
        devNum = ns.getInt("devNum");
        testNum = ns.getInt("testNum");
        portion = Double.valueOf(ns.getString("portion"));
        saveModel = ns.getBoolean("saveModel");
        readModel = ns.getBoolean("readModel") ;
        modelFile = ns.getString("modelFile");
        nnModelFile = ns.getString("nnModel");
        resultFile = ns.getString("resFile");
        gpuId = ns.getInt("gpuid");
        embeddingDimension = ns.getInt("embeddingDimension");
        embedding = ns.getString("embedding");
        evalDev = ns.getBoolean("evalDev");
        evalK = ns.getInt("evalK");
        revAsDifferentLabel = ns.getBoolean("revLabel") ;
        enableDescriptorArgRel = ns.getBoolean("enableda") ;
        descriptorBetArgs = ns.getBoolean("dabet") ;
        descriptorCoverArgs = ns.getBoolean("dacov") ;
        useGeneralTags = ns.getBoolean("gtags");
        useRetrofitVec = ns.getBoolean("retrofit");
        nnStruct = NeuralType.valueOf(ns.getString("nn"));
        hiddenSize = ns.getInt("hidden");
        layer2HiddenSize = ns.getInt("secondHidden");
        cnnWindowSize = ns.getInt("windowSize");
        dropout = ns.getDouble("dropout");
        dependencyFeatures = ns.getBoolean("dependencyFeature");
        useRelDiscreteFeatures = ns.getBoolean("useRelDiscreteFeat");
        useSimpleRelFeatures = ns.getBoolean("useSimpleRel");
        descriptorFeatures = ns.getBoolean("descritorFeat");
        nnDescriptorFeature = ns.getBoolean("nnDescriptorFeat");
        gruHiddenSize = ns.getInt("gruHiddenSize");
        epochLim = ns.getInt("epochLim");
        tanhGRU = ns.getBoolean("tanhGRU");
        gruDropout = ns.getDouble("gruDropout");
        useBetSentOnly = ns.getBoolean("betSent");
        positionIndicator = ns.getBoolean("positionIndicator");
        add = ns.getBoolean("add");
        attn = ns.getBoolean("attn");
        saveFeatWeights = ns.getBoolean("saveFeatWeights");
        twoLayerGRU = ns.getBoolean("twoGRU");
        bilinear = ns.getBoolean("bilinear");
        lrbound = ns.getInt("lrbound");
        desEmb = ns.getBoolean("desEmb");
        NetworkConfig.LOAD_EXTERNAL_WEIGHT = ns.getBoolean("loadFeatWeights");
        NetworkConfig.FIX_FEATURE_WEIGHT = ns.getBoolean("fixFeatWeights");
        List<Object> posObj = ns.getList("usePosEmbedding");
        Object pos = posObj.get(0);
        if (pos instanceof String) {
        	usePositionEmbeddings = Boolean.valueOf((String)pos);
        } else if (pos instanceof Boolean) {
        	usePositionEmbeddings = (boolean)pos;
        }
        Object obj = posObj.get(1);
        if (obj instanceof String) {
        	posEmbeddingSize = Integer.valueOf((String)obj);
        } else if (obj instanceof Integer) {
        	posEmbeddingSize = (int)obj;
        }
        toLowercase = ns.getBoolean("lowercase");
        zero_digit = ns.getBoolean("zerodigit");
        surroundingWords = ns.getBoolean("surroundWords");
        fixEmbedding = ns.getBoolean("fixEmbedding");
        wordNetFeat = ns.getBoolean("wordnetfeat");
        unkDescriptorAsSent = ns.getBoolean("unkdessent");
        useHeadWord = ns.getBoolean("headword");
        useSpanPenalty = ns.getBoolean("useSpanPenalty");
        penalty = ns.getDouble("penalty");
        maxSpanLen = ns.getInt("maxSpanLen");
        NetworkConfig.USE_NEURAL_FEATURES = ns.getBoolean("neural") ;
        NetworkConfig.REGULARIZE_NEURAL_FEATURES = ns.getBoolean("regNeural");
        NetworkConfig.OS = ns.getString("os");
        NetworkConfig.USE_BATCH_TRAINING = ns.getBoolean("useBatch");
        NetworkConfig.BATCH_SIZE = ns.getInt("batchSize");
        NetworkConfig.RANDOM_BATCH = ns.getBoolean("randomBatch");
        List<Object> optimList = ns.getList("optimizer");
        String optim = (String)optimList.get(0);
        double learningRate = Double.valueOf(String.valueOf(optimList.get(1)));
        if (optim.equals("sgd-clip")) {
        	NetworkConfig.STOPPING_CRITERIA = StoppingCriteria.MAX_ITERATION_REACHED;
        	optimizer = OptimizerFactory.getGradientDescentFactoryUsingGradientClipping(BestParamCriteria.BEST_ON_DEV, learningRate, 5.0);
        } else if (optim.equals("sgd")) {
        	NetworkConfig.STOPPING_CRITERIA = StoppingCriteria.MAX_ITERATION_REACHED;
        	optimizer = OptimizerFactory.getGradientDescentFactory(learningRate);
        } else if (optim.equals("adagrad")) {
        	NetworkConfig.STOPPING_CRITERIA = StoppingCriteria.MAX_ITERATION_REACHED;
        	optimizer = OptimizerFactory.getGradientDescentFactoryUsingAdaGrad( learningRate);
        } else if (optim.equals("lbfgs")) {
        	optimizer = OptimizerFactory.getLBFGSFactory();
        } /*else if (optim.equals("adam")) {
        	NetworkConfig.STOPPING_CRITERIA = StoppingCriteria.MAX_ITERATION_REACHED;
        	optimizer = OptimizerFactory.getGradientDescentFactoryUsingAdaM(learningRate);
        } */else {
        	throw new RuntimeException("unknown optimizer: " + optim);
        }
        if (saveModel && readModel) {
        	throw new RuntimeException("save model and read model can't be both true");
        }
        System.err.println(ns.getAttrs().toString());
	}
}
