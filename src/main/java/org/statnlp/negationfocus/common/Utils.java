package org.statnlp.negationfocus.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.statnlp.commons.types.Label;
import org.statnlp.negationfocus.common.NegationFeatureManager.FeaType;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Word;
import edu.stanford.nlp.trees.LabeledScoredTreeFactory;
import edu.stanford.nlp.trees.PennTreeReader;
import edu.stanford.nlp.trees.Tree;
import edu.stanford.nlp.trees.TreeFactory;
import edu.stanford.nlp.trees.TreeReader;

public class Utils {

	public static class Counter {
		HashMap<String, Integer> wordCount = new HashMap<String, Integer>();

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

		@Override
		public String toString() {
			StringBuffer sb = new StringBuffer();
			for (Integer i : stats.keySet()) {
				sb.append(i + "\t" + stats.get(i) + "<br>\n");
			}

			return sb.toString();
		}
	}

	public static int[] sorted(int[] arr) {
		int[] arr_sorted = arr.clone();
		Arrays.sort(arr_sorted);
		return arr_sorted;
	}

	public static NegationInstance[] portionInstances(NegationInstance[] instances, double percentage) {
		return portionInstances(instances, (int) (percentage * instances.length));
	}

	public static NegationInstance[] portionInstances(NegationInstance[] instances, int num) {
		// NegationInstance[] insts = new NegationInstance[num];
		if (num > instances.length)
			num = instances.length;
		System.out.println("Truncate " + num + " instances.");
		return Arrays.copyOf(instances, num);
	}

	public static NegationInstance[] mergeInstances(NegationInstance[] instances1, NegationInstance[] instances2) {
		NegationInstance[] instances = new NegationInstance[instances1.length + instances2.length];
		for (int i = 0; i < instances1.length; i++)
			instances[i] = instances1[i];

		for (int i = 0; i < instances2.length; i++) {
			instances[instances1.length + i] = instances2[i];
			instances[instances1.length + i].setInstanceId(instances1.length + 1 + i);
		}

		return instances;
	}

	public static boolean isPunctuation(char c) {
		return c == ',' || c == '.' || c == '!' || c == '?' || c == ':' || c == ';' || c == '`';
	}

	public static boolean isPunctuation(String s) {
		return (s.length() == 1 && isPunctuation(s.charAt(0))) || (s.equals("-LRB-") || s.equals("-RRB-") || s.equals("``") || s.equals("''"));
	}

	public static void writeVocab(String filename, NegationInstance[][] instancesList, boolean lowercase) {
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

		for (NegationInstance[] instances : instancesList)
			for (NegationInstance inst : instances) {
				ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();
				for (String[] token : input) {
					;
					String output = token[NegationInstance.FEATURE_TYPES.word.ordinal()];
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
		if (pos >= 0 && pos < inputs.size()) {
			return inputs.get(pos)[idx];
		} else if (pos == -1) {
			return "<START>";
		} else if (pos == inputs.size()) {
			return "<END>";
		} else {
			return "<PAD>";
		}
	}

	public static String getTokenDPHead(ArrayList<String[]> inputs, int pos, int idx) {
		return getTokenDPHead(inputs, pos, idx, 0).toLowerCase();
	}

	public static String getTokenDPHead(ArrayList<String[]> inputs, int pos, int idx, int wordIdx) {
		if (pos >= 0 && pos < inputs.size()) {
			int headIdx = Integer.parseInt(inputs.get(pos)[idx]) - 1;
			// System.out.println(headIdx + " " + wordIdx);
			if (headIdx >= 0)
				return inputs.get(headIdx)[wordIdx];
			else
				return "<Root>";
		} else if (pos == -1) {
			return "<START>";
		} else if (pos == inputs.size()) {
			return "<END>";
		} else {
			return "<PAD>";
		}
	}

	public static int spanCount(int[] a) {
		int count = 0;
		int lastV = 0;
		for (int i = 0; i < a.length; i++) {
			if (a[i] == 1 && lastV == 0) {
				count++;
			}

			lastV = a[i];
		}

		return count;
	}

	public static ArrayList<int[]> getSpans(int[] a) { //[,)
		ArrayList<int[]> spans = new ArrayList<int[]>();

		int lastV = 0;
		for (int i = 0; i < a.length; i++) {
			if (a[i] == 1 && lastV == 0) {
				spans.add(new int[] { i, -1 });
			}

			if (a[i] == 0 && lastV == 1) {
				int[] tmp = spans.get(spans.size() - 1);
				tmp[1] = i;
				spans.set(spans.size() - 1, tmp);
			}

			lastV = a[i];
		}

		if (lastV == 1) {
			int[] tmp = spans.get(spans.size() - 1);
			tmp[1] = spans.size();
			spans.set(spans.size() - 1, tmp);
		}

		return spans;
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
		if (a.length != b.length)
			return null;

		double[] c = a.clone();
		for (int i = 0; i < c.length; i++)
			c[i] += b[i];
		return c;
	}

	public static int[] vectorAdd(int[] a, int[] b) {
		if (a.length != b.length)
			return null;

		int[] c = a.clone();
		for (int i = 0; i < c.length; i++)
			c[i] += b[i];
		return c;
	}
	
	public static double[] vectorMax(double[] a, double[] b) {
		if (a.length != b.length)
			return null;

		double[] c = a.clone();
		for (int i = 0; i < c.length; i++)
			c[i] = a[i] > b[i] ? a[i] : b[i];
		return c;
	}

	public static double[] vectorScale(double[] a, double scale) {
		double[] c = a.clone();
		for (int i = 0; i < c.length; i++)
			c[i] *= scale;
		return c;
	}

	public static double center(int[] a) {
		int sum = 0;
		int count = 0;
		for (int i = 0; i < a.length; i++) {
			count += a[i];
			sum += a[i] * i;
		}

		return (sum + 0.0) / count;
	}

	public static boolean isNumeric(String str) {
		return str.matches("-?\\d+(\\.\\d+)?"); // match a number with optional
												// '-' and decimal.
	}

	public static String[] table2Col(ArrayList<String[]> table, int colIdx) {
		String[] col = new String[table.size()];
		for (int i = 0; i < col.length; i++) {
			col[i] = table.get(i)[colIdx];
		}
		return col;

	}
	
	public static void fixInput(SemanticRole sr, int[] focusVerb) {
		for (int i = 0; i < focusVerb.length; i++) {
			if (focusVerb[i] == 1) {
				int idx = sr.roletype[i];
				String roleName = (idx >= 0) ? sr.roleNameList.get(idx) : "*";
				if (!roleName.startsWith("V") && !roleName.startsWith("C-V")) {
					/*
					
					if (roleName.startsWith("*")) {
						
					}*/
					focusVerb[i] = 0;
				}
			}
		}
	}

	public static SemanticRole getSemanticRole(ArrayList<SemanticRole> srList, int[] focusVerb) {
		for (SemanticRole sr : srList) {

			boolean found = false;

			for (int i = 0; i < focusVerb.length; i++) {
				if (focusVerb[i] == 1) {
					int idx = sr.roletype[i];
					if (idx != -1) {
						String roleName = sr.roleNameList.get(idx);
						if (roleName.startsWith("V")) {
							found = true;
							break;
						}
					}
				}
			}

			if (found) {
				fixInput(sr, focusVerb);
				return sr;
			}

		}
		
		
		//no matched semantic role found
		

		return null;
	}

	public static Tree getParseTree(String pennString) {
		TreeFactory tf = new LabeledScoredTreeFactory();
		Reader r = new StringReader(pennString);
		TreeReader tr = new PennTreeReader(r, tf);

		Tree t = null;
		try {
			t = tr.readTree();
		} catch (IOException e) {
			e.printStackTrace();
			System.err.println();
			System.exit(-1);
		}

		/*
		 * for(Word word : t.yieldWords()) { if (!word.word().startsWith("*"))
		 * sentence.add(word);
		 * 
		 * }
		 */

		/*
		 * for(CoreLabel sub : t.taggedLabeledYield()) {
		 * 
		 * if (!sub.tag().equals("-NONE-")) { sentence.add(sub.word());
		 * //System.out.println(sub.word()); }
		 * 
		 * }
		 */

		return t;
	}

	public static Tree addLeaves(Tree t, String[] words) {

		
		List<Tree> leaves = t.getLeaves();
		
		for (int i = 0; i < leaves.size(); i++) {
			Tree leaf = leaves.get(i);
			CoreLabel label = (CoreLabel) leaf.label();
			label.setValue(words[i]);
			//label.setWord(words[i]);
		}
		

		return t;
	}

	public static ArrayList<String> getSentence(Tree t) throws IOException {

		ArrayList<String> sentence = new ArrayList<String>();

		
		  for(Word word : t.yieldWords()) { 
			  sentence.add(word.value());
			  System.out.print(word.value() + " ");
		  }
		 

		/*
		for (CoreLabel sub : t.taggedLabeledYield()) {

			if (!sub.tag().equals("-NONE-")) {
				sentence.add(sub.word());
				System.out.print(sub.word());
				// System.out.println(sub.word());
			}
			
			

		}*/
		System.out.println();

		return sentence;
	}
	
	public static Tree getAncestorTopMost(Tree root, int pos, String tag) {
		List<Tree> leaves = root.getLeaves();
		Tree curr_pos = leaves.get(pos);
		
		
		for(int i = root.depth(curr_pos); i >= 1 ; i--) {
			Tree ancestor = curr_pos.ancestor(i, root);
			CoreLabel label = (CoreLabel) ancestor.label();
			if (label.value().equals(tag)) {
				return ancestor;
			}
		}
		
		return null;
	}
	
	
	public static Tree getAncestorBtmMost(Tree root, int pos, String tag) {
		List<Tree> leaves = root.getLeaves();
		Tree curr_pos = leaves.get(pos);
		
		
		for(int i = 1; i <= root.depth(curr_pos) ; i++) {
			Tree ancestor = curr_pos.ancestor(i, root);
			CoreLabel label = (CoreLabel) ancestor.label();
			if (label.value().equals(tag)) {
				return ancestor;
			}
		}
		
		return null;
	}
	
	public static ArrayList<String> getTextSpanUntil(Tree ancestor, Tree curr) {
		ArrayList<String> words = new ArrayList<String>();
		
		List<Tree> leaves = ancestor.getLeaves();
		for (int i = 0; i < leaves.size(); i++) {
			Tree leaf = leaves.get(i);
			CoreLabel label = (CoreLabel) leaf.label();
			words.add(label.value());
			
			if (leaf == curr) break;
		}
		
		return words;
	}
	
	public static int getTextSpanBeginPosUntil(Tree root, Tree ancestor, int pos) {
		
		Tree firstLeaf = ancestor.getLeaves().get(0);
		
		int firstLeafPos = getLeafPos(root, firstLeaf);
		
		return firstLeafPos;
	}
	
	
	public static int getLeafPos(Tree root, Tree leaf) {
		
		List<Tree> leaves = root.getLeaves();
		for(int i = 0; i < leaves.size(); i++) {
			Tree l = leaves.get(i);
			if (l == leaf) 
				return i;
		}
		
		return -1;
		
	}
	
	public static ArrayList<String> getSeqTokens(ArrayList<String[]> inputs, int beginPos, int endPos, int featureIdx) { //endPos inclusive
		ArrayList<String> tokens = new ArrayList<String>();
		for(int i = beginPos; i <= endPos; i++) {
			tokens.add(inputs.get(i)[featureIdx]);
		}
		return tokens;
	}
	
	public static ArrayList<String> getSeqTokens(ArrayList<String[]> inputs, int[] filter, int featureIdx) { 
		ArrayList<String> tokens = new ArrayList<String>();
		for(int i = 0; i < inputs.size(); i++) {
			if (filter[i] == 1)
			tokens.add(inputs.get(i)[featureIdx]);
		}
		return tokens;
	}
	
	public static int getFirstOccurence(int[] filter) {
		for(int i = 0; i < filter.length; i++) {
			if (filter[i] == 1)
				return i;
		}
		
		return -1;
	}
	
	public static void fillSpan(int[] a, int[] span, int v) {
		for(int i = span[0]; i < span[1]; i++)
			a[i] = v;
	}
	
	public static void fillSpan(int[] a, int[] span) {
		fillSpan(a, span, 1);
	}
	
	
	public static Tree getLeaf(Tree root, int pos) {
		return root.getLeaves().get(pos);
	}
	
	public static String join(String Separator, List<String> list) {
		String ret = list.get(0).toString();
		
		for(int i = 1; i < list.size(); i++) {
			ret += Separator + list.get(i).toString();
		}
		
		return ret;
	}
	
	public static ArrayList<String> getSeqDepTokens(ArrayList<String[]> inputs, int beginPos, int endPos) {
		return null;
	}

	
	public static final Set<String> A1_POSTags = new HashSet<String>() {
		{
			add("DT");
			add("JJ");
			add("PRP");
			add("CD");
			add("RB");
			add("VB");
			add("WP");
		}
	};
	
	
	public static final Set<String> contentWordPOSTag = new HashSet<String>() {
		{
			
			add("JJ");
			add("NN");
			add("PRP");
			add("VB");
		}
	};
	
	public static int getSeqEndPos(int idx, int[] seq, int[] seq_begin) { // return [,)
		int v = seq[idx];
		int endPos = idx;

		while(true) {
			endPos++;
			if (endPos >= seq.length || seq[endPos] != v || seq_begin[endPos] != 0) break;
			
		}

		return endPos;

	}
	
	public static int getSeqEndPos(int idx, int[] seq) { // return [,)
		int v = seq[idx];
		int endPos = idx;

		while(true) {
			endPos++;
			if (endPos >= seq.length || seq[endPos] != v ) break;
			
		}

		return endPos;

	}
	
	public static boolean spanContains(int[] spanA, int[] spanB) { //spanA contains spanB
		return spanA[0] <= spanB[0] && spanA[1] >= spanB[1];
	}
	
	public static boolean spanEquals(int[] spanA, int[] spanB) { //spanA contains spanB
		return spanA[0] == spanB[0] && spanA[1] == spanB[1];
	}
	
	public static Stemmer s = new Stemmer();
	
	public static String strProcess(String token) {
		token = token.toLowerCase();
		if (token.length() > 1)
			token = token.replace(",", "");
		if (token.length() > 1)
			token = token.replace("/", "");
		if (isNumeric(token))
			token = "0";
		else
			token = s.stem(token);
		return token;
	}
	
	
	
}
