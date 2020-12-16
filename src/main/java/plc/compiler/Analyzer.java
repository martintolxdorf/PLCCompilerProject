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
            throw new AnalysisException("already defined");
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
            return new Ast.Statement.Assignment(ast.getName(), visit(ast.getExpression()));
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
        }else if(ast.getThenStatements().isEmpty()){
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
            List<Ast.Statement> getS = new ArrayList<>();
            for(int i=0;i<ast.getStatements().size();i++) {
                getS.add(i,visit(ast.getStatements().get(i)));
            }
            return new Ast.Statement.While(visit(ast.getCondition()), getS);
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
            double temp = ((BigDecimal) ast.getValue()).doubleValue();
            if(temp == Double.NEGATIVE_INFINITY || temp == Double.POSITIVE_INFINITY){
                throw new AnalysisException("double out of range");
            }
            return new Ast.Expression.Literal(Stdlib.Type.DECIMAL, temp);
        }else if (ast.getValue() instanceof String){
            if((ast.getValue().toString().matches("[A-Za-z0-9_!?/+-/* ]*"))){
                return new Ast.Expression.Literal(Stdlib.Type.STRING, ast.getValue().toString());
            }
        }

        throw new AnalysisException("howdy");
    }

    @Override
    public Ast.Expression.Group visit(Ast.Expression.Group ast) throws AnalysisException {
        return new Ast.Expression.Group(visit(ast.getExpression()));
    }

    @Override
    public Ast.Expression.Binary visit(Ast.Expression.Binary ast) throws AnalysisException {

        if(ast.getOperator().equals("==") || ast.getOperator().equals("!=")){

            if(visit(ast.getLeft()).getType() != Stdlib.Type.VOID && visit(ast.getLeft()).getType() != Stdlib.Type.VOID){
                return new Ast.Expression.Binary(Stdlib.Type.BOOLEAN,ast.getOperator(), visit(ast.getLeft()), visit(ast.getRight()));
            }else{
                throw new AnalysisException("void included");
            }

        }else if(ast.getOperator().equals("+")){

            if(visit(ast.getLeft()).getType() != Stdlib.Type.VOID && visit(ast.getRight()).getType() != Stdlib.Type.VOID){

                if(visit(ast.getLeft()).getType() == Stdlib.Type.STRING || visit(ast.getRight()).getType() == Stdlib.Type.STRING ){
                    return new Ast.Expression.Binary(Stdlib.Type.STRING, ast.getOperator(), visit(ast.getLeft()), visit(ast.getRight()));
                }else if(visit(ast.getLeft()).getType() == Stdlib.Type.INTEGER && visit(ast.getRight()).getType() == Stdlib.Type.INTEGER){
                    return new Ast.Expression.Binary(Stdlib.Type.INTEGER, ast.getOperator(), visit(ast.getLeft()), visit(ast.getRight()));
                }else{
                    return new Ast.Expression.Binary(Stdlib.Type.DECIMAL, ast.getOperator(), visit(ast.getLeft()), visit(ast.getRight()));
                }

            }

            throw new AnalysisException("void included");

        }else if(ast.getOperator().equals("-") || ast.getOperator().equals("*") || ast.getOperator().equals("/")){

            if(visit(ast.getLeft()).getType() == Stdlib.Type.INTEGER && visit(ast.getRight()).getType() == Stdlib.Type.INTEGER ){
                return new Ast.Expression.Binary(Stdlib.Type.INTEGER, ast.getOperator(), visit(ast.getLeft()), visit(ast.getRight()));
            }else if(visit(ast.getLeft()).getType() == Stdlib.Type.DECIMAL && visit(ast.getRight()).getType() == Stdlib.Type.INTEGER){
                return new Ast.Expression.Binary(Stdlib.Type.DECIMAL,ast.getOperator(), visit(ast.getLeft()), visit(ast.getRight()));
            }else if(visit(ast.getLeft()).getType() == Stdlib.Type.INTEGER && visit(ast.getRight()).getType() == Stdlib.Type.DECIMAL){
                return new Ast.Expression.Binary(Stdlib.Type.DECIMAL, ast.getOperator(), visit(ast.getLeft()), visit(ast.getRight()));
            }else{
                throw new AnalysisException("not int or decimal");
            }

        }

        throw new AnalysisException("uh oh wheres the operator chief");
    }

    @Override
    public Ast.Expression.Variable visit(Ast.Expression.Variable ast) throws AnalysisException {

        if(scope.lookup(ast.getName()) == null){
            throw new AnalysisException("var not def");
        }

        return new Ast.Expression.Variable(scope.lookup(ast.getName()), ast.getName());
    }

    @Override
    public Ast.Expression.Function visit(Ast.Expression.Function ast) throws AnalysisException {

        List<Ast.Expression> args = new ArrayList<>();

        for (int i = 0; i < ast.getArguments().size(); i++) {
            checkAssignable(visit(ast.getArguments().get(i)).getType(), Stdlib.getFunction(ast.getName(), ast.getArguments().size()).getParameterTypes().get(i));
            args.add(visit(ast.getArguments().get(i)));
        }

        return new Ast.Expression.Function(Stdlib.Type.VOID, Stdlib.getFunction(ast.getName(), ast.getArguments().size()).getJvmName(), args);

    }

    /**
     * Throws an AnalysisException if the first type is NOT assignable to the target type. * A type is assignable if and only if one of the following is true:
     *  - The types are equal, as according to Object#equals
     *  - The first type is an INTEGER and the target type is DECIMAL
     *  - The first type is not VOID and the target type is ANY
     */
    public static void checkAssignable(Stdlib.Type type, Stdlib.Type target) throws AnalysisException {

        if (!type.equals(target) && !(type.equals(Stdlib.Type.INTEGER) && target.equals(Stdlib.Type.DECIMAL)) && !(!type.equals(Stdlib.Type.VOID) && target.equals(Stdlib.Type.ANY))){
            throw new AnalysisException("not assignable");
        }

    }

}
