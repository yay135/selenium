package Instrument

import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.NotExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.ForStatement
import org.codehaus.groovy.ast.stmt.Statement

class ProcessIf {
	
	String head = "";
	
	String body = "";
	
	String tail = "";
	
	public ProcessIf(IfStatement ifStmt , def file, def programHash, ArrayList<ArrayList<String>> listForInput, ArrayList<String> listEnv, HashMap<String, String> mapForDefined) 
	{
		
		//String for declartion
		String declareVariable = null;
		
		//def location=ifStmt.getLineNumber()
		def booleanExp=ifStmt.booleanExpression.expression
		
		ArrayList<String> listForDef = new ArrayList();
		
		// a list to store lazy initialization
		ArrayList<String> listForLazy = new ArrayList();
		
		//a list to store the profiling information
		ArrayList<String> runtimeDataBase = new ArrayList();
		
		ProcessExpression conditionExpression = new ProcessExpression(booleanExp, listForDef, listForInput, listEnv, mapForDefined, declareVariable, listForLazy, runtimeDataBase);
		
		head = head + conditionExpression.getHead();
		tail = tail + conditionExpression.getTail();
		
		String condition  = conditionExpression.getExpression();
		
		//these statememts should be put in head, but I have no time to do this
		for(int i = 0; i< listForDef.size();i++)
		{

			String defstr = listForDef.get(i);
			//closure should not have an update API or program crashed
			if(defstr.length()>7 && defstr.substring(0, 7) == "CLOSURE")
			{
				body = body + defstr.substring(7, defstr.length());
				body = body + "";
			}
			else if(defstr.length()>4 && defstr.substring(0, 4) == "METD")
			{
				body = body + defstr.substring(4, defstr.length());
				body = body + "\n";
			}
			else
			{
				body = body + defstr
				body = body + ProcessExpressionAPI.executeStatement(null, defstr, "") + "\n"
			}
				
		}
		
		
		//int l = ifStmt.lineNumber
		//program = program + "log.debug \"RelyInfo for if on " + l + ":  \${state.relyInfo}\"\n"
		//program = program + "state.varInPC = \"\" \n\n"
		body = body + "//INSERT_RELY_FOR_IF \n"
		// output all the concrete variables
		//program = program + "log.debug \"\${getConcreteVariable(programState)}\" \n"
		
		body = body + "if("
		

		body = body + condition + "){" + "\n"
		
		body = body + "state.ifCount = state.ifCount + 1; \n\n"
		
		//Arraylist to store the expressions
		ArrayList<String> expSet = new ArrayList<String>();
		
		//-------------------------------insert API-------------------------------------
		//replace the " in condition
		String condition2 = condition.replace("\"","'")
		body = body + "AddPathCondition(state.pathCondition, programState, \"$condition2\");" + "\n"
		
		
		String SMT = ProcessExpressionSMTLIB.executeExpression(booleanExp, expSet, condition);
		//replace the " in SMT LIB formula
		SMT = SMT.replace("\"","'")
		//String for a variable that involve the condition, we can use it to get the type of every variables
		println (condition)
		String typeVariable = "getType(" + expSet.get(0) + " )"
		body = body + "AddSMTPathCondition(programState, " + "\"" + SMT + "\"," + typeVariable + "  )\n\n"
		
		//-------------------------------end-------------------------------------
		
		def ifBlock = ifStmt.ifBlock
		def elseBlock = ifStmt.elseBlock

		def ifStmts = ifBlock.statements
		
		ifStmts.each {stmt ->
			
			ProcessStatement statement =  new ProcessStatement(stmt, file, programHash, listEnv, listForInput, mapForDefined);
			String statementHead = statement.getHead();
			String mainStatement = statement.getStatement();
			String statementTail = statement.getTail();
			
			body = body + statementHead + mainStatement + statementTail;
		}
		
		//still for collecting profiling information
		body = body + "//INSERT_VARINPC_FOR_IF \n\n"
		
		body = body + "}" + "\n"
		
		def elseStmt
		if(elseBlock instanceof BlockStatement)
		{
			body = body + "else{ \n"
			body = body + "state.ifCount = state.ifCount + 1; \n\n"

			//-------------------------------insert API-------------------------------------
			//insert path condition
			body = body + "AddPathCondition(state.pathCondition, programState, \"!  $condition2\");" + "\n"
			
			body = body + "AddSMTPathCondition(programState, " + "\"(! " + SMT + ")\"," + typeVariable + ")\n\n"
			//-------------------------------end-------------------------------------
			elseStmt = elseBlock.statements
			elseStmt.each {stmt ->
				
				ProcessStatement statement =  new ProcessStatement(stmt, file, programHash, listEnv, listForInput, mapForDefined);
				String statementHead = statement.getHead();
				String mainStatement = statement.getStatement();
				String statementTail = statement.getTail();
			
				body = body + statementHead + mainStatement + statementTail;

			}
			//still for collecting profiling information
			body = body + "//INSERT_VARINPC_FOR_IF \n\n"
			
			body = body + "}" + "\n"
		}
		else
		{
			println ("No else block")
			body = body + "else{ \n"
			body = body + "state.ifCount = state.ifCount + 1; \n\n"
			//-------------------------------insert API-------------------------------------
			//insert path condition
			body = body + "AddPathCondition(state.pathCondition, programState, \"!  $condition2\");" + "\n"
			body = body + "AddSMTPathCondition(programState, " + "\"(! " + SMT + ")\"," + typeVariable + ")\n\n"
			//-------------------------------end-------------------------------------
			
			//still for collecting profiling information
			body = body + "//INSERT_VARINPC_FOR_IF \n\n"
			
			body = body + "}" + "\n"
		}
		
	}
	
	public String getHead()
	{
		return head;
	}
	
	public String getBody()
	{
		return body;
	}
	
	public String getTail()
	{
		return tail;
	}
	
	
}
