package org.statnlp.targetedsentiment.f.latent;

import java.util.ArrayList;




import java.util.Arrays;
import java.util.List;

import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.commons.types.LinearInstance;
import org.statnlp.commons.types.Sentence;
import org.statnlp.commons.types.Token;
import org.statnlp.example.base.BaseNetwork;
import org.statnlp.example.base.BaseNetwork.NetworkBuilder;
import org.statnlp.hypergraph.LocalNetworkParam;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkCompiler;
import org.statnlp.hypergraph.NetworkIDMapper;
import org.statnlp.targetedsentiment.common.TSInstance;
import org.statnlp.targetedsentiment.common.TargetSentimentGlobal;



public class TargetSentimentCompiler extends NetworkCompiler {

	int NEMaxLength = 3;
	int SpanMaxLength = 10;
	public static boolean visual = false;
	
	
	//TargetSentimentViewer viewer = new TargetSentimentViewer(this, null, 5);
	void visualize(Network network, String title, int networkId)
	{
		//viewer.visualizeNetwork(network, null, title + "[" + networkId + "]");
	}
	
	public TargetSentimentCompiler() {
		super(null);
		// TODO Auto-generated constructor stub
	}
	
	public TargetSentimentCompiler(int NEMaxLength, int SpanMaxLength) {
		super(null);
		this.NEMaxLength = NEMaxLength;
		this.SpanMaxLength = SpanMaxLength;
	}

	/**
	 * 
	 */
	private static final long serialVersionUID = 2100499563741744475L;
	
	public enum NodeType {Start, Span, End};
	
	public enum SubNodeType {B, e, A}
	
	public enum PolarityType {positive, negative, neutral}
	
	
	TargetSentimentFeatureManager fm;

	
	int PolarityTypeSize = PolarityType.values().length;
	
	int SubNodeTypeSize = SubNodeType.values().length;
	
	

	
	@Override
	public TSInstance decompile(Network network) {
		//Network network = (Network)network;
		
		TSInstance inst = (TSInstance)network.getInstance();

		List<String[]> inputs = (List<String[]>)inst.getInput();
		ArrayList<String> outputs = (ArrayList<String>)inst.getOutput();
		

		int size = inst.size();
		//InputToken[] input = inst.getInput();
	
		ArrayList<int[]> preds = new ArrayList<int[]>();
		ArrayList<int[]> preds_refine = new ArrayList<int[]>();

		
		int node_k = network.countNodes()-1;
		//node_k = 0;
		while(true){
			//System.out.print(node_k + " ");
			node_k = network.getMaxPath(node_k)[0];
			
			int[] ids = NetworkIDMapper.toHybridNodeArray(network.getNode(node_k));
			//System.out.println("ids:" + Arrays.toString(ids));
			
			if (ids[4] == NodeType.End.ordinal())
			{
				break;
			}
			
			int pos = size - ids[0];
			int polar = PolarityTypeSize - ids[1];
			int subnode = SubNodeTypeSize - ids[2];
			int node_type = ids[4];
			
			if(ids[4] == NodeType.Span.ordinal()){
				
				preds.add(new int[]{pos, polar, subnode});
				
			}
			
		}
		

		
		ArrayList<Label> predication_array = new ArrayList<Label>();
		PolarityType polar = null;
		int entity_begin = -1;
		
		ArrayList<int[]> scopes = new ArrayList<int[]>(); //for each entity
		scopes.add(new int[]{0, -1});
		
		for(int i = 0; i < preds.size(); i++)
		{
			int[] ids = preds.get(i);
			int pos = ids[0];
			int polar_index = ids[1];
			int subnode_index = ids[2];
			
			
			//System.out.println(pos + "," + SubNodeType.values()[subnode_index].name() 
			//		+ "," + PolarityType.values()[polar_index].name());
			
			
			//left node 
			if (subnode_index == SubNodeType.B.ordinal())
			{
				
				int[] next_ids =  preds.get(i + 1);
				int next_pos = next_ids[0];
				int next_polar_index = next_ids[1];
				int next_subnode_index = next_ids[2];
				
				//next node is before node
				if (next_subnode_index == SubNodeType.B.ordinal())
				{
					predication_array.add(new Label("O", 0));
					
				} 
				else if (next_subnode_index == SubNodeType.e.ordinal())
				{
					//entity_begin  = pos;
					
					polar = PolarityType.values()[next_polar_index];
					
					predication_array.add(new Label("B-" + polar.name(), 0) );
				}
					
				
				
			}
			else if (subnode_index == SubNodeType.e.ordinal())
			{
				int[] next_ids =  preds.get(i + 1);
				int next_pos = next_ids[0];
				int next_polar_index = next_ids[1];
				int next_subnode_index = next_ids[2];
				
				if (next_subnode_index == SubNodeType.e.ordinal())
				{
					predication_array.add(new Label("I-" + polar.name(), 0));	
				} else if (next_subnode_index == SubNodeType.A.ordinal())
				{
					//nothing
				}
				
			}
			else if (subnode_index == SubNodeType.A.ordinal())
			{
				if (pos < size - 1)
				{
					int[] next_ids =  preds.get(i + 1);
					int next_pos = next_ids[0];
					int next_polar_index = next_ids[1];
					int next_subnode_index = next_ids[2];
					
					//from After to next After
					if (next_subnode_index == SubNodeType.A.ordinal())
					{
						predication_array.add(new Label("O", 0));
					}
					else if (next_subnode_index == SubNodeType.B.ordinal())
					{
						//if (TargetSentimentGlobal.OUTPUT_SENTIMENT_SPAN)
						{
							//TargetSentimentGlobal.SENTIMENT_SPAN_SPLIT.add(pos);
							scopes.get(scopes.size() - 1)[1] = pos + 1;
							scopes.add(new int[]{pos + 1, -1});
							
						}
					}
				}
			}
			

		}
		
		scopes.get(scopes.size() - 1)[1] = size;
		inst.setScopes(scopes);
		
		//System.out.println();
		//if (TargetSentimentGlobal.OUTPUT_SENTIMENT_SPAN)
		{
			//TargetSentimentGlobal.SENTIMENT_SPAN_SPLIT.add(-1);
		}
		
		/*
		String[] prediction = new String[size];
		
		//System.out.println("\n~~\n");
		for(int k = 0; k < prediction.length; k++)
		{
			prediction[k] = predication_array.get(k);
			//System.out.print(prediction[k].getName() + " ");
		}*/
		//System.out.println();
		
		
		inst.setPrediction(predication_array);
		
		return inst;
	}
	
	
	
	private Network compileUnlabeledFixNE(int networkId, Instance instance, LocalNetworkParam param){
		TSInstance inst = (TSInstance)instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();
		
		List<String[]> inputs = (List<String[]>)inst.getInput();
		ArrayList<Label> outputs = (ArrayList<Label>)inst.getOutput();
		
			
		int size = inst.size();
		
		long start = this.toNode_start(size);
		lcrfNetwork.addNode(start);
		
		long[][][] node_array = new long[size][PolarityTypeSize][SubNodeTypeSize];
		
		//build node array
		for(int pos = 0; pos < size; pos++)
		{
			for(int polar = 0; polar < PolarityTypeSize; polar++)
			{
				for(int sub = 0; sub < SubNodeTypeSize; sub++)
				{
					long node = this.toNode_Span(size, pos, polar, sub);
					lcrfNetwork.addNode(node);
					node_array[pos][polar][sub] = node;
				}
			}
		}
		
		long end = this.toNode_end(inst.size());
		lcrfNetwork.addNode(end);
		
		
		////////////////////////////////////////////
		
		
		int last_entity_pos = -1;
		//PolarityType last_polar = null;
		//PolarityType polar = null;
		long from = -1;
		long to = -1;
		int entity_begin = -1;
		
		for(int pos = 0; pos < size; pos++)
		{
			String word = inputs.get(pos)[0];
			String label = outputs.get(pos).getForm();
			
			boolean start_entity = this.startOfEntity(pos, size, outputs);
			boolean end_entity = this.endofEntity(pos, size, outputs);
			
		
			if (start_entity) {

				for (PolarityType polar : PolarityType.values()) {

					from = node_array[pos][polar.ordinal()][SubNodeType.B.ordinal()];
					to = node_array[pos][polar.ordinal()][SubNodeType.e.ordinal()];
					lcrfNetwork.addEdge(from, new long[] { to });

					if (last_entity_pos == -1) {
						from = start;
						to = node_array[0][polar.ordinal()][SubNodeType.B.ordinal()];
						lcrfNetwork.addEdge(from, new long[] { to });

						/// directly from left to right
						for (int i = 0; i < pos; i++) {
							// from before node[pos] to before node at [pos+1]
							from = node_array[i][polar.ordinal()][SubNodeType.B.ordinal()];
							to = node_array[i + 1][polar.ordinal()][SubNodeType.B.ordinal()];
							lcrfNetwork.addEdge(from, new long[] { to });

						}

					} else {

						
							// latent path
							for (int i = last_entity_pos + 1; i < pos; i++) {
								// add A->A
								from = node_array[i - 1][polar.ordinal()][SubNodeType.A.ordinal()];
								to = node_array[i][polar.ordinal()][SubNodeType.A.ordinal()];
								lcrfNetwork.addEdge(from, new long[] { to });

								// add B->B
								
									from = node_array[i][polar.ordinal()][SubNodeType.B.ordinal()];
									to = node_array[i + 1][polar.ordinal()][SubNodeType.B.ordinal()];
									lcrfNetwork.addEdge(from, new long[] { to });
								

							}
							
						for (PolarityType last_polar : PolarityType.values()) 
						{

							for (int i = last_entity_pos; i < pos; i++) {
								// add A->B
								from = node_array[i][last_polar.ordinal()][SubNodeType.A.ordinal()];
								to = node_array[i + 1][polar.ordinal()][SubNodeType.B.ordinal()];
								lcrfNetwork.addEdge(from, new long[] { to });

							}
						}

					}
				}

				entity_begin = pos;

			}
			
			if (end_entity) {

				for (PolarityType polar : PolarityType.values()) {

					// add links between entity
					for (int i = entity_begin; i < pos; i++) {

						from = node_array[i][polar.ordinal()][SubNodeType.e.ordinal()];
						to = node_array[i + 1][polar.ordinal()][SubNodeType.e.ordinal()];
						lcrfNetwork.addEdge(from, new long[] { to });

					}

					// add link from entity to After
					from = node_array[pos][polar.ordinal()][SubNodeType.e.ordinal()];
					to = node_array[pos][polar.ordinal()][SubNodeType.A.ordinal()];
					lcrfNetwork.addEdge(from, new long[] { to });

					last_entity_pos = pos;
					//last_polar = polar;
				}

			}
			
		}
		
		
		//add the last column node to end
		for (PolarityType polar : PolarityType.values()) 
		{
			for(int pos = last_entity_pos + 1; pos < size; pos++)
			{
				from = node_array[pos - 1][polar.ordinal()][SubNodeType.A.ordinal()];
				to = node_array[pos][polar.ordinal()][SubNodeType.A.ordinal()];
				lcrfNetwork.addEdge(from,  new long[]{to});
			}
			
			from = node_array[size - 1][polar.ordinal()][SubNodeType.A.ordinal()];
			to = end;
			lcrfNetwork.addEdge(from,  new long[]{to});
		} 
		
		//else {
			//polar = PolarityType._;
			//from = start;
			//to = node_array[0][polar.ordinal()][SubNodeType.B.ordinal()];
			//network.addEdge(from,  new long[]{to});
			
			//System.out.println("No Entity found in this Instance, Discard!");
			/*
			for(int pos = 0; pos < size; pos++)
			{
				
			}*/
			
		//}
		


		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		
		if (visual)
			visualize(network, "Sentiment Model, fix NE:unlabeled", networkId);
		
//		System.err.println(network.countNodes()+" nodes.");
//		System.exit(1);
		
		return network;
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
	
	
	
	private long toNode_start(int size){
		return NetworkIDMapper.toHybridNodeID(new int[]{size + 1, 0, 0, 0, NodeType.Start.ordinal()});
	}
	
	
//	private long toNode_hiddenState(int size, int bIndex, OutputToken hiddenState){
//		//System.out.println("bIndex=" + bIndex);
//		return NetworkIDMapper.toHybridNodeID(new int[]{size - bIndex, 2, 0, hiddenState.getId(), nodeType.HiddenState.ordinal()});
//	}
	
	//private long toNode_Entity(int size, int bIndex, int row, EntityNodeType type){
		//System.out.println("bIndex=" + bIndex);
	//	return NetworkIDMapper.toHybridNodeID(new int[]{size - bIndex, NodeTypeSize - row, 0, 0, nodeType.Entity.ordinal()});
	//}
	
	private long toNode_Span(int size, int bIndex, int polar, int subnode){
		//System.out.println("bIndex=" + bIndex);
		return NetworkIDMapper.toHybridNodeID(new int[]{size - bIndex, PolarityTypeSize - polar, SubNodeTypeSize - subnode, 0, NodeType.Span.ordinal()});
	}
	
	
//	private long toNode_observation(int size, int bIndex, InputToken observation){
//		return NetworkIDMapper.toHybridNodeID(new int[]{size - bIndex, 1, 0, observation.getId(), nodeType.Observation.ordinal()});
//	}
//	
//	
	
	
	/**/
	private long toNode_end(int size){
		return NetworkIDMapper.toHybridNodeID(new int[]{0, 0, 0, 0, NodeType.End.ordinal()});
	}

	@Override
	public Network compileLabeled(int networkId, Instance instance, LocalNetworkParam param) {
		TSInstance inst = (TSInstance)instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();
		
		List<String[]> inputs = (List<String[]>)inst.getInput();
		ArrayList<Label> outputs = (ArrayList<Label>)inst.getOutput();
		//		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		
		
			
		int size = inst.size();
		
		long start = this.toNode_start(size);
		lcrfNetwork.addNode(start);
		
		long[][][] node_array = new long[size][PolarityTypeSize][SubNodeTypeSize];
		
		//build node array
		for(int pos = 0; pos < size; pos++)
		{
			for(int polar = 0; polar < PolarityTypeSize; polar++)
			{
				for(int sub = 0; sub < SubNodeTypeSize; sub++)
				{
					long node = this.toNode_Span(size, pos, polar, sub);
					lcrfNetwork.addNode(node);
					node_array[pos][polar][sub] = node;
				}
			}
		}
		
		long end = this.toNode_end(inst.size());
		lcrfNetwork.addNode(end);
		
		
		////////////////////////////////////////////
		
		
		int last_entity_pos = -1;
		PolarityType last_polar = null;
		PolarityType polar = null;
		long from = -1;
		long to = -1;
		int entity_begin = -1;
		
		for(int pos = 0; pos < size; pos++)
		{
			String word = inputs.get(pos)[0];
			String label = outputs.get(pos).getForm();
			
			boolean start_entity = this.startOfEntity(pos, size, outputs);
			boolean end_entity = this.endofEntity(pos, size, outputs);
			
		
			if (start_entity) {
				try{polar = PolarityType.valueOf(label.substring(2));} catch (Exception e)
				{System.err.println("PolarityType Error");}
				from = node_array[pos][polar.ordinal()][SubNodeType.B.ordinal()];
				to = node_array[pos][polar.ordinal()][SubNodeType.e.ordinal()];
				lcrfNetwork.addEdge(from,  new long[]{to});
				
				if (last_entity_pos == -1)
				{
					from = start;
					to = node_array[0][polar.ordinal()][SubNodeType.B.ordinal()];
					lcrfNetwork.addEdge(from,  new long[]{to});
					
					/// directly from left to right
					for(int i = 0; i < pos; i++)
					{
						//from before node[pos] to before node at [pos+1]
						from = node_array[i][polar.ordinal()][SubNodeType.B.ordinal()];
						to = node_array[i + 1][polar.ordinal()][SubNodeType.B.ordinal()];
						lcrfNetwork.addEdge(from,  new long[]{to});
						
					}
					
					
					
				} else {
					
					//latent path
					for(int i = last_entity_pos + 1; i < pos; i++)
					{
						//add A->A
						from = node_array[i - 1][last_polar.ordinal()][SubNodeType.A.ordinal()];
						to = node_array[i][last_polar.ordinal()][SubNodeType.A.ordinal()];
						lcrfNetwork.addEdge(from,  new long[]{to});
						
						//add B->B
						from = node_array[i][polar.ordinal()][SubNodeType.B.ordinal()];
						to = node_array[i + 1][polar.ordinal()][SubNodeType.B.ordinal()];
						lcrfNetwork.addEdge(from,  new long[]{to});
						
					}
					
					
					for(int i = last_entity_pos; i < pos; i++)
					{
						//add A->B
						from = node_array[i][last_polar.ordinal()][SubNodeType.A.ordinal()];
						to = node_array[i + 1][polar.ordinal()][SubNodeType.B.ordinal()];
						lcrfNetwork.addEdge(from,  new long[]{to});
						
					}
					

					
					
				}
				
				
				entity_begin = pos;
				
			}
			
			if (end_entity) {
				
				//add links between entity
				for(int i = entity_begin; i < pos; i++)
				{
					from = node_array[i][polar.ordinal()][SubNodeType.e.ordinal()];
					to = node_array[i + 1][polar.ordinal()][SubNodeType.e.ordinal()];
					lcrfNetwork.addEdge(from,  new long[]{to});
					
				}
				
				//add link from entity to After
				from = node_array[pos][polar.ordinal()][SubNodeType.e.ordinal()];
				to = node_array[pos][polar.ordinal()][SubNodeType.A.ordinal()];
				lcrfNetwork.addEdge(from,  new long[]{to});
				
				
				last_entity_pos = pos;
				last_polar = polar;
				
			}
			
		}
		
		
		//add the last column node to end
		if (polar != null)
		{
			for(int pos = last_entity_pos + 1; pos < size; pos++)
			{
				from = node_array[pos - 1][polar.ordinal()][SubNodeType.A.ordinal()];
				to = node_array[pos][polar.ordinal()][SubNodeType.A.ordinal()];
				lcrfNetwork.addEdge(from,  new long[]{to});
			}
			
			from = node_array[size - 1][polar.ordinal()][SubNodeType.A.ordinal()];
			to = end;
			lcrfNetwork.addEdge(from,  new long[]{to});
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
		
//		System.err.println(network.countNodes()+" nodes.");
//		System.exit(1);
		
		if (visual)
			visualize(network, "Sentiment Model:labeled", networkId);
		return network;
	}

	@Override
	public Network compileUnlabeled(int networkId, Instance instance, LocalNetworkParam param) {
		if (TargetSentimentGlobal.FIXNE)
		{
			return compileUnlabeledFixNE(networkId, instance, param);
		}
		
		TSInstance inst = (TSInstance)instance;
		NetworkBuilder<BaseNetwork> lcrfNetwork = NetworkBuilder.builder();
		
		List<String[]> inputs = (List<String[]>)inst.getInput();
		ArrayList<Label> outputs = (ArrayList<Label>)inst.getOutput();
		//OutputToken[] outputs = inst.getOutput();
		
		int size = inst.size();
		

		
		long start = this.toNode_start(size);
		lcrfNetwork.addNode(start);
		
		long[][][] node_array = new long[size][PolarityTypeSize][SubNodeTypeSize];
		
		//build node array
		for(int pos = 0; pos < size; pos++)
		{
			for(int polar = 0; polar < PolarityTypeSize; polar++)
			{
				for(int sub = 0; sub < SubNodeTypeSize; sub++)
				{
					long node = this.toNode_Span(size, pos, polar, sub);
					lcrfNetwork.addNode(node);
					node_array[pos][polar][sub] = node;
				}
			}
		}
		
		long end = this.toNode_end(inst.size());
		lcrfNetwork.addNode(end);
		

		long from = -1, to = -1;
		
		
			
		//add first column of span node from start
		for(int j = 0; j < PolarityTypeSize; j++)
		{
			from = start;
			//System.out.println("inputs[0]:" + inputs[0].getName() + "\tj:" + j + "\tB:" + SubNodeType.B.ordinal());
			//System.out.println("\t" + node_array[0][j].length);
			to = node_array[0][j][SubNodeType.B.ordinal()];
			lcrfNetwork.addEdge(from, new long[]{to});
		}
		
		

		
		for(int pos = 0; pos < size ; pos++)
		{
			for(int j = 0; j < PolarityTypeSize; j++)
			{
				//before to next before
				if (pos < size - 1)
				{
					from =  node_array[pos][j][SubNodeType.B.ordinal()];
					to = node_array[pos + 1][j][SubNodeType.B.ordinal()];
					lcrfNetwork.addEdge(from, new long[]{to});
				}
				
				//before to current entity
				from =  node_array[pos][j][SubNodeType.B.ordinal()];
				to = node_array[pos][j][SubNodeType.e.ordinal()];
				lcrfNetwork.addEdge(from, new long[]{to});
				
				
				
				
				//entity to after
				from =  node_array[pos][j][SubNodeType.e.ordinal()];
				to = node_array[pos][j][SubNodeType.A.ordinal()];
				lcrfNetwork.addEdge(from, new long[]{to});
				
				//entity to next entity
				if (pos < size - 1)
				{
					from =  node_array[pos][j][SubNodeType.e.ordinal()];
					to = node_array[pos + 1][j][SubNodeType.e.ordinal()];
					lcrfNetwork.addEdge(from, new long[]{to});
				}
				
				
				
				//after to next after
				if (pos < size - 1)
				{
					from =  node_array[pos][j][SubNodeType.A.ordinal()];
					to = node_array[pos + 1][j][SubNodeType.A.ordinal()];
					lcrfNetwork.addEdge(from, new long[]{to});
				}
				
				//after to next before
				if (pos < size - 1)
				{
				
					for(int k = 0; k < PolarityTypeSize; k++)
					{
						from =  node_array[pos][j][SubNodeType.A.ordinal()];
						to = node_array[pos + 1][k][SubNodeType.B.ordinal()];
						lcrfNetwork.addEdge(from, new long[]{to});
						
					}
				}
				
				
				
			}
			
		}
		
		//add last column of span node to end
		for(int j = 0; j < PolarityTypeSize; j++)
		{
			from = node_array[size - 1][j][SubNodeType.A.ordinal()];
			to = end;
			lcrfNetwork.addEdge(from, new long[]{to});
		}

	
		
		
		BaseNetwork network = lcrfNetwork.build(networkId, inst, param, this);
		
		return network;
		
//		System.err.println(network.countNodes()+" nodes.");
//		System.exit(1);
	}
	
	
	
	
	

}
