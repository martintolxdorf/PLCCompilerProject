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
            newline(0);
            newline(1);
            print("public static void main(String[] args) {");
            newline(2);

            for(int i=0;i<asts.size();i++){
                visit(asts.get(i));
            }

            newline(1);
            print("}");
            newline(0);
            print("}");
        }



        return null;
    }

    @Override
    public Void visit(Ast.Statement.Expression ast) {

        // TODO:  Generate Java to handle Expression node.

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

        // TODO:  Generate Java to handle Assignment node.

        return null;
    }

    @Override
    public Void visit(Ast.Statement.If ast) {

        // TODO:  Generate Java to handle If node.

        return null;
    }

    @Override
    public Void visit(Ast.Statement.While ast) {

        // TODO:  Generate Java to handle While node.

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Literal ast) {

        // TODO:  Generate Java to handle Literal node.

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Group ast) {

        // TODO:  Generate Java to handle Group node.

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Binary ast) {

        // TODO:  Generate Java to handle Binary node.

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Variable ast) {

        // TODO:  Generate Java to handle Variable node.

        return null;
    }

    @Override
    public Void visit(Ast.Expression.Function ast) {

        List<Ast.Expression> asts = ast.getArguments();
        String out = "";
        for(int i=0;i<asts.size();i++){
            Ast temp = asts.get(0);
            Class h = temp.getClass();
            if(temp instanceof Ast.Expression.Literal){
                out+=((Ast.Expression.Literal) temp).getValue();
            }
        }
        print("print(\"",out,"\")");

        return null;
    }

}
