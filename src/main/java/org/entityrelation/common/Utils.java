package org.entityrelation.common;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.jgrapht.Graph;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.DijkstraShortestPath;
import org.jgrapht.graph.DefaultEdge;
import org.jgrapht.graph.SimpleGraph;
import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.commons.types.Sentence;
import org.statnlp.hypergraph.Network;
import org.statnlp.hypergraph.NetworkConfig;

import gnu.trove.stack.TIntStack;
import gnu.trove.stack.array.TIntArrayStack;


public class Utils {
	
	public static Random rnd = new Random(NetworkConfig.RANDOM_INIT_FEATURE_SEED);
	
	public static class Counter {
		HashMap<String, Integer> wordCount = new HashMap<String, Integer>();
		
		HashMap<String, Double> wordCountPercentage = new HashMap<String, Double>();
		
		public int maxCount = -0;
		
		public Counter() {
			wordCount.clear();
		}
		
		public int addWord(String word) {
			Integer count = wordCount.get(word);
			if (count == null) {		
				count = 1;
			} else {
				count = count + 1;
			}
			
			wordCount.put(word, count);
			
			return count;
		}
		
		public int getCount(String word) {
			Integer count = wordCount.get(word);
			return (count == null) ? 0 : count;
		}
		
		public int getVocabSize() {
			return wordCount.size();
		}
		
		public void computeFrequency() {
			int sum = 0;
			for(String key : this.wordCount.keySet()) {
				int v = this.wordCount.get(key);;
				sum += v;
				if (maxCount < v)
					maxCount = v;
			}
			
			wordCountPercentage.clear();
			
			for(String key : this.wordCount.keySet()) {
				double v = this.wordCount.get(key);
				v = v / sum;
				wordCountPercentage.put(key, v);
				
			}
		}
		
		@Override
		public String toString() {
			if (wordCountPercentage.isEmpty()) {
				computeFrequency();
			}
			
			StringBuffer sb = new StringBuffer();
			sb.append("#Key\tCount\tPer%\n");
			for(String key : this.wordCount.keySet()) {
				int count = wordCount.get(key);
				double v = wordCountPercentage.get(key);
				sb.append(key + "\t" + count + "\t" + v + "\n" );
			}
			
			
			return sb.toString();
			
		}
	}
	
	public static class IntStat {
		HashMap<Integer, Integer> stats = new HashMap<Integer, Integer>();
		
		public IntStat() {
			stats.clear();
		}
		
		public int addInt(int a) {
			Integer count = stats.get(a);
			if (count == null) {
				count = 1;
			} else {
				count = count + 1;
			}
			
			stats.put(a, count);
			return count;
		}
		
		public int getCount(int a) {
			Integer count = stats.get(a);
			return (count == null) ? 0 : count;
		}
		
		int getTotalCount() {
			int total = 0;
			for(Integer key : stats.keySet()) {
				total += stats.get(key);
			}
			return total;
		}
		
		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			double total = this.getTotalCount();
			for(Integer i : stats.keySet()) {
				sb.append(i + "\t" + stats.get(i) + "\t" + (stats.get(i) * 100 / total) + "\n");
			}
			
			
			
			return sb.toString();
		}
	}

	
	public static int[] sorted(int[] arr)
	{
		int[] arr_sorted = arr.clone();
		Arrays.sort(arr_sorted);
		return arr_sorted;
	}
	
	public static EntityRelationInstance[] portionInstances(EntityRelationInstance[] instances, double percentage) {
		return portionInstances(instances, (int)(percentage * instances.length));
	}
	
	public static EntityRelationInstance[] portionInstances(EntityRelationInstance[] instances, int num) {
		//EntityRelationInstance[] insts = new EntityRelationInstance[num];
		if (num > instances.length)
			num = instances.length;
		System.out.println("Truncate " + num + " instances.");
		return Arrays.copyOf(instances, num);
	}
	
	public static EntityRelationInstance[] mergeInstances(EntityRelationInstance[] instances1, EntityRelationInstance[] instances2) {
		EntityRelationInstance[] instances = new EntityRelationInstance[instances1.length + instances2.length];
		for(int i = 0; i < instances1.length; i++)
			instances[i] = instances1[i];
		
		
		
		for(int i = 0; i < instances2.length; i++) {
			instances[instances1.length + i] = instances2[i];
			instances[instances1.length + i].setInstanceId(instances1.length + 1 + i);
		}
		
		return instances;
	}
	
	public static boolean isPunctuation(char c) {
        return c == ','
            || c == '.'
            || c == '!'
            || c == '?'
            || c == ':'
            || c == ';'
            || c == '`'
            ;
    }
	
	public static boolean isPunctuation(String s) {
        return (s.length() == 1 && isPunctuation(s.charAt(0))) || (s.equals("-LRB-") || s.equals("-RRB-") || s.equals("``") || s.equals("''"));
    }
	
	public static void writeVocab(String filename, EntityRelationInstance[][] instancesList, boolean lowercase) {
		PrintWriter p = null;
		try {
			p = new PrintWriter(new File(filename), "UTF-8");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (EntityRelationInstance[] instances : instancesList)
			for (EntityRelationInstance inst : instances) {
				ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();
				for (String[] token : input) {
					;
					String output = token[EntityRelationInstance.FEATURE_TYPES.word.ordinal()];
					if (lowercase)
						output = output.toLowerCase();
					p.println(output);
				}
			}
		
		p.close();
	}
	
	public static boolean startOfEntity(int pos, int size, ArrayList<Label> outputs) {
		String label = outputs.get(pos).getForm();
		if (label.startsWith("B"))
			return true;

		if (pos == 0 && label.startsWith("I"))
			return true;

		if (pos > 0) {
			String prev_label = outputs.get(pos - 1).getForm();
			if (label.startsWith("I") && prev_label.startsWith("O"))
				return true;
		}

		return false;
	}

	public static boolean endofEntity(int pos, int size, ArrayList<Label> outputs) {
		String label = outputs.get(pos).getForm();
		if (!label.startsWith("O")) {
			if (pos == size - 1)
				return true;
			else {
				String next_label = outputs.get(pos + 1).getForm();
				if (next_label.startsWith("O") || next_label.startsWith("B"))
					return true;
			}
		}

		return false;
	}
	
	
	public void addPrefix(ArrayList<String[]> featureArr, String[] f, String prefix, String Sent, String NE) {

		featureArr.add(new String[] { prefix + f[0], f[1], f[2] });
	}
	
	public String escape(String s) {
		for (Character val : string_map.keySet()) {
			String target = val + "";
			if (s.indexOf(target) >= 0) {
				String repl = string_map.get(val);
				s = s.replace(target, "");

			}
		}

		return s;
	}

	public String norm_digits(String s) {
		s = s.replaceAll("\\d+", "0");

		return s;

	}

	public String clean(String s) {
		String str;
		if (s.startsWith("http://") || s.startsWith("https://")) {
			str = "<WEBLINK>";
		} else if (s.startsWith("@")) {
			str = "<USERNAME>";
		} else {
			str = norm_digits(s.toLowerCase());
			// String str1 = escape(str);
			// if (str1.length() >= 0)

			String str1 = str.replaceAll("[^A-Za-z0-9_]", "");
			if (str1.length() > 0)
				str = str1;
		}

		return str;
	}

	public static final HashMap<Character, String> string_map = new HashMap<Character, String>() {
		{
			put('.', "_P_");
			put(',', "_C_");
			put('\'', "_A_");
			put('%', "_PCT_");
			put('-', "_DASH_");
			put('$', "_DOL_");
			put('&', "_AMP_");
			put(':', "_COL_");
			put(';', "_SCOL_");
			put('\\', "_BSL_");
			put('/', "_SL_");
			put('`', "_QT_");
			put('?', "_Q_");
			put('¿', "_QQ_");
			put('=', "_EQ_");
			put('*', "_ST_");
			put('!', "_E_");
			put('¡', "_EE_");
			put('#', "_HSH_");
			put('@', "_AT_");
			put('(', "_LBR_");
			put(')', "_RBR_");
			put('\"', "_QT0_");
			put('Á', "_A_ACNT_");
			put('É', "_E_ACNT_");
			put('Í', "_I_ACNT_");
			put('Ó', "_O_ACNT_");
			put('Ú', "_U_ACNT_");
			put('Ü', "_U_ACNT0_");
			put('Ñ', "_N_ACNT_");
			put('á', "_a_ACNT_");
			put('é', "_e_ACNT_");
			put('í', "_i_ACNT_");
			put('ó', "_o_ACNT_");
			put('ú', "_u_ACNT_");
			put('ü', "_u_ACNT0_");
			put('ñ', "_n_ACNT_");
			put('º', "_deg_ACNT_");
		}
	};
	
	public static String getToken(ArrayList<String[]> inputs, int pos, int idx) {
		if (idx >= 0 && idx < inputs.get(pos).length) {
			return inputs.get(pos)[idx];
		}  else {
			return "<PAD>";
		}
	}

	public static boolean isAllCap(String word) {
		for (int i = 0; i < word.length(); i++)
			if (Character.isLowerCase(word.charAt(i)))
				return false;

		return true;
	}
	
	public static boolean isFirstCap(String word) {
		return !Character.isLowerCase(word.charAt(0));
	}
	
	public static double[] vectorAdd(double[] a, double[] b) {
		
		if (a == null)
			return b;
		

		if (a.length != b.length)
			return null;
		
		double[] c = a.clone();
		for(int i = 0; i < c.length; i++)
			c[i] += b[i];
		return c;
	}
	
	public static int[] vectorAdd(int[] a, int[] b) {
		if (a.length != b.length)
			return null;
		
		int[] c = a.clone();
		for(int i = 0; i < c.length; i++)
			c[i] += b[i];
		return c;
	}
	
	public static double[] vectorScale(double[] a, double scale) {
		double[] c = a.clone();
		for(int i = 0; i < c.length; i++)
			c[i] *= scale;
		return c;
	}
	
	public static double center(int[] a) {
		int sum = 0;
		int count = 0;
		for(int i = 0; i < a.length; i++) {
			count += a[i];
			sum += a[i] * i;
		}
		
		return (sum + 0.0) / count;
	}
	
	public static boolean isNumeric(String str)
	{
	  return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
	}
	
	public static boolean listContains(List<int[]> list, int[] o) {
		for(int i = 0; i < list.size(); i++) {
			if (Arrays.equals(list.get(i), o)) 
				return true;
		}
		return false;
	}
	
	public static String getPhrase(List<String[]> inputs, int fromIdx, int toIdx, int featureIdx) {
		return getPhrase(inputs, fromIdx, toIdx, featureIdx, true);
	}
	
	public static String getPhrase(List<String[]> inputs, int fromIdx, int toIdx, int featureIdx, boolean isToLowercase) {
		List<String> list = new ArrayList<String>();
		for(int i = fromIdx; i <= toIdx && i < inputs.size(); i++) {
			if (isToLowercase)
				list.add(inputs.get(i)[featureIdx].toLowerCase());
			else
				list.add(inputs.get(i)[featureIdx]);
		}
		
		return join(" ", list);
	}
	
	public static List<String> getPhraseList(List<String[]> inputs, int fromIdx, int toIdx, int featureIdx, boolean isToLowercase) {
		List<String> list = new ArrayList<String>();
		for(int i = fromIdx; i <= toIdx && i < inputs.size(); i++) {
			if (isToLowercase)
				list.add(inputs.get(i)[featureIdx].toLowerCase());
			else
				list.add(inputs.get(i)[featureIdx]);
		}
		
		return list;
	}
	
	
	public static List<String> getPhraseList(List<String[]> inputs, int featureIdx, boolean isToLowercase) {
		List<String> list = new ArrayList<String>();
		for(int i = 0; i < inputs.size(); i++) {
			if (isToLowercase)
				list.add(inputs.get(i)[featureIdx].toLowerCase());
			else
				list.add(inputs.get(i)[featureIdx]);
		}
		
		return list;
	}
	
	
	

	public static String join(String Separator, List<String> list) {
		String ret = list.get(0).toString();
		
		for(int i = 1; i < list.size(); i++) {
			ret += Separator + list.get(i).toString();
		}
		
		return ret;
	}
	
	
	public static String join(List list, String sep) {
		String ret = "";
		if (list.size() > 0) {
			ret = list.get(0).toString();
			
			for(int i = 1; i < list.size(); i++) {
				ret += sep + list.get(i).toString();
			}
			
		}
		
		
		return ret;
	}
	
	
	public static int getPosNextToken(List<String[]> inputs, String target, int fromIdx, int featureIdx) {
		
		for(int i = fromIdx; i < inputs.size() ; i++) {
			String token = inputs.get(i)[featureIdx];
			if (token.equals(target))
				return i;
		}
		
		return -1;
	}
	
	public static int[] getBoundary(int[] a) {
		int[] boundary = new int[] {0, a.length - 1};
		for(int i = 0; i < a.length; i++) {
			if (a[i] == 1) {
				boundary[0] = i;
				break;
			}
		}
		
		for(int i = a.length - 1; i >= 0; i--) {
			if (a[i] == 1) {
				boundary[1] = i;
				break;
			}
		}
		
		return boundary;
	}
	
	public static ArrayList<EntityRelationInstance> convertOutputPred(EntityRelationInstance inst) {
		return null;
	}
	
	
	public static int countVal(int[] a, int val) {
		int count = 0;
		for(int i = 0; i < a.length; i++)
			if (a[i] == val)
				count++;
		
		return count;
	}
	
	
	public static double cueF(int[] cueGold, int[] cuePred) {
		
		int size = cueGold.length;
		
		int matched = 0;
		for(int i = 0; i < size; i++) {
			if (cueGold[i] == 1 && cueGold[i] == cuePred[i]) {
				matched++;
			}
		}
		
		int spanGold = countVal(cueGold, 1);
		int spanPred = countVal(cuePred, 1);
		
		double p = (matched + 0.0) / spanPred;
		double r = (matched + 0.0) / spanGold;
		double f = (Math.abs(p + r) < 1e-5) ? 0 : 2 * p * r / (p + r);
		
		return f;
	}
	
	public static boolean spanArrayEqual(int[] cueGold, int[] cuePred) {
		if (cueGold.length != cuePred.length)
			return false;
		
		for(int i = 0; i < cueGold.length; i++)
			if (cueGold[i] != cuePred[i])
				return false;
		
		return true;
	}
	
	
	public static boolean spanArrayEqual(int[] cueGold, int from, int to) {
		return (cueGold[from] == 1 && cueGold[to - 1] == 1);
	}
	
	
	public static int getSpanEnd(int[] a, int val, int startIdx) { //exclusive
		for(int i = startIdx; i < a.length; i++) {
			if (a[i] != val)
				return i;
		}
		
		return a.length;
	}
	
	public static int[] getNumSpan(int[] a, int val) {
		int p = 0;
		int spanCount = 0;
		
		ArrayList<Integer> spanLengthCount = new ArrayList<Integer>();
		
		while(p < a.length) {
			if (a[p] == val) {
				int endIdx = getSpanEnd(a, val, p);
				spanLengthCount.add(endIdx - p);
				spanCount++;
				p = endIdx - 1;
				
				
			}
			
			p++;
		}
		
		int[] spanLengthArr = new int[spanLengthCount.size()];
		for(int i = 0; i < spanLengthArr.length; i++)
			spanLengthArr[i] = spanLengthCount.get(i);
		
		return spanLengthArr;
	}
	
	
	public static ArrayList<int[]> getAllSpans(int[] a, int val) { //fromIdx, endIdx, val
		int p = 0;
		
		ArrayList<int[]> spans = new ArrayList<int[]>();
		
		while(p < a.length) {
			if (a[p] == val) {
				int endIdx = getSpanEnd(a, val, p);
				spans.add(new int[] {p, endIdx, val});
				p = endIdx - 1;
				
			}
			
			p++;
		}
		
		return spans;
	}
	
	
	public static void setUnlabel(Instance[] insts) {
		for(Instance inst : insts)
			inst.setUnlabeled();
	}
	
	
	
	public static List<Integer> getShortestDepPath(EntityRelationInstance inst, ArrayList<String[]> input, Entity arg1Span, Entity arg2Span) {
		Graph<Integer, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);
		int size = input.size();
		int[] des = new int[size];
		for (int i = 0; i < size; i++) {
			g.addVertex(i);
		}
		for (int i = 0; i < size; i++) {
			int head = inst.depIdx[i];
			if (head != -1) {
				g.addEdge(i, head);
			}
		}
		DijkstraShortestPath<Integer, DefaultEdge> dg = new DijkstraShortestPath<>(g);
		GraphPath<Integer, DefaultEdge> results = dg.getPath(arg1Span.span[1], arg2Span.span[1]);
		List<Integer> list =  results.getVertexList();
		StringBuilder sb = new StringBuilder();
		/*
		for (int v : list) {
			if (v >= arg1Span.span[0] && v <= arg1Span.span[1]) continue;
			if (v >= arg2Span.span[0] && v <= arg2Span.span[1]) continue;
			if (v == arg1Span.span[1] || v == arg2Span.span[1]) continue;
			des[v] = 1;
			sb.append(input.get(v)[0] + " ");
		}*/
//		System.out.println(sb.toString());
		return list;
	}
	
	
	public static int[] getShortestDepPath(EntityRelationInstance inst, ArrayList<String[]> input, int i, int k) {
		
		
		Graph<Integer, DefaultEdge> g = new SimpleGraph<>(DefaultEdge.class);
		int size = input.size();
		
		for (int j = 0; j < size; j++) {
			g.addVertex(j);
		}
		for (int j = 0; j < size; j++) {
			int head = inst.depIdx[j];
			if (head != -1) {
				g.addEdge(j, head);
			}
		}
		DijkstraShortestPath<Integer, DefaultEdge> dg = new DijkstraShortestPath<>(g);
		GraphPath<Integer, DefaultEdge> results = dg.getPath(i, k);
		List<Integer> list =  results.getVertexList();
		
		int[] des = new int[list.size()];
		for(int j = 0; j < des.length; j++) {
			des[j] = list.get(j);
		}
		/*
		StringBuilder sb = new StringBuilder();
		for (int v : list) {
			des[v] = 1;
			sb.append(input.get(v)[0] + " ");
		}*/
//		System.out.println(sb.toString());
		return des;
	}
	
	
	public static String getPhrase(List<String[]> inputs, int[] path) {
		return getPhrase(inputs, path, 0, false);
	}
	
	public static String getPhrase(List<String[]> inputs, int[] path, int featureIdx, boolean isToLowercase) {
		List<String> pathList = new ArrayList<String>();
		for(int i = 0; i < path.length; i++) {
			pathList.add(inputs.get(path[i])[featureIdx]);
		}
		return join("-", pathList);
	}
	
	public static void writeLog(String filename, String content) throws FileNotFoundException {
		PrintWriter p = new PrintWriter(filename);
		p.print(content);
		p.close();
	}
	
	
	public static List<String[]> flipList(List<String[]> input) {
		List<String[]> input_flip = new ArrayList<String[]>();
		for(int i = input.size() - 1; i >= 0; i--) {
			input_flip.add(input.get(i));
		}
		return input_flip;
	}
	
	
	public static int[] flipArray(int[] input) {
		int[] input_flip = new int[input.length];
		for(int i = input.length - 1; i >= 0; i--) {
			input_flip[input.length - i - 1] = input[i];
		}
		return input_flip;
	}
	
	public static String getGeneralTag(String posTag) {
		if (posTag.equals("CD") || posTag.equals("JJ") || posTag.equals("JJR") || posTag.equals("JJS"))
			return "ADJ";
		else if (posTag.equals("VB") || posTag.equals("VBD") || posTag.equals("VBG") || posTag.equals("VBN") || posTag.equals("VBP") || posTag.equals("VBZ")|| posTag.equals("MD"))
			return "V";
		else if (posTag.equals("NN") || posTag.equals("NNS") || posTag.equals("NNP") || posTag.equals("NNPS"))
			return "N";
		else if (posTag.equals("RB") || posTag.equals("RBR") || posTag.equals("RBS") || posTag.equals("RP") || posTag.equals("WRB"))
			return "ADV";
		else if (posTag.equals("DET") || posTag.equals("PDT") || posTag.equals("WDT") || posTag.equals("POS"))
			return "DET";
		else if (posTag.equals("PRP") || posTag.equals("WP")) 
			return "PRP";
		else if (posTag.equals("PRP$") || posTag.equals("WP$"))
			return "PRP$";
		else if (posTag.equals("TO") || posTag.equals("IN"))
			return "PREP";
		else if (posTag.equals("CC"))
			return "CONJ";
		else if (posTag.equals("EX") || posTag.equals("FW") || posTag.equals("SYM") || posTag.equals("UH") || posTag.equals("LS"))
			return "OTHER";
		else return posTag;
	}
	
	
//	/**
//	 * Check if hm1Idx and hm2Idx are equal to each other.
//	 * @param sent
//	 * @param hm1Idx
//	 * @param hm2Idx
//	 */
//	public static void findPath(Sentence sent, int hm1Idx, int hm2Idx, Network network, String rel, List<Integer> fs) {
//		StringBuilder path = new StringBuilder("path:");
//		TIntStack stack1 = new TIntArrayStack();
//		TIntStack stack2 = new TIntArrayStack();
//		boolean found1 =  sent.depTree.dfsFind(hm1Idx, stack1);
//		boolean found2 = sent.depTree.dfsFind(hm2Idx, stack2);
//		if (!(found1 && found2)) throw new RuntimeException("Not found in the dependency tree?");
//		int[] trace1 = stack1.toArray();
//		int[] trace2 = stack2.toArray();
//		int contIdx1 = containsElement(trace1, hm2Idx);
//		int contIdx2 = containsElement(trace2, hm1Idx);
//		if (contIdx1 >= 0 || contIdx2 >= 0) {
//			if (contIdx1 >= 0 && contIdx2 >= 0 && hm1Idx != hm2Idx) throw new RuntimeException("impossible");
//			if (contIdx1 >= 0) {
//				for (int k = 0; k < contIdx1 ; k++) { // because the trace is backtracked.
//					String depLabel = sent.get(trace1[k]).getDepLabel();
//					fs.add(this._param_g.toFeature(network, FeaType.depLabel.name(), rel, depLabel));
//					if (k==0) path.append(depLabel);
//					else path.append(" " + depLabel);
//				}
//			} else {
//				for (int k = contIdx2 - 1; k >=0 ; k--) { // because the trace is backtracked.
//					String depLabel = sent.get(trace2[k]).getDepLabel();
//					fs.add(this._param_g.toFeature(network, FeaType.depLabel.name(), rel, depLabel));
//					if (k==contIdx2 - 1) path.append(depLabel);
//					else path.append(" " + depLabel);
//				}
//			}
//		} else {
//			int stamp1 = trace1.length - 1;
//			int stamp2 = trace2.length - 1;
//			while (stamp1 >= 0 && stamp2 >= 0) {
//				if (trace1[stamp1] != trace2[stamp2]) {
//					stamp1++;
//					stamp2++;
//					break;
//				} else {
//					stamp1--;
//					stamp2--;
//				}
//			}
//			//trace1[stamp1] should be equal to trace2[stamp2]
//			for (int k = 0; k < stamp1; k++) {
//				String depLabel = sent.get(trace1[k]).getDepLabel();
//				fs.add(this._param_g.toFeature(network, FeaType.depLabel.name(), rel, depLabel));
//				if (k==0) path.append(depLabel);
//				else path.append(" " + depLabel);
//			}
//			for (int k = stamp2 - 1; k >=0; k--) {
//				String depLabel = sent.get(trace2[k]).getDepLabel();
//				fs.add(this._param_g.toFeature(network, FeaType.depLabel.name(), rel, depLabel));
//				path.append(" " + depLabel);
//			}
//		}
//		//Note: the path is label path
//		fs.add(this._param_g.toFeature(network, FeaType.depPath.name(), rel, path.toString()));
//	}
	
}
