package org.statnlp.example.tagging;

import org.statnlp.hypergraph.decoding.Metric;

public class TSMetric implements Metric {

	//P, R, F
	double[] NERStat = new double[3];
	double[] SentStat = new double[3];
	

	
	public int numEntityGold = 0;
	public int numEntityPred = 0;
	public int numEntityMatch = 0;
	public int numSentimentMatch = 0;
	
	public TSMetric() {
		
	}


	@Override
	public boolean isBetter(Metric other) {
		TSMetric metric = (TSMetric)other;
		return (NERStat[2] == metric.NERStat[2]) ? (SentStat[2] > metric.SentStat[2]) : NERStat[2] > metric.NERStat[2];
	}

	@Override
	public String getMetricValue() {
		return this.NERStat[2] + " " + this.SentStat[2];
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("NER  [F=" + NERStat[2] + ", P=" + NERStat[0] + ", R= " + NERStat[1] + "]\n");
		sb.append("Sent [F=" + SentStat[2] + ", P=" + SentStat[0] + ", R= " + SentStat[1] + "]\n");
		return sb.toString();
	}
	
	public TSMetric compute() {
		this.NERStat = compute(numEntityGold, numEntityPred, numEntityMatch);
		this.SentStat =  compute(numEntityGold, numEntityPred, numSentimentMatch);
		return this;
	}
	
	double[] compute(int gold, int pred, int match) {
		double[] stat = new double[3];
		
		double m = match;
		
		stat[0] = m / pred;
		stat[1] = m / gold;
		stat[2] = 2 * stat[0] * stat[1] / (stat[0] + stat[1]);
		
		return stat;
	}
	
}
