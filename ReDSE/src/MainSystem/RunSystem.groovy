package MainSystem

import Interact.*
import PE.*
import Profile.*

class RunSystem {
	
	//Dir for files that need to be tested
	String testAppsPath;
	//The api file
	String APIpath;
	//output folder for instrumented file
	String outputPathForIns;
	
	//output folder for instrumented profiling file
	String outputFileForPro;
	
	//the path for testing multiple paths files
	String outputPathForMutiplePathFiles;
	
	//dir for outputing SMT files
	String outputFileForSMT;
	
	//the arraylist for storing the inputs of every test case
	ArrayList<HashMap<String, String>> allPathInputs = new ArrayList<ArrayList<String>>();
	
	//variables and their titles and subtitles
	HashMap<String, String> titleMap;
	
	//the bug we found
	ArrayList<String> bugList = new ArrayList<String>();
	
	//work list(contains path to be explored)
	ArrayList<PCPackage> workList = new  ArrayList<PCPackage>();

	public RunSystem(testAppsPath, APIpath, outputPathForIns, outputFileForPro, outputFileForSMT, outputPathForMutiplePathFiles)
	{
		this.testAppsPath = testAppsPath;
		this.APIpath = APIpath;
		this.outputPathForIns = outputPathForIns;
		this.outputFileForPro = outputFileForPro;
		this.outputPathForMutiplePathFiles = outputPathForMutiplePathFiles;
		this.outputFileForSMT = outputFileForSMT;
		
	}
	
	//instrument all the file in the dir
	public int instrumentFile() 
	{
		
		SimpleFile sf = new SimpleFile()
		sf.processAllApps(testAppsPath, APIpath, outputPathForIns);
		titleMap = sf.getTitleMap();
		
		//Delete all the temp files
		new File(outputPathForIns).eachFile { file ->
			
			if(file.getName().contains("temp_"))
			{
				println "deleting ${file.getName()}"
				file.delete();
			}
		}
		
		return 0;
	}
	
	//dependence from first time run
	ArrayList<String> dependence = new ArrayList<String>();
	//first time solution
	String firstTimeSolution;
	
	//the instrumented file
	File file;
	
	//the inputs for a run
	ArrayList<String> preferencesInputs = new ArrayList<String>();
	ArrayList<String> simulatorsInputs = new ArrayList<String>();
	
	//run the instrument files
	public int runInstrumentFile() 
	{
		//*********************************************************run all the files in that Dir*******************************************************************************
		new File(outputPathForIns).eachFile { instrumentedfile ->
			 println "run ${instrumentedfile.getName()}"
			 file = instrumentedfile;			 
		 }
			 
		String codePath = file.getPath();
			 
		String log = "";
		int numberOfRun = 0;
		int numberOfRun2 = 0;
			 
		int flag = 1;
			 
		preferencesInputs.add("PCbound:#PCBOUND@empty");
			 
		while(flag != 0)
		{
			while(log == "")
			{
				if(numberOfRun == 10)
				{
					println "The log part failed 10 times and may have a problem1"
					return 1;
							 
				}
				try {
					//def se = new selenium2(preferencesInputs, simulatorsInputs, codePath, "50");
					def se = new selenium2_20s(preferencesInputs, simulatorsInputs, codePath, "50");
					se.test();
					log = se.getLog();
				}
				catch(Exception e)
				{
					println "A problem happened in runtimecode"
				}
				numberOfRun++;
			}
				 
			println "numberOfRun: " + numberOfRun;
				 
			numberOfRun = 0;
		 
			//process the log
			LogProcessing lp = new LogProcessing();
			flag = lp.processingInstrumentLog(log);
			bugList.addAll(lp.getBugList());
				 
			//no path
			if(flag == 2)
			{
				println "Finished! Only one path. No more paths to explore"
				return 2;
			}
					 
			firstTimeSolution = lp.getfirstTimeSolution();
			dependence = lp.getDependence();
					 
			log = "";
			numberOfRun2++;
		}
		return 0;
	}
	
	//ProfileInfo Database
	HashMap<String, ArrayList<String>> profileDatabase = new HashMap<String, ArrayList<String>>();
	
	//the result of profiling
	ArrayList<String> profileInfoList = new ArrayList<String>();
	
	//make the profile file
	public int profileFile() 
	{
		//get declare
		PCPackage pcktemp = new PCPackage(firstTimeSolution, null, null);
		
		ArrayList<String> tempDeclareList = pcktemp.getDeclareStatements();
		
		ProfileFile pf = new ProfileFile();
		pf.processApp(file, outputFileForPro, file.getName(), dependence, profileDatabase, tempDeclareList)
		
		profileInfoList.addAll(pf.getProfileList());
		
		return 0;
	}
	
	//run the profiled file
	public int runProfiledFile() 
	{
		new File(outputFileForPro).eachFile { file2 ->
			println "profiling  ${file2.getName()}"
			
			String[] tempPathName = file2.getName().split(".groovy")
			
			String pathName = tempPathName[0];
			
			String codePath2 = file2.getPath();
			String log = "";
			int numberOfRun = 0;
			int numberOfRun2 = 0;
			
			int flag = 1;
			//prepare input
			preferencesInputs.clear();
			preferencesInputs.add("PCbound:#PCBOUND@empty");
			
			
			while(flag != 0)
			{
				while(log == "")
				{
					if(numberOfRun == 10)
					{
						println "The log part failed 10 times and may have a problem"
						return 1;
							
					}
					try {
						def se = new selenium2(preferencesInputs, simulatorsInputs, codePath2, "50");
						se.test();
						log = se.getLog();
					}
					catch(Exception e)
					{
						println "A problem happened in runtimecode"
					}
					numberOfRun++;
				}
				
				println "numberOfRun: " + numberOfRun;
				
				numberOfRun = 0;
		
				//process the log
				LogProcessing lp2 = new LogProcessing();
				flag = lp2.processingProfileLog(log);
				bugList.addAll(lp2.getBugList());
				
				
				profileInfoList.addAll(lp2.getProfileList());
				
				profileDatabase.put(pathName, lp2.getProfileList());

				log = "";
				numberOfRun2++;
			}
			
		}
		
		//clear all the files in the profile dir
		println "deleting profile files"
		new File(outputFileForPro).eachFile { profiledFile ->
				profiledFile.delete();
		}
		
		//make the profiling info to SMT2
		ArrayList<String> processedProfileInfo = new ArrayList<String>();
		ArrayList<String> ifCountList = new ArrayList<String>();
		
		String profileInfo = ""
		int ifcount = -1;
		if(profileInfoList.size()>0) {
			ProcessProfilingInfo ppi = new ProcessProfilingInfo(profileInfoList);
			
			processedProfileInfo = ppi.getSMTProfilingInfo();
			ifCountList = ppi.getIfCount();
		}
		else
		{
			processedProfileInfo.add(profileInfo);
			ifCountList.add(ifcount);
		}
		
		
		PCPackage pck = new PCPackage(firstTimeSolution, processedProfileInfo, ifCountList);
		
		workList.add(pck);
		
		return 0;
	}
	
	//the number of paths that has been explored
	int pathNumber = 0;
	//set to store the unsatisfied paths
	Set<String> unSATSet = new HashSet<String>();
	
	//explore the paths
	public int explorePaths() 
	{
		while(workList.size()!=0)
		{
				
				PathExplore pe = new PathExplore(workList, unSATSet);
				ArrayList<SolutionPackage> solutionList = pe.explorePath();
				pathNumber = pathNumber + workList.size();
				workList.clear();
				
				
				
				//a list to store all the paths and their solutions
				ArrayList<HashMap<String, String>> pathInputs = new ArrayList<ArrayList<String>>();
				
				//store all the new logs for these solutions
				HashMap<String, String> mapForLogs = new HashMap<String, String>();
				
				int solutionNumber = solutionList.size();
				
				println solutionNumber + " solutions in a turn"
				
				//decide if there are more solutions
				if(solutionNumber == 0)
				{
					continue;
				}
					
				//process the solutions and put them into the list
				for(int i = 0; i< solutionNumber; i++)
				{
				
					//Parse the solutionPackage to input format
					SolutionPackage sp = solutionList.get(i);
					String str = sp.getSolution();
					String[] tempSolution = str.split("#");
					
					HashMap<String, String> pairSolution = new HashMap<String, String>();
					
					for(int j = 1; j<tempSolution.size(); j++)
					{
						String pair = tempSolution[j];
						String[] pairtemp = pair.split("@");
						String name = pairtemp[0];
						String value = pairtemp[1];
						
						//solution < 0
						if(value.contains("("))
						{
							value = value.replace("(","");
							value = value.replace(" ","");
						}
						
						pairSolution.put(name, value);
					}
					String pathbound = String.valueOf(sp.getPathBound());
					pairSolution.put("PCBOUND", pathbound);
					
					pathInputs.add(pairSolution);
					allPathInputs.add(pairSolution);
				}
				
				//make the mutiple path solutions file
				PathExploreImprovement pei = new PathExploreImprovement(pathInputs, outputPathForMutiplePathFiles, file);
				pei.processMutiplePathApp();
				String codePathofMutiple = pei.getCodepath();
				//Two lists for inputs
				//format: title@subtitle@value
				preferencesInputs.clear();
				//format:#title@value
				simulatorsInputs.clear();
				
				//delete all the SMT file
				println "deleting SMT files"
				new File("E:\\OutPut\\SMTFile\\").eachFile { file4 ->
					file4.delete();
				}
				
				
					
				//dependence in loop
				ArrayList<String> dependenceInloop = new ArrayList<String>();
				//solution in loop
				String solutionInLoop = "";
					
				String log = "";
				int numberOfRun = 0;
					
				int flagInLoop = 1;
				while(flagInLoop != 0)
				{
					while(log == "")
					{
						if(numberOfRun == 10)
						{
							println "The log part failed 10 times and may have a problem3"
							return 1;
										
						}
						try {
							def se = new selenium2_ten_run(3, preferencesInputs, simulatorsInputs, codePathofMutiple, "50");
							se.test();
							log = se.getLog();
						}
						catch(Exception e)
						{
							println "A problem happened in runtimecode"
						}
						numberOfRun++;
					}
					println "numberOfRun: " + numberOfRun;

					numberOfRun = 0;
					//process the log
					LogProcessing lp = new LogProcessing();
					mapForLogs = lp.processMutiplePathLog(log);
					
					//we miss some logs
					if(mapForLogs.size() != solutionNumber)
					{
						flagInLoop = 1;
					}
					else
					{
						flagInLoop = 0;
					}
					log = "";
				}
				
				//delete mutiple path file
				println "deleting mutiple path files"
				new File(outputPathForMutiplePathFiles).eachFile { file5 ->
					file5.delete();
				}

				
				for(String key : mapForLogs.keySet())
				{
					String logOfAPath = mapForLogs.get(key);
					
					//process the log
					LogProcessing lp = new LogProcessing();
					flagInLoop = lp.processingInstrumentLog(logOfAPath);
					
					bugList.addAll(lp.getBugList());
								
					solutionInLoop = lp.getfirstTimeSolution();
					dependenceInloop = lp.getDependence();
					
					//get declare
					PCPackage pcktempInLoop = new PCPackage(solutionInLoop, null, null);
					
					ArrayList<String> tempDeclareListInLoop = pcktempInLoop.getDeclareStatements();
								
					ProfileFile pfInLoop = new ProfileFile();
					pfInLoop.processApp(file, outputFileForPro, file.getName(), dependenceInloop, profileDatabase, tempDeclareListInLoop);
					
					
					//*********************************************************Get Profile Info*******************************************************************************
					 ArrayList<String> profileInfoListInLoop = new ArrayList<String>();
					 new File(outputFileForPro).eachFile { file2 ->
						 //println "profiling  ${file2.getName()}"
						 
						 //String[] tempPathName = file2.getName().split(".")
						 String[] tempPathName = file2.getName().split(".groovy")
						 String pathName = tempPathName[0];
						 
						 
						 String codePath2 = file2.getPath();
						 log = "";
						 numberOfRun = 0;
						 
						 int flag = 1;
						 
						 while(flag != 0)
						 {
							 while(log == "")
							 {
								 if(numberOfRun == 10)
								 {
									 println "The log part failed 10 times and may have a problem4"
									 return 1;
										 
								 }
								 try {
									 def se = new selenium2(preferencesInputs, simulatorsInputs, codePath2, "50");
									 se.test();
									 log = se.getLog();
								 }
								 catch(Exception e)
								 {
									 println "A problem happened in runtimecode"
								 }
								 numberOfRun++;
							 }
							 
							 println "numberOfRun: " + numberOfRun;
							 
							 numberOfRun = 0;
					 
							 //process the log
							 LogProcessing lp2 = new LogProcessing();
							 flag = lp2.processingProfileLog(log);
							 profileInfoListInLoop.addAll(lp2.getProfileList());
							 
							 profileDatabase.put(pathName, lp2.getProfileList());
		 
							 log = "";
						 }
						 
					 }
					 
					 //clear all the files in the profile dir
					 println "deleting profile files"
					 new File(outputFileForPro).eachFile { file3 ->
							 file3.delete();
					 }
					 
					 //make the profiling info to SMT2
					 ArrayList<String> processedProfileInfoInLoop = new ArrayList<String>();
					 ArrayList<String> ifCountListInLoop = new ArrayList<String>();
					 
					 String profileInfoInLoop = ""
					 int ifcountInLoop = -1;
					 if(profileInfoListInLoop.size()>0) {
						 ProcessProfilingInfo ppiInLoop = new ProcessProfilingInfo(profileInfoListInLoop);
						 
						 processedProfileInfoInLoop = ppiInLoop.getSMTProfilingInfo();
						 ifCountListInLoop = ppiInLoop.getIfCount();
					 }
					 else
					 {
						 processedProfileInfoInLoop.add(profileInfoInLoop);
						 ifCountListInLoop.add(ifcountInLoop);
					 }
					 
					 PCPackage pckInLoop = new PCPackage(solutionInLoop, processedProfileInfoInLoop, ifCountListInLoop);
 
					 workList.add(pckInLoop);
					 
					 println "Finish one turn"
					 
					 
				}
			}
			
			return 0;
	}

	//output the result
	public void outputResult() 
	{
		new File(outputFileForSMT).eachFile { SMTFile ->
			SMTFile.delete();
		}
		
		println "Finish the file  $file, explore $pathNumber paths";
		
		println "all the paths are: \n" + allPathInputs;
		
		if(unSATSet.size()>0)
		{
			String temp = "Except the init paths, " + unSATSet.size() + " paths did not reach which are:\n";
			Iterator<String> it = unSATSet.iterator();
			while (it.hasNext()) {
					String str = it.next();
					temp = temp + str + "\n";
			}
			println(temp);
		}
		
	
		
	}
	
	
}
