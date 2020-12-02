package plc.compiler;

import java.io.PrintWriter;
import java.util.List;

public final class Generator implements Ast.Visitor<Void> {

    private final PrintWriter writer;
    private int indent = 0;

    public Generator(PrintWriter writer) {
        this.writer = writer;
    }

    private void print(Object... objects) {
        for (Object object : objects) {
            if (object instanceof Ast) {
                visit((Ast) object);
            } else {
                writer.write(object.toString());
            }
        }
    }

    private void newline(int indent) {
        writer.println();
        for (int i = 0; i < indent; i++) {
            writer.write("    ");
        }
    }

    @Override
    public Void visit(Ast.Source ast) {

        List<Ast.Statement> asts = ast.getStatements();

        if(!asts.isEmpty()){
            print("public final class Main {");
            newline(indent);
            indent++;
            newline(indent);
            print("public static void main(String[] args) {");
            indent++;
            newline(indent);

            for(int i=0;i<asts.size();i++){
                visit(asts.get(i));
                if(i!=asts.size()-1){
                    newline(indent);
                }
            }
            indent--;
            newline(indent);
            print("}");
            indent--;
            newline(indent);
            newline(indent);
            print("}");
            newline(indent);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {
        visit(ast.getExpression());
        print(";");
        return null;
    }

    @Override
    public Void visit(Ast.Statement.Declaration ast) {

        print(ast.getType(), " ", ast.getName());
        if (ast.getValue().isPresent()) {
            print(" = ", ast.getValue().get());
        }
        print(";");

        return null;

    }

    @Override
    public Void visit(Ast.Statement.Assignment ast) {

        print(ast.getName(), " = ");
        visit(ast.getExpression());
        print(";");

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {

        List<Ast.Statement> thenStatements = ast.getThenStatements();
        List<Ast.Statement> elseStatements = ast.getElseStatements();

        print("if (",ast.getCondition(),") {");
        if(!thenStatements.isEmpty()) {
            indent++;
            newline(indent);
            print(thenStatements.get(0));
            indent--;
            newline(indent);
        }

        print("}");
        if(!elseStatements.isEmpty()){
            print(" else {");
            indent++;
            newline(indent);
            print(elseStatements.get(0));
            indent--;
            newline(indent);
            print("}");
        }

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {

        print("while (", ast.getCondition(), ") {");
        indent++;
        newline(indent);

        List<Ast.Statement> statements = ast.getStatements();
        if(!statements.isEmpty()){
            for(int i=0;i<statements.size();i++){
                visit(statements.get(i));
                if(i!=statements.size()-1){
                    newline(indent);
                }
            }
        }
        indent--;
        newline(indent);
        print("}");


        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {

        Object val = ast.getValue();
        if(ast.getValue() instanceof String){
            print("\"",val,"\"");
        }else{
            print(val);
        }

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {

        print("(");
        visit(ast.getExpression());
        print(")");

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {

        visit(ast.getLeft());
        print(" ",ast.getOperator()," ");
        visit(ast.getRight());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Variable ast) {

        print(ast.getName());

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {
        print(ast.getName());
        if (!ast.getArguments().isEmpty()) {
            print("(");
            if(!ast.getArguments().isEmpty()) {
                for (int i = 0; i < ast.getArguments().size(); i++) {
                    print(ast.getArguments().get(i));
                    if (i != ast.getArguments().size() - 1) {
                        print(", ");
                    }
                }
                print(")");
            }
        }
        return null;
    }

}
