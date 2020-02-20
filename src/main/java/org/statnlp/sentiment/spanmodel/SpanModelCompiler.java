package org.statnlp.sentiment.spanmodel;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.example.base.BaseNetwork;
import org.statnlp.example.base.BaseNetwork.NetworkBuilder;
import org.statnlp.hypergraph.LocalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkIDMapper;
import org.statnlp.hypergraph.TableLookupNetwork;
import org.statnlp.sentiment.spanmodel.common.SentimentDict;
import org.statnlp.sentiment.spanmodel.common.SentimentInstance;
import org.statnlp.sentiment.spanmodel.common.SpanModelSuperCompiler;



public class SpanModelCompiler extends SpanModelSuperCompiler {


	public static boolean visual = false;
	
	public static BaseNetwork[] labelNetworks = new BaseNetwork[9000];
	public static BaseNetwork[] unlabelNetworks = new BaseNetwork[9000];

	//SpanModelViewer viewer = new SpanModelViewer(null);
	void visualize(TableLookupNetwork network, String title, int networkId)
	{
		if (labelNetworks[networkId] != null && unlabelNetworks[networkId - 1] != null)
		{
			System.out.println("contains:" + unlabelNetworks[networkId - 1].contains(labelNetworks[networkId]));
		}
	}
	
	public SpanModelCompiler() {
		// TODO Auto-generated constructor stub
	}
	
	public SpanModelCompiler(SentimentDict dict) {
		this.setSentimentDict(dict);
		// TODO Auto-generated constructor stub
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2100499563741744475L;
	
	public enum NodeType {Root, Sentiment, OP, Leaf};
	
	public enum OPNodeType {Magnify_neg,  Medium_neg, Neg_neg,  Medium_neu, Magnify_pos, Medium_pos, Neg_pos, LastNeg_4, LastNeg_3, LastNeg_2,  LastNeg_1, LastNeu_, LastPos_1, LastPos_2, LastPos_3, LastPos_4 };
	
	public enum SentimentStateType {very_negative, negative, neutral, positive, very_positive};

	int SentimentStateTypeSize = SentimentStateType.values().length;
	int OPNodeTypeSize = OPNodeType.values().length;
	int NormalOPNodeTypeSize = OPNodeType.Neg_pos.ordinal() + 1;

	

	
	@Override
	public SentimentInstance decompile(Network network) {
		BaseNetwork lcrfNetwork = (BaseNetwork)network;
		
		SentimentInstance inst = (SentimentInstance)network.getInstance();

		List<String> inputs = (List<String>)inst.getInput();
		//Integer output = (Integer)inst.getOutput();		
		
		int size = inst.textSpanList.size();
		//System.err.println("sent:" + inst.getSentence());

	
		ArrayList<Integer> pred_sentiment_reverse = new ArrayList<Integer>();
		ArrayList<Integer> pred_op_reverse = new ArrayList<Integer>();
		

		long rootNode = toNode_root(size);
		int root = Arrays.binarySearch(lcrfNetwork.getAllNodes(), rootNode);
		
		int[] current= network.getMaxPath(root);

		int[] ids = NetworkIDMapper.toHybridNodeArray(network.getNode(current[0]));
		
		Integer prediction = ids[1];
		

		while(true){
			current = network.getMaxPath(current[0]);
		
	
			int[] ids_sentiment = NetworkIDMapper.toHybridNodeArray(network.getNode(current[0]));
			
			if (ids_sentiment[4] == NodeType.Leaf.ordinal())
			{
				break;
			}
			
			int[] ids_op = NetworkIDMapper.toHybridNodeArray(network.getNode( current[1]));
			//System.out.println("ids:" + Arrays.toString(ids));
			
			
			
			int pos = ids_sentiment[0];
			int polar = ids_sentiment[1];
			int node_type = ids_sentiment[4];
			
			int OPIndex = ids_op[1];
			
			pred_sentiment_reverse.add(polar);
			pred_op_reverse.add(OPIndex);
		}
		

		for(int i = pred_sentiment_reverse.size() - 1; i >= 0 ; i--)
		{
			inst.predictSentiment.add(pred_sentiment_reverse.get(i));
		}
		
		for(int i = pred_op_reverse.size() - 1; i >= 0 ; i--)
		{
			inst.predictOP.add(pred_op_reverse.get(i));
		}
		
		
		inst.setPrediction(prediction);
		
		return inst;
	}
	
	
	
	boolean startOfEntity(int pos, int size, ArrayList<Label> outputs)
	{
		String label = outputs.get(pos).getForm();
		if (label.startsWith("B"))
			return true;
		
		if (pos == 0 && label.startsWith("I"))
			return true;
		
		if (pos > 0)
		{
			String prev_label =  outputs.get(pos - 1).getForm();
			if (label.startsWith("I") && prev_label.startsWith("O"))
				return true;
		}
		
		
		return false;
	}
	
	boolean endofEntity(int pos, int size,  ArrayList<Label> outputs)
	{
		String label = outputs.get(pos).getForm();
		if (!label.startsWith("O"))
		{
			if (pos == size - 1)
				return true;
			else {
				String next_label =  outputs.get(pos + 1).getForm();
				if (next_label.startsWith("O") || next_label.startsWith("B"))
					return true;
			}
		}
		
		return false;
	}
	
	
	private long toNode_leaf(int size){
		return NetworkIDMapper.toHybridNodeID(new int[]{0, 0, 0, 0, NodeType.Leaf.ordinal()});
	}
	
	private long toNode_SentimentState(int pos, int polar, int size){
		return NetworkIDMapper.toHybridNodeID(new int[]{pos + 1, polar, 0, 0, NodeType.Sentiment.ordinal()});
	}
	
	private long toNode_OP(int pos, int OPIndex,  int from, int to,int size){
		return NetworkIDMapper.toHybridNodeID(new int[]{pos + 1, OPIndex, from, to, NodeType.OP.ordinal()});
	}
	
	private long toNode_root(int size){
		return NetworkIDMapper.toHybridNodeID(new int[]{size + 2, 0, 0, 0, NodeType.Root.ordinal()});
	}
	

	public long[][] buildCommonNetwork(NetworkBuilder<BaseNetwork> lcrfNetwork, SentimentInstance inst, boolean labeled)
	{
		List<String> inputs = (List<String>)inst.getInput();
		Integer output = (Integer)inst.getOutput();	
		int size = inst.textSpanList.size();
		
		
		long[][] node_array = new long[size + 1][SentimentStateTypeSize + OPNodeTypeSize];
		
		for (int pos = 0; pos <= size; pos++) {
			
			for (int j = 0; j < SentimentStateTypeSize; j++) {
				long node = this.toNode_SentimentState(pos, j, size);
				node_array[pos][j] = node;
				lcrfNetwork.addNode(node);
			}

			if (pos < size) 
			{
				int[] textSpanIdx = (int[]) inst.textSpanList.get(pos);
				for (int j = 0; j < OPNodeTypeSize; j++) {
					long node = this.toNode_OP(pos, j, textSpanIdx[0], textSpanIdx[1], size);
					node_array[pos][SentimentStateTypeSize + j] = node;
					lcrfNetwork.addNode(node);
				}
			}
		}
		
		
		long leaf = this.toNode_leaf(size);
		lcrfNetwork.addNode(leaf);
		
		lcrfNetwork.addEdge(node_array[0][SentimentStateType.neutral.ordinal()],  new long[]{leaf});
		
		ArrayList<Integer> sentimentStateCandidate = new ArrayList<Integer>();
		sentimentStateCandidate.add(SentimentStateType.neutral.ordinal());
		
		ArrayList<Integer> nextSentimentStateCandidate = new ArrayList<Integer>();
		
		
		for(int pos = 0; pos < size; pos++)
		{
			int[] textSpanIdx = (int[])inst.textSpanList.get(pos);
			//System.err.println("size:"+ size + '\t' + "pos:" + pos + "\tinput:" + inst.input );
			//System.err.println("sentimentWordList:" + inst.sentimentWordList );
			ArrayList<String> textSpan = new ArrayList<String>();
			int numNegationWord = 0;
			int OPPolar = -1;
			
			if (pos < size - 1)
			{
				Integer wordIdx = (Integer)inst.sentimentWordList.get(pos);
				String word = inputs.get(wordIdx);
				OPPolar = dict.queryPolar(word);
				
				for(int i = textSpanIdx[0]; i < textSpanIdx[1]; i++)
				{
					String w = inputs.get(i);
					textSpan.add(w);
					
					if (negations.contains(w))
					{
						numNegationWord++;
					}
				}
			}
			else
			{
				OPPolar = 0;
			}
				
			nextSentimentStateCandidate.clear();
			
			
			if (pos < size - 1)
			for(Integer j : sentimentStateCandidate)
			{
				
				for (int k = 0; k < NormalOPNodeTypeSize; k++) {
						
					//for pos < size - 1, we do not use Medium OP
					if (pos < size - 1)
					{
						if (k == OPNodeType.Medium_neu.ordinal())
							continue;
						
						if (k ==  OPNodeType.Neg_pos.ordinal() || k == OPNodeType.Neg_neg.ordinal())
						{
							if (numNegationWord == 0) continue;
						}
					}
					else
					{
						if (k != OPNodeType.Medium_neg.ordinal() && k != OPNodeType.Medium_pos.ordinal()
						  && k!= OPNodeType.Medium_neu.ordinal() && k != OPNodeType.Magnify_pos.ordinal()
						  && k!= OPNodeType.Magnify_neg.ordinal())
							continue;
					}
						
						//We do not use OP state that is different OPPolar
					if (OPPolar * (k - OPNodeType.Medium_neu.ordinal()) < 0) //sentiment word polar
						continue;
						
						
						
					int toNextSentimentState = -1;

						//Compute the possible nextSentimentState
						if (k == OPNodeType.Magnify_pos.ordinal()) {
							toNextSentimentState = j + 2;
						} else if (k == OPNodeType.Magnify_neg.ordinal()) {
							toNextSentimentState = j - 2;
						} else if (k == OPNodeType.Medium_pos.ordinal()) {
							toNextSentimentState = j + 1;
						} else if (k == OPNodeType.Medium_neg.ordinal()) {
							toNextSentimentState = j - 1;
						} else if (k == OPNodeType.Medium_neu.ordinal()) {
							toNextSentimentState = j;
						}
						else if (k == OPNodeType.Neg_pos.ordinal()) {
							toNextSentimentState = j - 1;
						} else if (k == OPNodeType.Neg_neg.ordinal()) {
							toNextSentimentState = j + 1;
						}
						
						
						
						if (toNextSentimentState >= SentimentStateTypeSize + 1  || toNextSentimentState < -1) continue;
						
						if (toNextSentimentState >= SentimentStateTypeSize)
							toNextSentimentState = SentimentStateTypeSize - 1;
						
						if (toNextSentimentState < 0)
							toNextSentimentState = 0;
						

					long edgeFrom = node_array[pos + 1][toNextSentimentState];

					long[] edgeTo = new long[] { node_array[pos][j], node_array[pos][SentimentStateTypeSize + k] };

					lcrfNetwork.addEdge(edgeFrom, edgeTo);
						
					//Add next sentiment state candidate
					if (!nextSentimentStateCandidate.contains(toNextSentimentState))
					{
						nextSentimentStateCandidate.add(toNextSentimentState);
					}

				}
					
				
			}
			else
			{
				if (labeled)
				{
					nextSentimentStateCandidate.add(output);
				}
				else
				{
					for(int j = 0; j < SentimentStateTypeSize; j++)
					{
						nextSentimentStateCandidate.add(j);
					}
				}
				
				for(Integer i : nextSentimentStateCandidate)
				for(Integer j : sentimentStateCandidate)
				{
					int k = -1;
										
					k = OPNodeType.LastNeu_.ordinal() + i - j;// + OPNodeType.LastNeg2_.ordinal();
					
					if (i - j > 0)
						k = OPNodeType.LastPos_1.ordinal();
					else if (i - j < 0)
						k =  OPNodeType.LastNeg_1.ordinal();
					else
						k =  OPNodeType.Medium_neu.ordinal();
					
					/*
					if (i - j == 1)
						k = OPNodeType.LastPos_1.ordinal();
					else if (i - j == 2)
						k = OPNodeType.LastPos_2.ordinal();
					else if (i - j > 2)
						k = OPNodeType.LastPos_3.ordinal();
					else if (i - j == -1)
						k =  OPNodeType.LastNeg_1.ordinal();
					else if (i - j == -2)
						k =  OPNodeType.LastNeg_2.ordinal();
					else if (i - j < -2)
						k =  OPNodeType.LastNeg_3.ordinal();
					else
						k =  OPNodeType.Medium_neu.ordinal();*/
					
					//k =  OPNodeType.LastNeu_.ordinal();
					
					long edgeFrom = node_array[pos + 1][i];
					//System.out.println("SentimentStateTypeSize+OPNodeTypeSize:" + (SentimentStateTypeSize+ OPNodeTypeSize));
					long[] edgeTo = new long[] { node_array[pos][j], node_array[pos][SentimentStateTypeSize + k] };

					lcrfNetwork.addEdge(edgeFrom, edgeTo);
					
				}
				
			}
			
			
			sentimentStateCandidate.clear();
			sentimentStateCandidate.addAll(nextSentimentStateCandidate);
			
			
		}
		
		long root = this.toNode_root(size);
		lcrfNetwork.addNode(root);
		if (labeled)
		{
			lcrfNetwork.addEdge(root,  new long[]{node_array[size][output]});
		}
		else
		{	
			for(int j = 0; j < SentimentStateTypeSize; j++)
				if (sentimentStateCandidate.contains(j))
					lcrfNetwork.addEdge(root,  new long[]{node_array[size][j]});
		}
		
		
		return node_array;
		
	}
	

	@Override
	public Network compileLabeled(int networkId, Instance instance, LocalNetworkParam param) {
		SentimentInstance inst = (SentimentInstance)instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();
		
		List<String> inputs = (List<String>)inst.getInput();
		Integer output = (Integer)inst.getOutput();		
		
		int size = inst.textSpanList.size();
		

		
		long[][] node_array = buildCommonNetwork(lcrfNetwork, inst, true);
	
			
		
		

		
		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		
//		System.err.println(network.countNodes()+" nodes.");
//		System.exit(1);
		//labelNetworks[networkId] = network;
		
		
		
		if (visual)
			visualize(network, "Sentiment Model:labeled", networkId);
		return network;
	}

	@Override
	public Network compileUnlabeled(int networkId, Instance instance, LocalNetworkParam param) {
		SentimentInstance inst = (SentimentInstance)instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();
		
		List<String> inputs = (List<String>)inst.getInput();
		Integer output = (Integer)inst.getOutput();		
		
		int size = inst.textSpanList.size();
		

	
		long[][] node_array = buildCommonNetwork(lcrfNetwork, inst, false);
		
		
		
		

		
		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		
//		System.err.println(network.countNodes()+" nodes.");
//		System.exit(1);
		//unlabelNetworks[networkId] = network;
		//if (visual) visualize(network, "Sentiment Model:unlabeled", networkId);
		return network;
	}
	
	
	Set<String> negations = new HashSet<String>() {{
	    add("not");
	    add("no");
	    add("n't");
	    add("nothing");
	    add("none");
	    add("null");
	    add("nil");
	}};
	
	
	

}
