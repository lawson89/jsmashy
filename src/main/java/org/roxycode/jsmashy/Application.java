package org.roxycode.jsmashy;

import io.micronaut.configuration.picocli.PicocliRunner;

public class Application {
    public static void main(String[] args) {
        PicocliRunner.run(JSmashyCommand.class, args);
    }
}
