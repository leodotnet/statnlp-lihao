package org.statnlp.targetedsentiment.common;

import java.util.Arrays;

import org.statnlp.hypergraph.decoding.Metric;

public class TSMetric implements Metric {

	public static String SEPARATOR = "\t";
	
	//P, R, F
	double[] NERStat = new double[3];
	double[] SentStat = new double[3];
	double[][] SentPolarStat = new double[3][3];

	

	
	public int numEntityGold = 0;
	public int numEntityPred = 0;
	public int numEntityMatch = 0;
	public int numSentimentMatch = 0;
	public int[] numSentimentPolarGold = new int[3]; // + 0 -
	public int[] numSentimentPolarPred = new int[3]; // + 0 -
	public int[] numSentimentPolarMatch = new int[3]; // + 0 -

	
	public boolean aggregation = false;
	
	public int numResult = 0;
	
	public TSMetric() {
		
	}
	
	public TSMetric(boolean aggregation) {
		this.aggregation = aggregation;
		
		if (this.aggregation) {
			Arrays.fill(NERStat, 0);
			Arrays.fill(SentStat, 0);
		}
	}
	
	public void aggregate(TSMetric metric) {
		
		if (this.aggregation) {
			numResult++;
			
			this.NERStat = vectorAdd(this.NERStat, metric.NERStat);
			this.SentStat = vectorAdd(this.SentStat, metric.SentStat);
			for(int i = 0; i< 3; i++){
				this.SentPolarStat[i] = vectorAdd(this.SentPolarStat[i], metric.SentPolarStat[i]);
			}
			
			this.numEntityGold += metric.numEntityGold;
			this.numEntityPred += metric.numEntityPred;
			this.numEntityMatch += metric.numEntityMatch;
			this.numSentimentMatch += metric.numSentimentMatch;
			
			for(int i = 0; i < 3; i++) {
				this.numSentimentPolarGold[i] += metric.numSentimentPolarGold[i];
				this.numSentimentPolarPred[i] += metric.numSentimentPolarPred[i];
				this.numSentimentPolarMatch[i] += metric.numSentimentPolarMatch[i];
			}
		}
		
	}
	
	double[] vectorAdd(double[] a, double[] b) {
		if (a.length != b.length)
			return null;
		
		double[] c = a.clone();
		for(int i = 0; i < c.length; i++)
			c[i] += b[i];
		return c;
	}
	
	double[] vectorScale(double[] a, double scale) {
		double[] c = a.clone();
		for(int i = 0; i < c.length; i++)
			c[i] *= scale;
		return c;
	}


	@Override
	public boolean isBetter(Metric other) {
		TSMetric metric = (TSMetric)other;
		if (metric.NERStat[2] == Double.NaN)
			metric.NERStat[2] = 0.0;
		//return NERStat[2] > metric.NERStat[2];
		//return SentStat[2] > metric.SentStat[2];
		return Math.abs(NERStat[2] - metric.NERStat[2]) < 1e-8 ? (SentStat[2] > metric.SentStat[2]) : NERStat[2] > metric.NERStat[2];
	}

	@Override
	public String getMetricValue() {
		return this.NERStat[2] + " " + this.SentStat[2];
	}
	
	String double2Str(double x) {
		return String.format ("%.2f", x * 100);
	}
	
	String metric2Str(double[] m) {
		String ret = "";
		for(int i = 0; i < m.length; i++) {
			ret += double2Str(m[i]) + this.SEPARATOR;
		}
		
		return ret;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("Metric  [P\tR\tF\n");
		sb.append("NER     [" + metric2Str(NERStat) + "]\n");
		sb.append("Sent    [" + metric2Str(SentStat) + "]\n");
		for(int i = 0; i< 3; i++){
			sb.append(TSInstance.PolarityType.values()[i] + " [" +  metric2Str(SentPolarStat[i]) + "]\n");
		}
		sb.append("#E Gold\t\t#E Pred\t\t#E Match\t\tS Match\n");
		sb.append("  " + numEntityGold + "\t\t");
		sb.append("  " + numEntityPred + "\t\t");
		sb.append("  " + numEntityMatch + "\t\t");
		sb.append("  " + numSentimentMatch + "\t\t");
		sb.append("\n");
		return sb.toString();
	}
	
	public TSMetric compute() {
		
		if (this.aggregation) {
			this.NERStat = this.vectorScale(this.NERStat, 1.0 / this.numResult);
			this.SentStat = this.vectorScale(this.SentStat, 1.0 / this.numResult);
			
			for(int i = 0; i< 3; i++){
				this.SentPolarStat[i] = this.vectorScale(this.SentPolarStat[i], 1.0 / this.numResult);
			}
			

			for(int i = 0; i < 3; i++) {
				this.numSentimentPolarGold[i] /= (this.numResult + 0.0);
				this.numSentimentPolarPred[i]  /= (this.numResult + 0.0);
				this.numSentimentPolarMatch[i]  /= (this.numResult + 0.0);
			}
		}
		else {
			this.NERStat = compute(numEntityGold, numEntityPred, numEntityMatch);
			this.SentStat =  compute(numEntityGold, numEntityPred, numSentimentMatch);
			
			for(int i = 0; i< 3; i++){
				this.SentPolarStat[i] = compute(numSentimentPolarGold[i], numSentimentPolarPred[i], numSentimentPolarMatch[i]);
			}
		}
		return this;
	}
	
	double[] compute(int gold, int pred, int match) {
		double[] stat = new double[3];
		
		double m = match;
		
		
		if (pred == 0) 
			stat[0] = 0;
		else
			stat[0] = m / pred; 
		
		stat[1] = m / gold;
		
		if (Math.abs(stat[0] + stat[1]) < 1e-8) 
			stat[2] = 0;
		else 
			stat[2] = 2 * stat[0] * stat[1] / (stat[0] + stat[1]);
		
		
		
		
		return stat;
	}
	
}
