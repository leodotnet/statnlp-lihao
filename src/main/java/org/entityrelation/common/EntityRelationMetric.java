package org.entityrelation.common;

import java.util.Arrays;

import org.statnlp.hypergraph.decoding.Metric;

public class EntityRelationMetric implements Metric {

	public static String SEPARATOR = "\t";
	
	public enum EVALOPTION {Span};
	public static EVALOPTION BESTON = EVALOPTION.Span;
	
	int[] spanStat = new int[3];  //gold pred match
	double[] spanRet = new double[3];  //P R F
	
	int[] onlySpanStat = new int[3];  //gold pred match
	double[] onlySpanRet = new double[3];  //P R F
	
	int[] spanRStat = new int[3];  //gold pred match
	double[] spanRRet = new double[3];  //P R F
	
	
	int[] relationStat = new int[3]; //gold pred match
	double[] relationRet = new double[3];  //P R F
	
	
	
	
	public boolean aggregation = false;
	
	public int numResult = 0;
	
	public EntityRelationMetric() {
		Arrays.fill(spanStat, 0);
		Arrays.fill(spanRet, 0);
		Arrays.fill(relationStat, 0);
		Arrays.fill(relationRet, 0);
		Arrays.fill(spanRStat, 0);
		Arrays.fill(spanRRet, 0);
		Arrays.fill(onlySpanStat, 0);
		Arrays.fill(onlySpanRet, 0);
		
	}
	
	public EntityRelationMetric(boolean aggregation) {
		this.aggregation = aggregation;
		
		if (this.aggregation) {
			numResult = 0;
			
			Arrays.fill(spanRet, 0);
			Arrays.fill(relationRet, 0);
			Arrays.fill(spanRRet, 0);
			Arrays.fill(onlySpanRet, 0);
		}
	}
	
	public void aggregate(EntityRelationMetric metric) {
		
		if (this.aggregation) {
			numResult++;
			
			this.spanRet = Utils.vectorAdd(this.spanRet, metric.spanRet);
			this.onlySpanRet = Utils.vectorAdd(this.onlySpanRet, metric.onlySpanRet);
			this.relationRet = Utils.vectorAdd(this.relationRet, metric.relationRet);
			this.spanRRet = Utils.vectorAdd(this.spanRRet, metric.spanRRet);
		}
		
	}

	@Override
	public boolean isBetter(Metric other) {
		EntityRelationMetric metric = (EntityRelationMetric)other;
		
		boolean better = false;
		
		if (BESTON == EVALOPTION.Span) {
			if (metric.spanRet[2] == Double.NaN)
			metric.spanRet[2] = 0.0;
			better = Math.abs(spanRet[2] - metric.spanRet[2]) > 1e-5 ? spanRet[2] > metric.spanRet[2] : relationRet[2] > metric.relationRet[2];
		} 
		
		
		return better;
	}

	@Override
	public String getMetricValue() {
		return this.spanRet[2] + "\t" + this.spanRRet[2] + "\t" + relationRet[2];
	}
	
	String double2Str(double x) {
		return String.format ("%.2f", x * 100);
	}
	
	String metric2Str(double[] m) {
		String ret = "";
		for(int i = 0; i < m.length; i++) {
			ret += double2Str(m[i]) + "" + this.SEPARATOR;
		}
		
		return ret;
	}
	
	String metric2Str(int[] m) {
		String ret = "";
		for(int i = 0; i < m.length; i++) {
			ret += m[i] + this.SEPARATOR;
		}
		
		return ret;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		/*
		sb.append("Metric (- Punc)   [P     \tR  \tF]\n");
		sb.append("Exact Scope A     [" + metric2Str(spanExactStatA) + "]\n");
		sb.append("Exact Scope A*    [" + metric2Str(numSpanExactA) + "]\n");
		
		sb.append("Exact Scope B     [" + metric2Str(spanExactStat) + "]\n");
		sb.append("Exact Scope B*    [" + metric2Str(numSpanExact) + "]\n");
		
		sb.append("PCS               " + double2Str(spanExactStatA[1]) + "\n");
		sb.append("Token Scope       [" + metric2Str(spanTokenStat) + "]\n");
		sb.append("Token Scope *     [" + metric2Str(numSpanToken) + "]\n");
	
		
		sb.append("\n");
		*/
		sb.append("Metric   [P     \tR  \tF]\n");
		
		/*
		sb.append("Exact Scope A     [" + metric2Str(spanExactStatAPunc) + "]\n");
		sb.append("Exact Scope A*    [" + metric2Str(numSpanExactAPunc) + "]\n");
		*/
		sb.append("Span Ret  [" + metric2Str(spanRet) + "]\n");
		sb.append("Span Stat [" + metric2Str(spanStat) + "]\n");
		
//		sb.append("SpanOnly Ret  [" + metric2Str(onlySpanRet) + "]\n");
//		sb.append("SpanOnly Stat [" + metric2Str(onlySpanStat) + "]\n");
		
		
		sb.append("SpanRRet  [" + metric2Str(spanRRet) + "]\n");
		sb.append("SpanRStat [" + metric2Str(spanRStat) + "]\n");
		
		/*
		sb.append("PCS               " + double2Str(spanExactStatAPunc[1]) + "\n");
		//sb.append("PCS *             " + total + "\n");
		 */
	
		
		sb.append("Rel Ret   [" + metric2Str(relationRet) + "]\n");
		sb.append("Rel Stat  [" + metric2Str(relationStat) + "]\n");
		
		if (this.aggregation) {
			sb.append("#Total Test: " + numResult  + "\n");
		}
		
		sb.append("\n");
		return sb.toString();
	}
	
	public EntityRelationMetric compute() {
		
		if (this.aggregation) {
			
			double r = 1.0 / this.numResult;
			
			this.spanRet = Utils.vectorScale(this.spanRet, r);
			this.relationRet = Utils.vectorScale(this.relationRet, r);
			this.spanRRet = Utils.vectorScale(this.spanRRet, r);
			this.onlySpanRet = Utils.vectorScale(this.onlySpanRet, r);
						
		}
		else {
			this.spanRet = compute(spanStat[0], spanStat[1], spanStat[2]);
			this.relationRet = compute(relationStat[0], relationStat[1], relationStat[2]);
			this.spanRRet = compute(spanRStat[0], spanRStat[1], spanRStat[2]);
			this.onlySpanRet = compute(onlySpanStat[0], onlySpanStat[1], onlySpanStat[2]);
			
		}
		return this;
	}
	
	double[] compute(int[] stat) {
		return compute(stat[0], stat[1], stat[2]);
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
