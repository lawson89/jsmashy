package org.roxycode.jsmashy.visitor;

import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.roxycode.jsmashy.generated.Java20BaseListener;
import org.roxycode.jsmashy.generated.Java20Lexer;
import org.roxycode.jsmashy.generated.Java20Parser;

public class JavaSkeletonListener extends Java20BaseListener {

    private final TokenStreamRewriter rewriter;

    public JavaSkeletonListener(TokenStreamRewriter rewriter) {
        this.rewriter = rewriter;
        stripComments();
    }

    private void stripComments() {
        org.antlr.v4.runtime.BufferedTokenStream tokens = (org.antlr.v4.runtime.BufferedTokenStream) rewriter.getTokenStream();
        tokens.fill();
        for (int i = 0; i < tokens.size(); i++) {
            Token t = tokens.get(i);
            if (t.getType() == Java20Lexer.COMMENT || t.getType() == Java20Lexer.LINE_COMMENT) {
                rewriter.delete(i);
            }
        }
    }

    @Override
    public void enterMethodDeclaration(Java20Parser.MethodDeclarationContext ctx) {
        if (ctx.methodBody() != null) {
            rewriter.replace(ctx.methodBody().start, ctx.methodBody().stop, ";");
        }
    }

    @Override
    public void enterConstructorDeclaration(Java20Parser.ConstructorDeclarationContext ctx) {
        if (ctx.constructorBody() != null) {
            rewriter.replace(ctx.constructorBody().start, ctx.constructorBody().stop, ";");
        }
    }
}
