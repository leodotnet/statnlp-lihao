package org.statnlp.targetedsentiment.common;

import java.util.ArrayList;


public class TSFeature {
	
	public ArrayList<String[]>[] feature = null;
	public ArrayList<String[]>[] horizon_feature = null;
	
	public void init(int size)
	{
		feature = new ArrayList[size];
		horizon_feature = new ArrayList[size];
	}
	

}
