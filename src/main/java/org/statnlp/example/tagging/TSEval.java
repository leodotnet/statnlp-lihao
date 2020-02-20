package org.statnlp.example.tagging;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Sentence;
import org.statnlp.hypergraph.decoding.Metric;
import org.statnlp.targetedsentiment.common.TSInstance;


public class TSEval {

	public static Metric eval(Instance[] insts)
	{
		TSMetric metric = new TSMetric();
		
		for(int i = 0; i < insts.length; i++)
		{
			TagInstance inst = (TagInstance)insts[i];
			Sentence sent = inst.getInput();
			List<String> gold = (List<String>)inst.getOutput();
			List<String> pred = (List<String>)inst.getPrediction();
			try{
			evalNERonSingleSentence(gold, pred, metric);
			}
			catch(Exception e) {
				System.err.println("evalNERonSingleSentence Error:" + e.getMessage());
				evalNERonSingleSentence(gold, pred, metric);
				System.err.println(sent);
				System.err.println(pred.toString());
				System.exit(0);
			}
		}
		return metric.compute();
	}
	
	/**
	 * 
	 * @param testInsts
	 * @param nerOut: word, true pos, true entity, pred entity
	 * @throws IOException
	 */
	public static void evalNERonSingleSentence(List<String> gold, List<String> pred, TSMetric metric){
		
		List<int[]> goldEntityList = getEntityList(gold);
		List<int[]> predEntityList = getEntityList(pred);
		
		//System.err.println(goldEntityList.size() + "\t" + predEntityList.size());
		
		int numEntityMatch = 0;
		int numSentimentMatch = 0;
		
		for(int[] pred_entity : predEntityList) {
			
			for(int[] gold_entity : goldEntityList) {
				if (pred_entity[0] == gold_entity[0] && pred_entity[1] == gold_entity[1]) {
					numEntityMatch++;
					
					if (pred_entity[2] == gold_entity[2]) {
						numSentimentMatch++;
					}
				}
			}
		}
		
		metric.numEntityGold += goldEntityList.size();
		metric.numEntityPred += predEntityList.size();
		metric.numEntityMatch += numEntityMatch;
		metric.numSentimentMatch += numSentimentMatch;
		
	}
	
	
	/***
	 * return an arrraylist of entity list <start pos, end pos>,  entities do not overlap with each other
	 * @param output
	 * @return
	 */
	public static List<int[]> getEntityList(List<String> output) {
		
		List<int[]> entityList = new ArrayList<int[]>();
		int size = output.size();
		for(int pos = 0; pos < size; pos++) {
			boolean startOfEntity = TaggingGlobal.startOfEntityStr(pos, size, output);
			boolean endOfEntity = TaggingGlobal.endofEntityStr(pos, size, output);
			
			if (startOfEntity) {
				
				String polarStr = output.get(pos).substring(2);
				int polarIdx = TSInstance.PolarityType.valueOf(polarStr).ordinal();
				
				entityList.add(new int[]{pos, -1, polarIdx});
			}
			
			if (endOfEntity) {
				entityList.get(entityList.size() - 1)[1] = pos;
			}
		}
		
		return entityList;
	}
}
