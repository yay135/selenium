package Instrument

class ProcessEntryMethods {
	
	//String for the output program
	String MethodCode = "";
	
	//Map for the whole program    I put it here because it will be used in different objects of ProcessStatement (if,else,for also have blocks of statements)
	HashMap<Integer, String> programHash = new HashMap<Integer, String>();
	
	//Method Node made by InsnVisitor
	def methodNode;
	
	//Original file
	def file;
	
	//The file contains API code
	def apiFile;
	
	//a list to store the environment variables
	ArrayList<String> listEnv;
	
	//a flag to see if the method is an entry method
	def isEntry;
	
	//a map to store the title for environment variables; the title will be used in preferences
	HashMap<String, String> titleMap
	
	//method name
	def methodName;
	
	
	
	public ProcessEntryMethods(def methodNode, def file, def apiFile, ArrayList<String> listEnv, def isEntry, HashMap<String, String> titleMap)
	{
		this.methodNode = methodNode;
		this.file = file;
		this.apiFile = apiFile;
		this.listEnv = listEnv;
		this.isEntry = isEntry;
		this.titleMap = titleMap;
		
		//Read the file and store it in hash
		ReadFile.StoreWholeFileInHash(file,programHash);
		
		//Get Method name
		methodName =methodNode.getName()
		
	}
	
	//String to store the name of evt parameter
	String evtName = "";
	
	public String makeHead() 
	{
		String head = "";
		
		//put it into string
		head = head + "def " + methodName + "(";
		
		//find parameters and put them into string
		def parameters=methodNode.parameters
		
		if(isEntry)
		{
			parameters.each { p ->
				//println "Contains parameters : ${p.name}"
				//evt doesn't have def but others have
				//whatever the entry method should have one parameter (evt) ONLY
				if(p.name == "evt")
				{
					head = head + p.name + ",";
					evtName = p.name;
				}
				else
				{
					head = head + p.name + ",";
					evtName = p.name;
				}
			}
		}
		else
		{
			//The other methods should not have evt as parameter
			parameters.each { p ->
				println "Contains parameters : ${p.name}"
				head = head + "def " + p.name + ",";
				
			}
		}
		
		//delete the last "," we added
		if(parameters.size()!=0)
		{
			head = head.substring(0, head.length()-1)
		}
		
		//Add the begin { into string
		head = head + "){ \n"
		
		if(isEntry)
		{
			head = head + "//PEI_HEAD\n\n"
			head = head + "\n//INSERT_FOR_START" + "\n\n"
		}
		else
		{
			head = head + "//INSERT_FOR_API" + "\n"
		}
		
		//-------------------------------insert API-------------------------------------
		head = head + "//***********************************************One String for path Condition******************************************* \n"
		//Only init the PC when the method is entry method (appeared in subscribe)
		if(isEntry)
		{
			head = head + "state.pathCondition = \"\" \n";
			head = head + "state.pcSmt = \"\" \n";
			head = head + "state.declare = \"\" \n";
			head = head + "state.tag = 1 \n\n";
			//string for relations between symbolic variables
			head = head + "state.relyInfo = \"\" \n"
			head = head + "state.varInPC = \"\" \n"
			head = head + "state.objEnv = \"\" \n"
			head = head + "state.ifCount = 0 \n"
			head = head + "state.logString = \"\" \n"
			head = head + "int forCountI = 0;\n"
			 
		}
		else
		{
			head = head + "int forCountI = 0;\n"
		}
		
		head = head + "//new map for state \n Map<String, List<Object>> programState = new HashMap<String, List<Object>>(); \n //new map for environment \n Map<String, Object> programEnvironment = new HashMap<String, List<Object>>(); \n //new map for profiling \n Map<String, String> profilingInfo = new HashMap<String, String>(); \n //new list for lazy initialization \n ArrayList<String> lazyList = new ArrayList<String>(); \n";
		head = head + "//*********************************************Add all the symbolic value from input***************************************** \n AddInitSymbolicValue(programState); \n"
		//inser1t parameter into state
		head = head + "//For function involve parameter, we need to add parameter variable \n"
		parameters.each { p ->
			//for evt we need to add evt.name
			if(p.name == "evt")
			{
				head = head + "AddIntoMap(programState, \"" + p.name + "\""  +  ",\"" + "\${" +  p.name + ".name}\" , \"evt\"); \n\n"
			}
			else
			{
				head = head + "AddIntoMap(programState, \"" + p.name + "\""  +  ",\"" + "\$" +  p.name + "\", getType(${p.name}) ); \n\n"
			}
			
			
		}
		
		//------------------------------------end--------------------------------------
		return head;
	}
	
	//add a list to store the input of preference
	ArrayList<ArrayList<String>> listForInput = new ArrayList();
	
	public String makeContains() 
	{
		//the code Block inside the method
		String contains = "";
		
		
		//add a map to store the defined variable and temp name
		HashMap<String, String> mapForDefined = new HashMap<String, String>();
		
		//find statements includes assign and if else
		def stmts = methodNode.getCode().statements
		
		//put evtName into environment
		listEnv.add(evtName);
		
		//Process each statement
		stmts.each {stmt ->
			ProcessStatement statement =  new ProcessStatement(stmt ,file, programHash, listEnv, listForInput, mapForDefined);
			String statementHead = statement.getHead();
			String mainStatement = statement.getStatement();
			String statementTail = statement.getTail();
			
			contains = contains + "\n" + statementHead + mainStatement + statementTail;
		}
		
		return contains;
		
	}
	
	public String makeTail() 
	{
		 String tail = "";
		 tail = tail + "\n\n//******************************************************Output Data we get**************************************************\n"
		 tail = tail + "//log.debug \"\$programState\" "  + "\n"
		 tail = tail + "//log.debug \"\$programEnvironment\" "  + "\n"
		 tail = tail + "//log.debug \"\$lazyList\" "  + "\n"
		 tail = tail + "//log.debug \"\$profilingInfo\" "  + "\n"
		 
		 tail = tail + "processLogString(" + "\"\$profilingInfo\"" + ")\n"
		 if(isEntry)
		 {
			 tail = tail + "//log.debug \"PC:  \${state.pathCondition}\" " + "\n"
			 tail = tail + "processLogString(" + "\"PC:  \${state.pathCondition}\"" + ")\n"
			 
			 tail = tail + "//log.debug \"Declare:  \${state.declare}\" " + "\n"
			 tail = tail + "processLogString(" + "\"Declare:  \${state.declare}\"" + ")\n"
			 
			 tail = tail + "state.pcSmt = state.pcSmt + \"#\" + PCBOUND" + "\n"
			 tail = tail + "//log.debug \"PC in SMTLIB version:  \${state.pcSmt}\" " + "\n"
			 tail = tail + "processLogString(" + "\"PC in SMTLIB version:  \${state.pcSmt}\"" + ")\n"
			 
			 tail = tail + "//log.debug \"RelyInfo:  \${state.relyInfo}\" " + "\n"
			 tail = tail + "//log.debug \"varInPC:  \${state.varInPC}\" " + "\n"
			 
			 tail = tail + "//log.debug \"\${state.logString}\"" + "\n"
			 
			 tail = tail + "outputLog();" + "\n"
			 
			 tail = tail + "//PEI_TAIL\n"
		 }
	 
		 
		 tail = tail + "}" + "\n";
		 
		 return tail;
		
	}
	
	
	
	public String makePreference() 
	{
		//-------------------------------insert API Preference----------------------------------
		
		String preference = ""
		preference = preference + "preferences {\n"
		preference = preference + "section(\"Environment variables:\") {\n"
		
		for (int i = 0; i<listForInput.size();i++)
		{
			preference = preference + "input " + "\"" + listForInput.get(i).get(0) + "\"" + ", " + "\"" + listForInput.get(i).get(1) + "\", " + "title: " + "\""  + listForInput.get(i).get(2) + "\"" + ", required: false \n"
			
			//add into title map
			String section = "Environment variables of" + methodName + ":";
			String subtitle = listForInput.get(i).get(2);
			
			String varName = listForInput.get(i).get(0);
			
			String title = section + "#" + subtitle
			
			titleMap.put(varName, title);
			
			
		}
		preference = preference + "}\n}\n\n"
		
		//------------------------------------end--------------------------------------
		
		
		
		if(listForInput.size() != 0)
		{
			return preference;
		}
		else
		{
			return "";
		}
	}
	
	
	
}
