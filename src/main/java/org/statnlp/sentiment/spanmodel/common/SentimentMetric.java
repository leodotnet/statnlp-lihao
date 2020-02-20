package org.statnlp.sentiment.spanmodel.common;

import org.statnlp.hypergraph.decoding.Metric;

public class SentimentMetric implements Metric {

	double precision;
	double recall;
	double fscore;
	double mse;
	double rmse;
	double r;

	public SentimentMetric(double precision) {
		this(precision, -1, -1, -1);
	}
	
	public SentimentMetric(double precision, double mse, double rmse, double r) {
		this.precision = precision;
		this.mse = mse;
		this.rmse = rmse;
		this.r = r;
	}

	@Override
	public boolean isBetter(Metric other) {
		SentimentMetric metric = (SentimentMetric)other;
		return precision > metric.precision;
	}

	@Override
	public Double getMetricValue() {
		return this.precision;
	}

	@Override
	public String toString() {
		return "[precision=" + precision + ", mse=" + mse + ", rmse= " + rmse + ", r=" + r + "]";
	}
	
}
