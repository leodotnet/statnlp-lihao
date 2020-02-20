package org.statnlp.sentiment.spanmodel.common;

import java.io.IOException;


import org.statnlp.commons.types.Instance;

import org.statnlp.hypergraph.decoding.Metric;

public class SentimentEval {

	public static Metric eval(Instance[] pred)
	{
		int[] predict = new int[pred.length];
		int[] gold = new int[pred.length];
		

		for(int i = 0; i < pred.length; i++)
		{
			predict[i] = (Integer)pred[i].getPrediction();
			gold[i] = (Integer)pred[i].getOutput();
		}
		
		return eval(predict, gold);
	}
	
	/**
	 * 
	 * @param testInsts
	 * @param nerOut: word, true pos, true entity, pred entity
	 * @throws IOException
	 */
	public static Metric eval(int[] predict, int[] gold){
		double acc = Stats.Accuracy(predict, gold);
		double mse = Stats.MSE(predict, gold);
		double rmse = Math.sqrt(mse);
		double r = Stats.PearsonCorrelation(predict, gold);
		return new SentimentMetric(acc, mse, rmse, r);
	}
}
