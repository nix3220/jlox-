package com.nix.lox;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import com.nix.lox.Expr.Coalesce;
import com.nix.lox.Expr.Get;
import com.nix.lox.Expr.GetStatic;
import com.nix.lox.Expr.New;
import com.nix.lox.Expr.Set;
import com.nix.lox.Expr.Super;
import com.nix.lox.Expr.This;
import com.nix.lox.Stmt.Class;
import com.nix.lox.Stmt.GetFile;
import com.nix.lox.Stmt.Interface;
import com.nix.lox.Stmt.Module;
import com.nix.lox.Stmt.When;

public class Resolver implements Expr.Visitor<Void>, Stmt.Visitor<Void>{
  private final Interpreter interpreter;
  private final Stack<Map<String, Boolean>> scopes = new Stack<>();
  private FunctionType currentFunction = FunctionType.NONE;
  private boolean inTest = false;

  private enum ClassType {
    NONE,
    CLASS,
    SUBCLASS
  }

  private ClassType currentClass = ClassType.NONE;

  Resolver(Interpreter interpreter) {
    this.interpreter = interpreter;
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    beginScope();
    resolve(stmt.statements);
    endScope();
    return null;
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    declare(stmt.name);
    if (stmt.initializer != null) {
      resolve(stmt.initializer);
    }
    define(stmt.name);
    return null;
  }

  @Override
  public Void visitVariableExpr(Expr.Variable expr){
    if(!scopes.isEmpty() && scopes.peek().get(expr.name.lexeme) == Boolean.FALSE){
      Lox.error(expr.name, "Can't read local variable in it's own initializer");
    }
    resolveLocal(expr, expr.name);
    return null;
  }

  @Override
  public Void visitAssignExpr(Expr.Assign expr) {
    resolve(expr.value);
    resolveLocal(expr, expr.name);
    return null;
  }

  private void resolveFunction(Stmt.Function function, FunctionType type) {
    FunctionType enclosingFunction = currentFunction;
    currentFunction = type;

    beginScope();
    for (Token param : function.params) {
      declare(param);
      define(param);
    }
    if(function.hasBody) resolve(function.body);
    endScope();
    currentFunction = enclosingFunction;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    declare(stmt.name);
    define(stmt.name);

    resolveFunction(stmt, FunctionType.FUNCTION);
    return null;
  }

  @Override
  public Void visitTestStmt(Stmt.Test stmt) {
    resolve(stmt.name);

    beginScope();
    resolve(stmt.body);
    endScope();

    return null;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    resolve(stmt.expression);
    return null;
  }

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if (stmt.elseBranch != null) resolve(stmt.elseBranch);
    return null;
  }

  @Override
  public Void visitWhenStmt(When stmt) {
    resolve(stmt.condition);
    resolve(stmt.thenBranch);
    if(stmt.finallyBranch != null) resolve(stmt.finallyBranch);
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    if (currentFunction == FunctionType.NONE) {
      Lox.error(stmt.keyword, "Can't return from top-level code.");
    }

    if (stmt.value != null) {
      if(currentFunction == FunctionType.INITIALIZER){
        Lox.error(stmt.keyword, "Can't return a value from a keyword");
      }

      resolve(stmt.value);
    }

    return null;
  }

  @Override
  public Void visitExpectStmt(Stmt.Expect stmt) {
    if (stmt.value != null) {
      resolve(stmt.value);
    }

    return null;
  }

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    return null;
  }

  @Override
  public Void visitContinueStmt(Stmt.Continue stmt) {
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    resolve(stmt.condition);
    resolve(stmt.body);
    return null;
  }

  

  @Override
  public Void visitBinaryExpr(Expr.Binary expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitCallExpr(Expr.Call expr) {
    resolve(expr.callee);

    for (Expr argument : expr.arguments) {
      resolve(argument);
    }

    return null;
  }

  @Override
  public Void visitGroupingExpr(Expr.Grouping expr) {
    resolve(expr.expression);
    return null;
  }

  @Override
  public Void visitLiteralExpr(Expr.Literal expr) {
    return null;
  }

  @Override
  public Void visitLogicalExpr(Expr.Logical expr) {
    resolve(expr.left);
    resolve(expr.right);
    return null;
  }

  @Override
  public Void visitUnaryExpr(Expr.Unary expr) {
    resolve(expr.right);
    return null;
  }

  

  private void resolveLocal(Expr expr, Token name) {
    for (int i = scopes.size() - 1; i >= 0; i--) {
      if(scopes.get(i).containsKey(name.lexeme)){
        interpreter.resolve(expr, scopes.size() - 1 - i);
        return;
      }
    }
  }

  private void declare(Token name) {
    if(scopes.isEmpty()) return;

    Map<String, Boolean> scope = scopes.peek();

    if(scope.containsKey(name.lexeme)) {
      Lox.error(name, "Duplicate variable in scope");
    }

    scope.put(name.lexeme, false);
  }

  private void define(Token name) {
    if(scopes.isEmpty()) return;
    scopes.peek().put(name.lexeme, true);
  }

  void resolve(List<Stmt> statements) {
    for(Stmt statement : statements) {
      resolve(statement);
    }
  }

  private void resolve(Stmt stmt){
    stmt.accept(this);
  }

  private void resolve(Expr expr) {
    expr.accept(this);
  }

  private void beginScope(){
    scopes.push(new HashMap<String, Boolean>());
  }

  private void endScope(){
    scopes.pop();
  }


  @Override
  public Void visitClassStmt(Class stmt) {
    ClassType enclosingClass = currentClass;
    currentClass = ClassType.CLASS;

    declare(stmt.name);
    define(stmt.name);

    if(stmt.superclass != null && stmt.name.lexeme.equals(stmt.superclass.name.lexeme)){
      Lox.error(stmt.superclass.name, "A class cant inherit from itself");
    }

    if(stmt.superclass != null){
      currentClass = ClassType.SUBCLASS;
      resolve(stmt.superclass);
    }

    if(stmt.superclass != null){
      beginScope();
      scopes.peek().put("super", true);
    }

    beginScope();
    scopes.peek().put("this", true);

    for(Stmt.Function method : stmt.methods) {
      FunctionType declaration = FunctionType.METHOD;
      if (method.name.lexeme.equals("constructor")) {
        declaration = FunctionType.INITIALIZER;
      }
      resolveFunction(method, declaration);
    }

    endScope();

    if(stmt.superclass != null) endScope();

    currentClass = enclosingClass;
    return null;
  }

  @Override
  public Void visitInterfaceStmt(Interface stmt) {
    
    declare(stmt.name);
    define(stmt.name);

    beginScope();
    for(FunctionTemplate func : stmt.methods){
      resolveFuncTemplate(func);
    }
    endScope();

    return null;
  }

  @Override
  public Void visitEnumStmt(Stmt.Enum stmt) {
    declare(stmt.name);
    define(stmt.name);

    return null;
  }

  @Override
  public Void visitSwitchStmt(Stmt.Switch stmt) {
    resolve(stmt.value);
    for(Stmt.Case c : stmt.cases){
      resolve(c);
    }
    return null;
  }

  @Override
  public Void visitCaseStmt(Stmt.Case stmt) {
    if(stmt.value != null) resolve(stmt.value);
    resolve(stmt.body);
    return null;
  }

  private void resolveFuncTemplate(FunctionTemplate template) {
    beginScope();
    for(Token param : template.params){
      declare(param);
      define(param);
    }
    endScope();
  }

  @Override
  public Void visitGetExpr(Get expr) {
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitCoalesceExpr(Coalesce expr) {
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitSetExpr(Set expr) {
    resolve(expr.value);
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitThisExpr(This expr) {
    if(currentClass == ClassType.NONE){
      Lox.error(expr.keyword, "Can't use 'this' outside of a class");
    }

    resolveLocal(expr, expr.keyword);
    return null;
  }

  @Override
  public Void visitSuperExpr(Super expr) {
    if (currentClass == ClassType.NONE) {
      Lox.error(expr.keyword,
          "Can't use 'super' outside of a class.");
    } else if (currentClass != ClassType.SUBCLASS) {
      Lox.error(expr.keyword,
          "Can't use 'super' in a class with no superclass.");
    }
    
    resolveLocal(expr, expr.keyword);
    return null;
  }

  @Override
  public Void visitGetStaticExpr(GetStatic expr) {
    resolve(expr.object);
    return null;
  }

  @Override
  public Void visitNewExpr(New expr) {
    resolveLocal(expr, expr.keyword);
    return null;
  }

  @Override
  public Void visitGetFileStmt(GetFile stmt) {
    return null;
  }

  @Override
  public Void visitModuleStmt(Module stmt) {
    return null;
  }

  
  
}
