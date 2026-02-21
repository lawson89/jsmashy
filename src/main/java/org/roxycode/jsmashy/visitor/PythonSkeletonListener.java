package org.roxycode.jsmashy.visitor;

import org.antlr.v4.runtime.BufferedTokenStream;
import org.antlr.v4.runtime.Token;
import org.antlr.v4.runtime.TokenStreamRewriter;
import org.roxycode.jsmashy.generated.Python3ParserBaseListener;
import org.roxycode.jsmashy.generated.Python3Lexer;
import org.roxycode.jsmashy.generated.Python3Parser;

public class PythonSkeletonListener extends Python3ParserBaseListener {

    private final TokenStreamRewriter rewriter;

    public PythonSkeletonListener(TokenStreamRewriter rewriter) {
        this.rewriter = rewriter;
    }

    @Override
    public void enterFuncdef(Python3Parser.FuncdefContext ctx) {
        if (ctx.block() != null) {
             // Just replace the block.
             // To avoid overlapping, we should ensure we only replace the outermost block we care about.
             // But for functions, we want to replace all of them.
             rewriter.replace(ctx.block().start, ctx.block().stop, " ...");
        }
    }
}