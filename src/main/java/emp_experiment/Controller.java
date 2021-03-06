package emp_experiment;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import emp_experiment.model.Dolly;
import emp_experiment.model.EquivalentMutantDetection;
import emp_experiment.model.Mutant;
import emp_experiment.model.MutationSystem;
import emp_experiment.model.Session;
import emp_experiment.utils.CompilerException;
import emp_experiment.utils.CopyFilesException;
import emp_experiment.utils.MutationException;
import emp_experiment.utils.Utils;

public class Controller {

	private static Controller controller;
	private Session session;
	private Dolly dolly;
	private List<MutationSystem> mutationSystems;
	private EquivalentMutantDetection equivalentDetection;

	private Controller() {
		super();
		this.mutationSystems = new ArrayList<MutationSystem>();
	}

	public static Controller getInstance() throws Exception {
		if (controller == null) {
			controller = new Controller();
		}
		return controller;
	}

	public Session getSession() {
		return session;
	}

	public void setSession(Session session) {
		this.session = session;
	}

	public Dolly getDolly() {
		return dolly;
	}

	public void setDolly(Dolly dolly) {
		this.dolly = dolly;
	}

	public List<MutationSystem> getMutationSystems() {
		return mutationSystems;
	}

	public void setMutationSystem(List<MutationSystem> mutationSystems) {
		this.mutationSystems = mutationSystems;
	}

	public void addMutationSystem(MutationSystem system) {
		this.mutationSystems.add(system);
	}

	public EquivalentMutantDetection getEquivalentDetection() {
		return equivalentDetection;
	}

	public void setEquivalentDetection(EquivalentMutantDetection equivalentDetection) {
		this.equivalentDetection = equivalentDetection;
	}

	/**
	 * Aqui continua a execução 1 - Compilar 2 - Executar o teste de mutação 3 -
	 * Analisar equivalencia
	 * @throws IOException 
	 * 
	 * @throws Exception
	 */
	public void startAnalysis(File testDir, List<Mutant> allMutants) throws IOException {
		System.out.println("Start Analysis of: " + testDir.getName());		
		
		try {
			session.compileProgram(testDir);
			session.createDirectory(testDir);
			equivalentDetection.setupStructure(testDir);
			for (MutationSystem mutationSystem : getMutationSystems()) {
				mutationSystem.setupStructure(testDir);
				mutationSystem.mutate(testDir);
				List<Mutant> mutants = mutationSystem.getMutants();
				if (mutants != null && mutants.size() > 0) {
					System.out.println("Total Mutants: " + mutationSystem.getMutants().size());
					equivalentDetection.execute(mutationSystem);
				}
				allMutants.addAll(mutants);
			}
			
		} catch(CompilerException e) {
			System.out.println(e.getMessage());
			logCompileError(testDir);			
		} catch(CopyFilesException e) {
			System.out.println(e.getMessage());
			logCompileError(testDir);	
		} catch (MutationException e) {
			System.out.println(e.getMessage());
			logCompileError(testDir);
		} finally {
			session.deleteClassesDir();
			logGeneralInfo();
			tagTestAsAnalyzed(testDir);
		}
	}

	private void logGeneralInfo() throws IOException {
		List<String> lines = session.getLogInfo();
		for (MutationSystem mutationSystem : getMutationSystems()) {
			lines.addAll(mutationSystem.getLogInfo());
		}
		lines.addAll(equivalentDetection.getLogInfo());
		Utils.logWrite(session.getPath(), "general_info", lines);
	}

	private void logCompileError(File testDir) throws IOException {
		List<String> lines = new ArrayList<String>();
		lines.add(testDir.getName() + " didn't compile.");
		Utils.logAppend(session.getPath(), "failed_compile", lines);
	}

	// Create a file inside each program to confirm
	// that it was already analyzed
	private void tagTestAsAnalyzed(File testDir) throws IOException {
		List<String> lines = new ArrayList<String>();
		Calendar c = Calendar.getInstance();
		lines.add("Analyzed at: " + c.getTime());
		Utils.logWrite(testDir.getAbsolutePath(), "alredy_analyzed", lines);
	}

}
