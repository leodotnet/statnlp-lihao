package org.statnlp.example.descriptor;

import org.statnlp.commons.types.Instance;

public class RelationDescriptorEvaluator {

	private boolean excludeOTHERRelation = false;
	
	public RelationDescriptorEvaluator() {
		
	}

	public RelationDescriptorEvaluator(boolean excludeOTHERRelation) {
		this.excludeOTHERRelation = excludeOTHERRelation;
	}
	
	/**
	 * Before using this, make sure the type should be gold type, otherwise, should 
	 * do joint evaluation.
	 * Two metrics are returned, one is overlapping match accuracy, 
	 * the other is exact match. 
	 * @param results: accuracy, precision, recall, fscore
	 * @return
	 */
	public double[][] evaluateOverlapAndExactMatchDescriptor(Instance[] results) {
		double[][] metrics = new double[2][4];
		int overlapCount = 0;
		int totalCount = 0;
		int overlapCorr = 0;
		int totalPredict = 0;
		int totalExpected = 0;
		int exactCorr = 0;
		int exactCount = 0;
		for (Instance inst : results) {
			RelInstance res = (RelInstance)inst;
			RelationDescriptor prediction = res.getPrediction();
			RelationDescriptor gold = res.getOutput();
			if (prediction.left >= 0 && prediction.right >=0 && gold.left >=0 && gold.right >= 0) {
				if (prediction.left > gold.right || prediction.right < gold.left) {
					//do nothing
				} else {
					overlapCorr++;
					overlapCount++;
				}
				if (prediction.left == gold.left && prediction.right == gold.right) {
					exactCorr++;
					exactCount++;
				}
			} else if (prediction.left < 0 && prediction.right < 0 && gold.left < 0 && gold.right < 0) {
				overlapCount++;
				exactCount++;
			} 
			if (gold.left >= 0 && gold.right >= 0) {
				totalExpected++;
			}
			if (prediction.left >= 0 && prediction.right >= 0) {
				totalPredict++;
			}
			totalCount++;
		}
		metrics[0][0] = overlapCount * 1.0 / totalCount *100;
		metrics[0][1] = overlapCorr * 1.0 / totalPredict * 100;
		metrics[0][2] = overlapCorr * 1.0 / totalExpected * 100;
		metrics[0][3] = 2.0 * overlapCorr / (totalPredict + totalExpected) * 100;
		metrics[1][0] = exactCount * 1.0 / totalCount * 100;
		metrics[1][1] = exactCorr * 1.0 / totalPredict * 100;
		metrics[1][2] = exactCorr * 1.0 / totalExpected * 100;
		metrics[1][3] = 2.0 * exactCorr / (totalPredict + totalExpected) * 100;
		System.out.printf("[Result Overlap Match] Acc.: %.2f%%, Prec: %.2f%%, Rec.: %.2f%%, F1.: %.2f%%\n", metrics[0][0], metrics[0][1], metrics[0][2], metrics[0][3]);
		System.out.printf("[Result Exact Match] Acc.: %.2f%%, Prec: %.2f%%, Rec.: %.2f%%, F1.: %.2f%%\n", metrics[1][0], metrics[1][1], metrics[1][2], metrics[1][3]);
		return metrics;
	}
	
	/**
	 * Evaluate the relation
	 * @param results
	 * @return the precision, recall and fscore
	 */
	public RelMetric evaluateRelation(Instance[] results) {
		//calculating the precision fist.
		int[] p = new int[RelationType.RELS.size()];
		int[] totalPredict = new int[RelationType.RELS.size()];
		int[] totalInData = new int[RelationType.RELS.size()];
		double[] metrics = new double[3];
		for (Instance inst : results) {
			RelInstance res = (RelInstance)inst;
			RelationType prediction = res.getPrediction().type;
			RelationType gold = res.getOutput().type;
			String predForm = prediction.form;
			String goldForm = gold.form;
			int corrPredId = prediction.id;
			int corrGoldId = gold.id;
			if (predForm.endsWith(Config.REV_SUFF)) {
				if (RelationType.RELS.containsKey(predForm.replace(Config.REV_SUFF, "")))
				corrPredId = RelationType.get(predForm.replace(Config.REV_SUFF, "")).id;
			}
			if (goldForm.endsWith(Config.REV_SUFF)) {
				if (RelationType.RELS.containsKey(goldForm.replace(Config.REV_SUFF, "")))
				corrGoldId = RelationType.get(goldForm.replace(Config.REV_SUFF, "")).id;
			}
			if (prediction.equals(gold)) {
				p[corrPredId]++;
			}
			totalPredict[corrPredId]++;
			totalInData[corrGoldId]++;
		}
		
		int allP = 0;
		int allPredict = 0;
		int allInData = 0;
		double macroP = 0.0;
		double macroR = 0.0;
		double macroF = 0.0;
		int num =0;
		for (int r = 0; r < RelationType.RELS.size(); r++) {
			if (RelationType.get(r).form.equals(Config.NR) || RelationType.get(r).form.endsWith(Config.REV_SUFF)) continue;
			double precision = p[r] * 1.0/totalPredict[r] * 100;
			double recall = p[r] * 1.0 / totalInData[r] * 100;
			double fscore = 2.0 * p[r] / (totalPredict[r] + totalInData[r]) * 100;
			String spacing = "\t";
			System.out.printf("[Result] %s: %sPrec.:%.2f%%\tRec.:%.2f%%\tF1.:%.2f%%\n", 
					RelationType.get(r).form, spacing, precision, recall, fscore);
			if (this.excludeOTHERRelation && RelationType.get(r).form.equals("Other")) continue;
			macroP += precision;
			macroR += recall;
			macroF += fscore;
			num++;
			allP += p[r];
			allPredict += totalPredict[r];
			allInData += totalInData[r];
		}
		double precision = allP * 1.0/ allPredict * 100;
		double recall = allP * 1.0 / allInData * 100;
		double fscore = 2.0 * allP / (allPredict + allInData) * 100;
		metrics[0] = precision;
		metrics[1] = recall;
		metrics[2] = fscore;
		macroP /= num;
		macroR /= num;
		macroF /= num;
		System.out.printf("[Result] All micro: \t\tPrec.:%.2f%%\tRec.:%.2f%%\tF1.:%.2f%%\n", 
				precision, recall, fscore);
		System.out.printf("[Result] All macro: \t\tPrec.:%.2f%%\tRec.:%.2f%%\tF1.:%.2f%%\n", 
				macroP, macroR, macroF);
		RelMetric metric = new RelMetric(new double[]{macroP, macroR, macroF});
		metric.setAdd(precision, recall, fscore);
		return metric;
	}
	
}
