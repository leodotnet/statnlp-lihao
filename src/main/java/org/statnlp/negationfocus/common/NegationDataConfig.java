package org.statnlp.negationfocus.common;

import java.util.HashMap;

public class NegationDataConfig {

	public static String DATA = "data";

	public static String PRETRAIN_MODEL_PATH = "models";


	public static final HashMap<String, String> dataset2Path = new HashMap<String, String>() {
		{
			put("pbfoc_en", "pbfoc");
		}
	};

	public static final HashMap<String, String[]> dataset2Files = new HashMap<String, String[]>() {
		{
			//put("pbfoc_en", new String[] {"trial.txt", /*"SEM-2012-SharedTask-PB-FOC-tr.merged",*/ "SEM-2012-SharedTask-PB-FOC-de.merged",  "SEM-2012-SharedTask-PB-FOC-te.merged" });
			put("pbfoc_en", new String[] {"SEM-2012-SharedTask-PB-FOC-tr.merged", "SEM-2012-SharedTask-PB-FOC-de.merged",  "SEM-2012-SharedTask-PB-FOC-te.merged" });
		
		}
	};

	public static String[] NineFold = new String[] { "1", "2", "3", "4", "5", "6", "7", "8", "9" };
	public static String[] TenFold = new String[] { "0", "1", "2", "3", "4", "5", "6", "7", "8", "9" };

	public static final HashMap<String, String[]> dataset2Fold = new HashMap<String, String[]>() {
		{

		}
	};

	public static String pathJoin(String path, String filename) {
		return path + "//" + filename;
	}

	public static String[] getDataFiles(String dataSet, String lang) {
		return dataset2Files.get(dataSet + "_" + lang);
	}

	public static String[] getDataFold(String dataSet, String lang) {
		return dataset2Fold.get(dataSet + "_" + lang);
	}

	public static String getDataPath(String dataSet, String lang) {
		return pathJoin(DATA, dataset2Path.get(dataSet + "_" + lang));
	}

}
