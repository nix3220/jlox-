package com.nix.lox;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.nix.lox.Expr.This;

/**
 * LoxObject
 */
public class LoxMath extends LoxNative{

  //public static LoxSystem system;

  LoxMath(Environment environment, Interpreter interpreter, LoxClass type){
    super(null, null, null, null, type);
    setDetails("Math", new LoxObject(environment, interpreter, "Math", this.type), defineFunctions(environment), interpreter);
  }

  public Map<String, LoxFunction> defineFunctions(Environment environment) {
    Map<String, LoxFunction> methods = new HashMap<>();
    methods.put("round", round(environment));
    methods.put("floor", floor(environment));
    methods.put("min", min(environment));
    methods.put("max", max(environment));
    methods.put("abs", abs(environment));
    return methods;
  }

  private LoxFunction round(Environment environment) {
    return new LoxFunction(new LoxCallable() {

      @Override
      public int arity() {
        return 1;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double)Math.round((double)arguments.get(0));
      }
      
    }, environment, false, true);
  }

  private LoxFunction floor(Environment environment) {
    return new LoxFunction(new LoxCallable() {

      @Override
      public int arity() {
        return 1;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double)Math.floor((double)arguments.get(0));
      }
      
    }, environment, false, true);
  }

  private LoxFunction abs(Environment environment) {
    return new LoxFunction(new LoxCallable() {

      @Override
      public int arity() {
        return 1;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double)Math.abs((double)arguments.get(0));
      }
      
    }, environment, false, true);
  }

  private LoxFunction min(Environment environment) {
    return new LoxFunction(new LoxCallable() {

      @Override
      public int arity() {
        return 2;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double)Math.min((double)arguments.get(0), (double)arguments.get(1));
      }
      
    }, environment, false, true);
  }

  private LoxFunction max(Environment environment) {
    return new LoxFunction(new LoxCallable() {

      @Override
      public int arity() {
        return 2;
      }

      @Override
      public Object call(Interpreter interpreter, List<Object> arguments) {
        return (double)Math.max((double)arguments.get(0), (double)arguments.get(1));
      }
      
    }, environment, false, true);
  }


  @Override
  public void defineFields() {
    
  }
}