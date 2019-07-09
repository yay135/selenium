package Instrument

import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.DeclarationExpression
import org.codehaus.groovy.ast.expr.ElvisOperatorExpression
import org.codehaus.groovy.ast.expr.GStringExpression
import org.codehaus.groovy.ast.expr.MethodCallExpression
import org.codehaus.groovy.ast.expr.NotExpression
import org.codehaus.groovy.ast.expr.PropertyExpression
import org.codehaus.groovy.ast.expr.TupleExpression
import org.codehaus.groovy.ast.expr.VariableExpression
import org.codehaus.groovy.ast.stmt.BlockStatement
import org.codehaus.groovy.ast.stmt.BreakStatement
import org.codehaus.groovy.ast.stmt.CaseStatement
import org.codehaus.groovy.ast.stmt.EmptyStatement
import org.codehaus.groovy.ast.stmt.ExpressionStatement
import org.codehaus.groovy.ast.stmt.ForStatement
import org.codehaus.groovy.ast.stmt.IfStatement
import org.codehaus.groovy.ast.stmt.ReturnStatement
import org.codehaus.groovy.ast.stmt.Statement
import org.codehaus.groovy.ast.stmt.SwitchStatement
import org.codehaus.groovy.ast.stmt.WhileStatement

class ProcessStatement {
	//the API before statement
	String head = "";
	
	//the statement itself
	String statement = "";
	
	//the API following the statement
	String tail = "";
	
	public ProcessStatement(def stmt, def file, HashMap<Integer, String> programHash, ArrayList<String> listEnv, ArrayList<ArrayList<String>> listForInput, HashMap<String, String> mapForDefined) 
	{
		String declareVariable = null;
		
		if(stmt instanceof ExpressionStatement) {
			println ("stmt: ExpressionStatement")
			ArrayList<String> listForDef = new ArrayList();
	
			// a list to store lazy initialization
			ArrayList<String> listForLazy = new ArrayList();
	
	
			//a list to store the profiling information
			ArrayList<String> runtimeDataBase = new ArrayList();
	
			ProcessExpression expression  = new ProcessExpression(stmt.expression, listForDef, listForInput, listEnv, mapForDefined, declareVariable, listForLazy, runtimeDataBase);

			String headOfExpression = expression.getHead();
			String expressionBody = expression.getExpression();
			String tailOfExpression = expression.getTail();
			
			statement = expressionBody + "\n";
			
			//pass the profiling information
			for(int i = 0; i< runtimeDataBase.size();i++)
			{
				String head = "AddIntoProfilingMap(profilingInfo," + "\"" + runtimeDataBase.get(i) + "\"" + ") \n"
			}
	
	
			for(int i = 0; i< listForDef.size();i++)
			{

				String defstr = listForDef.get(i);
				//closure should not have an update API or program crashed
				if(defstr.length()>7 && defstr.substring(0, 7) == "CLOSURE")
					{
						head = head + defstr.substring(7, defstr.length());
						head = head + "";
					}
					else if(defstr.length()>4 && defstr.substring(0, 4) == "METD")
					{
						head = head + defstr.substring(4, defstr.length());
						head = head + "\n";
					}
					else
					{
						head = head + defstr
						head = head + ProcessExpressionAPI.executeStatement(null, defstr, "") + "\n"
					}
		
			}
	
			//-------------------------------insert API-------------------------------------
			//we cannot add API in processExpression because some expression are in if condition and binary expression
			//we need to add API here for statement. To add Api, we need information of both statement and expression
			//However, in here, we can get little information about the expression So, we have to make a new class to handle it
	
			tail = tail + ProcessExpressionAPI.executeStatement(stmt.expression, expressionBody, "") + "\n"
	
			//-------------------------------end-------------------------------------
			
			head = head + headOfExpression;
			
		} 
		else if (stmt instanceof IfStatement){
			println ("stmt: IfStatement")
			//add code to get the infotmation of profiling
	
			//int l = stmt.lineNumber
			//program = program + "log.debug \"RelyInfo for if on " + l + ":  \${state.relyInfo}\"\n"
			//program = program + "state.varInPC = \"\" \n\n"
	
			ProcessIf ifBlock = new ProcessIf(stmt, file, programHash, listForInput, listEnv, mapForDefined)
			
			String headOfExpression = ifBlock.getHead();
			String expressionBody = ifBlock.getBody();
			String tailOfExpression = ifBlock.getTail();
			
			head = headOfExpression + "\n";
			statement = expressionBody + "\n";
			tail = tailOfExpression + "\n";	
		} 
		else if (stmt instanceof ForStatement){
			println ("stmt: ForStatement")

			ProcessFor forBlock = new ProcessFor(stmt, file, programHash, listForInput, listEnv, mapForDefined)
			
			String headOfExpression = forBlock.getHead();
			String expressionBody = forBlock.getBody();
			String tailOfExpression = forBlock.getTail();
			
			head = headOfExpression + "\n";
			statement = expressionBody + "\n";
			tail = tailOfExpression + "\n";
		}
		//*******************************************to be continue****************************************
		else if (stmt instanceof WhileStatement){
			println ("stmt: WhileStatement")
			//WhileStmt.execute (stmt, currentPath, pathesToProcess, stackedPath)
	
		} 
		else if (stmt instanceof SwitchStatement){
			println ("stmt: SwitchStatement")
			//SwitchStmt.execute (stmt, currentPath, pathesToProcess, stackedPath)
	
		}
		else if(stmt instanceof ReturnStatement)
		{
			head = "";
			statement = stmt.getText() + "\n";
			tail = "";
		}
		else {
			println ("stmt: pay attention other statement!!! : " + stmt.toString())
		}

		println("finish this statement" + "\n")

	}
	
	public String getHead() 
	{
		return head;
	}
	
	public String getStatement() 
	{
		return statement;
	}
	
	public String getTail() 
	{
		return tail;
	}
}
