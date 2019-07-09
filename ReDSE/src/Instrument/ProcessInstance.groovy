package Instrument

import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ConstantExpression
import org.codehaus.groovy.ast.expr.ConstructorCallExpression
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

class ProcessInstance {
	
	String head = "";
	
	String body = "";
	
	String tail  = "";
	
	public ProcessInstance(def exp ,ArrayList<String> listForDef, ArrayList<ArrayList<String>> listForInput, ArrayList<String> listEnv, HashMap<String, String> mapForDefined, String declareVariable, ArrayList<String> listForLazy, ArrayList<String> runtimeDataBase) 
	{
		//list for storing expressions in arguments
		ArrayList<String> list = new ArrayList<String>();
		
		//list for storing the arguments temp name
		ArrayList<String> listTemp = new ArrayList<String>();
		

		int tag = 0;
		
		def arguments = exp.getArguments().expressions;
		
		arguments.each { a ->
			
			int column = a.columnNumber;
			
			int line = a.lineNumber
			
			if(column < 0) 
			{
				column = -1 * column;
			}
			
			if(line < 0) 
			{
				line = -1 * line;
			}

			String left = "def tempVariable_" + column + line + " = "
			
			String tempName = "tempVariable_" + column + line
			
			listTemp.add(tag, tempName)
			
			ProcessExpression unKnownExpression = new ProcessExpression(a, listForDef, listForInput, listEnv, mapForDefined, declareVariable, listForLazy, runtimeDataBase);
			
			head = head + unKnownExpression.getHead();
			
			tail = tail + unKnownExpression.getTail();
			
			left = left + unKnownExpression.getExpression() + "\n"
			
			
			listForDef.add(left)
			
			tag = tag + 1;
		}
		
		
		def type = exp.getType().getName();
		
		
		body = body  + " new " + type + "("
		
		if(tag == 0)
		{
			body = body + ")" + "\n"
		}
		else
		{
			tag = tag - 1;
			for(int i = 0; i< tag; i++)
			{
				body = body + listTemp.get(i) + ", "
			}
	
			body = body + listTemp.get(tag)
			body = body + ")" + "\n"
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
