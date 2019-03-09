/*
 *                             Sun Public License
 *
 * The contents of this file are subject to the Sun Public License Version
 * 1.0 (the "License").  You may not use this file except in compliance with
 * the License.  A copy of the License is available at http://www.sun.com/
 *
 * The Original Code is the SLAMD Distributed Load Generation Engine.
 * The Initial Developer of the Original Code is Neil A. Wilson.
 * Portions created by Neil A. Wilson are Copyright (C) 2004-2010.
 * Some preexisting portions Copyright (C) 2002-2006 Sun Microsystems, Inc.
 * All Rights Reserved.
 *
 * Contributor(s):  Neil A. Wilson
 */
package com.slamd.scripting.engine;



import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import com.slamd.common.Constants;
import com.slamd.job.JobClass;
import com.slamd.stat.StatTracker;
import com.slamd.scripting.general.BooleanLiteral;
import com.slamd.scripting.general.BooleanVariable;
import com.slamd.scripting.general.CategoricalTrackerVariable;
import com.slamd.scripting.general.FileURLVariable;
import com.slamd.scripting.general.IncrementalTrackerVariable;
import com.slamd.scripting.general.IntegerLiteral;
import com.slamd.scripting.general.IntegerValueTrackerVariable;
import com.slamd.scripting.general.IntegerVariable;
import com.slamd.scripting.general.RateLimiterVariable;
import com.slamd.scripting.general.ScriptVariable;
import com.slamd.scripting.general.StringArrayVariable;
import com.slamd.scripting.general.StringLiteral;
import com.slamd.scripting.general.StringVariable;
import com.slamd.scripting.general.TimeTrackerVariable;
import com.slamd.scripting.general.ValuePatternVariable;



/**
 * This class implements a mechanism for parsing and executing script files that
 * specify instructions for performing operations in a SLAMD job.  The set of
 * reserved words understood by this parser is hard-coded, but the set of
 * data types that may be used is flexible and may be extended by the end user.
 *
 *
 * @author   Neil A. Wilson
 */
public class ScriptParser
{
  /**
   * The set of variable types that will be automatically included and available
   * to scripts without the need for an explicit "use" definition.
   */
  public static final String[] AUTO_INCLUDED_VARIABLE_TYPES = new String[]
  {
    ScriptVariable.class.getName(),
    ScriptVariable.class.getName(),
    BooleanVariable.class.getName(),
    CategoricalTrackerVariable.class.getName(),
    FileURLVariable.class.getName(),
    IncrementalTrackerVariable.class.getName(),
    IntegerValueTrackerVariable.class.getName(),
    IntegerVariable.class.getName(),
    RateLimiterVariable.class.getName(),
    StringVariable.class.getName(),
    StringArrayVariable.class.getName(),
    TimeTrackerVariable.class.getName(),
    ValuePatternVariable.class.getName()
  };



  /**
   * The Java class name that is the superclass of all variable types that may
   * be used in SLAMD scripts.
   */
  public static final String VARIABLE_TYPE_SUPERCLASS_NAME =
       "com.slamd.scripting.engine.Variable";



  /**
   * The reserved word that is used to define a new variable type for use in the
   * SLAMD script.
   */
  public static final String RESERVED_WORD_USE = "use";



  /**
   * The reserved word that is used to define a new variable instance for use
   * in the SLAMD script.
   */
  public static final String RESERVED_WORD_VARIABLE = "variable";



  /**
   * The reserved word that is used to indicate the beginning of an instruction
   * block.
   */
  public static final String RESERVED_WORD_BEGIN = "begin";



  /**
   * The reserved word that is used to indicate the end of an instruction block.
   */
  public static final String RESERVED_WORD_END = "end";



  /**
   * The reserved word that is used to execute an instruction if the associated
   * boolean value is true.
   */
  public static final String RESERVED_WORD_IF = "if";



  /**
   * The reserved word that is used to execute an instruction if the associated
   * boolean value is false.
   */
  public static final String RESERVED_WORD_IF_NOT = "ifnot";



  /**
   * The reserved word that is used to execute an instruction if the boolean
   * value associated with the immediately preceding if statement is false.
   */
  public static final String RESERVED_WORD_ELSE = "else";



  /**
   * The reserved word that is used to execute an instruction a specified number
   * of times.
   */
  public static final String RESERVED_WORD_LOOP = "loop";



  /**
   * The reserved word that is used to execute an instruction until a specified
   * condition becomes false.
   */
  public static final String RESERVED_WORD_WHILE = "while";



  /**
   * The reserved word that is used to execute an instruction until a specified
   * condition becomes true.
   */
  public static final String RESERVED_WORD_WHILE_NOT = "whilenot";



  /**
   * The reserved word that is used to indicate that execution of the current
   * loop should stop immediately and that execution should resume with the next
   * instruction immediately after the loop.
   */
  public static final String RESERVED_WORD_BREAK = "break";



  /**
   * The reserved word that is used to indicate that the execution of the
   * current loop iteration should stop immediately and that the next iteration
   * should begin if appropriate.
   */
  public static final String RESERVED_WORD_CONTINUE = "continue";



  // The map of script arguments.
  private HashMap<String,String> scriptArgumentHash;

  // The line number from which the last token was read.
  private int tokenLine;

  // The character in the line at which the last token started.
  private int tokenStartPos;

  // The character in the line at which the last token ended.
  private int tokenEndPos;

  // The lines contained in the script file being parsed.
  private char[][] lines;



  // The set of instructions that have been read from the script file.
  private ArrayList<Instruction> instructionList;

  // The superclass of all variable type definitions.
  private Class<?> variableTypeSuperclass;

  // The set of variables defined and used in the script.
  private HashMap<String,Variable> variableHash;

  // The correlation between variable type names and the Java class that
  // implements them.
  private HashMap<String,String> variableTypeHash;



  /**
   * Creates a new script parser that can be used to parse and execute a SLAMD
   * script.
   *
   * @throws  ScriptException  If there is a problem initializing the script
   *                           parser or loading the automatically included
   *                           variable types.
   */
  public ScriptParser()
         throws ScriptException
  {
    variableHash     = new HashMap<String,Variable>();
    variableTypeHash = new HashMap<String,String>();
    instructionList  = new ArrayList<Instruction>();

    tokenLine     = 0;
    tokenStartPos = -1;
    tokenEndPos   = -1;

    // Load the variable type superclass.
    try
    {
      variableTypeSuperclass =
           Constants.classForName(VARIABLE_TYPE_SUPERCLASS_NAME);
    }
    catch (Exception e)
    {
      throw new ScriptException("Could not find the variable type superclass " +
                                VARIABLE_TYPE_SUPERCLASS_NAME, e);
    }

    // Load all the automatically-included variable type definitions.
    for (int i=0; i < AUTO_INCLUDED_VARIABLE_TYPES.length; i++)
    {
      registerVariableType(AUTO_INCLUDED_VARIABLE_TYPES[i]);
    }

    // Create a new script variable and add it to the variable hash.
    ScriptVariable scriptVariable = new ScriptVariable();
    scriptVariable.setName("script");
    variableHash.put(scriptVariable.getName(), scriptVariable);
  }



  /**
   * Registers a variable type definition for use in the SLAMD script.
   *
   * @param  className  The name of the Java class that provides the variable
   *                    type definition.
   *
   * @throws  ScriptException  If the specified class cannot be found, cannot
   *                           be instantiated, does not define a variable type,
   *                           or defines a variable type that is already in
   *                           use.
   */
  public void registerVariableType(String className)
         throws ScriptException
  {
    Class<?> variableTypeClass;
    try
    {
      variableTypeClass = Constants.classForName(className);
    }
    catch (ClassNotFoundException cnfe)
    {
      if (tokenStartPos < 0)
      {
        throw new ScriptException("Could not find a Java class named " +
                                  className, cnfe);
      }
      else
      {
        throw new ScriptException(tokenLine, tokenStartPos,
                                  "Could not find a Java class named " +
                                  className, cnfe);
      }
    }

    // Make sure that the Java class is a variable type definition
    if (! variableTypeSuperclass.isAssignableFrom(variableTypeClass))
    {
      if (tokenStartPos < 0)
      {
        throw new ScriptException("Java class " + className +
                                  " does not provide a valid variable type " +
                                  "definition.");
      }
      else
      {
        throw new ScriptException(tokenLine, tokenStartPos,
                                  "Java class " + className +
                                  " does not provide a valid variable type " +
                                  "definition.");
      }
    }

    // Instantiate the class and get the type name from it to add to the
    // variable type hash.
    try
    {
      Variable variableType = (Variable) variableTypeClass.newInstance();

      // See if there is already a variable type defined with the same
      // variable type name
      String variableTypeName =
           variableType.getVariableTypeName().toLowerCase();
      if (variableTypeHash.get(variableTypeName) != null)
      {
        String variableClassName = variableTypeHash.get(variableTypeName);
        if (! variableClassName.equals(className))
        {
          if (tokenStartPos < 0)
          {
            throw new ScriptException("Variable type " + variableTypeName +
                                      " is already defined by class " +
                                      variableClassName);
          }
          else
          {
            throw new ScriptException(tokenLine, tokenStartPos,
                                      "Variable type " + variableTypeName +
                                      " is already defined by class " +
                                      variableClassName);
          }
        }
      }
      else
      {
        variableTypeHash.put(variableTypeName, className);
      }
    }
    catch (Exception e)
    {
      if (tokenStartPos < 0)
      {
        throw new ScriptException("Could not create an instance of Java " +
                                  "class " + className + ":  " + e, e);
      }
      else
      {
        throw new ScriptException(tokenLine, tokenStartPos,
                                  "Could not create an instance of Java " +
                                  "class " + className + ":  " + e, e);
      }
    }
  }



  /**
   * Reads the specified script file into memory.
   *
   * @param  filename  The path and name of the script file to be read.
   *
   * @throws  IOException  If there is a problem reading from the script file.
   */
  public void read(String filename)
         throws IOException
  {
    FileInputStream inputStream = new FileInputStream(filename);
    read(inputStream);
    inputStream.close();
  }



  /**
   * Reads the script file data from the specified input stream into memory.
   *
   * @param  inputStream  The input stream from which the script file data will
   *                      be read.
   *
   * @throws  IOException  If there is a problem reading the script file data.
   */
  public void read(InputStream inputStream)
         throws IOException
  {
    ArrayList<String> lineList = new ArrayList<String>();

    BufferedReader reader =
         new BufferedReader(new InputStreamReader(inputStream));

    String line = reader.readLine();
    while (line != null)
    {
      lineList.add(line);
      line = reader.readLine();
    }
    reader.close();

    lines = new char[lineList.size()][];
    for (int i=0; i < lines.length; i++)
    {
      lines[i] = lineList.get(i).toCharArray();
    }
  }



  /**
   * Specifies the lines to use for the script.
   *
   * @param  lines  The lines to use for the script.
   */
  public void setScriptLines(String[] lines)
  {
    this.lines = new char[lines.length][];
    for (int i=0; i < lines.length; i++)
    {
      this.lines[i] = lines[i].toCharArray();
    }
  }



  /**
   * Specifies the script arguments to use with this script.
   *
   * @param  scriptArgumentHash  The set of arguments to use in conjunction with
   *                             this script.
   */
  public void setScriptArguments(HashMap<String,String> scriptArgumentHash)
  {
    this.scriptArgumentHash = scriptArgumentHash;
  }



  /**
   * Retrieves the value of the specified script argument.
   *
   * @param  argumentName  The name of the script argument to retrieve.
   *
   * @return  The value of the requested script argument, or <CODE>null</CODE>
   *          if it was not defined.
   */
  public String getScriptArgument(String argumentName)
  {
    return scriptArgumentHash.get(argumentName);
  }



  /**
   * Parses the script information and tokenizes the instructions so that they
   * may be more easily and efficiently executed.
   *
   * @throws  ScriptException  If the script cannot be parsed as a valid SLAMD
   *                           script (i.e., if the script contains one or more
   *                           syntax errors).
   */
  public void parse()
         throws ScriptException
  {
    // Initialize the tokenizer.
    tokenLine     = 0;
    tokenStartPos = 0;
    tokenEndPos   = -1;


    // First, get any variable type definitions out of the way.
    String token = peekAtNextToken();
    if (token == null)
    {
      return;
    }
    while (token.equals(RESERVED_WORD_USE))
    {
      nextToken();
      handleUseStatement();
      token = peekAtNextToken();
      if (token == null)
      {
        return;
      }
    }


    // Next, take care of the variable declarations.
    while (token.equals(RESERVED_WORD_VARIABLE))
    {
      nextToken();
      handleVariableDeclaration();
      token = peekAtNextToken();
      if (token == null)
      {
        return;
      }
    }


    // The remainder of the file should be all instructions.
    Instruction i = nextInstruction();
    while (i != null)
    {
      instructionList.add(i);
      i = nextInstruction();
    }
  }



  /**
   * Parses the next instruction from the script.
   *
   * @return  The next instruction that has been parsed from the script.
   *
   * @throws  ScriptException  If the next instruction could not be parsed
   *                           because of a syntax error.
   */
  private Instruction nextInstruction()
          throws ScriptException
  {
    String token = nextToken();

    // If there are no more tokens, then we are at the end of the script.
    if (token == null)
    {
      return null;
    }

    if (token.equals(RESERVED_WORD_USE))
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Variable type definitions may only occur at " +
                                "the beginning of the script.");
    }
    else if (token.equals(RESERVED_WORD_VARIABLE))
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Variable type definitions may only occur at " +
                                "the beginning of the script.");
    }
    else if (token.equals(RESERVED_WORD_IF))
    {
      return parseIfInstruction();
    }
    else if (token.equals(RESERVED_WORD_IF_NOT))
    {
      return parseIfNotInstruction();
    }
    else if (token.equals(RESERVED_WORD_LOOP))
    {
      return parseLoopInstruction();
    }
    else if (token.equals(RESERVED_WORD_WHILE))
    {
      return parseWhileInstruction();
    }
    else if (token.equals(RESERVED_WORD_WHILE_NOT))
    {
      return parseWhileNotInstruction();
    }
    else if (token.equals(RESERVED_WORD_BREAK))
    {
      String nextToken = nextToken();
      if (! nextToken.equals(";"))
      {
        throw new ScriptException(tokenLine, tokenStartPos,
                                  "A break instruction must be followed by a " +
                                  "semicolon.");
      }

      return new BreakInstruction(tokenLine);
    }
    else if (token.equals(RESERVED_WORD_CONTINUE))
    {
      String nextToken = nextToken();
      if (! nextToken.equals(";"))
      {
        throw new ScriptException(tokenLine, tokenStartPos,
                                  "A continue instruction must be followed " +
                                  "by a semicolon.");
      }

      return new ContinueInstruction(tokenLine);
    }
    else
    {
      // This could either be an assignment instruction or a method call
      // instruction.  First, verify that the token provided is a valid variable
      // and then look at the next token to see if it is a "=" (assignment
      // instruction) or a "." (method call instruction).
      Variable v = variableHash.get(token);
      if (v == null)
      {
        throw new ScriptException(tokenLine, tokenStartPos,
                                  '"' + token +
                                  "\" is not a defined variable name.");
      }

      String token2 = nextToken();
      if (token2.equals("="))
      {
        return parseAssignmentInstruction(v);
      }
      else if (token2.equals("."))
      {
        return parseMethodCallInstruction(v);
      }
      else
      {
        throw new ScriptException(tokenLine, tokenStartPos,
                                  "Equal sign or period expected after " +
                                  "variable name " + token + '.');
      }
    }
  }



  /**
   * Parses a line starting with the "use" reserved word and registers the
   * specified variable type.
   *
   * @throws  ScriptException  If there is a syntax error on the line, or if
   *                           there is a problem registering the variable type.
   */
  private void handleUseStatement()
          throws ScriptException
  {
    // The reserved word "use" must be immediately followed by a Java class
    // name.
    String className = nextClassNameToken();
    if (className == null)
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "The use reserved word must be followed " +
                                "by a fully-qualified Java class name");
    }

    // Make sure that the Java class exists
    Class<?> variableTypeClass;
    try
    {
      variableTypeClass = Constants.classForName(className);
    }
    catch (ClassNotFoundException cnfe)
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Could not find a Java class named " +
                                className, cnfe);
    }

    // Make sure that the Java class is a variable type definition
    if (! variableTypeSuperclass.isAssignableFrom(variableTypeClass))
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Java class " + className +
                                " does not provide a valid variable type " +
                                "definition.");
    }

    // Instantiate the class and get the type name from it to add to the
    // variable type hash.
    try
    {
      Variable variableType = (Variable) variableTypeClass.newInstance();

      // See if there is already a variable type defined with the same
      // variable type name
      String variableTypeName =
           variableType.getVariableTypeName().toLowerCase();
      if (variableTypeHash.get(variableTypeName) != null)
      {
        String existingName = variableTypeHash.get(variableTypeName);
        if (! existingName.equals(className))
        {
          throw new ScriptException(tokenLine, tokenStartPos,
                                    "Variable type " + variableTypeName +
                                    " is already defined by class " +
                                    existingName);
        }
      }
      else
      {
        variableTypeHash.put(variableTypeName, className);
      }
    }
    catch (Exception e)
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Could not create an instance of Java " +
                                "class " + className + ":  " + e, e);
    }

    // The next token must be a semicolon to end the variable type
    // definition.
    String token = nextToken();
    if ((token == null) || (! token.equals(";")))
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Semicolon expected after variable type " +
                                "definition.");
    }
  }



  /**
   * Parses a line starting with the "variable" reserved word and defines the
   * specified variable.
   *
   * @throws  ScriptException  If there is a syntax error on the line, if the
   *                           variable type is not known, or if there is
   *                           already a variable defined with the specified
   *                           name.
   */
  private void handleVariableDeclaration()
          throws ScriptException
  {
    // The next token must be the variable type.
    String variableType = nextToken();
    if (variableType == null)
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Variable type expected.");
    }

    // Make sure that the variable type has been defined.
    String className = variableTypeHash.get(variableType);
    if (className == null)
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Undefined variable type " + variableType);
    }

    // The next token must be the variable name.
    String variableName = nextToken();
    if (variableName == null)
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Variable name expected.");
    }

    // Make sure the variable name is a valid identifier.
    if (! Variable.isValidIdentifier(variableName))
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Is not a valid identifier for a variable " +
                                "name.");
    }

    // Make sure the variable name is not already in use.
    if (variableHash.get(variableName) != null)
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "There is already a variable defined " +
                                "with the name of " + variableName);
    }

    // The variable declaration must end with a semicolon
    String token = nextToken();
    if ((token == null) || (! token.equals(";")))
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "A variable declaration must end with a " +
                                "semicolon");
    }

    // Create the new variable and add it into the variable hash
    try
    {
      Class<?> variableClass = Constants.classForName(className);
      Variable v = (Variable) variableClass.newInstance();
      v.setName(variableName);
      variableHash.put(variableName, v);
    }
    catch (Exception e)
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Unable to create variable " +
                                variableName + ":  " + e, e);
    }
  }



  /**
   * Parses an argument from the script.  An argument may be an argument to a
   * method, a condition in an if or while statement, or the number of
   * iterations to perform in a loop statement.
   *
   * @return  The argument that was parsed from the script.
   *
   * @throws  ScriptException  If the argument could not be parsed for some
   *                           reason.
   */
  private Argument parseArgument()
          throws ScriptException
  {
    // Read the first token and look at the first character of that token.
    String token = nextToken();
    if ((token == null) || (token.length() == 0))
    {
      return null;
    }
    char c = token.charAt(0);


    // Look at the first character of the token and determine what kind of token
    // If the token starts with a quotation mark, then it's a string literal.
    if (c == '"')
    {
      // Strip the opening and closing quotes.
      return new StringLiteral(token.substring(1, token.length() - 1));
    }


    // If the token starts with a dash or a digit, then it's an integer literal.
    else if ((c == '-') || ((c >= '0') && (c <= '9')))
    {
      try
      {
        return new IntegerLiteral(Integer.parseInt(token));
      }
      catch (NumberFormatException nfe)
      {
        throw new ScriptException(tokenLine, tokenStartPos,
                                  "Unable to parse \"" + token +
                                  "\" as an integer literal.", nfe);
      }
    }


    // If the token is "true" or "false", then it's a Boolean literal
    else if (token.equals(BooleanLiteral.BOOLEAN_TRUE_VALUE))
    {
      return new BooleanLiteral(true);
    }
    else if (token.equals(BooleanLiteral.BOOLEAN_FALSE_VALUE))
    {
      return new BooleanLiteral(false);
    }


    // Otherwise, the token must be the name of a variable.
    else
    {
      // Make sure it is a valid variable name.
      int lineNumber = tokenLine;
      Variable v = variableHash.get(token);
      if (v == null)
      {
        throw new ScriptException(tokenLine, tokenStartPos,
                                  "Unrecognized argument " + token);
      }

      // Look at the next token.  If it is a period, then this is actually a
      // method call argument.  Otherwise, the argument is just the variable.
      token = peekAtNextToken();
      if ((token != null) && token.equals("."))
      {
        // It is a method call, so figure out what the method name is.
        nextToken();
        String methodName = nextToken();
        if (token == null)
        {
          throw new ScriptException(tokenLine, tokenStartPos,
                                    "Expected a method name after reference " +
                                    "to variable " + v.getName());
        }


        // The next token must be an opening parenthesis.
        token = nextToken();
        if ((token == null) || (! token.equals("(")))
        {
          throw new ScriptException(tokenLine, tokenStartPos,
                                    "Expected an opening parenthesis in " +
                                    "call to method " + v.getName() + '.' +
                                    methodName);
        }

        // Parse the argument list.  The next token must be either an argument
        // or a closing parenthesis.
        ArrayList<Argument> argumentList = new ArrayList<Argument>();
        token = peekAtNextToken();
        if (token == null)
        {
          throw new ScriptException(tokenLine, tokenStartPos,
                                    "Unterminated argument list detected in " +
                                    "call to " + v.getName() + '.' +
                                    methodName + "()");
        }

        if (! token.equals(")"))
        {
          while (! token.equals(")"))
          {
            argumentList.add(parseArgument());

            // The next token must be a comma or a close parenthesis.
            token = nextToken();
            if ((token == null) || (! (token.equals(",") || token.equals(")"))))
            {
              throw new ScriptException(tokenLine, tokenStartPos,
                                        "Comma or closing parenthesis " +
                                        "expected in argument list for " +
                                        "method call " + v.getName() + '.' +
                                        methodName);
            }
          }
        }
        else
        {
          nextToken();
        }

        // Create an array of the arguments
        Argument[] arguments = new Argument[argumentList.size()];
        argumentList.toArray(arguments);

        // Create an array of the argument types.
        String[] argumentTypes = new String[argumentList.size()];
        for (int i=0; i < argumentTypes.length; i++)
        {
          argumentTypes[i] = argumentList.get(i).getArgumentType();
        }

        // If the variable does not have a method with the specified signature,
        // then return an error.
        int methodNumber = v.getMethodNumber(methodName, argumentTypes);
        if (methodNumber < 0)
        {
          String message = "Variable " + v.getName() +
                           " does not have a method " + methodName + '(';
          String separator = "";
          for (int i=0; i < argumentTypes.length; i++)
          {
            message += separator + argumentTypes[i];
            separator = ",";
          }
          message += ")";

          throw new ScriptException(tokenLine, tokenStartPos, message);
        }

        // It is a valid method call, so return the method call argument type.
        return new MethodCallInstruction(lineNumber, v, methodName,
                                         methodNumber, arguments);
      }
      else
      {
        return v;
      }
    }
  }



  /**
   * Parses an "if" instruction, including the optional else clause if it is
   * present.
   *
   * @return  The if instruction parsed from the script.
   *
   * @throws  ScriptException  If any problem occurs while parsing the if
   *          instruction.
   */
  private Instruction parseIfInstruction()
          throws ScriptException
  {
    // The line number on which this if instruction starts.
    int ifLineNumber = tokenLine;

    // The first thing that comes in an if instruction is the argument.
    Argument condition = parseArgument();

    // The argument that we have parsed must be of a Boolean type.
    if ((condition == null) ||
        (! condition.getArgumentType().equals(
                BooleanVariable.BOOLEAN_VARIABLE_TYPE)))
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "An if instruction must be followed by an " +
                                "argument that has a Boolean value.");
    }

    // Next, we need to determine if there is a single instruction to execute
    // or a set of instructions.
    Instruction ifInstruction;
    String token = peekAtNextToken();
    if (token == null)
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Instruction expected in if statement.");
    }
    else if (token.equals(RESERVED_WORD_BEGIN))
    {
      // There is a set of instructions.  Read past the begin, and then keep
      // going until we reach the end.
      nextToken();
      int beginLineNumber = tokenLine;

      token = peekAtNextToken();
      if (token == null)
      {
        throw new ScriptException(beginLineNumber,
                                  "Unterminated begin block detected.");
      }

      ArrayList<Instruction> ifInstructionList = new ArrayList<Instruction>();
      while (! token.equals(RESERVED_WORD_END))
      {
        ifInstructionList.add(nextInstruction());
        token = peekAtNextToken();
        if (token == null)
        {
          throw new ScriptException(beginLineNumber,
                                    "Unterminated begin block detected.");
        }
      }

      // If we've gotten here, then we found the end.  Read it, and convert the
      // set of instructions that have been read into an instruction block.
      nextToken();
      Instruction[] instructions = new Instruction[ifInstructionList.size()];
      ifInstructionList.toArray(instructions);
      ifInstruction = new InstructionBlock(beginLineNumber, instructions);

      // There must be a semicolon after the end.  Make sure it's there.
      token = nextToken();
      if ((token == null) || (! token.equals(";")))
      {
        throw new ScriptException(tokenLine, tokenStartPos,
                                  "Semicolon required after end.");
      }
    }
    else
    {
      // There should only be a single instruction, so read it.
      ifInstruction = nextInstruction();
    }


    // Now check to see if there is an else clause.
    Instruction elseInstruction = null;
    token = peekAtNextToken();
    if (token.equals(RESERVED_WORD_ELSE))
    {
      // There is an else clause.  See if it is a single instruction or if it
      // is a set of instructions.
      nextToken();

      token = peekAtNextToken();
      if (token == null)
      {
        throw new ScriptException(tokenLine, tokenStartPos,
                                  "Instruction expected in else statement.");
      }
      else if (token.equals(RESERVED_WORD_BEGIN))
      {
        nextToken();
        int beginLineNumber = tokenLine;

        token = peekAtNextToken();
        if (token == null)
        {
          throw new ScriptException(beginLineNumber,
                                    "Unterminated begin block detected.");
        }

        ArrayList<Instruction> elseInstructionList =
             new ArrayList<Instruction>();
        while (! token.equals(RESERVED_WORD_END))
        {
          elseInstructionList.add(nextInstruction());
          token = peekAtNextToken();
          if (token == null)
          {
            throw new ScriptException(beginLineNumber,
                                      "Unterminated begin block detected.");
          }
        }

        nextToken();
        Instruction[] instructions =
             new Instruction[elseInstructionList.size()];
        elseInstructionList.toArray(instructions);
        elseInstruction = new InstructionBlock(beginLineNumber, instructions);

        // There must be a semicolon after the end.  Make sure it's there.
        token = nextToken();
        if ((token == null) || (! token.equals(";")))
        {
          throw new ScriptException(tokenLine, tokenStartPos,
                                    "Semicolon required after end.");
        }
      }
      else
      {
        elseInstruction = nextInstruction();
      }
    }


    return new IfElseInstruction(ifLineNumber, condition, ifInstruction,
                                 elseInstruction);
  }



  /**
   * Parses an "ifnot" instruction, including the optional else clause if it is
   * present.
   *
   * @return  The ifnot instruction parsed from the script.
   *
   * @throws  ScriptException  If any problem occurs while parsing the ifnot
   *          instruction.
   */
  private Instruction parseIfNotInstruction()
          throws ScriptException
  {
    // The line number on which this ifnot instruction starts.
    int ifNotLineNumber = tokenLine;

    // The first thing that comes in an ifnot instruction is the argument.
    Argument condition = parseArgument();

    // The argument that we have parsed must be of a Boolean type.
    if ((condition == null) ||
        (! condition.getArgumentType().equals(
                BooleanVariable.BOOLEAN_VARIABLE_TYPE)))
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "An ifnot instruction must be followed by an " +
                                "argument that has a Boolean value.");
    }

    // Next, we need to determine if there is a single instruction to execute
    // or a set of instructions.
    Instruction ifNotInstruction;
    String token = peekAtNextToken();
    if (token == null)
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Instruction expected in ifnot statement.");
    }
    else if (token.equals(RESERVED_WORD_BEGIN))
    {
      // There is a set of instructions.  Read past the begin, and then keep
      // going until we reach the end.
      nextToken();
      int beginLineNumber = tokenLine;

      token = peekAtNextToken();
      if (token == null)
      {
        throw new ScriptException(beginLineNumber,
                                  "Unterminated begin block detected.");
      }

      ArrayList<Instruction> ifNotInstructionList =
           new ArrayList<Instruction>();
      while (! token.equals(RESERVED_WORD_END))
      {
        ifNotInstructionList.add(nextInstruction());
        token = peekAtNextToken();
        if (token == null)
        {
          throw new ScriptException(beginLineNumber,
                                    "Unterminated begin block detected.");
        }
      }

      // If we've gotten here, then we found the end.  Read it, and convert the
      // set of instructions that have been read into an instruction block.
      nextToken();
      Instruction[] instructions = new Instruction[ifNotInstructionList.size()];
      ifNotInstructionList.toArray(instructions);
      ifNotInstruction = new InstructionBlock(beginLineNumber, instructions);

      // There must be a semicolon after the end.  Make sure it's there.
      token = nextToken();
      if ((token == null) || (! token.equals(";")))
      {
        throw new ScriptException(tokenLine, tokenStartPos,
                                  "Semicolon required after end.");
      }
    }
    else
    {
      // There should only be a single instruction, so read it.
      ifNotInstruction = nextInstruction();
    }


    // Now check to see if there is an else clause.
    Instruction elseInstruction = null;
    token = peekAtNextToken();
    if (token.equals(RESERVED_WORD_ELSE))
    {
      // There is an else clause.  See if it is a single instruction or if it
      // is a set of instructions.
      nextToken();

      token = peekAtNextToken();
      if (token == null)
      {
        throw new ScriptException(tokenLine, tokenStartPos,
                                  "Instruction expected in else statement.");
      }
      else if (token.equals(RESERVED_WORD_BEGIN))
      {
        nextToken();
        int beginLineNumber = tokenLine;

        token = peekAtNextToken();
        if (token == null)
        {
          throw new ScriptException(beginLineNumber,
                                    "Unterminated begin block detected.");
        }

        ArrayList<Instruction> elseInstructionList =
             new ArrayList<Instruction>();
        while (! token.equals(RESERVED_WORD_END))
        {
          elseInstructionList.add(nextInstruction());
          token = peekAtNextToken();
          if (token == null)
          {
            throw new ScriptException(beginLineNumber,
                                      "Unterminated begin block detected.");
          }
        }

        nextToken();
        Instruction[] instructions =
             new Instruction[elseInstructionList.size()];
        elseInstructionList.toArray(instructions);
        elseInstruction = new InstructionBlock(beginLineNumber, instructions);

        // There must be a semicolon after the end.  Make sure it's there.
        token = nextToken();
        if ((token == null) || (! token.equals(";")))
        {
          throw new ScriptException(tokenLine, tokenStartPos,
                                    "Semicolon required after end.");
        }
      }
      else
      {
        elseInstruction = nextInstruction();
      }
    }


    return new IfNotInstruction(ifNotLineNumber, condition, ifNotInstruction,
                                elseInstruction);
  }



  /**
   * Parses a "loop" instruction from the script.
   *
   * @return  The loop instruction parsed from the script.
   *
   * @throws  ScriptException  If any problem occurs while parsing the loop
   *          instruction.
   */
  private Instruction parseLoopInstruction()
          throws ScriptException
  {
    // The line number on which this loop instruction starts.
    int loopLineNumber = tokenLine;

    // The first thing that comes in a loop instruction is the argument.
    Argument iterations = parseArgument();

    // The argument that we have parsed must be of an integer type.
    if ((iterations == null) ||
        (! iterations.getArgumentType().equals(
                IntegerVariable.INTEGER_VARIABLE_TYPE)))
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "A loop instruction must be followed by an " +
                                "argument that has an integer value.");
    }

    // Next, we need to determine if there is a single instruction to execute
    // or a set of instructions.
    Instruction loopInstruction;
    String token = peekAtNextToken();
    if (token == null)
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Instruction expected in loop statement.");
    }
    else if (token.equals(RESERVED_WORD_BEGIN))
    {
      // There is a set of instructions.  Read past the begin, and then keep
      // going until we reach the end.
      nextToken();
      int beginLineNumber = tokenLine;

      token = peekAtNextToken();
      if (token == null)
      {
        throw new ScriptException(beginLineNumber,
                                  "Unterminated begin block detected.");
      }

      ArrayList<Instruction> loopInstructionList = new ArrayList<Instruction>();
      while (! token.equals(RESERVED_WORD_END))
      {
        loopInstructionList.add(nextInstruction());
        token = peekAtNextToken();
        if (token == null)
        {
          throw new ScriptException(beginLineNumber,
                                    "Unterminated begin block detected.");
        }
      }

      // If we've gotten here, then we found the end.  Read it, and convert the
      // set of instructions that have been read into an instruction block.
      nextToken();
      Instruction[] instructions =
           new Instruction[loopInstructionList.size()];
      loopInstructionList.toArray(instructions);
      loopInstruction = new InstructionBlock(beginLineNumber, instructions);

      // There must be a semicolon after the end.  Make sure it's there.
      token = nextToken();
      if ((token == null) || (! token.equals(";")))
      {
        throw new ScriptException(tokenLine, tokenStartPos,
                                  "Semicolon required after end.");
      }
    }
    else
    {
      // There should only be a single instruction, so read it.
      loopInstruction = nextInstruction();
    }


    return new LoopInstruction(loopLineNumber, iterations, loopInstruction);
  }



  /**
   * Parses a "while" instruction from the script.
   *
   * @return  The while instruction parsed from the script.
   *
   * @throws  ScriptException  If any problem occurs while parsing the while
   *          instruction.
   */
  private Instruction parseWhileInstruction()
          throws ScriptException
  {
    // The line number on which this while instruction starts.
    int whileLineNumber = tokenLine;

    // The first thing that comes in a while instruction is the argument.
    Argument condition = parseArgument();

    // The argument that we have parsed must be of a Boolean type.
    if ((condition == null) ||
        (! condition.getArgumentType().equals(
                BooleanVariable.BOOLEAN_VARIABLE_TYPE)))
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "A while instruction must be followed by an " +
                                "argument that has a Boolean value.");
    }

    // Next, we need to determine if there is a single instruction to execute
    // or a set of instructions.
    Instruction whileInstruction;
    String token = peekAtNextToken();
    if (token == null)
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Instruction expected in loop statement.");
    }
    else if (token.equals(RESERVED_WORD_BEGIN))
    {
      // There is a set of instructions.  Read past the begin, and then keep
      // going until we reach the end.
      nextToken();
      int beginLineNumber = tokenLine;

      token = peekAtNextToken();
      if (token == null)
      {
        throw new ScriptException(beginLineNumber,
                                  "Unterminated begin block detected.");
      }

      ArrayList<Instruction> whileInstructionList =
           new ArrayList<Instruction>();
      while (! token.equals(RESERVED_WORD_END))
      {
        whileInstructionList.add(nextInstruction());
        token = peekAtNextToken();
        if (token == null)
        {
          throw new ScriptException(beginLineNumber,
                                    "Unterminated begin block detected.");
        }

      }

      // If we've gotten here, then we found the end.  Read it, and convert the
      // set of instructions that have been read into an instruction block.
      nextToken();
      Instruction[] instructions =
           new Instruction[whileInstructionList.size()];
      whileInstructionList.toArray(instructions);
      whileInstruction = new InstructionBlock(beginLineNumber, instructions);

      // There must be a semicolon after the end.  Make sure it's there.
      token = nextToken();
      if ((token == null) || (! token.equals(";")))
      {
        throw new ScriptException(tokenLine, tokenStartPos,
                                  "Semicolon required after end.");
      }
    }
    else
    {
      // There should only be a single instruction, so read it.
      whileInstruction = nextInstruction();
    }


    return new WhileInstruction(whileLineNumber, condition, whileInstruction);
  }



  /**
   * Parses a "whilenot" instruction from the script.
   *
   * @return  The whilenot instruction parsed from the script.
   *
   * @throws  ScriptException  If any problem occurs while parsing the whilenot
   *          instruction.
   */
  private Instruction parseWhileNotInstruction()
          throws ScriptException
  {
    // The line number on which this while instruction starts.
    int whileLineNumber = tokenLine;

    // The first thing that comes in a while instruction is the argument.
    Argument condition = parseArgument();

    // The argument that we have parsed must be of a Boolean type.
    if ((condition == null) ||
        (! condition.getArgumentType().equals(
                BooleanVariable.BOOLEAN_VARIABLE_TYPE)))
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "A while instruction must be followed by an " +
                                "argument that has a Boolean value.");
    }

    // Next, we need to determine if there is a single instruction to execute
    // or a set of instructions.
    Instruction whileInstruction;
    String token = peekAtNextToken();
    if (token == null)
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Instruction expected in loop statement.");
    }
    else if (token.equals(RESERVED_WORD_BEGIN))
    {
      // There is a set of instructions.  Read past the begin, and then keep
      // going until we reach the end.
      nextToken();
      int beginLineNumber = tokenLine;

      token = peekAtNextToken();
      if (token == null)
      {
        throw new ScriptException(beginLineNumber,
                                  "Unterminated begin block detected.");
      }

      ArrayList<Instruction> whileInstructionList =
           new ArrayList<Instruction>();
      while (! token.equals(RESERVED_WORD_END))
      {
        whileInstructionList.add(nextInstruction());
        token = peekAtNextToken();
        if (token == null)
        {
          throw new ScriptException(beginLineNumber,
                                    "Unterminated begin block detected.");
        }

      }

      // If we've gotten here, then we found the end.  Read it, and convert the
      // set of instructions that have been read into an instruction block.
      nextToken();
      Instruction[] instructions =
           new Instruction[whileInstructionList.size()];
      whileInstructionList.toArray(instructions);
      whileInstruction = new InstructionBlock(beginLineNumber, instructions);

      // There must be a semicolon after the end.  Make sure it's there.
      token = nextToken();
      if ((token == null) || (! token.equals(";")))
      {
        throw new ScriptException(tokenLine, tokenStartPos,
                                  "Semicolon required after end.");
      }
    }
    else
    {
      // There should only be a single instruction, so read it.
      whileInstruction = nextInstruction();
    }


    return new WhileNotInstruction(whileLineNumber, condition,
                                   whileInstruction);
  }



  /**
   * Parses an assignment from the script.
   *
   * @param  variable  The variable that is being assigned.
   *
   * @return  The assignment instruction parsed from the script.
   *
   * @throws  ScriptException  If a problem occurred while parsing the
   *                           assignment.
   */
  private Instruction parseAssignmentInstruction(Variable variable)
          throws ScriptException
  {
    int lineNumber = tokenLine;
    Argument argument = parseArgument();

    // Make sure that the argument type is the same as the variable type.
    if (! argument.getArgumentType().equals(variable.getVariableTypeName()))
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Cannot assign an argument of type " +
                                argument.getArgumentType() +
                                " to a variable of type " +
                                variable.getVariableTypeName());
    }

    // There must be a semicolon after the argument.  Make sure it's there.
    String token = nextToken();
    if ((token == null) || (! token.equals(";")))
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Semicolon required after an assignment.");
    }

    return new AssignmentInstruction(lineNumber, variable, argument);
  }



  /**
   * Parses a method call from the script.
   *
   * @param  variable  The variable on which the action is to be taken.
   *
   * @return  The method call instruction parsed from the script.
   *
   * @throws  ScriptException  If a problem occurred while parsing the method
   *                           call.
   */
  private Instruction parseMethodCallInstruction(Variable variable)
          throws ScriptException
  {
    // Make sure that it is an actual variable name.
    int lineNumber = tokenLine;
    String variableName = variable.getName();

    // The next token must be the name of the method.
    String methodName = nextToken();
    if (methodName == null)
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Method name expected after reference to " +
                                "variable " + variableName);
    }

    // Make sure that it is a valid method for the specified variable.
    if (! variable.hasMethod(methodName))
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                '"' + methodName +
                                "\" is not a valid method name for " +
                                variableName + " variables.");
    }

    // The next token must be an opening parenthesis to start the argument list.
    String token = nextToken();
    if ((token == null) || (! token.equals("(")))
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Expected an opening parenthesis in " +
                                "call to method " + variableName + '.' +
                                methodName);
    }

    // Parse the argument list.  The next token must be either an argument
    // or a closing parenthesis.
    ArrayList<Argument> argumentList = new ArrayList<Argument>();
    token = peekAtNextToken();
    if (token == null)
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Unterminated argument list detected in " +
                                "call to " + variableName + '.' +
                                methodName + "()");
    }

    if (! token.equals(")"))
    {
      while (! token.equals(")"))
      {
        argumentList.add(parseArgument());

        // The next token must be a comma or a close parenthesis.
        token = nextToken();
        if ((token == null) || (! (token.equals(",") || token.equals(")"))))
        {
          throw new ScriptException(tokenLine, tokenStartPos,
                                    "Comma or closing parenthesis " +
                                    "expected in argument list for " +
                                    "method call " + variableName + '.' +
                                    methodName);
        }
      }
    }
    else
    {
      nextToken();
    }

    // Create an array of the arguments
    Argument[] arguments = new Argument[argumentList.size()];
    argumentList.toArray(arguments);

    // Create an array of the argument types.
    String[] argumentTypes = new String[argumentList.size()];
    for (int i=0; i < argumentTypes.length; i++)
    {
      argumentTypes[i] = argumentList.get(i).getArgumentType();
    }

    // If the variable does not have a method with the specified signature,
    // then return an error.
    int methodNumber = variable.getMethodNumber(methodName, argumentTypes);
    if (methodNumber < 0)
    {
      String message = "Variable " + variableName +
                       " does not have a method " + methodName + '(';
      String separator = "";
      for (int i=0; i < argumentTypes.length; i++)
      {
        message += separator + argumentTypes[i];
        separator = ",";
      }
      message += ")";

      throw new ScriptException(tokenLine, tokenStartPos, message);
    }

    // The variable instruction must end with a semicolon.
    token = nextToken();
    if ((token == null) || (! token.equals(";")))
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "Semicolon expected after call to " +
                                variableName + '.' + methodName);
    }

    // It is a valid method call, so return the method call argument type.
    return new MethodCallInstruction(lineNumber, variable, methodName,
                                     methodNumber, arguments);
  }



  /**
   * Retrieves the next token from the script but preserves the internal markers
   * used to keep track of the position in the script.  That is, it retrieves
   * the next token that will be returned by the call to
   * <CODE>nextToken()</CODE>.
   *
   * @return  The next token that will be returned by <CODE>nextToken()</CODE>.
   *
   * @throws  ScriptException  If there is a problem while parsing the next
   *                           token.
   */
  public String peekAtNextToken()
         throws ScriptException
  {
    int saveLine     = tokenLine;
    int saveStartPos = tokenStartPos;
    int saveEndPos   = tokenEndPos;

    String token = nextToken();

    this.tokenLine     = saveLine;
    this.tokenStartPos = saveStartPos;
    this.tokenEndPos   = saveEndPos;

    return token;
  }



  /**
   * Reads the next token from the script.  A token is classified as any single
   * element that is not a comment or whitespace, and will be one of the
   * following:
   *
   * <UL>
   *   <LI>An alphabetic character followed by a set of alphabetic characters,
   *       numeric digits, or underscore characters in any combination.  All
   *       alphabetic characters will be converted to lowercase.</LI>
   *   <LI>A set of numeric digits, optionally beginning with a dash</LI>
   *   <LI>A period</LI>
   *   <LI>A semicolon</LI>
   *   <LI>A comma</LI>
   *   <LI>An opening or closing parenthesis</LI>
   *   <LI>A quotation mark, followed by a sequence of any characters and
   *       ending with another quotation mark that is not preceded immediately
   *       by a backslash.  The closing quotation mark must occur on the same
   *       line as the opening quotation mark.  All capitalization will be
   *       preserved.</LI>
   * </UL>
   *
   * @return  The next token read from the script.
   *
   * @throws  ScriptException  If a problem occurs while attempting to parse the
   *                           next token.
   */
  private String nextToken()
         throws ScriptException
  {
    // Set the point at which to start looking.
    tokenStartPos = tokenEndPos + 1;

    // If we're at the beginning of a line, make sure it is not blank or a
    // comment.
    if (tokenStartPos == 0)
    {
      while ((tokenLine < lines.length) &&
             ((lines[tokenLine].length == 0) || (lines[tokenLine][0] == '#') ||
              (new String(lines[tokenLine]).trim().startsWith("#"))))
      {
        tokenLine++;
      }

      if (tokenLine >= lines.length)
      {
        return null;
      }
    }


    // If the point at which to start looking is past the end of the line, then
    // start the next line.
    if (tokenStartPos >= lines[tokenLine].length)
    {
      tokenStartPos = 0;

      // Keep going until we find a line that is not blank and not a comment.
      do
      {
        tokenLine++;

        // If we were on the last line, then return null to indicate there are
        // no more tokens.
        if (tokenLine >= lines.length)
        {
          return null;
        }
      } while ((lines[tokenLine].length == 0) || (lines[tokenLine][0] == '#') ||
               (new String(lines[tokenLine]).trim().length() == 0) ||
               (new String(lines[tokenLine]).trim().startsWith("#")));
    }

    // Now read until we find something other than whitespace.
    char c = lines[tokenLine][tokenStartPos];
    while ((c == ' ') || (c == '\t'))
    {
      tokenStartPos++;

      // If we've reached the end of the line, then it must be an entire line
      // of just whitespace.  If that happens, then find the next line that's
      // not blank and not a comment.
      if (tokenStartPos >= lines[tokenLine].length)
      {
        tokenStartPos = 0;

        // Keep going until we find a line that is not blank and not a comment.
        do
        {
          tokenLine++;

          // If we were on the last line, then return null to indicate there are
          // no more tokens.
          if (tokenLine >= lines.length)
          {
            return null;
          }
        } while ((lines[tokenLine].length == 0) ||
                 (lines[tokenLine][0] == '#'));
      }

      c = lines[tokenLine][tokenStartPos];
    }

    // Finally, something other than whitespace.  See what the character is.
    switch (lines[tokenLine][tokenStartPos])
    {
      // Check for the single characters that we return by themselves.
      case '.':
      case '=':
      case ';':
      case ',':
      case '(':
      case ')':
        tokenEndPos = tokenStartPos;
        return new String(new char[] { lines[tokenLine][tokenStartPos] });


      // Check for a numeric value.  If it is a numeric value, then it must be
      // followed only by other numeric digits and ended with a space, a tab,
      // a comma, a close parenthesis, or the end of the line.
      case '-':
      case '0':
      case '1':
      case '2':
      case '3':
      case '4':
      case '5':
      case '6':
      case '7':
      case '8':
      case '9':
        int i;
        for (i=tokenStartPos+1; i < lines[tokenLine].length; i++)
        {
          c = lines[tokenLine][i];

          if ((c == ' ') || (c == ';') || (c == '\t') || (c == ',') ||
              (c == ')'))
          {
            break;
          }
          else if (! ((c >= '0') && (c <= '9')))
          {
            throw new ScriptException(tokenLine, i,
                                      "Unexpected character '" + c +
                                      "' while parsing an integer value");
          }
        }

        tokenEndPos = i-1;
        String s = new String(lines[tokenLine], tokenStartPos,
                              (i-tokenStartPos));

        // If the token is only a single dash character, then it's invalid.
        if ((lines[tokenLine][tokenStartPos] == '-') && (s.length() == 1))
        {
          throw new ScriptException(tokenLine, tokenStartPos,
                                    "The dash character may only appear when " +
                                    "when followed immediately by a numeric " +
                                    "value to indicate a negative integer");
        }

        return s;


      // Check for a quotation mark.  If we find one, then look for the next
      // unescaped quotation mark and return the token as a string literal.
      case '"':
        for (i=tokenStartPos+1; i < lines[tokenLine].length; i++)
        {
          c = lines[tokenLine][i];
          if (c == '"')
          {
            if (lines[tokenLine][i-1] != '\\')
            {
              break;
            }
          }
        }

        if ((i > tokenStartPos) && (i < lines[tokenLine].length) &&
            (lines[tokenLine][i] == '"') && (lines[tokenLine][i-1] != '\\'))
        {
          tokenEndPos = i;
          s = new String(lines[tokenLine], tokenStartPos, (i+1-tokenStartPos));
          return s;
        }
        else
        {
          throw new ScriptException(tokenLine, tokenStartPos,
                                    "Detected an unterminated string literal.");
        }


      // It must be a reserved word, variable name, method name, or a Boolean
      // literal.  Make sure that it only starts with a letter and only
      // contains letters, digits, and underscores.
      default:
        c = lines[tokenLine][tokenStartPos];

        // Make sure it starts with a letter.
        if (! (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'))))
        {
          throw new ScriptException(tokenLine, tokenStartPos,
                                    "Unexpected character '" + c +
                                    "' at the beginning of the token.");
        }

        // Now loop until we find something other than a letter, a number, or
        // an underscore.
        for (i=tokenStartPos+1; i < lines[tokenLine].length; i++)
        {
          c = lines[tokenLine][i];
          if (! (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')) ||
                 ((c >= '0') && (c <= '9')) || (c == '_')))
          {
            tokenEndPos = i-1;
            break;
          }
        }

        if (i >= lines[tokenLine].length)
        {
          tokenEndPos = lines[tokenLine].length - 1;
        }

        s = new String(lines[tokenLine], tokenStartPos, (i - tokenStartPos));
        return s.toLowerCase();
    }
  }



  /**
   * Reads the next token from the script as a Java class name.  This has
   * special requirements (periods are not delimiters, and only alphabetic,
   * numeric, and underscore characters are permitted between periods) that
   * makes it appropriate to handle this separately.
   *
   * @return  The next Java class name read from the script.
   *
   * @throws  ScriptException  If a problem occurs while reading the next token.
   */
  private String nextClassNameToken()
          throws ScriptException
  {
    // Set the point at which to start looking.
    tokenStartPos = tokenEndPos + 1;

    // If we're at the beginning of a line, make sure it is not blank or a
    // comment.
    if (tokenStartPos == 0)
    {
      while ((tokenLine < lines.length) &&
             ((lines[tokenLine].length == 0) || (lines[tokenLine][0] == '#')))
      {
        tokenLine++;
      }

      if (tokenLine >= lines.length)
      {
        return null;
      }
    }


    // If the point at which to start looking is past the end of the line, then
    // start the next line.
    if (tokenStartPos >= lines[tokenLine].length)
    {
      tokenStartPos = 0;

      // Keep going until we find a line that is not blank and not a comment.
      do
      {
        tokenLine++;

        // If we were on the last line, then return null to indicate there are
        // no more tokens.
        if (tokenLine >= lines.length)
        {
          return null;
        }
      } while ((lines[tokenLine].length == 0) || (lines[tokenLine][0] == '#'));
    }

    // Now read until we find something other than whitespace.
    char c = lines[tokenLine][tokenStartPos];
    while ((c == ' ') || (c == '\t'))
    {
      tokenStartPos++;

      // If we've reached the end of the line, then it must be an entire line
      // of just whitespace.  If that happens, then find the next line that's
      // not blank and not a comment.
      if (tokenStartPos >= lines[tokenLine].length)
      {
        tokenStartPos = 0;

        // Keep going until we find a line that is not blank and not a comment.
        do
        {
          tokenLine++;

          // If we were on the last line, then return null to indicate there are
          // no more tokens.
          if (tokenLine >= lines.length)
          {
            return null;
          }
        } while ((lines[tokenLine].length == 0) ||
                 (lines[tokenLine][0] == '#'));
      }

      c = lines[tokenLine][tokenStartPos];
    }

    // Finally, something other than whitespace.  For a class name, the first
    // character must be alphabetic.
    if (! (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z'))))
    {
      throw new ScriptException(tokenLine, tokenStartPos,
                                "A variable type class name must begin with " +
                                "an alphabetic character");
    }

    // Now keep reading until we find either whitespace or a semicolon.  Until
    // then, the only characters allowed must be alphabetic, numeric, or
    // periods.
    int i;
    for (i=tokenStartPos+1; i < lines[tokenLine].length; i++)
    {
      c = lines[tokenLine][i];

      if ((c == ' ') || (c == '\t') || (c == ';'))
      {
        break;
      }
      else if (! (((c >= 'a') && (c <= 'z')) || ((c >= 'A') && (c <= 'Z')) ||
                  (c == '.') || (c == '_')))
      {
        throw new ScriptException(tokenLine, i,
                                  "Unexpected character '" + c +
                                  "' while parsing Java class name.");
      }
    }

    tokenEndPos = i - 1;
    return new String(lines[tokenLine], tokenStartPos, (i - tokenStartPos));
  }



  /**
   * Indicates whether the specified token is one of the reserved words in the
   * SLAMD scripting environment.  It is expected that the token will be in all
   * lowercase characters.
   *
   * @param  token  The token for which to make the determination.
   *
   * @return  <CODE>true</CODE> if the specified token is one of the reserved
   *          words, or <CODE>false</CODE> if not.
   */
  public static boolean isReservedWord(String token)
  {
    return (token.equals(RESERVED_WORD_USE) ||
            token.equals(RESERVED_WORD_VARIABLE) ||
            token.equals(RESERVED_WORD_IF) ||
            token.equals(RESERVED_WORD_IF_NOT) ||
            token.equals(RESERVED_WORD_ELSE) ||
            token.equals(RESERVED_WORD_LOOP) ||
            token.equals(RESERVED_WORD_WHILE) ||
            token.equals(RESERVED_WORD_WHILE_NOT) ||
            token.equals(RESERVED_WORD_BEGIN) ||
            token.equals(RESERVED_WORD_END));
  }



  /**
   * Executes the script.  The script must have already been read in and parsed
   * into tokens.
   *
   * @param  jobThread  The job thread that will be executing the script.
   *
   * @throws  ScriptException  If a problem occurs while executing the script.
   */
  public void execute(JobClass jobThread)
         throws ScriptException
  {
    // Make sure to associate the script variable with the job thread.
    ScriptVariable scriptVariable = (ScriptVariable) variableHash.get("script");
    scriptVariable.setJobThread(jobThread);
    scriptVariable.setParser(this);


    // Start all of the stat trackers.
    for (Variable v : variableHash.values())
    {
      v.startStatTrackers(jobThread);
    }


    // Execute each instruction in the instruction set.
    try
    {
      for (int i=0; i < instructionList.size(); i++)
      {
        Instruction instruction = instructionList.get(i);
        instruction.execute(jobThread);
      }
    }
    catch (StopRunningException sre)
    {
      jobThread.logMessage(sre.getMessage());
    }


    // Stop all of the stat trackers.
    for (Variable v : variableHash.values())
    {
      v.stopStatTrackers();
    }
  }



  /**
   * Executes the script in debug mode, sending debug information to the
   * client's message writer.  The script must have already been read in and
   * parsed into tokens.
   *
   * @param  jobThread  The job thread that will be executing the script.
   *
   * @throws  ScriptException  If a problem occurs while executing the script.
   */
  public void debugExecute(JobClass jobThread)
         throws ScriptException
  {
    // Make sure to associate the script variable with the job thread.
    ScriptVariable scriptVariable = (ScriptVariable) variableHash.get("script");
    scriptVariable.setJobThread(jobThread);
    scriptVariable.setParser(this);


    // Start all of the stat trackers.
    for (Variable v : variableHash.values())
    {
      v.startStatTrackers(jobThread);
    }


    // Execute each instruction in the instruction set.
    try
    {
      for (int i=0; i < instructionList.size(); i++)
      {
        Instruction instruction = instructionList.get(i);
        instruction.debugExecute(jobThread);
      }
    }
    catch (StopRunningException sre)
    {
      jobThread.logMessage(sre.getMessage());
    }


    // Stop all of the stat trackers.
    for (Variable v : variableHash.values())
    {
      v.stopStatTrackers();
    }
  }



  /**
   * Retrieves the set of statistics gathered while running this script.
   *
   * @return  The set of statistics gathered while running this script.
   */
  public StatTracker[] getStatTrackers()
  {
    ArrayList<StatTracker> trackerList = new ArrayList<StatTracker>();

    for (Variable v : variableHash.values())
    {
      StatTracker[] trackers = v.getStatTrackers();
      if ((trackers != null) && (trackers.length > 0))
      {
        for (int i=0; i < trackers.length; i++)
        {
          trackerList.add(trackers[i]);
        }
      }
    }

    StatTracker[] trackers = new StatTracker[trackerList.size()];
    trackerList.toArray(trackers);
    return trackers;
  }
}

