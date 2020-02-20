package org.statnlp.example.descriptor;

import org.statnlp.hypergraph.decoding.Metric;

public class RelMetric implements Metric {

	double precision;
	double recall;
	double fscore;
	
	double addPrec;
	double addRec;
	double addfscore;

	public RelMetric(double fscore) {
		this(-1, -1, fscore);
	}
	
	public RelMetric(double precision, double recall, double fscore) {
		this.precision = precision;
		this.recall = recall;
		this.fscore = fscore;
	}
	
	public void setAdd(double precision, double recall, double fscore) {
		this.addPrec = precision;
		this.addRec = recall;
		this.addfscore = fscore;
	}

	public RelMetric(double[] metrics) {
		this(metrics[0], metrics[1], metrics[2]);
	}
	
	/**
	 * Micro score god first;
	 * @return
	 */
	public double[][] getAll () {
		double[][] all = new double[2][3];
		all[0][0] = addPrec; all[0][1] = addPrec;  all[0][2] = addPrec; 
		all[1][0] = precision; all[1][1] = recall;  all[1][2] = fscore;
		for (int i = 0; i< all.length; i++)
			for (int j = 0; j < all[i].length; j++)
				if (Double.isNaN(all[i][j]))
					all[i][j] = 0;
		return all;
	}
	
	@Override
	public boolean isBetter(Metric other) {
		RelMetric metric = (RelMetric)other;
		return fscore > metric.fscore;
	}

	@Override
	public Object getMetricValue() {
		return this.fscore;
	}
	
	@Override
	public String toString() {
		return "NEMetric [precision=" + precision + ", recall=" + recall + ", fscore=" + fscore + "]";
	}

}
