package org.entityrelation.common;

import java.util.HashMap;

public class EntityRelationDataConfig {

	public static String DATA = "data";

	

	public static final HashMap<String, String> dataset2Path = new HashMap<String, String>() {
		{
			put("ace2005_en", "ace2005");
			put("ace2005-noOLhead_en", "ace2005-noOLhead");
		}
	};

	public static HashMap<String, String[]> dataset2Files = new HashMap<String, String[]>() {
		{
			put("ace2005_en", new String[] {"train.data", "dev.data", "test.data"});
			put("ace2005-noOLhead_en", new String[] {"train.data", "dev.data", "test.data"});
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
