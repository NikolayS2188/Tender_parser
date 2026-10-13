package ru.ten.parser.cli;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import picocli.CommandLine;
import picocli.CommandLine.IFactory;

/**
 * Запускает picocli-команду после старта Spring-контекста.
 * Без этого класса ParserCommand не выполнится.
 */
@Component
public class ParserRunner implements CommandLineRunner {

    private final ParserCommand parserCommand;
    private final IFactory factory;

    @Autowired
    public ParserRunner(ParserCommand parserCommand, IFactory factory) {
        this.parserCommand = parserCommand;
        this.factory = factory;
    }

    @Override
    public void run(String... args) {
        int exitCode = new CommandLine(parserCommand, factory).execute(args);
        System.exit(exitCode);
    }
}
