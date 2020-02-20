package org.entityrelation.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;

import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.example.linear_ne.NEMetric;
import org.statnlp.hypergraph.decoding.Metric;

import java.nio.file.Paths;



import cern.colt.Arrays;

public class EntityRelationEval {

	public static Metric eval(Instance[] insts)
	{
		EntityRelationMetric metric = new EntityRelationMetric();
		
		
		for(int i = 0; i < insts.length; i++)
		{
			EntityRelationInstance inst = (EntityRelationInstance)insts[i];
			
				
			ArrayList<String[]> input = (ArrayList<String[]>)inst.getInput(); 
			EntityRelationOutput gold = (EntityRelationOutput)inst.getOutput();
			EntityRelationOutput pred = (EntityRelationOutput)inst.getPrediction();
			
			
			
			evalEntityRelationSpanonSingleSentence(gold, pred, metric, input);
			
		}
		
		
		
		
		return metric.compute();
	}
	
	
	/**
	 * 
	 * @param testInsts
	 * @param nerOut: word, true pos, true entity, pred entity
	 * @throws IOException
	 */
	public static void evalEntityRelationSpanonSingleSentence(EntityRelationOutput golds, EntityRelationOutput preds, EntityRelationMetric metric, ArrayList<String[]> input){
		
		int[] span = new int[]{golds.entities.size(), preds.entities.size(), 0}; //gold, pred, match
		int[] onlySpan = new int[]{golds.entities.size(), preds.entities.size(), 0}; //gold, pred, match
		int[] relation = new int[]{golds.relations.size(), preds.relations.size(), 0}; //gold, pred, match
		
	
		for(Entity pred : preds.entities) {
			for(Entity gold : golds.entities) {
				if (pred.spanEquals(gold)) {
					span[2]++;
				}
				
				if (pred.onlySpanEquals(gold)) {
					onlySpan[2]++;
				}
				
			}
			
		}
		
		
		ArrayList<Entity> goldREntityList = new ArrayList<Entity>();
		
		for(Relation rel : golds.relations) {
			if (rel.isNORelation() || rel.isSelfRelation())
				continue;
			int p = EntityRelationOutput.getEntityIdx(goldREntityList, rel.arg1);
			if (p < 0)
				goldREntityList.add(rel.arg1);
			
			p = EntityRelationOutput.getEntityIdx(goldREntityList, rel.arg2);
			if (p < 0)
				goldREntityList.add(rel.arg2);
		}
		
		
		ArrayList<Entity> predREntityList = new ArrayList<Entity>();
		for(Relation rel : preds.relations) {
			if (rel.isNORelation() || rel.isSelfRelation())
				continue;
			int p = EntityRelationOutput.getEntityIdx(predREntityList, rel.arg1);
			if (p < 0)
				predREntityList.add(rel.arg1);
			
			p = EntityRelationOutput.getEntityIdx(predREntityList, rel.arg2);
			if (p < 0)
				predREntityList.add(rel.arg2);
		}
		
		
		int[] spanR = new int[]{0, 0, 0}; //gold, pred, match
		
		spanR[0] = goldREntityList.size();
		spanR[1] = predREntityList.size();
		
		for(Entity pred : predREntityList) {
			for(Entity gold : goldREntityList) {
				if (pred.spanEquals(gold)) {
					spanR[2]++;
				}
			}
			
		}
		
	
		
	
		
		
		int numPredRelation = 0; //Non-self-relation
		int numGoldRelation = 0;
		
		for(Relation pred : preds.relations) {
			
			if (pred.isSelfRelation())
				continue;
			
			if (pred.isNORelation()) 
				continue;
			
			numPredRelation++;
		}
		
		
		for(Relation gold : golds.relations) {
		
			if (gold.isSelfRelation())
				continue;
			
			if (gold.isNORelation()) 
				continue;
			
			numGoldRelation++;
		}
		
		
		for(Relation pred : preds.relations) {
			
			
			if (pred.isSelfRelation())
				continue;
			
			if (pred.isNORelation()) 
				continue;
			
			for(Relation gold : golds.relations) {
				
				
				if (gold.isSelfRelation())
					continue;
				
				if (gold.isNORelation())
					continue;
				
				if (pred.spanEquals(gold)) {
					relation[2]++;
				}
			}
			
		}
		
		relation[0] = numGoldRelation;
		relation[1] = numPredRelation;
	
		metric.spanStat = Utils.vectorAdd(metric.spanStat, span);
		metric.onlySpanStat = Utils.vectorAdd(metric.onlySpanStat, onlySpan);
		metric.relationStat = Utils.vectorAdd(metric.relationStat, relation);
		metric.spanRStat = Utils.vectorAdd(metric.spanRStat, spanR);
		
		
	}
	
	public static String evalScript = "scripts/pb-foc_evaluation.py";  //remember to make the script runnable
	
	public static void evalbyScript(String goldfile, String predfile, String workingDir){
		evalbyScript(EntityRelationDataConfig.pathJoin(workingDir, goldfile), EntityRelationDataConfig.pathJoin(workingDir , predfile));
	}
	
	public static void evalbyScript(String goldfile, String predfile){
		
		
		
		double fscore = 0;
		try{
			String cmd = "/usr/bin/python";// + evalScript ;//+" -g "+ goldfile + " -s " + predfile;
			System.err.println(cmd + " " + evalScript + " "+ predfile + " " + goldfile);
			ProcessBuilder pb = null;
			/*if(windows){
				pb = new ProcessBuilder("D:/Perl64/bin/perl","E:/Framework/data/semeval10t1/conlleval.pl"); 
			}else
			{*/
			pb = new ProcessBuilder(cmd, evalScript, predfile, goldfile); 
			//}
			//pb.redirectInput(new File(outputFile));
			//pb.redirectOutput(Redirect.INHERIT);
			//pb.redirectError(Redirect.INHERIT);
			Process process = pb.start();
			
			BufferedReader br = new BufferedReader(new InputStreamReader(process.getInputStream(), "UTF-8")) ;
			while (!br.ready()) ; // wait until buffered reader is ready.
			System.out.println("ready");
			while (br.ready()) {
				String line = br.readLine();
				System.out.println(line);
				/*
				if (line.startsWith("accuracy")) {
					String[] vals = line.trim().split("\\s+");
					fscore = Double.valueOf(vals[vals.length - 1]);
				}*/
			}
			br.close();
			pb = null;
		}catch(IOException ioe){
			ioe.printStackTrace();
		}
		//return new NEMetric(fscore);
	}
	
	/*
	public static void main(String[] args) {
		
	
		String goldfile = "data//EntityRelation//SEM-2012-SharedTask-CD-SCO-test-merge-GOLD.txt";
		String predfile = "experiments//EntityRelation//models//EntityRelationspanOIBAN6//en//20171201_discrete//SEM-2012-SharedTask-CD-SCO-test-merge-GOLD.txt.out";
		
		evalbyScript(goldfile, predfile);
	}*/
	
}
