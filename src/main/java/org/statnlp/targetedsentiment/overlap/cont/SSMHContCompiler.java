package org.statnlp.targetedsentiment.overlap.cont;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Stack;

import org.statnlp.commons.types.Instance;
import org.statnlp.example.base.BaseNetwork;
import org.statnlp.example.base.BaseNetwork.NetworkBuilder;
import org.statnlp.hypergraph.LocalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkCompiler;
import org.statnlp.hypergraph.NetworkIDMapper;
import org.statnlp.sentiment.spanmodel.common.SentimentInstance;
import org.statnlp.targetedsentiment.common.TSInstance;
import org.statnlp.targetedsentiment.common.TargetSentimentGlobal;



public class SSMHContCompiler extends NetworkCompiler {
	
		
	public SSMHContCompiler() {
		super(null);
		setCapacity();
	}
	
	public void setCapacity()
	{
		int MAX_SENTENCE_LENGTH = TargetSentimentGlobal.MAX_LENGTH_LENGTH;
		NetworkIDMapper.setCapacity(new int[]{MAX_SENTENCE_LENGTH, NodeTypeSize + 1, TTypeSize + 1});
	
	}


	private static final long serialVersionUID = 2100499563741744475L;
	
	public enum NodeType {X, IA ,IW,IB ,T, E, A, Root};
	
	public enum TType {positive, negative, neutral} // {Volitional}
	
	//public enum PolarityType {positive, negative, neutral}

	int NodeTypeSize = NodeType.values().length;
	
	//int PolarityTypeSize = PolarityType.values().length;
	
	int TTypeSize = TType.values().length;
	
	
	
	
	
	
	
	
	
	private long toNode_root(int size){
		return NetworkIDMapper.toHybridNodeID(new int[]{size + 1, NodeType.Root.ordinal(), 0 });
	}
	
	//pos: position of word in the sentence starting from 0 to size - 1
	private long toNode_Span(int size, int pos, int node_type, int type){
		return NetworkIDMapper.toHybridNodeID(new int[]{size - pos, node_type, type});
	}
		
	
	private long toNode_A(int size, int pos){
		return NetworkIDMapper.toHybridNodeID(new int[]{size - pos, NodeType.A.ordinal(), 0});
	}
	
	private long toNode_E(int size, int pos){
		return NetworkIDMapper.toHybridNodeID(new int[]{size - pos, NodeType.E.ordinal(), 0});
	}
	
	private long toNode_T(int size, int pos, int type){
		return NetworkIDMapper.toHybridNodeID(new int[]{size - pos, NodeType.T.ordinal(), type});
	}
	
	private long toNode_IB(int size, int pos, int type){
		return NetworkIDMapper.toHybridNodeID(new int[]{size - pos, NodeType.IB.ordinal(), type});
	}
	
	private long toNode_IW(int size, int pos, int type){
		return NetworkIDMapper.toHybridNodeID(new int[]{size - pos, NodeType.IW.ordinal(), type});
	}
	
	private long toNode_IA(int size, int pos, int type){
		return NetworkIDMapper.toHybridNodeID(new int[]{size - pos, NodeType.IA.ordinal(), type});
	}
	
	
	
	private long toNode_X(){
		return NetworkIDMapper.toHybridNodeID(new int[]{0, 0, 0});
	}

	@Override
	public Network compileLabeled(int networkId, Instance instance, LocalNetworkParam param) {
		TSInstance inst = (TSInstance) instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();

		ArrayList<String[]> inputs = (ArrayList<String[]>) inst.getInput();
		ArrayList<String> outputs = (ArrayList<String>) inst.getOutput();

		int size = inst.size();
		

		long root = this.toNode_root(size);
		lcrfNetwork.addNode(root);
		
		long[][][] node_arr = new long[NodeTypeSize][TTypeSize][size + 2];
		
		long[] A = node_arr[NodeType.A.ordinal()][0];
		long[] E = node_arr[NodeType.E.ordinal()][0];
		long[][] T = node_arr[NodeType.T.ordinal()];   //T[type][pos]
		long[][] IB = node_arr[NodeType.IB.ordinal()]; //IB[type][pos] 
		long[][] IW = node_arr[NodeType.IW.ordinal()];  
		long[][] IA = node_arr[NodeType.IA.ordinal()];  
		
		
		/******build node array ******/
		long node = -1;
		
		for(int node_type = NodeType.A.ordinal(); node_type > 0; node_type--) {
			
			int max_type = (node_type >= NodeType.E.ordinal()) ? 1 : TTypeSize;
			for(int type = 0; type < max_type; type++) {
				for(int pos = 0; pos < size; pos++) {	
					node = this.toNode_Span(size, pos, node_type, type);
					node_arr[node_type][type][pos + 1] = node;
				}
			}
		}
		
		long X = this.toNode_X();
		lcrfNetwork.addNode(X);
		/******build node array ******/

		
		////////////////////////////////////////////
		
		
		int last_entity_pos = -1;
		TType last_polar = null;
		TType polar = null;
		long from = -1;
		long to = -1;
		long to1 = -1;
		int entity_begin = -1;
		HashSet<String> edgeSet = new HashSet<String>();
		
		from = root;
		to = A[0];
		lcrfNetwork.addEdge(from, new long[] { to });
		
		for(int pos = 0; pos < size; pos++)
		{
			String word = inputs.get(pos)[0];
			String label = outputs.get(pos);
			
			boolean start_entity = TargetSentimentGlobal.startOfEntityStr(pos, size, outputs);
			boolean end_entity = TargetSentimentGlobal.endofEntityStr(pos, size, outputs);

			from = A[pos];
			to = E[pos];
			if (pos < size - 1) {
				to1 = A[pos + 1];
				lcrfNetwork.addEdge(from, new long[] { to, to1 });
			} else {
				lcrfNetwork.addEdge(from, new long[] { to });
			}
			
			
			from = E[pos];
			lcrfNetwork.addEdge(from, T[pos]);
		
		
			if (start_entity) {
				polar = TType.valueOf(label.substring(2));
				int polarIdx = polar.ordinal();
				
				
				
				for(int t = 0; t < TTypeSize; t++)
				{
					
					//add T[t != polarIdx] to X
					if (t != polarIdx) {
						from = T[pos][t];
						to = X;
						lcrfNetwork.addEdge(from,  new long[]{to});
					}
				}
				
				if (last_entity_pos == -1)
				{	
					/// directly from left to right
					for(int i = 0; i < pos; i++)
					{
						//from before node[pos] to before node at [pos+1]
						for(int t = 0; t < TTypeSize; t++)
						{
							//T=>IB
							
							
							/*
							//add IB=>X
							from = IB[i][t][last_polar.ordinal()];
							to = end;
							network.addEdge(from, new long[] { to });
							*/
							
							//IB=>IB
							
							
						}
						
					}
					
					
					
				} else {
					
					
					
					
				}
				
				
				entity_begin = pos;
				
			}
			
			if (end_entity) {
				
				//add links between entity
				for (int t = 0; t < TTypeSize; t++) 
				{
					
					
					
				}
				
				
				
				
				last_entity_pos = pos;
				last_polar = polar;
				
			}
			
		}
		
		
		//add the last column node to end
		if (polar != null)
		{
			for (int t = 0; t < TTypeSize; t++) 
			{
				for (int pos = last_entity_pos + 1; pos < size; pos++) 
				{
					
				}

			}
		} else {
			//polar = PolarityType._;
			//from = start;
			//to = node_array[0][polar.ordinal()][SubNodeType.B.ordinal()];
			//network.addEdge(from,  new long[]{to});
			
			System.out.println("No Entity found in this Instance, Discard!");
			/*
			for(int pos = 0; pos < size; pos++)
			{
				
			}*/
			
		}
		
		
		
		
		
		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		return network;
	}

	@Override
	public Network compileUnlabeled(int networkId, Instance inst,
			LocalNetworkParam param) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Instance decompile(Network network) {
		// TODO Auto-generated method stub
		return null;
	}
	
	
	
	
	

}
