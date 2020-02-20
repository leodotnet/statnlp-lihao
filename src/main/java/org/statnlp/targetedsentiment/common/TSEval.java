package org.statnlp.targetedsentiment.common;

import java.io.IOException;
import java.util.ArrayList;

import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.hypergraph.decoding.Metric;

import cern.colt.Arrays;

public class TSEval {

	public static Metric eval(Instance[] insts)
	{
		TSMetric metric = new TSMetric();
		
		for(int i = 0; i < insts.length; i++)
		{
			TSInstance inst = (TSInstance)insts[i];
			ArrayList<Label> gold = (ArrayList<Label>)inst.getOutput();
			ArrayList<Label> pred = (ArrayList<Label>)inst.getPrediction();
			
			if (TargetSentimentGlobal.ALLOW_NULL_TARGET && TargetSentimentGlobal.FIXNE) {
				
				int numEntityMatch = 0;
				int numSentimentMatch = 0;
				
				for(int j = 0; j < inst.targets_pred.size(); j++) {
					
					int[] pred_entity = (int[])inst.targets_pred.get(j);
					int[] gold_entity = (int[])inst.targets.get(j);
					
					if (TargetSentimentGlobal.EVAL_EXCLUDE_NULL_TARGET && gold_entity[0] == -1)
						continue;
					
					
								
					numEntityMatch++;
					metric.numSentimentPolarPred[pred_entity[2]]++;
					metric.numSentimentPolarGold[gold_entity[2]]++;
					
					if (pred_entity[2] == gold_entity[2]) {
						numSentimentMatch++;
						metric.numSentimentPolarMatch[pred_entity[2]]++;
					}
					
					
				}
				
				metric.numEntityGold += numEntityMatch;
				metric.numEntityPred += numEntityMatch;
				metric.numEntityMatch += numEntityMatch;
				metric.numSentimentMatch += numSentimentMatch;
				
				
			} else {
				evalNERonSingleSentence(gold, pred, metric);
			}
			
			/*
			if (TargetSentimentGlobal.ALLOW_NULL_TARGET && inst.numNULLTarget > 0)
				evalNullTargetonSingleSentence(inst, metric);*/
		}
		return metric.compute();
	}
	
	
	public static void evalNullTargetonSingleSentence(TSInstance inst, TSMetric metric) {
		
		metric.numEntityGold += inst.numNULLTarget;
		metric.numEntityPred += inst.numNULLTarget;
		metric.numEntityMatch += inst.numNULLTarget;
		metric.numSentimentMatch += inst.numNULLTarget;
		
		if (inst.NULLTarget.equals(inst.NULLTargetPred)) {
		
			
			int polarGoldIdx = TSInstance.PolarityType.valueOf(inst.NULLTarget).ordinal();
			int polarPredIdx = TSInstance.PolarityType.valueOf(inst.NULLTargetPred).ordinal();
			
			metric.numSentimentPolarPred[polarGoldIdx] += inst.numNULLTarget;
			metric.numSentimentPolarGold[polarPredIdx] += inst.numNULLTarget;
			metric.numSentimentPolarMatch[polarGoldIdx] += inst.numNULLTarget;
		} else {
			
			
			int polarGoldIdx = TSInstance.PolarityType.valueOf(inst.NULLTarget).ordinal();
			int polarPredIdx = TSInstance.PolarityType.valueOf(inst.NULLTargetPred).ordinal();
			metric.numSentimentPolarPred[polarGoldIdx] += inst.numNULLTarget;
			metric.numSentimentPolarGold[polarPredIdx] += inst.numNULLTarget;
		}
	}
	
	/**
	 * 
	 * @param testInsts
	 * @param nerOut: word, true pos, true entity, pred entity
	 * @throws IOException
	 */
	public static void evalNERonSingleSentence(ArrayList<Label> gold, ArrayList<Label> pred, TSMetric metric){
		

		ArrayList<int[]> goldEntityList = getEntityList(gold);
		ArrayList<int[]> predEntityList = getEntityList(pred);
		
		int numEntityMatch = 0;
		int numSentimentMatch = 0;
		
		for(int[] pred_entity : predEntityList) {
			
			for(int[] gold_entity : goldEntityList) {
				if (pred_entity[0] == gold_entity[0] && pred_entity[1] == gold_entity[1]) {
					numEntityMatch++;
					
					metric.numSentimentPolarPred[pred_entity[2]]++;
					metric.numSentimentPolarGold[gold_entity[2]]++;
					
					if (pred_entity[2] == gold_entity[2]) {
						numSentimentMatch++;
						
						metric.numSentimentPolarMatch[pred_entity[2]]++;
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
	public static ArrayList<int[]> getEntityList(ArrayList<Label> output) {
		
		ArrayList<int[]> entityList = new ArrayList<int[]>();
		int size = output.size();
		for(int pos = 0; pos < size; pos++) {
			boolean startOfEntity = TargetSentimentGlobal.startOfEntity(pos, size, output);
			boolean endOfEntity = TargetSentimentGlobal.endofEntity(pos, size, output);
			
			if (startOfEntity) {
				
				String polarStr = output.get(pos).getForm().substring(2);
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
