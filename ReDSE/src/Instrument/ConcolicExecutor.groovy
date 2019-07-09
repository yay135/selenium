package Instrument


class ConcolicExecutor {
	
	//HashSet to store all the methods which are not entry methods
	public static Set<String> notEntryMethod=new HashSet<String>()

	//stores the variables come from input method
	Map<String,String> Input
	//stores the entry methods name
	Set<String> EntryMethodsName
	
	//ArrayList for output
	ArrayList<String> output = new ArrayList<String>();
	
	
	public ConcolicExecutor(def allMethodNodes, def visitor, def file, def apiFile, ArrayList<String> listEnv, HashMap<String, String> titleMap)
	{
		
		
		//get the environment variables of Input
		Input=visitor.varNames
		
		//Get the entryMethods
		EntryMethodsName = visitor.allEntryMethods
		
		//Put the method we dont want to analyze into notEntryMethod
		notEntryMethod.add("main");
		notEntryMethod.add("run");
		notEntryMethod.add("installed");
		notEntryMethod.add("updated");
		notEntryMethod.add("initialize");
		

		println ("**********************")
		/*symbolic execute a method if it is an entry method*/
		//flag to record if this is the first method we process
		int flag = 0;
		//process all the entry methods
		allMethodNodes.each { node->
			//only process entry method
			if(node.lineNumber!=-1 && !(notEntryMethod.contains(node.getName()))) {
				//read the head of the file before read the entry methods
				if(flag == 0)
				{
					String beforeEntry = ReadFile.ReadBeforeLine(file, node.lineNumber)
					output.add(beforeEntry)
				}
				//then entry methods
				ProcessEntryMethods PM;
				if(EntryMethodsName.contains(node.getName()))
				{
					PM = new ProcessEntryMethods(node, file, apiFile, listEnv, true, titleMap)
				}
				else
				{
					PM = new ProcessEntryMethods(node, file, apiFile, listEnv, false, titleMap)
				}
				String prefer = PM.makePreference();
				String methodHead= PM.makeHead();
				String contains = PM.makeContains();
				String methodTail = PM.makeTail();

				output.add(prefer)
				output.add(methodHead)
				output.add(contains)
				output.add(methodTail)
				
				flag = flag + 1;
				println ("**********************")
			}
		}
		//-------------------------------insert PCBound-------------------------------
		String PCboundPreference = "preferences {\n"
		PCboundPreference = PCboundPreference + "section(\"PCbound:\") {\n"
		
		PCboundPreference = PCboundPreference + "input " + "\"PCBOUND\"" + ", \"number\", title: \"PCBOUND\"" + ", required: false \n"
		PCboundPreference = PCboundPreference + "}\n}\n\n"
		output.add(PCboundPreference)
		
		//-------------------------------insert API-------------------------------------
		//Add dynamic API
		String dynamicAPI = "//*************************************************APIs WE ADD************************************************************\n\n"
		dynamicAPI = dynamicAPI +"def AddInitSymbolicValue(Map<String, List<Object>> programState){ \n";
		dynamicAPI = dynamicAPI + "//Add into Program State \n";
		for (Map.Entry<String, String> entry : Input.entrySet()) {
			//println ("Key = ${entry.getKey()}  Value =  ${entry.getValue()}")
			
			dynamicAPI = dynamicAPI + "AddIntoMap(programState, \"${entry.getKey()}\",\"${entry.getValue()}\",\"${entry.getValue()}\");" + "\n";
		}
		dynamicAPI = dynamicAPI + "} \n\n";
		//-------------------------------end---------------------------------------------
		output.add(dynamicAPI)
		
		
		//-------------------------------insert API-------------------------------------
		//add static API

		File concolicFile = new File(apiFile);
		String staticAPI = ReadFile.ReadAllFile(concolicFile)
		//-------------------------------end---------------------------------------------
		output.add(staticAPI)
		
	
	}
	
	def GetOutput()
	{
		return output;
	}

}
