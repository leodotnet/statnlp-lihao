package org.entityrelation.common;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;



import org.statnlp.commons.types.Instance;
import org.statnlp.commons.types.Label;
import org.statnlp.hypergraph.NetworkConfig;


public class EntityRelationReader {
	
	public static HashMap<String, EntityRelationInstance[]> backupInstances = new HashMap<String, EntityRelationInstance[]>();
	//public static HashMap<String, EntityRelationInstance[]> backupPipelineInstances = new HashMap<String, EntityRelationInstance[]>();
	

	public static EntityRelationInstance<Label>[] readData(String dataSet, String fileName, boolean withLabels, boolean isLabeled, int TRIAL, boolean discardNoNgeation) {

		EntityRelationInstance[] insts = null;

		if (dataSet.startsWith("ace")) {
			
				try {
					insts = readACEInstances(fileName, withLabels, isLabeled);
					
					if (TRIAL > 0)
						insts = portionInstances(insts, TRIAL);
					
					if (EntityRelationGlobal.PIPELINE_SPAN != null && !isLabeled) {
						//backupInstances.put(fileName, insts);
						String pipelineFilename = fileName;
						int p = pipelineFilename.lastIndexOf("//");
						String dir = pipelineFilename.substring(0, p);
						String targetFile = pipelineFilename.substring(p + 1);
						String span = EntityRelationGlobal.HEAD_AS_SPAN ? "head" : "full";
						pipelineFilename = dir + "//" + "pipeline_" + EntityRelationGlobal.PIPELINE_SPAN + "_" + span + "span/" + targetFile + ".out.span"; 
						appendPipelineSpanResult(pipelineFilename, fileName, insts);
						
					}
					
					if (EntityRelationGlobal.modelname.equals("LR")) {
						backupInstances.put(fileName, insts);
						insts = toLRInstance(insts, isLabeled);
					}
				} catch (FileNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			
		}
	

		return insts;
	}

	public static EntityRelationInstance[] readACEInstances(String fileName, boolean withLabels, boolean isLabeled) throws FileNotFoundException   {

		System.out.println("Reading data from [isLabeled]=" + isLabeled + ": " + fileName);
		
		
		if (EntityRelationGlobal.ADD_SELF_RELATION) {
			EntityRelationOutput.getRELATIONS(Relation.SELF_RELATION);
		}
		
		
		if (EntityRelationGlobal.ADD_NO_RELATION) {
			EntityRelationGlobal.NoRelationIndex = EntityRelationOutput.getRELATIONS(Relation.NO_RELATION).id;
		}
		//EntityRelationOutput.getRELATIONS("No-Relation");
		
		ArrayList<EntityRelationInstance> result = new ArrayList<EntityRelationInstance>();
		Scanner scanner = new Scanner(new File(fileName), "UTF-8");
		
		EntityRelationInstance inst_max_l = null;
		
		Utils.IntStat spanlengthStat = new Utils.IntStat();
		Utils.IntStat relationDistStat = new Utils.IntStat();
		
		int numEntity = 0;
		int numRelation = 0;

		int sentenceId = 0;
		int instanceId = 0;
		String line = "";
		String[] fields = null;
		boolean lastWordAsHead = EntityRelationGlobal.LAST_WORD_AS_HEAD;

		while (scanner.hasNextLine()) {

			line = scanner.nextLine().trim();
			if (line.length() == 0)
				break;
			
			ArrayList<String[]> inputs = new ArrayList<String[]>();
			fields = line.split(" ");
			for(int i = 0; i < fields.length; i++) {
				inputs.add(new String[4]);
			}
			
			
			for(int i = 0; i < fields.length; i++) {
				String[] feature = inputs.get(i);
				feature[0] = fields[i];
			}
			
			int size = fields.length;
			
			for(int featureIdx = 1; featureIdx < 4; featureIdx++) {
				line = scanner.nextLine().trim();
				fields = line.split(" ");
				
				for(int i = 0; i < size; i++) {
					String[] feature = inputs.get(i);
					feature[featureIdx] = fields[i];
				}
			}
			
			ArrayList<Entity> entities = new ArrayList<Entity>();
			ArrayList<Relation> relations = new ArrayList<Relation>();
			
			
			line = scanner.nextLine().trim();
			if (line.length() > 0) {
				fields = line.split("\\|");
				
				for(String entityStr : fields) {
					Entity entity = new Entity(entityStr);
					entity.setHeadIdx(inputs, entity, lastWordAsHead);
					int p = -1;
					if (EntityRelationGlobal.REMOVE_DUPLICATE) {
						p = EntityRelationOutput.getEntityIdx(entities, entity);
					}
					if (p < 0) {
						entities.add(entity);
						entity.entityIdx = entities.size();
					} else {
						entity.entityIdx = p;
					}
				}
				
			}
			
			if (EntityRelationGlobal.REMOVE_OL_ENTITY) {
				for(Entity e : entities) {
					if (EntityRelationOutput.isOLEntity(entities, e)) {
						e.setOL();
					}
				}
				
				ArrayList<Entity> tmp = new ArrayList<Entity>(entities);
				entities.clear();
				
				for(int i = 0; i < tmp.size(); i++) {
					Entity e = tmp.get(i);
					if (!e.ol) {
						entities.add(e);
					}
				}
				
			}
			
			int entitySize = entities.size();
			boolean[][] hasRelation = new boolean[entitySize][entitySize];
			
			
			line = scanner.nextLine().trim();
			if (line.length() > 0) {
				fields = line.split("\\|");
				
				
				if (!EntityRelationGlobal.ONLY_SELF_RELATION) {
				
					for(String relationStr : fields) {
						Relation relation = Relation.parseRelation(entities, relationStr);
						
						if (relation == null)
							continue;
						
						int p = EntityRelationOutput.getRelationIdx(relations, relation);
						if (p < 0) {
							relations.add(relation);
							hasRelation[relation.arg1Idx][relation.arg2Idx] = true;
						}
						
						int[] relationEntityTriple = relation.getRelationArr();
						int pIdx = EntityRelationGlobal.getTripleIdx(relationEntityTriple);
						if (pIdx == -1) {
							EntityRelationGlobal.relationEntityTriples.add(relationEntityTriple);
						}
						
						if (EntityRelationGlobal.ADD_REVERSE_RELATION) {
							Relation relationR = relation.getReverseRelation();
							p = EntityRelationOutput.getRelationIdx(relations, relation);
							if (p < 0) {
								relations.add(relation);
								hasRelation[relation.arg1Idx][relation.arg2Idx] = true;
							}
							
							relationEntityTriple = relation.getRelationArr();
							pIdx = EntityRelationGlobal.getTripleIdx(relationEntityTriple);
							if (pIdx == -1) {
								EntityRelationGlobal.relationEntityTriples.add(relationEntityTriple);
							}
							
						}
					}
					
					
					if (EntityRelationGlobal.EXPAND_WITH_NO_RELATION && isLabeled) {
						assert (EntityRelationGlobal.ADD_NO_RELATION);
						for(int j = 0; j < entities.size(); j++) {
							Entity entity1 = entities.get(j);
							for(int k = 0; k < entities.size(); k++) {
								Entity entity2 = entities.get(k);
								if (!hasRelation[j][k]) { //j != k && 
									
									/*
									if (isLabeled && Utils.rnd.nextDouble() > EntityRelationGlobal.NO_RELATION_PROB) {
										continue;
									}
									*/
									
									Relation relation = Relation.buildNoRelation(entities, entity1, entity2);
									relations.add(relation);
									
									int[] relationEntityTriple = relation.getRelationArr();
									int pIdx = EntityRelationGlobal.getTripleIdx(relationEntityTriple);
									if (pIdx == -1) {
										EntityRelationGlobal.relationEntityTriples.add(relationEntityTriple);
									}
									
								}
							}
						}
					}
					
				}
				
			}
			
			line = scanner.nextLine().trim();
			
			EntityRelationOutput outputs = new EntityRelationOutput(entities, relations);
			
			instanceId++;
			sentenceId++;
			EntityRelationInstance inst = new EntityRelationInstance(instanceId, 1.0, inputs, outputs);
			inst.sentenceId = sentenceId;
			inst.preprocess();

			if (isLabeled) {
				inst.setLabeled(); // Important!
			} else {
				inst.setUnlabeled();
			}

			
			boolean discard = false;
			

			if (isLabeled) {
				
				for (Entity entity : entities) {
					int lenSpan = entity.getSpanLength();
					
					
					if (lenSpan > EntityRelationGlobal.L_SPAN_MAX_LIMIT)
					{
						discard = true;
						break;
					}
					
					spanlengthStat.addInt(lenSpan);
					
					if (EntityRelationGlobal.L_SPAN_MAX < lenSpan) {
						EntityRelationGlobal.L_SPAN_MAX = lenSpan + 1;
						inst_max_l = inst;
						System.err.println("***max entity:"+entity);
					}
					
					/*
					int lenHead = entity.getHeadLength();
					if (EntityRelationGlobal.L_HEAD_MAX < lenHead) {
						EntityRelationGlobal.L_HEAD_MAX = lenHead + 1;
					}*/
				}
				
				for(Relation relation : relations) {
					int dist = relation.arg2.span[0] - relation.arg1.span[0];
					
					
					if (dist > EntityRelationGlobal.RELATION_ENTITY_DISTANCE_MAX_LIMIT[1] || dist < EntityRelationGlobal.RELATION_ENTITY_DISTANCE_MAX_LIMIT[0]) {
						discard = true;
						break;
					}
				
					
					relationDistStat.addInt(dist);
					if (dist > EntityRelationGlobal.RELATION_ENTITY_DISTANCE_MAX[1]) {
						EntityRelationGlobal.RELATION_ENTITY_DISTANCE_MAX[1] = dist;
					}
					
					if (dist < EntityRelationGlobal.RELATION_ENTITY_DISTANCE_MAX[0]) {
						EntityRelationGlobal.RELATION_ENTITY_DISTANCE_MAX[0] = dist;
					}
				}
				
				
				if (EntityRelationGlobal.FIX_ENTITY) {
					
				}
				
				if (EntityRelationGlobal.ONLY_SELF_RELATION) {
					relations.clear();
				}
				
				if (EntityRelationGlobal.FIX_ENTITY_PAIR) {
					if (relations.size() == 0)
						discard = true;
				}
				
				
				
				
				
				
				
				if (!discard) {
					if (EntityRelationGlobal.SENTENCE_LENGTH_MAX < inputs.size()) {
						EntityRelationGlobal.SENTENCE_LENGTH_MAX = inputs.size() + 1;
					}
					
					if (EntityRelationGlobal.RELATION_MAX < EntityRelationOutput.RELATIONS.size()) {
						EntityRelationGlobal.RELATION_MAX = EntityRelationOutput.RELATIONS.size();
					}
					
					if (EntityRelationGlobal.ENTITY_TYPE_MAX < EntityRelationOutput.ENTITYTYPE.size()) {
						EntityRelationGlobal.ENTITY_TYPE_MAX = EntityRelationOutput.ENTITYTYPE.size();
					}
				}

			
			}
			
			if (EntityRelationGlobal.SENTENCE_LENGTH_MAX < inputs.size()) {
				EntityRelationGlobal.SENTENCE_LENGTH_MAX = inputs.size() + 1;
			}
			
			if (!discard) {
				result.add(inst);
				numEntity += entities.size();
				numRelation += relations.size();
			}

		}

		scanner.close();
		
		
		System.out.println("relationEntityTriples:" + EntityRelationGlobal.relationEntityTriples.size());
		
		//add self relation
		if (EntityRelationGlobal.ADD_SELF_RELATION) {
			for(int i = 0; i < EntityRelationOutput.ENTITYTYPE.size(); i++) {
				int selfRelationId = EntityRelationOutput.getRELATIONS(Relation.SELF_RELATION).id;
				
				int[] relationEntityTriple = new int[] {selfRelationId, i, i};
				int pIdx = EntityRelationGlobal.getTripleIdx(relationEntityTriple);
				if (pIdx == -1) {
					EntityRelationGlobal.relationEntityTriples.add(relationEntityTriple);
				}
			}
			
			System.out.println("relationEntityTriples[self-relation added]:" + EntityRelationGlobal.relationEntityTriples.size());
		}
		
		
		
		System.out.println(fileName);
		System.out.println("#inst:" + result.size());
		System.out.println("MAX_SENTENCE_LENGTH:" + EntityRelationGlobal.SENTENCE_LENGTH_MAX);
		System.out.println("L_SPAN_MAX:" + EntityRelationGlobal.L_SPAN_MAX + "\tL_HEAD_MAX:" + EntityRelationGlobal.L_HEAD_MAX);
		//System.out.println("Instance with max span length:\n" + inst_max_l);
		System.out.println("Span Length Stats:\n" + spanlengthStat);
		
		System.out.println("RELATION_MAX:" + EntityRelationGlobal.RELATION_MAX);
		System.out.println("RELATIONS:" + EntityRelationOutput.RELATIONS);
		System.out.println("RELATION_ENTITY_DISTANCE_MAX: " + Arrays.toString(EntityRelationGlobal.RELATION_ENTITY_DISTANCE_MAX));
		System.out.println("RELATION_ENTITY_DISTANCE_MAX Stats:\n" + relationDistStat);
		System.out.println("ENTITY_TYPE_MAX:" + EntityRelationGlobal.ENTITY_TYPE_MAX);
		System.out.println("ENTITY_TYPE:" + EntityRelationOutput.ENTITYTYPE);
		
		System.out.println("#Entity:" + numEntity + "\t#Relation:" + numRelation);
		
		System.out.println();

		return result.toArray(new EntityRelationInstance[result.size()]);
	}

	public static EntityRelationInstance[] portionInstances(EntityRelationInstance[] instances, double percentage) {
		if (percentage > 0)
			return portionInstances(instances, (int) (percentage * instances.length));
		else
			return instances;
	}

	public static EntityRelationInstance[] portionInstances(EntityRelationInstance[] instances, int num) {
		// EntityRelationInstance[] insts = new EntityRelationInstance[num];
		if (num <= 0 || num > instances.length) {
			num = instances.length;
		}
		
		System.out.println("Truncate " + num + " instances.");
		return Arrays.copyOf(instances, num);
		
	}
	
	public static void revertBackup(EntityRelationInstance[] insts) {
		for(EntityRelationInstance inst : insts) {
			inst.revertBackup();
		}
	}
	
	public static void appendPipelineSpanResult(String filename, String dataFilename,EntityRelationInstance[] insts) throws FileNotFoundException {
		Scanner scanner = new Scanner(new File(filename));
		int instId = -1;
		int numEntity = 0;
		boolean lastWordAsHead = true;
		
		while(scanner.hasNextLine()) {
			String line = scanner.nextLine().trim();
			instId++;
			
			ArrayList<Entity> entities = new ArrayList<Entity>();
			ArrayList<Relation> relations = new ArrayList<Relation>();
			
			if (line.length() > 0) {
				String[] fields = line.split("\\|");
				
				for(String entityStr : fields) {
					Entity entity = new Entity(entityStr);
					Entity.setHeadIdx((ArrayList<String[]>)insts[instId].input, entity, lastWordAsHead);
					int p = -1;
					if (EntityRelationGlobal.REMOVE_DUPLICATE) {
						p = EntityRelationOutput.getEntityIdx(entities, entity);
					}
					if (p < 0) {
						entities.add(entity);
						numEntity++;
					}
				}
				
			}
			
			EntityRelationOutput output = new EntityRelationOutput(entities, relations);
			
			if (instId < insts.length) {			
				insts[instId].backup();
				insts[instId].setOutput(output);
			}
			
		}
		scanner.close();
		
		System.out.println("#Entity from pipeline span:" + numEntity);
	}
	
	public static EntityRelationInstance[] toLRInstance(EntityRelationInstance[] insts, boolean isLabeled) {
		ArrayList<EntityRelationInstance> result = new ArrayList<EntityRelationInstance>();
		
		ArrayList<EntityRelationInstance> resultNORel = new ArrayList<EntityRelationInstance>();
		
		Utils.Counter counter = new Utils.Counter();
		
		
		int instanceId = 0;
		for(int i = 0; i < insts.length; i++) {
			
			EntityRelationOutput output = (EntityRelationOutput)insts[i].output;
			ArrayList<String[]> input = (ArrayList<String[]>)insts[i].input;
			ArrayList<Entity> goldEntities = output.entities;
			ArrayList<Relation> goldRelations = output.relations;
			int entitySize = goldEntities.size();
			
			boolean[][] hasRelation = new boolean[entitySize][entitySize];
			
			
			for(Relation relation : output.relations) {
				
				hasRelation[relation.arg1Idx][relation.arg2Idx] = true;
				
				ArrayList<Entity> entities = new ArrayList<Entity>();
				entities.add(relation.arg1);
				entities.add(relation.arg2);
				
				ArrayList<Relation> relations = new ArrayList<Relation>();
				relations.add(new Relation(entities, relation.type.form, relation.arg1, relation.arg2));
				
				EntityRelationOutput o = new EntityRelationOutput(entities, relations);
				
				instanceId++;
				EntityRelationInstance inst = new EntityRelationInstance(instanceId, 1.0, input , o);
				inst.copyInput(insts[i]);
				inst.sentenceId = insts[i].sentenceId;
				
				if (isLabeled) {
					inst.setLabeled();
				} else {
					inst.setUnlabeled();
				}
				
				counter.addWord(relation.type.form);
				
				result.add(inst);
			}
			
			
			if (EntityRelationGlobal.ADD_NO_RELATION) {
				for(int j = 0; j < goldEntities.size(); j++) {
					Entity entity1 = goldEntities.get(j);
					for(int k = 0; k < goldEntities.size(); k++) {
						Entity entity2 = goldEntities.get(k);
						if (j != k && !hasRelation[j][k]) {
							
							if (isLabeled && Utils.rnd.nextDouble() > EntityRelationGlobal.NO_RELATION_PROB) {
								continue;
							}
							
							ArrayList<Entity> entities = new ArrayList<Entity>();
							entities.add(entity1);
							entities.add(entity2);
							
							ArrayList<Relation> relations = new ArrayList<Relation>();
							Relation relation = Relation.buildNoRelation(entities, entity1, entity2);
							relations.add(relation);
							
							EntityRelationOutput o = new EntityRelationOutput(entities, relations);
							
							instanceId++;
							EntityRelationInstance inst = new EntityRelationInstance(instanceId, 1.0, input, o);
							inst.copyInput(insts[i]);
							
							if (isLabeled) {
								inst.setLabeled();
							} else {
								inst.setUnlabeled();
							}
							counter.addWord(relation.type.form);
							result.add(inst);
							
						}
					}
				}
			}
			
		}
		
		System.out.println(counter);
		
		return result.toArray(new EntityRelationInstance[result.size()]);
	}

	public static EntityRelationInstance[][] splitData(EntityRelationInstance[] insts, String splitMethod, double ratio) {

		EntityRelationInstance[][] splitInsts = new EntityRelationInstance[2][];

		if (splitMethod.equals("static")) {
			int trainSize = (int) (ratio * insts.length);
			splitInsts[0] = new EntityRelationInstance[trainSize];
			splitInsts[1] = new EntityRelationInstance[insts.length - trainSize];

			for (int i = 0; i < trainSize; i++) {
				splitInsts[0][i] = insts[i];
			}

			for (int i = trainSize; i < insts.length; i++) {
				splitInsts[1][i - trainSize] = insts[i];
			}
		} else if (splitMethod.equals("folds")) {

		}

		return splitInsts;
	}
	
	
	public static void writeResult(Instance[] predictions, String pathJoin, String result_file) {
		PrintWriter p = null;
		String filename_output = result_file;
		
		/*
		if (EntityRelationGlobal.OUTPUT_SEM2012_FORMAT) {

			
			p = new PrintWriter(new File(filename_output), "UTF-8");
			
			if (EntityRelationGlobal.DEBUG)
				System.out.println("Result: ");

			for (int i = 0; i < predictions.length; i++) {
				EntityRelationInstance inst = (EntityRelationInstance) predictions[i];

				
				p.write(inst2Str(inst));
				p.write("\n");

			}
			p.close();
		}
		*/
		
		try {
			p = new PrintWriter(new File(filename_output + ".seq"), "UTF-8");
		} catch (FileNotFoundException | UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		for (int i = 0; i < predictions.length; i++) {
			EntityRelationInstance inst = (EntityRelationInstance) predictions[i];
			p.write(inst2SeqStr(inst));
		}

		p.close();
		
		
		
		if (EntityRelationGlobal.OUTPUT_HTML_SPAN) {
			try {
				p = new PrintWriter(new File(filename_output + ".span.html"), "UTF-8");
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (UnsupportedEncodingException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			String css = "/Users/Leo/workspace/ui/overlap.css";
			if (NetworkConfig.OS.equals("linux")) {
				css = "/home/lihao/workspace/ui/overlap.css";
			}

			String header = "<html><head><link rel='stylesheet' type='text/css' href='" + css + "' /></head> <body><br><br>\n";
			String footer = "\n</body></html>";
			p.write(header);


			for (int i = 0; i < predictions.length; i++) {
				EntityRelationInstance inst = (EntityRelationInstance) predictions[i];
				ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();

				ArrayList<Label> gold = (ArrayList<Label>) inst.getOutput();
				ArrayList<Label> pred = (ArrayList<Label>) inst.getPrediction();
				
//				String goldText = "";
//				for(int j = 0; j < gold.size(); j++) {
//					
//				}

				//ArrayList<int[]> scopes = inst.scopes;

				String t = "";

//				t += gold + "<br>\n";
//
//				t += pred + "<br>\n";

				
				t += outputSent(gold, inst, "positive");

				t += outputSent(pred, inst, "negative");

				t += "<br>\n";
				
				t += "<hr>\n";

				p.println(t);

			}

			p.write(footer);

			p.close();

		}
		
		if (EntityRelationGlobal.modelname.equals("LinearCRF")) {
			try {
				p = new PrintWriter(new File(filename_output + ".span"));
				for(int i= 0; i < predictions.length; i++) {
					EntityRelationInstance inst = (EntityRelationInstance)predictions[i];
					EntityRelationOutput pred = (EntityRelationOutput)inst.prediction;
					p.write(Utils.join(pred.entities, "|") + "\n");
				}
				p.close();
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
		}
		
		
		

		if (EntityRelationGlobal.DEBUG) {
			System.out.println("\n");
		}
		System.out.println(EntityRelationGlobal.modelname + " Evaluation Completed");

		// NegEval.evalbyScript(goldfile, filename_output);

	}
	

	
	@SuppressWarnings("unchecked")
	public static String inst2SeqStr(EntityRelationInstance inst) {
		StringBuffer sb = new StringBuffer();
		ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();
		EntityRelationOutput gold = (EntityRelationOutput) inst.getOutput();
		EntityRelationOutput pred = (EntityRelationOutput) inst.getPrediction();
		
	
		for (int i = 0; i < inst.size(); i++) {
			//String[] fields = input.get(i) + " ";
			sb.append(input.get(i)[0] + " ");
			//sb.append((gold.get(i).getForm().startsWith("I") ? 1 : 0) + "\t");
			//sb.append((pred.get(i).getForm().startsWith("I") ? 1 : 0));
			//sb.append("\n");
		}

		sb.append("\n");
		
		/*
		if (gold.relations.size() <= 0 || pred.relations.size() <= 0)
			return "";
		
		if (gold.relations.get(0).isNORelation() && pred.relations.get(0).isNORelation())
			return "";
		 */
		
		sb.append(gold.toString());
		sb.append(pred.toString());
		sb.append("\n");
		return sb.toString();
	}
	
	public static int[] convertArr(ArrayList<Label> output) {
		int[] ret = new int[output.size()];
		for(int i = 0; i < ret.length; i++) {
			ret[i] = output.get(i).getForm().startsWith("I") ? 1 : 0;
		}
		return ret;
	}
	
	
	
	static String outputSent(ArrayList<Label> output, EntityRelationInstance inst, String color) {

		String t = "";
		char lastTag = 'O';
		ArrayList<String[]> input = (ArrayList<String[]>) inst.getInput();
		int[] ret = convertArr(output);

		for (int k = 0; k < input.size(); k++) {
			
			String labelStr = output.get(k).getForm();
			char tag = labelStr.charAt(0);
			char nextTag = (k + 1 < input.size()) ? output.get(k + 1).getForm().charAt(0) : 'O';

			if (tag != 'O' && lastTag == 'O') {
				t += "<div class='tooltip entity_" + color + "'>";
			}

			/*
			if (inst.negation.cue[k] == 1) {
				t += "<div class='tooltip entity_neutral_incorrect'>";
			}*/
			
			for(int i = 0; i < input.get(k).length; i++) {
				t += input.get(k)[i] + " ";
			}
			t += "&nbsp;";

			/*
			if (inst.negation.cue[k] == 1) {
				t += "</div>&nbsp;";
			}*/

			if (tag != 'O' && nextTag == 'O') {
				t += "</div>&nbsp;";
			}

			lastTag = tag;
		}

		t += "<br>\n";

		return t;

	}
	
	public static Instance[] postprocess(String fileName, Instance[] insts) {
		
		
		Instance[] ret = insts;
		
		if (EntityRelationGlobal.modelname.equals("LR")) {
			
			EntityRelationInstance[] backInstances = backupInstances.get(fileName);
			//EntityRelationInstance[] backPipelineInstances = backupPipelineInstances.get(fileName);
			
			//revertBackup(backInstances);
			
			for(int i = 0; i < backInstances.length; i++) {
				EntityRelationInstance inst = backInstances[i];
				//EntityRelationInstance instPipeline = backPipelineInstances[i];
				
				EntityRelationOutput pred = new EntityRelationOutput();
				EntityRelationOutput output = (EntityRelationOutput)inst.output;
				pred.entities = output.entities;
				inst.setPrediction(pred);
				inst.revertBackup();
			}
			
			ArrayList<EntityRelationInstance> relationInsts = new ArrayList<EntityRelationInstance>();
//			for(int i = 0; i < insts.length; i++) {
//				
//				EntityRelationInstance inst = (EntityRelationInstance)insts[i];
//				EntityRelationOutput pred = (EntityRelationOutput)inst.prediction; 
//				if (pred.relations.size() > 0 && !pred.relations.get(0).isNORelation()) {
//					relationInsts.add(inst);
//				}
//			}
			
			for(int i = 0; i < insts.length; i++) {
				EntityRelationInstance inst = (EntityRelationInstance)insts[i];
				int sentenceId = inst.sentenceId;
				
				EntityRelationOutput pred = (EntityRelationOutput)inst.prediction; 
				
				EntityRelationOutput aggregatePred = (EntityRelationOutput) backInstances[sentenceId - 1].prediction;
					
				aggregatePred.addRelations(pred.relations, true);
				
			}
			
			
			ret = (EntityRelationInstance[])backInstances;
		} else {
			if (EntityRelationGlobal.EXPAND_WITH_NO_RELATION) {
				for(Instance instance : insts) {
					EntityRelationInstance inst = (EntityRelationInstance)instance;
					EntityRelationOutput output = (EntityRelationOutput)inst.output;
					ArrayList<Relation> relations = new ArrayList<Relation>();
					for(Relation rel : output.relations) {
						if (rel.isNORelation() || rel.isSelfRelation())
							continue;
						
						relations.add(rel);
					}
					
					output.relations = relations;
				}
			}
		}
		
		
		
		return ret;
	}
	

}
