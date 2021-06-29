/* File MicroC/Machine.java
   A unified-s abstract machine for imperative ps.
   sestoft@itu.dk * 2001-03-21, 2009-09-24

   To execute a p file using this abstract machine, do:

      java Machine <pfile> <arg1> <arg2> ...

   or, to get a trace of the p execution:

      java Machinetrace <pfile> <arg1> <arg2> ...

*/
import CType.*;
import java.io.*;
import java.util.*;

class Machine {
  public static void main(String[] args)        
    throws FileNotFoundException, IOException {
    if (args.length == 0) 
      System.out.println("Usage: java Machine <pfile> <arg1> ...\n");
    else
      execute(args, false);
  }

  // These numeric instruction codes must agree with Machine.fs:

  final static int 
    CSTI = 0, ADD = 1, SUB = 2, MUL = 3, DIV = 4, MOD = 5, 
    EQ = 6, LT = 7, NOT = 8, 
    DUP = 9, SWAP = 10, 
    LDI = 11, STI = 12, 
    GETBP = 13, GETSP = 14, INCSP = 15, 
    GOTO = 16, IFZERO = 17, IFNZRO = 18, CALL = 19, TCALL = 20, RET = 21, 
    PRINTI = 22, PRINTC = 23, 
    LDARGS = 24,
    STOP = 25,CSTF = 26,
    CSTC = 27,THROW = 28,PUSHHR = 29,POPHR = 30;



  final static int sSIZE = 1000;
  
  // Read code from file and execute it

  static void execute(String[] args, boolean trace) 
    throws FileNotFoundException, IOException {
    int[] p = readfile(args[0]);                // Read the p from file
    CBaseType[] s = new CBaseType[sSIZE];               // The evaluation s
    CBaseType[] iargs = new CBaseType[args.length-1];
           for (int i = 1; i < args.length; i++) {
            if(Pattern.compile("(?i)[a-z]").matcher(args[i]).find()){
                char[] input = args[i].toCharArray();
                CCharType[] array = new CCharType[input.length];
                for(int j = 0; j < input.length; ++j) {
                    array[j] = new CCharType(input[j]);
                }
                iargs[i-1] = new CArrayType(array);
            }
            else if(args[i].contains(".")){
                iargs[i-1] = new CFloatType(new Float(args[i]).floatValue());
            }
            else {
                iargs[i-1] = new CIntType(new Integer(args[i]).intValue());
            }
        }
    long starttime = System.currentTimeMillis();
    execcode(p, s, iargs, trace);            // Execute p proper
    long runtime = System.currentTimeMillis() - starttime;
    System.err.println("\nRan " + runtime/1000.0 + " seconds");
  }

  // The machine: execute the code starting at p[pc] 

  static int execcode(ArrayList<Integer> p, CBaseType[] s, CBaseType[] iargs, boolean trace) {
    int bp = -999;	// Base pointer, for local variable access 
    int sp = -1;	// s top pointer
    int pc = 0;		// p counter: next instruction
    int hr = -1;
    for (;;) {
      if (trace) 
        printsppc(s, bp, sp, p, pc);
            switch (p.get(pc++)) {
                case CSTI:
                    s[sp + 1] = new CIntType(p.get(pc++)); sp++; break;
                case CSTF:
                    s[sp + 1] = new CFloatType(Float.intBitsToFloat(p.get(pc++))); sp++; break;
                case CSTC:
                    s[sp + 1] = new CCharType((char)(p.get(pc++).intValue())); sp++; break;
                case ADD: {
                    s[sp - 1] = binaryOperator(s[sp-1], s[sp], "+");
                    sp--;
                    break;
                }
                case SUB:{
                    s[sp - 1] = binaryOperator(s[sp-1], s[sp], "-");
                    sp--;
                    break;
                }

                case MUL: {
                    s[sp - 1] = binaryOperator(s[sp-1], s[sp], "*");
                    sp--;
                    break;
                }
                case DIV:
                    if(((CIntType)s[sp]).getValue()==0)
                    {
                        System.out.println("hr:"+hr+" exception:"+1);
                        while (hr != -1 && ((CIntType)s[hr]).getValue() != 1 )
                        {
                            hr = ((CIntType)s[hr+2]).getValue();
                            System.out.println("hr:"+hr+" exception:"+new CIntType(p.get(pc)).getValue());
                        }
                            
                        if (hr != -1) { 
                            sp = hr-1;    
                            pc = ((CIntType)s[hr+1]).getValue();
                            hr = ((CIntType)s[hr+2]).getValue();    
                        } else {
                            System.out.print(hr+"not find exception");
                            return sp;
                        }
                    }
                    else{
                        s[sp - 1] = binaryOperator(s[sp-1], s[sp], "/");
                        sp--; 
                    }
                    
                    break;
                case MOD:
                    s[sp - 1] = binaryOperator(s[sp-1], s[sp], "%");
                    sp--;
                    break;
                case EQ:
                    s[sp - 1] = binaryOperator(s[sp-1], s[sp], "==");
                    sp--;
                    break;
                case LT:
                    s[sp - 1] = binaryOperator(s[sp-1], s[sp], "<");
                    sp--;
                    break;
                case NOT: {
                    Object result = null;
                    if(s[sp] instanceof CFloatType){
                        result = ((CFloatType)s[sp]).getValue();
                    }else if (s[sp] instanceof CIntType){
                        result = ((CIntType)s[sp]).getValue();
                    }
                    s[sp] = (Float.compare(new Float(result.toString()), 0.0f) == 0 ? new CIntType(1) : new CIntType(0));
                    break;
                }
                case DUP:
                    s[sp+1] = s[sp];
                    sp++;
                    break;
                case SWAP: {
                    CBaseType tmp = s[sp];  s[sp] = s[sp-1];  s[sp-1] = tmp;
                    break;
                }
                case LDI:
                    s[sp] = s[((CIntType)s[sp]).getValue()]; break;
                case STI:
                    s[((CIntType)s[sp-1]).getValue()] = s[sp]; s[sp-1] = s[sp]; sp--; break;
                case GETBP:
                    s[sp+1] = new CIntType(bp); sp++; break;
                case GETSP:
                    s[sp+1] = new CIntType(sp); sp++; break;
                case INCSP:
                    sp = sp + p.get(pc++); break;
                case GOTO:
                    pc = p.get(pc); break;
                case IFZERO: {
                    Object result = null;
                    int index = sp--;
                    if(s[index] instanceof CIntType){
                        result = ((CIntType)s[index]).getValue();
                    }else if(s[index] instanceof CFloatType){
                        result = ((CFloatType)s[index]).getValue();
                    }
                    pc = (Float.compare(new Float(result.toString()), 0.0f) == 0 ? p.get(pc) : pc + 1);
                    break;
                }
                case IFNZRO: {
                    Object result = null;
                    int index = sp--;
                    if (s[index] instanceof CIntType) {
                        result = ((CIntType) s[index]).getValue();
                    } else if (s[index] instanceof CFloatType) {
                        result = ((CFloatType) s[index]).getValue();
                    }
                    pc = (Float.compare(new Float(result.toString()), 0.0f) != 0 ? p.get(pc) : pc + 1);
                    break;
                }
                case CALL: {
                    int argc = p.get(pc++);
                    for (int i=0; i<argc; i++)
                        s[sp-i+2] = s[sp-i];
                    s[sp-argc+1] = new CIntType(pc+1); sp++;
                    s[sp-argc+1] = new CIntType(bp);   sp++;
                    bp = sp+1-argc;
                    pc = p.get(pc);
                    break;
                }
                case TCALL: {
                    int argc = p.get(pc++);
                    int pop  = p.get(pc++);
                    for (int i=argc-1; i>=0; i--)
                        s[sp-i-pop] = s[sp-i];
                    sp = sp - pop; pc = p.get(pc);
                } break;
                case RET: {
                    CBaseType res = s[sp];
                    sp = sp - p.get(pc); bp = ((CIntType)s[--sp]).getValue(); pc = ((CIntType)s[--sp]).getValue();
                    s[sp] = res;
                } break;
                case PRINTI: {
                    Object result;
                    if(s[sp] instanceof CIntType){
                        result = ((CIntType)s[sp]).getValue();
                    }else if(s[sp] instanceof CFloatType){
                        result = ((CFloatType)s[sp]).getValue();
                    }else {
                        result = ((CCharType)s[sp]).getValue();
                    }

                    System.out.print(String.valueOf(result) + " ");
                    break;
                }
                case PRINTC:
                    System.out.print((((CCharType)s[sp])).getValue()); break;
                case LDARGS:
                    for (int i=0; i < iargs.length; i++) // Push commandline arguments
                        s[++sp] = iargs[i];
                    break;
                case STOP:
                    return sp;
                case PUSHHR:{
                    s[++sp] = new CIntType(p.get(pc++));    //exn
                    int tmp = sp;       //exn address
                    sp++;
                    s[sp++] = new CIntType(p.get(pc++));   //jump address
                    s[sp] = new CIntType(hr);
                    hr = tmp;
                    break;
                }
                case POPHR:
                    hr = ((CIntType)s[sp--]).getValue();sp-=2;break;
                case THROW:
                    System.out.println("hr:"+hr+" exception:"+new CIntType(p.get(pc)).getValue());

                    while (hr != -1 && ((CIntType)s[hr]).getValue() != p.get(pc) )
                    {
                        hr = ((CIntType)s[hr+2]).getValue(); //find exn address
                        System.out.println("hr:"+hr+" exception:"+new CIntType(p.get(pc)).getValue());
                    }
                        
                    if (hr != -1) { // Found a handler for exn
                        sp = hr-1;    // remove s after hr
                        pc = ((CIntType)s[hr+1]).getValue();
                        hr = ((CIntType)s[hr+2]).getValue(); // with current handler being hr     
                    } else {
                        System.out.print(hr+"not find exception");
                        return sp;
                    }break;

                default:
                    throw new RuntimeException("Illegal instruction " + p.get(pc-1)
                            + " at address " + (pc-1));

            }


        }


    }

  // Print the s machine instruction at p[pc]

  static String insname(int[] p, int pc) {
    switch (p[pc]) {
    case CSTI:   return "CSTI " + p[pc+1]; 
    case ADD:    return "ADD";
    case SUB:    return "SUB";
    case MUL:    return "MUL";
    case DIV:    return "DIV";
    case MOD:    return "MOD";
    case EQ:     return "EQ";
    case LT:     return "LT";
    case NOT:    return "NOT";
    case DUP:    return "DUP";
    case SWAP:   return "SWAP";
    case LDI:    return "LDI";
    case STI:    return "STI";
    case GETBP:  return "GETBP";
    case GETSP:  return "GETSP";
    case INCSP:  return "INCSP " + p[pc+1];
    case GOTO:   return "GOTO " + p[pc+1];
    case IFZERO: return "IFZERO " + p[pc+1];
    case IFNZRO: return "IFNZRO " + p[pc+1];
    case CALL:   return "CALL " + p[pc+1] + " " + p[pc+2];
    case TCALL:  return "TCALL " + p[pc+1] + " " + p[pc+2] + " " + p[pc+3];
    case RET:    return "RET " + p[pc+1];
    case PRINTI: return "PRINTI";
    case PRINTC: return "PRINTC";
    case LDARGS: return "LDARGS";
    case STOP:   return "STOP";
    default:     return "<unknown>";
    }
  }

  // Print current s and current instruction

  static void printsppc(int[] s, int bp, int sp, int[] p, int pc) {
    System.out.print("[ ");
    for (int i=0; i<=sp; i++)
      System.out.print(s[i] + " ");
    System.out.print("]");
    System.out.println("{" + pc + ": " + insname(p, pc) + "}"); 
  }

  // Read instructions from a file

  public static int[] readfile(String filename) 
    throws FileNotFoundException, IOException
  {
    ArrayList<Integer> rawp = new ArrayList<Integer>();
    Reader inp = new FileReader(filename);
    StreamTokenizer tstream = new StreamTokenizer(inp);
    tstream.parseNumbers();
    tstream.nextToken();
    while (tstream.ttype == StreamTokenizer.TT_NUMBER) {
      rawp.add(new Integer((int)tstream.nval));
      tstream.nextToken();
    }
    inp.close();
    final int psize = rawp.size();
    int[] p = new int[psize];
    for (int i=0; i<psize; i++)
      p[i] = ((Integer)(rawp.get(i))).intValue();
    return p;
  }
}

// Run the machine with tracing: print each instruction as it is executed

class Machinetrace {
  public static void main(String[] args)        
    throws FileNotFoundException, IOException {
    if (args.length == 0) 
      System.out.println("Usage: java Machinetrace <pfile> <arg1> ...\n");
    else
      Machine.execute(args, true);
  }

public static CBaseType binaryOperator(CBaseType lhs, CBaseType rhs, String operator) throws ImcompatibleTypeError, OperatorError {
        Object left;
        Object right;
        int flag = 0;
        if (lhs instanceof CFloatType) {
            left = ((CFloatType) lhs).getValue();
            flag = 1;
        } else if (lhs instanceof CIntType) {
            left = ((CIntType) lhs).getValue();
        } else {
            throw new ImcompatibleTypeError("ImcompatibleTypeError: Left type is not int or float");
        }

        if (rhs instanceof CFloatType) {
            right = ((CFloatType) rhs).getValue();
            flag = 1;
        } else if (rhs instanceof CIntType) {
            right = ((CIntType) rhs).getValue();
        } else {
            throw new ImcompatibleTypeError("ImcompatibleTypeError: Right type is not int or float");
        }
        CBaseType result = null;

        switch(operator){
            case "+":{
                if (flag == 1) {
                    result =  new CFloatType(Float.parseFloat(String.valueOf(left)) + Float.parseFloat(String.valueOf(right)));
                } else {
                    result = new CIntType(Integer.parseInt(String.valueOf(left)) + Integer.parseInt(String.valueOf(right)));
                }
                break;
            }
            case "-":{
                if (flag == 1) {
                    result = new CFloatType(Float.parseFloat(String.valueOf(left)) - Float.parseFloat(String.valueOf(right)));
                } else {
                    result = new CIntType(Integer.parseInt(String.valueOf(left)) - Integer.parseInt(String.valueOf(right)));
                }
                break;
            }
            case "*":{
                if (flag == 1) {
                    result = new CFloatType(Float.parseFloat(String.valueOf(left)) * Float.parseFloat(String.valueOf(right)));
                } else {
                    result = new CIntType(Integer.parseInt(String.valueOf(left)) * Integer.parseInt(String.valueOf(right)));
                }
                break;
            }
            case "/":{
                if(Float.compare(Float.parseFloat(String.valueOf(right)), 0.0f) == 0){
                    throw new OperatorError("OpeatorError: Divisor can't not be zero");
                }
                if (flag == 1) {
                    result = new CFloatType(Float.parseFloat(String.valueOf(left)) / Float.parseFloat(String.valueOf(right)));
                } else {
                    result = new CIntType(Integer.parseInt(String.valueOf(left)) / Integer.parseInt(String.valueOf(right)));
                }
                break;
            }
            case "%":{
                if (flag == 1) {
                    throw new OperatorError("OpeatorError: Float can't mod");
                } else {
                    result = new CIntType(Integer.parseInt(String.valueOf(left)) % Integer.parseInt(String.valueOf(right)));
                }
                break;
            }
            case "==":{
                if (flag == 1) {
                    if((float) left == (float) right){
                        result = new CIntType(1);
                    }
                    else{
                        result = new CIntType(0);
                    }
                } else {
                    if((int) left == (int) right){
                        result = new CIntType(1);
                    }
                    else{
                        result = new CIntType(0);
                    }
                }
                break;
            }
            case "<":{
                if (flag == 1) {
                    if((float) left < (float) right){
                        result = new CIntType(1);
                    }
                    else{
                        result = new CIntType(0);
                    }
                } else {
                    if((int) left < (int) right){
                        result = new CIntType(1);
                    }
                    else{
                        result = new CIntType(0);
                    }
                }
                break;
            }
        }
        return result;
    }

}
 
