package org.roxycode.jsmashy.generated;

import org.antlr.v4.runtime.Parser;
import org.antlr.v4.runtime.TokenStream;

public abstract class Python3ParserBase extends Parser {
    protected Python3ParserBase(TokenStream input) {
        super(input);
    }

    protected boolean CannotBePlusMinus() {
        return _input.LA(1) != Python3Parser.ADD && _input.LA(1) != Python3Parser.MINUS;
    }

    protected boolean CannotBeDotLpEq() {
        return _input.LA(1) != Python3Parser.DOT && _input.LA(1) != Python3Parser.OPEN_PAREN && _input.LA(1) != Python3Parser.ASSIGN;
    }
}