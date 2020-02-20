package org.statnlp.negationfocus.common;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

public class SemanticRole {
	
	public static int NOTYPE = -1;
	HashMap<String, Integer> type2Idx = new HashMap<String, Integer>();
	public ArrayList<String> roleNameList = new ArrayList<String>();
	
	public int[] roletype;
	public int[] roletypeBegin;
	
	
	public int l_focusScope_pos = -1;
	public int r_focusScope_pos = -1;
	
	public int l_focusVerb_pos = -1;
	public int r_focusVerb_pos = -1;
	
	public boolean combined = false;
	

	public SemanticRole() {
		
	}
	
	public SemanticRole(String[] col) {
		parseColumn(col);
	}
	
	
	public void parseColumn(String[] col) {
		
		roletype = new int[col.length];
		roletypeBegin = new int[col.length];
		roleNameList.clear();
		//type2Idx.clear();
		
		
		Arrays.fill(roletype, NOTYPE);
		Arrays.fill(roletypeBegin, 0);
		
		int currRoleIdx = NOTYPE;
		
		for(int i = 0; i < col.length; i++) {
			if (col[i].startsWith("(")) {
				String roleName = col[i].substring(1);
				if (roleName.endsWith(")")) {
					roleName = roleName.substring(0, roleName.length() - 1);
				}
				
				
				if (type2Idx.get(roleName) == null)
				{
					currRoleIdx = roleNameList.size();
					roleNameList.add(roleName);
					type2Idx.put(roleName, currRoleIdx);
				} else {
					currRoleIdx = type2Idx.get(roleName);
				}
				
				roletypeBegin[i] = 1;
				
				
			}
			
			roletype[i] = currRoleIdx;
			
			
			if (col[i].endsWith(")")) {
				currRoleIdx = NOTYPE;
			}
		}
	}
	
	


	public SemanticRole clone() {
		SemanticRole sr = new SemanticRole();
		//sr.type2Idx = this.type2Idx;
		sr.roleNameList = this.roleNameList;
		sr.roletype = this.roletype;
		return sr;
	}

}
