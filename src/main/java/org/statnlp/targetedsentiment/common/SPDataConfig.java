package org.statnlp.targetedsentiment.common;

import java.util.HashMap;

public class SPDataConfig {
	
	public static String DATA = "data";

	public static final HashMap<String , String> dataset2Path = new HashMap<String , String>(){
	{
		put("T_data_en", "T_data_en");
		put("Z_data_en", "Z_data_en");
		put("Zc_data_en", "Zc_data_en");
		put("opendomain_en", "Twitter_en");
		put("opendomain_es", "Twitter_es");
		put("semeval2016_rest_en",  "semeval2016//rest");
		
	}};
	
	
	public static final HashMap<String , String[]> dataset2Files = new HashMap<String , String[]>(){
		{
			put("Z_data_en", new String[]{"trn.dat", "dev.dat", "tst.dat"});
			put("Zc_data_en", new String[]{"trn.dat", "dev.dat", "tst.dat"});
			put("T_data_en", new String[]{"train.posneg", null, "test.posneg"});
			put("opendomain_en",  new String[]{"train.[*].coll", "dev.[*].coll", "test.[*].coll"});
			put("opendomain_es",  new String[]{"train.[*].coll", "dev.[*].coll", "test.[*].coll"});
			//put("semeval2016_rest_en",  new String[]{"train.11.coll", "dev.11.coll", "test.11.coll"});
			put("semeval2016_rest_en",  new String[]{"train.en.dat", "dev.en.dat", "test.en.dat"});
			
		}};
	
	public static String pathJoin(String path, String filename) {
		return path + "//" + filename;
	}
	
	public static String[] getDataFiles(String dataSet, String lang)
	{
		return dataset2Files.get(dataSet + "_" + lang);
	}
	
	public static String getDataPath(String dataSet, String lang)
	{
		return pathJoin(DATA, dataset2Path.get(dataSet + "_" + lang));
	}

}
