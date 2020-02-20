package org.statnlp.sentiment.spanmodel.common;

import cern.colt.Arrays;


public class Stats {
	/*
	public static void main(String[] args) {
		String a = "aaa<|>bbb";
		String[] b = a.split("<\\|>");
		System.out.println(Arrays.toString(b));
	}*/

	
	public static double MSE(int[] predicted, int[] gold) {
		double diff;
		int n = gold.length;
		double sum = 0.0;
		for (int i = 0; i < n; i++) {
			diff = gold[i] - predicted[i];
			sum = sum + diff * diff;
		}
		double mse = sum / n;

		return mse;
	}
	
	
	public static double Accuracy(int[] predicted, int[] gold) {
		int n = gold.length;
		int match = 0;
		for (int i = 0; i < n; i++) {
			if (predicted[i] == gold[i])
				match++;
		}
		
		double acc = (match + 0.0) / n;
		return acc;
	}
	
	public static double Sum(int[] x)
	{
		double sum = 0.0;
		for(int i = 0; i < x.length; i++)
			sum += x[i];
		return sum;
	}
	
	public static double Average(int[] x)
	{
		return Sum(x) / x.length;
	}
	
	public static double SquareError(int[] x)
	{
		double sum = 0.0;
		double x_average = Average(x);
		for(int i = 0; i < x.length; i++)
		{
			double diff = x[i] - x_average;
			sum += diff * diff;
		}
		
		return sum;
	}
	
	public static double PearsonCorrelation(int[] x, int[] y)
	{
		
		
		double SE_x = SquareError(x);
		double SE_y = SquareError(y);
		double avg_x = Average(x);
		double avg_y = Average(y);
		
		double sum_XY = 0.0;
		for(int i = 0; i < x.length; i++)
		{
			sum_XY += (x[i] - avg_x) * (y[i] - avg_y);	
		}
		
		double r = sum_XY / Math.sqrt(SE_x * SE_y);
		
		return r;
	}
	
	

}
