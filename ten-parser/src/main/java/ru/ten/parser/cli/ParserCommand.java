package ru.ten.parser.cli;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import ru.ten.parser.service.TenderService;

import java.util.List;
import java.util.concurrent.Callable;

@Component
@Command(
        name = "ten-parser",
        mixinStandardHelpOptions = true,
        version = "ten-parser 1.0.0",
        description = "Парсер тендерных площадок"
)
public class ParserCommand implements Callable<Integer> {

    private static final Logger log = LoggerFactory.getLogger(ParserCommand.class);

    private final TenderService tenderService;

    @Option(
            names = "--keyword",
            arity = "1..*",
            required = true,
            paramLabel = "<keyword>",
            description = "Ключевые слова для поиска (можно несколько через пробел)"
    )
    private List<String> keywords;

    @Option(
            names = "--status",
            split = ",",
            defaultValue = "В процессе проведения",
            paramLabel = "<status>",
            description = "Статусы закупок (через запятую)"
    )
    private List<String> statuses;

    @Option(
            names = "--pages",
            defaultValue = "2",
            paramLabel = "<pages>",
            description = "Сколько страниц выгрузить (по умолчанию 2)"
    )
    private int pages;

    @Option(
            names = "--platform",
            defaultValue = "sberbank",
            paramLabel = "<platform>",
            description = "Площадка: sberbank, rts"
    )
    private String platform;

    public ParserCommand(TenderService tenderService) {
        this.tenderService = tenderService;
    }

    @Override
    public Integer call() {
        if (pages < 1) {
            log.error("Параметр --pages должен быть >= 1, получено: {}", pages);
            return 2;
        }

        log.info("Платформа: {}", platform);
        log.info("Ключевые слова: {}", keywords);
        log.info("Статусы: {}", statuses);
        log.info("Страниц: {}", pages);

        try {
            tenderService.searchAndPrint(platform, keywords, statuses, pages);
            return 0;
        } catch (Exception e) {
            log.error("Ошибка при выполнении поиска", e);
            return 1;
        }
    }
}
