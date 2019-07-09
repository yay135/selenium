package FrontEnd

import MainSystem.RunSystem

class Front {
	
	/*My Windows dir*/
	//dir for source files
	static String souceFileDir = "E:\\ConcolicExecuter\\test"
	
	//dir for the API file
	static String apiFileDir = "E:\\ConcolicExecuter\\APIFile.groovy"
	
	//dir for outputing instrumented files
	static String outputFileForIns = "E:\\OutPut\\ConcolicFile\\"
	
	//dir for outputing the files for testing multiple paths
	static String multiplePathFilepath = "E:\\OutPut\\MultiplePathFile\\"
	
	//dir for outputing profiled files
	static String outputFileForPro = "E:\\OutPut\\ProfiledFile\\";
	
	//dir for outputing SMT files
	static String outputFileForSMT = "E:\\OutPut\\SMTFile\\";

	
	//main entry
	static void main(args) {

		//delete all the generated files in the last time
		new File(multiplePathFilepath).eachFile { file ->
			file.delete();
		}
		new File(outputFileForIns).eachFile { file ->
			file.delete();
		}
		new File(outputFileForPro).eachFile { file ->
			file.delete();
		}
		new File(outputFileForSMT).eachFile { SMTFile ->
			SMTFile.delete();
		}
		
		//record the running time
		println "System running"
		long startTime=System.currentTimeMillis();
		
		//run the system
		RunSystem sys = new RunSystem(souceFileDir, apiFileDir, outputFileForIns, outputFileForPro, outputFileForSMT, multiplePathFilepath);
		//instrument the testing file
		int status = sys.instrumentFile();
		checkStatus(status, startTime);
		status = sys.runInstrumentFile();
		checkStatus(status, startTime);
		status = sys.profileFile();
		checkStatus(status, startTime);
		status = sys.runProfiledFile();
		checkStatus(status, startTime);
		status = sys.explorePaths();
		checkStatus(status, startTime);
		sys.outputResult();
		
		long endTime=System.currentTimeMillis();
		println "Test finished";
		println "runtime: " + (endTime-startTime) + "ms";		
		
	}
	
	static checkStatus(int status, long startTime) 
	{
		switch(status)
		{
			case 0:
				println "one phase finished"
				break;
			case 1:
				println "test failed, there is an error happen in broswer part"
				System.exit(0);
			case 2:
				long endTime=System.currentTimeMillis();
				println "runtime: " + (endTime-startTime) + "ms";
				System.exit(0);
					
		}
	}
}
