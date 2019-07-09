package Instrument

import org.codehaus.groovy.ast.expr.Expression
import org.codehaus.groovy.ast.Parameter
import org.codehaus.groovy.ast.expr.ArgumentListExpression
import org.codehaus.groovy.ast.expr.BinaryExpression
import org.codehaus.groovy.ast.expr.ClassExpression
import org.codehaus.groovy.ast.expr.ClosureExpression
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

class ProcessExpression {
	
	//the API before statement
	String head = "";
	
	//the statement itself
	String expression = "";
	
	//the API following the statement
	String tail = "";
	
	public ProcessExpression(Expression exp, ArrayList<String> listForDef, ArrayList<ArrayList<String>> listForInput, ArrayList<String> listEnv, HashMap<String, String> mapForDefined, String declareVariable, ArrayList<String> listForLazy, ArrayList<String> runtimeDataBase) 
	{
		
		//DeclarationExpression must be put before BinaryExpression
		if (exp instanceof DeclarationExpression) {
			
			Expression leftExp = exp.leftExpression
			Expression rightExp = exp.rightExpression
				
			
			//Processing the expression
			ProcessExpression left = new ProcessExpression (leftExp, listForDef, listForInput, listEnv, mapForDefined, declareVariable, listForLazy, runtimeDataBase);
			
			String expressionHeadLeft = left.getHead();
			String expressionBodyLeft = left.getExpression();
			String expressionTailLeft = left.getTail();
			
			declareVariable = expressionBodyLeft;

			expression = "def " + expressionBodyLeft + " = "
			
			ProcessExpression right = new ProcessExpression (rightExp, listForDef, listForInput, listEnv, mapForDefined, declareVariable, listForLazy, runtimeDataBase);
			
			String expressionHeadRight = right.getHead();
			String expressionBodyRight = right.getExpression();
			String expressionTailRight = right.getTail();

			head = head + expressionHeadLeft + "\n" + expressionHeadRight;
			
			expression = expression + expressionBodyRight + "\n";
			
			tail = tail + expressionTailLeft + "\n" + expressionTailRight;
			
		}
		else if (exp instanceof BinaryExpression) {
			 
			
			Expression leftExp = exp.leftExpression
			Expression rightExp = exp.rightExpression
			 
			 
			String op = exp.operation.getText()

			ProcessExpression left  = new ProcessExpression(leftExp, listForDef, listForInput, listEnv, mapForDefined, declareVariable, listForLazy, runtimeDataBase)
			
			String expressionHeadLeft = left.getHead();
			String expressionBodyLeft = left.getExpression();
			String expressionTailLeft = left.getTail();
			
			ProcessExpression right = new ProcessExpression(rightExp, listForDef, listForInput, listEnv, mapForDefined, declareVariable, listForLazy, runtimeDataBase)
			
			String expressionHeadRight = right.getHead();
			String expressionBodyRight = right.getExpression();
			String expressionTailRight = right.getTail();
			
			//binary expression should not have \n inside
			expressionBodyLeft = expressionBodyLeft.replace("\n","");
			expressionBodyRight = expressionBodyRight.replace("\n","");
			
			head = head + expressionHeadLeft + "\n" + expressionHeadRight;
			
			expression = expression + "(" + expressionBodyLeft + op + expressionBodyRight + ")";
			
			tail = tail + expressionTailLeft + "\n" + expressionTailRight;
			
		}
		else if (exp instanceof ConstantExpression) {
			
			def obj=exp.getValue()
			if(obj instanceof String)
			{
				expression = expression + "\"" + exp.getText() + "\"";
			}
			else
			{
				expression = expression + exp.getText();
			}
			
		}
		else if (exp instanceof MethodCallExpression) {

			String str = exp.getText()
			if(str.contains("log"))
			{
				expression = expression + "//" + str
			}
			else
			{
				
				ProcessMethodCall method = new ProcessMethodCall(exp, listForDef, listForInput, listEnv, mapForDefined, declareVariable, listForLazy, runtimeDataBase)
				String methodHead = method.getHead();
				String methodBody = method.getBody();
				String methodTail = method.getTail();
				
				head = head + methodHead + "\n";
				expression = expression + methodBody + "\n";
				tail = tail + methodTail + "\n";

			}
			
		}
		else if(exp instanceof PropertyExpression)
		{
			def object = exp.getObjectExpression();
			def property = exp.getProperty();
			
							
			ProcessExpression objectExpression = new ProcessExpression(object, listForDef, listForInput, listEnv, mapForDefined, declareVariable, listForLazy, runtimeDataBase) + "." + ProcessExpression.execute(property, listForDef, listForInput, listEnv, mapForDefined, declareVariable, listForLazy, runtimeDataBase)
			String objectHead = objectExpression.getHead();
			String objectBody = objectExpression.getExpression();
			String objectTail = objectExpression.getTail();
			
			String objPro = objectBody;
			//because AST treat property and objPro as String, so they will be surrounded by "" because our prior process
			objPro = objPro.replace("\"", "");
			
			
			//re define it
			//this should be a head, but I dont have that much time, I shall leave it for future
			String left = "tempVariable_" + property.columnNumber + property.lineNumber + " = "
			
			//check if the property has already been redefined
			//IF COUSURE THEN STILL NEED DIFINE
			if(mapForDefined.containsKey(objPro) && listForDef.size()>0 && listForDef.get(0) != "CLOSURE")
			{
				// use the defined temp name of this variable
				String tempName = mapForDefined.get(objPro);
				expression = expression + tempName;
			}
			else
			{
				String tempName = "tempVariable_" + property.columnNumber + property.lineNumber;
				mapForDefined.put(objPro, tempName)
				left = left + objPro + "\n"
				left = "def " + left
				
				listForDef.add(left)
				
				expression = expression + "tempVariable_" + property.columnNumber + property.lineNumber;
				
				//Compare with environment database to see if it is environment variable
				CompareWithEnvironment cwe = new CompareWithEnvironment(object.getText(), property.getText(), listEnv)
				if(cwe.ReturnIsDirectAccessBasicValue())
				{
					// add input for it
					ArrayList<ArrayList<String>> tempList = new ArrayList<ArrayList<String>>();
					String name = "tempVariable_" + property.columnNumber + property.lineNumber
					String type = cwe.getType()
					String title = "The input value for " + objPro.replace("\"", "");
					
					String tempName2 = name + "pair";
					tempList.add(tempName2)
					tempList.add(type)
					tempList.add(title)
					
					listForInput.add(tempList)
					
					// to be finish add getEnvironment method
					
					String tempMethod = "";
					tempMethod = "set" + property.getText().substring(0, 1).toUpperCase() +  property.getText().substring(1, property.getText().length());
					String Name = object.getText() + "." + tempMethod;
					
					String update;
					
					update = name + " = EnvironmentGetValue(programState, programEnvironment, "
					
					update = update + "\"" + Name + "\" ," + "\"" + name + "\"," + name + ", " + name + "pair);\n";
					
					listForDef.add(update)
					
				}
				
				//if is Env Object we need to add the declare variable into the envList
				if(cwe.ReturnIsDirectAccessObject())
				{
					if(declareVariable != null)
					{
						listEnv.add(declareVariable);
					}
					
					String flag = "true";
					
					String update = "LazyInit(programState, lazyList, "
					
					update = update + "\"" + object.getText() + "\", " + "\""  + "\", " + "\"" + tempName  + "\", " +  "\"" + flag + "\"" +");\n";
					
					expression = expression + "\n" + update + "\n";
				}
			}
			
		}
		else if (exp instanceof ConstructorCallExpression) {

			ProcessInstance instance = new ProcessInstance(exp, listForDef, listForInput, listEnv, mapForDefined, declareVariable, listForLazy, runtimeDataBase)
		
			String instanceHead = instance.getHead();
			String instanceBody = instance.getBody();
			String instanceTail = instance.getTail();
			
			head = head + instanceHead + "\n";
			expression = expression + instanceBody + "\n";
			tail = tail + instanceTail + "\n";
		}
		else if (exp instanceof VariableExpression) {
			

			String name = exp.getText()
			
			//if is Env variable we need to add the declare variable into the envList
			if(listEnv.contains(name)) {
				if(declareVariable != null)
				{
					listEnv.add(declareVariable);
				}
			}

			
			expression = expression + name
		}
		else if(exp instanceof NotExpression)
		{

			String str=exp.getText()

			
			expression = expression + " ! " + str
		}
		else if(exp instanceof ClosureExpression)
		{
			// Closure is not decided right now
		}
		else if(exp instanceof ClassExpression)
		{
			expression = expression + exp.getText();
		}
		else if(exp instanceof GStringExpression)
		{
			expression = expression + "\"" + exp.getText() + "\"";
		}
		else {
			println ("exp: pay attention other expressions!!! : " + exp.toString())
			String str=exp.getText()
				
			expression = expression + str
		}
	}
	
	
	public String getHead()
	{
		return head;
	}
	
	public String getExpression()
	{
		return expression;
	}
	
	public String getTail()
	{
		return tail;
	}
	
}
