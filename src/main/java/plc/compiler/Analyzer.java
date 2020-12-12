package plc.compiler;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * See the specification for information about what the different visit
 * methods should do.
 */
public final class Analyzer implements Ast.Visitor<Ast> {

    public Scope scope;

    public Analyzer(Scope scope) {
        this.scope = scope;
    }

    @Override
    public Ast visit(Ast.Source ast) throws AnalysisException {
        if(ast.getStatements().isEmpty()){
            throw new AnalysisException("empty!");
        }else{
            return new Ast.Source(ast.getStatements());
        }
    }

    /**
     * Statically validates that visiting a statement returns a statement.
     */
    private Ast.Statement visit(Ast.Statement ast) throws AnalysisException {
        return (Ast.Statement) visit((Ast) ast);
    }

    @Override
    public Ast.Statement.Expression visit(Ast.Statement.Expression ast) throws AnalysisException {
        if (visit(ast.getExpression()) instanceof Ast.Expression.Function) {
            return new Ast.Statement.Expression(visit(ast.getExpression()));
        }else {
            throw new AnalysisException("not function");
        }
    }

    @Override
    public Ast.Statement.Declaration visit(Ast.Statement.Declaration ast) throws AnalysisException {
        scope.define(ast.getName(), Stdlib.getType((ast.getType())));
        if(scope.lookup(ast.getName()) == null) {
            throw new AnalysisException("Already defined");
        }
        else if(Stdlib.getType(ast.getType()) == Stdlib.Type.VOID){
            throw new AnalysisException("VOID is not allowed");
        }
        else if(!ast.getValue().isPresent()) {
            return new Ast.Statement.Declaration(ast.getName(), Stdlib.getType(ast.getType()).getJvmName(), Optional.empty());
        }
        else {
            return new Ast.Statement.Declaration(ast.getName(), Stdlib.getType(ast.getType()).getJvmName(), Optional.of(visit(ast.getValue().get())));
        }
    }

    @Override
    public Ast.Statement.Assignment visit(Ast.Statement.Assignment ast) throws AnalysisException {
        if(ast.getExpression().getType() == scope.lookup(ast.getName())){
            return new Ast.Statement.Assignment(ast.getName(), ast.getExpression());
        }
        throw new AnalysisException("not same");
    }

    @Override
    public Ast.Statement.If visit(Ast.Statement.If ast) throws AnalysisException {

        List<Ast.Statement> thenS = new ArrayList<>();
        for(int i=0;i<ast.getThenStatements().size();i++){
            thenS.add(visit(ast.getThenStatements().get(i)));
        }

        List<Ast.Statement> elseS = new ArrayList<>();
        for(int i=0;i<ast.getElseStatements().size();i++) {
            elseS.add(visit(ast.getElseStatements().get(i)));
        }

        if(visit(ast.getCondition()).getType() != Stdlib.Type.BOOLEAN) {
            throw new AnalysisException("not bool");
        }else if(ast.getThenStatements().size() <1){
            throw new AnalysisException("then empty");
        }else {
            return new Ast.Statement.If(visit(ast.getCondition()), thenS, elseS);
        }

    }

    @Override
    public Ast.Statement.While visit(Ast.Statement.While ast) throws AnalysisException {

        if (visit(ast.getCondition()).getType() != Stdlib.Type.BOOLEAN) {
            throw new AnalysisException("not bool");
        }else {
            //do we need???????????????????
            Scope j = scope;
            Scope k = scope;
            scope = new Scope(j);
            for(int i=0;i<ast.getStatements().size();i++) {
                visit(ast.getStatements().get(i));
            }
            scope = k;
            return new Ast.Statement.While(ast.getCondition(), ast.getStatements());
        }

    }

    /**
     * Statically validates that visiting an expression returns an expression.
     */
    private Ast.Expression visit(Ast.Expression ast) throws AnalysisException {
        return (Ast.Expression) visit((Ast) ast);
    }

    @Override
    public Ast.Expression.Literal visit(Ast.Expression.Literal ast) throws AnalysisException {

        if (ast.getValue() instanceof Boolean) {
            return new Ast.Expression.Literal(Stdlib.Type.BOOLEAN, ast.getValue());
        } else if (ast.getValue() instanceof BigInteger) {

            if(((BigInteger)ast.getValue()).compareTo(new BigInteger(String.valueOf(Integer.MIN_VALUE)))<0 || ((BigInteger)ast.getValue()).compareTo(new BigInteger(String.valueOf(Integer.MAX_VALUE)))>0) {
                throw new AnalysisException("out of range");
            }
            return new Ast.Expression.Literal(Stdlib.Type.INTEGER, ((BigInteger) ast.getValue()).intValue());

        }else if (ast.getValue() instanceof BigDecimal){
            //how to check if out of range?
            return new Ast.Expression.Literal(Stdlib.Type.DECIMAL, ((BigDecimal) ast.getValue()).doubleValue());
        }else if (ast.getValue() instanceof String){
            if(((String) ast.getValue()).matches("[A-Za-z0-9_!?/+-/* ]")){
                return new Ast.Expression.Literal(Stdlib.Type.STRING, ast.getValue().toString());
            }
        }

        throw new UnsupportedOperationException();
    }

    @Override
    public Ast.Expression.Group visit(Ast.Expression.Group ast) throws AnalysisException {
        return new Ast.Expression.Group(visit(ast.getExpression()));
    }

    @Override
    public Ast.Expression.Binary visit(Ast.Expression.Binary ast) throws AnalysisException {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Ast.Expression.Variable visit(Ast.Expression.Variable ast) throws AnalysisException {
        throw new UnsupportedOperationException(); //TODO
    }

    @Override
    public Ast.Expression.Function visit(Ast.Expression.Function ast) throws AnalysisException {
        throw new UnsupportedOperationException(); //TODO
    }

    /**
     * Throws an AnalysisException if the first type is NOT assignable to the target type. * A type is assignable if and only if one of the following is true:
     *  - The types are equal, as according to Object#equals
     *  - The first type is an INTEGER and the target type is DECIMAL
     *  - The first type is not VOID and the target type is ANY
     */
    public static void checkAssignable(Stdlib.Type type, Stdlib.Type target) throws AnalysisException {
        throw new UnsupportedOperationException(); //TODO
    }

}
